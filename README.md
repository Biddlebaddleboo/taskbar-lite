![Taskbar Lite](http://i.imgur.com/gttRian.png)

Taskbar Lite puts a start menu on top of your screen that's accessible at any time, increasing your productivity and turning your Android tablet (or phone) into a real multitasking machine!

On devices running Android 7.0+, Taskbar Lite can launch apps in freeform windows for a more desktop-like multitasking experience.  No root required!  (see below for instructions)

Taskbar Lite is a streamlined fork focused on two core ideas:

* Replace the home screen with a compact taskbar and start menu
* Launch apps in freeform windows for multitasking on supported devices

## Features
* Start menu - shows you all applications installed on the device, configurable as a list or as a grid
* Collapsible and hideable - show it when you need it, hide it when you don't
* Replace home screen - use Taskbar Lite as your default home app
* Freeform window mode - launch apps in floating windows on supported Android versions
* Basic customization - adjust the look and behavior without a lot of extra clutter
* Designed with keyboard and mouse in mind
* 100% free, open source, and no ads

#### Freeform window mode (Android 7.0+, no external display required)
Taskbar Lite lets you launch apps in freeform floating windows on Android 7.0+ devices.  No root access is required, although Android 8.0, 8.1, and 9 devices require an adb shell command to be run during initial setup.

Simply follow these steps to configure your device for launching apps in freeform mode:

1. Check the box for "Freeform window support" inside the Taskbar Lite app
2. Follow the directions that appear in the pop-up to enable the proper settings on your device (one-time setup)
3. Go to your device's recent apps page and clear all recent apps
4. Start Taskbar Lite, then select an app to launch it in a freeform window

For more information and detailed instructions, click "Help & instructions for freeform mode" inside the Taskbar Lite app.

## Changelog
To see some of the major new features in the latest Taskbar Lite release, visit the [changelog](https://github.com/farmerbb/Taskbar/blob/master/CHANGELOG.md).

## Download
Taskbar Lite can be downloaded as a standalone Android app from:

[<img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png"
      alt="Google Play"
      height="80"
      align="middle">](https://play.google.com/store/apps/details?id=com.jcdigitalsolutions.taskbarlite)
[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
      alt="F-Droid"
      height="80"
      align="middle">](https://f-droid.org/packages/com.jcdigitalsolutions.taskbarlite/)

Taskbar Lite is intended for Android devices where a lightweight launcher plus freeform multitasking is useful.

## How to Build
Prerequisites:
* Windows / MacOS / Linux
* JDK 8
* Android SDK
* Internet connection (to download dependencies)

Once all the prerequisites are met, make sure that the `ANDROID_HOME` environment variable is set to your Android SDK directory, then run `./gradlew assembleFreeDebug` at the base directory of the project to start the build. After the build completes, navigate to `app/build/outputs/apk/free/debug` where you will end up with an APK file ready to install on your Android device.

### Running tests

Taskbar Lite uses [Robolectric](https://github.com/robolectric/robolectric) as its unit testing framework.  The entire test suite can be run with `./gradlew testFreeDebug`, or you can generate a Jacoco coverage report using `./gradlew jacocoTestFreeDebugUnitTestReport` which will be output to the `app/build/jacoco/jacocoHtml` directory.  If you contribute code improvements such as bug fixes, we recommend writing tests alongside it using Robolectric.

## Contributors

Pull requests are welcome!  See the [contributor guidelines](https://github.com/farmerbb/Taskbar/blob/master/CONTRIBUTING.md) for more details.

* Mark Morilla (app logo)
* naofum (Japanese translation)
* HardSer (Russian translation)
* OfficialMITX (German translation)
* Whale Majida (Chinese translation)
* Mesut Han (Turkish translation)
* Zbigniew Zienko (Polish translation)
* utzcoz (Additional Chinese translation, code cleanup + unit testing)
* RaspberryPiFan (Additional German translation)
* Diego Sangunietti (Spanish translation)
* Tommy He (Chinese translation for Desktop Mode)
* Aaron Dewes (German translation updates)
* Ingo Brückl (German translation updates)

#### Special Thanks
* Mishaal Rahman (xda-developers)
* Jon West (Team Bliss)
* Chih-Wei Huang (Android-x86)
