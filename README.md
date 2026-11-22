# Device tree for POCO X8 Pro Max (dash), Project Infinity-X

Fork of [oss-dash/android_device_xiaomi_dash](https://github.com/oss-dash/android_device_xiaomi_dash)
used for the unofficial **Project Infinity-X 4.2 (Android 17)** builds.

| Branch | Content |
|---|---|
| `lineage-24.0` | Identical to oss-dash, never changed here |
| `infinity` | oss-dash with the stock (prebuilt) kernel, without the Kleaf and DSAC commits, plus the fixes below |
| `infinity-kleaf` | oss-dash with the kernel built from source (Kleaf, experimental), plus the fixes below |

## Fixes in this fork

This fork has the following fixes on top of oss-dash. **Should be fixed** means the cause is
fixed but the problem happened at random, so it is confirmed only by it not showing up anymore.

| Fix | Problem | Status |
|---|---|---|
| Accidental touches (Parts, sepolicy) | Random taps, for example MIUI Camera switching to Portrait or opening the beauty menu on its own: the edge filter of the Xiaomi touch service was off. Parts now sets it to the panel default and sends the display rotation to the touch service, as HyperOS does. | Should be fixed |
| AOSP auto brightness timing | Auto brightness kept moving (screen mirroring, short shadows on the under-display sensor) because of the HyperOS debounce and ambient light horizons. AOSP values are used now. | Fixed |
| AOSP brightness behaviour | HyperOS brightness thresholds, adjustment gamma, slider gamma conversion off and the 60 Hz lock screen limit removed: AOSP defaults apply. | Fixed |
| AOSP lmkd defaults | HyperOS lmkd properties removed. | Fixed |
| MIUI Camera from the lock screen | The lock screen camera shortcut opens MIUI Camera directly instead of a chooser (needs the matching `frameworks/base` patch). | Fixed |
| Infinity-X product | `infinity_dash` product for the Infinity-X build. | — |

These fixes from this fork are now part of oss-dash itself: 30 media volume steps, call volume
(audio HAL vendor extension), screen dim level, auto brightness threshold and ramps.

The fixes for Infinity-X (`frameworks/base`, `build/soong`) and the full description of every fix
are in [dash-Infinity-X-patches](https://github.com/alesx2io/dash-Infinity-X-patches).

> These fixes were developed with the help of AI (Claude), and every fix was built, flashed and
> tested on a real POCO X8 Pro Max before being published.
