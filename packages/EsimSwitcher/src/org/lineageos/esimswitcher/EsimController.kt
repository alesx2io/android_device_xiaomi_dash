/*
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.esimswitcher

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemProperties
import android.telephony.SubscriptionManager
import android.util.Log

class EsimController private constructor(private val context: Context) {

    enum class EsimState { Enabled, Disabled, Unavailable }

    companion object {
        private const val TAG = "EsimController"
        private const val STATE_TIMEOUT_MS = 5000L
        private const val PREFS_NAME = "esimswitcher"
        private const val PREF_LAST_STATE = "last_state"

        @Volatile private var instance: EsimController? = null
        fun getInstance(context: Context): EsimController =
            instance ?: synchronized(this) {
                instance ?: EsimController(context.applicationContext).also { instance = it }
            }
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun onBootCompleted() {
        // Reconcile the cached state with the hardware. The real state is
        // derived from the EID by init.esim.sh, so a query is authoritative.
        val state = queryState()
        prefs.edit().putString(PREF_LAST_STATE, state.name).apply()
        Log.i(TAG, "onBootCompleted: eSIM state $state")
    }

    /** Last state seen at boot or in the UI, or null if never queried. */
    fun getCachedState(): EsimState? =
        prefs.getString(PREF_LAST_STATE, null)?.let { runCatching { EsimState.valueOf(it) }.getOrNull() }

    fun getEsimActive(): Boolean {
        val subscriptionManager =
            context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
        return subscriptionManager?.activeSubscriptionInfoList?.any { it.isEmbedded } ?: false
    }

    /** Queries the hardware state, waiting at most STATE_TIMEOUT_MS. */
    fun queryState(): EsimState {
        val started = runCatching { SystemProperties.set("ctl.start", "vendor.esim_state") }
            .onFailure { Log.w(TAG, "ctl.start vendor.esim_state failed", it) }
            .isSuccess
        if (!started) return EsimState.Unavailable

        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < STATE_TIMEOUT_MS) {
            val state = runCatching { SystemProperties.get("sys.esim.state", "") }.getOrDefault("")
            when (state) {
                "enabled" -> return cache(EsimState.Enabled)
                "disabled" -> return cache(EsimState.Disabled)
            }
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            }
        }
        Log.e(TAG, "queryState: timeout waiting for sys.esim.state")
        return EsimState.Unavailable
    }

    /** Backwards-compatible boolean view used by older callers. */
    fun getEsimEnabled(): Boolean = queryState() == EsimState.Enabled

    /**
     * Requests a state change and verifies it actually took effect.
     * Returns true only when the hardware reports the requested state.
     */
    fun setEsimEnabled(isEnabled: Boolean): Boolean {
        Log.i(TAG, "setEsimEnabled: $isEnabled")
        val command = if (isEnabled) "vendor.esim_enable" else "vendor.esim_disable"
        val started = runCatching { SystemProperties.set("ctl.start", command) }
            .onFailure { Log.w(TAG, "ctl.start $command failed", it) }
            .isSuccess
        if (!started) return false

        // Wait for the service to finish before sampling the state again.
        // A mismatched state can be transitional, so keep polling until the
        // expected state is seen or we time out.
        val expected = if (isEnabled) EsimState.Enabled else EsimState.Disabled
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < STATE_TIMEOUT_MS) {
            val state = queryState()
            if (state == expected) {
                Log.i(TAG, "setEsimEnabled: verified $expected")
                return true
            }
            Log.w(TAG, "setEsimEnabled: state is $state, expected $expected")
            try {
                Thread.sleep(200)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            }
        }
        Log.e(TAG, "setEsimEnabled: verification timed out")
        return false
    }

    private fun cache(state: EsimState): EsimState {
        prefs.edit().putString(PREF_LAST_STATE, state.name).apply()
        return state
    }

    fun dispose() {
        Log.d(TAG, "dispose")
    }
}
