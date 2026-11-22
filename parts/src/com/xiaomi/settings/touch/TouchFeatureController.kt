/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.touch

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.ServiceManager
import android.util.Log
import android.view.Display
import vendor.xiaomi.hw.touchfeature.ITouchFeature

/**
 * Feeds the touch host processing the state HyperOS gives it from the framework.
 *
 * - Edge filter (mode 7): the service starts with it off; set it to the panel default, which is
 *   what a mode reset on stock leaves behind. Without it, fingers resting on the edges register
 *   as taps.
 * - Panel orientation (mode 8): the display rotation, as stock sends it on every rotation, so
 *   the edge filter rejects the right edges in landscape.
 *
 * Both are applied again if the touch service restarts.
 */
object TouchFeatureController {

    private const val TAG = "TouchFeatureController"
    private val DEBUG = Log.isLoggable(TAG, Log.DEBUG)

    private const val TOUCH_ID = 0
    private const val MODE_EDGE_FILTER = 7
    private const val MODE_PANEL_ORIENTATION = 8

    private lateinit var displayManager: DisplayManager
    private lateinit var handler: Handler
    @Volatile private var touchFeature: ITouchFeature? = null
    private var started = false

    private val deathRecipient =
        IBinder.DeathRecipient {
            Log.w(TAG, "touchfeature service died, applying the modes again")
            touchFeature = null
            handler.post { applyAll() }
        }

    private val displayListener =
        object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) {}

            override fun onDisplayRemoved(displayId: Int) {}

            override fun onDisplayChanged(displayId: Int) {
                if (displayId == Display.DEFAULT_DISPLAY) applyOrientation()
            }
        }

    @Synchronized
    fun start(context: Context) {
        if (started) return
        started = true
        displayManager = context.getSystemService(DisplayManager::class.java)
        handler = Handler(HandlerThread(TAG).apply { start() }.looper)
        displayManager.registerDisplayListener(displayListener, handler)
        handler.post { applyAll() }
    }

    private fun getTouchFeature(): ITouchFeature? =
        touchFeature
            ?: runCatching {
                    val binder =
                        ServiceManager.waitForDeclaredService("${ITouchFeature.DESCRIPTOR}/default")
                    ITouchFeature.Stub.asInterface(binder).apply {
                        asBinder().linkToDeath(deathRecipient, 0)
                    }
                }
                .onSuccess { touchFeature = it }
                .onFailure { e -> Log.e(TAG, "Failed to get the touchfeature service", e) }
                .getOrNull()

    private fun applyAll() {
        val service = getTouchFeature() ?: return
        runCatching {
                val edgeFilter = service.getModeDefaultValue(TOUCH_ID, MODE_EDGE_FILTER)
                if (DEBUG) Log.d(TAG, "edge filter -> $edgeFilter")
                service.setModeValue(TOUCH_ID, MODE_EDGE_FILTER, edgeFilter)
            }
            .onFailure { e -> Log.e(TAG, "Failed to set the edge filter", e) }
        lastRotation = -1
        applyOrientation()
    }

    private var lastRotation = -1

    private fun applyOrientation() {
        val rotation = displayManager.getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: return
        if (rotation == lastRotation) return
        val service = getTouchFeature() ?: return
        runCatching {
                if (DEBUG) Log.d(TAG, "panel orientation -> $rotation")
                service.setModeValue(TOUCH_ID, MODE_PANEL_ORIENTATION, rotation)
                lastRotation = rotation
            }
            .onFailure { e -> Log.e(TAG, "Failed to set the panel orientation", e) }
    }
}
