"""Build the Android 16 variant without modifying the Android 17 project or APK."""
import build_android as builder

builder.BUILD=builder.ROOT/'build/android16'
builder.TOOLS=builder.SDK/'build-tools/36.0.0'
builder.PLATFORM=builder.SDK/'platforms/android-36/android.jar'
if not builder.PLATFORM.is_file() or not (builder.TOOLS/'aapt2.exe').is_file():
    raise SystemExit('Run python tools/bootstrap_android16.py first.')
builder.build(min_api=36)
