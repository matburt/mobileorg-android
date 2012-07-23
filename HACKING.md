Setting up the build environment
================================

Prerequisites
-------------
* Java SDK >= 1.6
* ant >= 1.8
* APIs in  Android SDK Manager, currently
  * 13 (Android 3.2) for ActionBarSherlock
  * 15 (Android 4.0.3) for the rest
    * defined in project.properties
* source code cloned from github
* (optional Eclipse with Android plugins)

To get a working environment `cd` to the mobileorg-android directory and
perform the following steps:

Setup
-----
* run `./setup.sh`

Build
-----
* run `ant debug`

If you don't get an error, you managed to compile the project.  The .apk files
are in `bin/`.  I haven't tried to deploy them using `adb` since I used
Eclipse (see below).

Eclipse Setup
-------------
I imported the following projects using
"File > Import > Existing Android Code Into Workspace"
and selecting the project root directory:
* com.matburt.mobileorg.Gui.OutlineActivity
* com.twofortyfouram.locale.MarketActivity
* library

Note: To also import `test` you have to include other projects, as well.
But I haven't tried that out, yet.

Deployment
----------
* Uninstall the current mobileorg-android from your device.
  * Probably renaming the application package also works.
* Run `com.matburt.mobileorg.Gui.OutlineActivity` as "Android application".

Happy hacking!
