/*
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.esimswitcher

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import com.android.settingslib.widget.FooterPreference
import com.android.settingslib.widget.MainSwitchPreference
import com.android.settingslib.widget.SettingsBasePreferenceFragment

class EsimSettingsFragment :
    SettingsBasePreferenceFragment(), Preference.OnPreferenceChangeListener {

    companion object {
        private const val TAG = "EsimSettingsFragment"
    }

    private val esimController by lazy { EsimController.getInstance(requireContext()) }
    private val mainHandler = Handler(Looper.getMainLooper())

    private val switchBar by lazy { findPreference<MainSwitchPreference>("esim_enable")!! }
    private val footerPref by lazy { findPreference<FooterPreference>("esim_footer")!! }
    private val retryPref by lazy { findPreference<Preference>("esim_retry")!! }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        Log.d(TAG, "onCreatePreferences")
        setPreferencesFromResource(R.xml.settings_esim, rootKey)

        switchBar.onPreferenceChangeListener = this
        switchBar.isEnabled = false
        footerPref.title = getString(R.string.esim_footer_note)
        retryPref.isVisible = false
        retryPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            loadState()
            true
        }

        // Show the last known state immediately, then refresh from hardware.
        when (esimController.getCachedState()) {
            EsimController.EsimState.Enabled -> switchBar.isChecked = true
            EsimController.EsimState.Disabled -> switchBar.isChecked = false
            else -> { /* leave unchecked until queried */ }
        }
        loadState()
    }

    private fun loadState() {
        switchBar.isEnabled = false
        retryPref.isVisible = false
        footerPref.title = getString(R.string.esim_footer_note)

        Thread {
            val state = runCatching { esimController.queryState() }
                .getOrDefault(EsimController.EsimState.Unavailable)
            mainHandler.post {
                if (!isAdded) return@post
                when (state) {
                    EsimController.EsimState.Enabled -> switchBar.isChecked = true
                    EsimController.EsimState.Disabled -> switchBar.isChecked = false
                    EsimController.EsimState.Unavailable -> {
                        footerPref.title = getString(R.string.esim_state_error)
                        retryPref.isVisible = true
                    }
                }
                switchBar.isEnabled = state != EsimController.EsimState.Unavailable
            }
        }.start()
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        if (preference != switchBar) return true

        val isChecked = newValue as Boolean
        Log.d(TAG, "onPreferenceChange: $isChecked")
        if (esimController.getEsimActive()) {
            if (isChecked) return false
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.esim_warning_title)
                .setMessage(R.string.esim_warning_message)
                .setPositiveButton(android.R.string.ok, null)
                .setCancelable(false)
                .show()
            return false
        }

        // Disable while the toggle is in flight; verify before accepting.
        switchBar.isEnabled = false
        Thread {
            val success = runCatching { esimController.setEsimEnabled(isChecked) }
                .getOrDefault(false)
            mainHandler.post {
                if (!isAdded) return@post
                if (!success) {
                    switchBar.isChecked = !isChecked
                    Toast.makeText(
                        requireContext(),
                        R.string.esim_toggle_error,
                        Toast.LENGTH_LONG,
                    ).show()
                }
                switchBar.isEnabled = true
            }
        }.start()
        return true
    }
}
