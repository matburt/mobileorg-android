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
    
    public static void displayError(Context context,
    		ReportableError e)
    {
    	if(e.getOriginalError() != null) {
    		Log.e(LT, e.getOriginalError().toString());
    	}
    	ErrorReporter.displayError(context, e.getMessage());
    }
}
