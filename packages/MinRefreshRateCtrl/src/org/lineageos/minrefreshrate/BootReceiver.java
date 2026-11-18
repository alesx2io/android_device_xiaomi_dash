package org.lineageos.minrefreshrate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "MinRefreshRateCtrl";
    static final float MIN_REFRESH_RATE_HZ = 60.0f;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }
        applyFloor(context);
    }

    static void applyFloor(Context context) {
        try {
            float current = Settings.System.getFloat(
                    context.getContentResolver(), "min_refresh_rate", 0f);
            if (current >= MIN_REFRESH_RATE_HZ) {
                // Keep whatever the user selected in Settings (e.g. 90 or 120Hz).
                // Only values below the floor (including unset/0) need fixing.
                Log.i(TAG, "min_refresh_rate already " + current
                        + "Hz, keeping persisted value");
                return;
            }
            boolean applied = Settings.System.putFloat(
                    context.getContentResolver(),
                    "min_refresh_rate",
                    MIN_REFRESH_RATE_HZ);
            Log.i(TAG, applied
                    ? "min_refresh_rate enforced at " + MIN_REFRESH_RATE_HZ + "Hz"
                    : "min_refresh_rate write returned false");
        } catch (Exception e) {
            Log.e(TAG, "Failed to set min_refresh_rate", e);
        }
    }
}
