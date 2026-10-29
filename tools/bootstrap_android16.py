"""Install the official Android 16 (API 36) compile platform and build tools."""
from bootstrap_android import BASE,sdk_package

sdk_package('platforms;android-36',BASE/'sdk/platforms/android-36')
sdk_package('build-tools;36.0.0',BASE/'sdk/build-tools/36.0.0')
print('Android 16 build platform ready.',flush=True)
