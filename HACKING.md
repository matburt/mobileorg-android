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
are in `bin/`.   You can install the apk either directly on the device or via
'adb install'

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

Notes regarding pull requests
-----------------------------
Make sure that any changes or additions that you make will also build from the
command line.  This is a requirement before changes will be accepted into the
mainline branch.

Notes regarding dropbox.xml
---------------------------
The private keys used to access Dropbox accounts for users are not included.
That's not entirely true.  The file exists but contains dummy values so if you want
dropbox to work on your local builds then you will need to request your own keys from
Dropbox.

You'll also need to provide your app key in the AndroidManifest.xml file under: com.dropbox.client2.android.AuthActivity

Notes regarding debian(-like) amd64 build hosts
-----------------------------------------------

You might need to install 32bits version of some libraries.
Depending of your system version, you could install the 'ia32-libs'
package, on a 'multiarch' you will need: 'lib32z1', 'libc6-i386' and
'lib32stdc++6'.
