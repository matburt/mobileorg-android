package com.matburt.mobileorg;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;

public class ErrorReporter
{
	private static final String LT = "MobileOrg";
	
    public static void displayError(Context context,
    		String message)
    {
    	Log.e(LT, R.string.error_dialog_title + ":\n" + message);
    	AlertDialog.Builder builder = new AlertDialog.Builder(context);
    	builder.setMessage(message)
    		   .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
    			   public void onClick(DialogInterface dialog, int which) {
    				   dialog.dismiss();
    			   }
    		   })
    		   .setTitle(R.string.error_dialog_title)
    		   .show();
    }
}
