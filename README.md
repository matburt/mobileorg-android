An implementation of MobileOrg for the Android platform

for installation instructions see the wiki page:

[http://wiki.github.com/matburt/mobileorg-android/](http://wiki.github.com/matburt/mobileorg-android)

We have a Google+ Page:

[MobileOrg on Google+](https://plus.google.com/u/0/101083268903948579162)

And a dedicated Google Group for discussions and announcements:

[http://groups.google.com/group/mobileorg-android](http://groups.google.com/group/mobileorg-android)

If you want to hack on the code:

[HACKING.md](HACKING.md)

# News

## SSH

Instead of password authentication, you can also use public key authentication
with an SSH key protected by a passphrase.  Just enter the passphrase in the
password field of the SSH setup wizard.

SSH access now makes use of a known_hosts file with `StrictHostKeyChecking`
set to `ask` to avoid man-in-the-middle attacks (this is the default setting
for OpenSSH installations).  The path of that file is hard-coded as
`known_hosts` as app-local file (under directory
`/data/data/com.matburt.mobileorg/files`).  Moreover, `HashKnownHosts` is used
to hash entries in that file.

To create and add entries to the known_hosts file, use the “Check SSH Login”
button in the SSH setup wizard.  When you connect to some SSH server
for the first time or when the server’s fingerprint has changed, a pop-up
shows the fingerprint of the server’s key and asks for confirmation whether
that fingerprint is OK.  As usual, out-of-band verification is necessary.
Press “OK” if the fingerprint is the correct one and press “Check SSH Login”
for a second time.

Important! You must “Check SSH Login” a second time (when confirming the key
using “OK”, the key with its fingerprint is marked as acceptable for the next
connection attempt, during which it will be recorded in known_hosts), and you
should see a success message.  Otherwise, something went wrong.

On a rooted phone you may try to copy entries from your `~/.ssh/known_hosts`
to that file.  This did not work out-of-the-box for me since hostnames are not
enclosed in square brackets on my PC, while this seems to be required on the
phone.  (NullPointerExceptions arise.)  Thus, entries in that file should have
the following format before hashing:
`[host.example.org]:22222 ssh-rsa`

To generate a hashed file from one with plain hostnames, you may use:
`$ ssh-keygen -H -f known_hosts`


## Decryption of OpenPGP encrypted org files

This version of MobileOrg uses OpenKeyChain for decryption (instead of APG).
For every encrypted file, you may need to enter your passphrase again.  After
decryption, you may just see a blank screen.  Use the back button until
further passphrase pop-ups appear or MobileOrg exits.  Then start the app
again.  You should see the decrypted entries.


## Install asks for Locale

If you install the apk and choose "Open" to run the app, a pop-up may appear:
"Locale is required to use this plug-in. Would you like search for Locale on
the Android Market?"
You can safely cancel that request.  In fact, I don't have Android Market
installed, so the search fails anyways, and MobileOrg does not open.
MobileOrg is installed successfully, though.  Just open from your list of
installed apps.
