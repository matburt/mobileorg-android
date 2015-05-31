package com.matburt.mobileorg.util;

import android.util.Log;
import com.jcraft.jsch.UserInfo;

/** UserInfo for SSH connections with com.jcraft.jsch.Session.
 * Method promptYesNo(String), overridden here, is called to confirm
 * (a) some fingerprint (fpr) for a previously unknown host or
 * (b) a changed fpr for a known host.
 * According to Session.java, the user should confirm the new fpr in an
 * interactive dialogue.
 * As I don't know how to do that on Android, a three step process is
 * applied instead:
 *
 * Importantly, the argument to promptYesNo() contains the fpr of the
 * new key.
 * (1) Upon the first call to promptYesNo(), record the argument
 * (including fpr) and return false, which aborts the SSH connection.
 * (2) Asynchronously, an alert showing the argument (and fpr) is
 * displayed.  If the user confirms the new fpr, confirmNewKey() is
 * called, indicating that this fpr is acceptable for future
 * connections.
 * (3) If promptYesNo() is called again, for the same fpr, after
 * confirmNewKey() has been invoked, return true.
 */
public class MOUserInfo implements UserInfo {
    private final String LT = "MobileOrg";
    private boolean keyIsConfirmed;
    private String message;

    public MOUserInfo() {
	keyIsConfirmed = false;
	message = null;
    }

    public boolean hostKeyNeedsConfirmation() {
	Log.d(LT, "host fpr is confirmed: " + keyIsConfirmed);
	return !keyIsConfirmed;
    }

    public void confirmNewKey() {
	Log.d(LT, "host key confirmed");
	keyIsConfirmed = true;
    }

    public String getMessage() {
	return message;
    }

    @Override
    public boolean promptYesNo(String message) {
	Log.d(LT, "promptYesNo: " + message);
	if ((this.message == null) || (!this.message.equals(message))) {
	    Log.d(LT, "new message, fpr confirmation necessary");
	    keyIsConfirmed = false;
	    this.message = message;
	}
	Log.d(LT, "fpr is confirmed: " + keyIsConfirmed);
	return keyIsConfirmed;
    }

    @Override
    public String getPassphrase() {
	Log.d(LT, "getPassphrase");
	return null;
    }

    @Override
    public String getPassword() {
	Log.d(LT, "getPassword");
	return null;
    }

    @Override
    public boolean promptPassword(String message) {
	Log.d(LT, "promptPassword: " + message);
	return false;
    }

    @Override
    public boolean promptPassphrase(String message) {
	Log.d(LT, "promptPassphrase: " + message);
	return false;
    }

    @Override public void showMessage(String message) {
	Log.d(LT, "showMessage: " + message);
    }
}
