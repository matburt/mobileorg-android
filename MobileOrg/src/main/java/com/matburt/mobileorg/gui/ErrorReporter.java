package com.matburt.mobileorg.gui;

import android.app.AlertDialog;
import android.content.Context;

import com.matburt.mobileorg.R;

public class ErrorReporter
{
	private static final String LT = "MobileOrg";
	
	public static void displayError(Context context, String message) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(context); 
        dialog.setTitle(R.string.error_dialog_title);
        dialog.setMessage(message);
        dialog.setNeutralButton("Ok", null);
        dialog.create().show();
	}

	public static void displayError(Context context, Exception e) {
//		Log.e(LT, e.toString());
		ErrorReporter.displayError(context, e.getMessage());
	}
}
