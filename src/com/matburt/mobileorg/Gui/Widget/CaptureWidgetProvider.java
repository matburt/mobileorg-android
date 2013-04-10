package com.matburt.mobileorg.Gui.Widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.widget.RemoteViews;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Gui.Capture.EditActivity;
import com.matburt.mobileorg.Gui.Capture.EditActivityController;

public class CaptureWidgetProvider extends AppWidgetProvider {
	public static final String LOCATION = "location";
	public static final String TITLE = "title";
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		try {
			for (int i = 0; i < appWidgetIds.length; i++) {
				int appWidgetId = appWidgetIds[i];
				updateWidget(appWidgetId, appWidgetManager, context);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void updateWidget(int appWidgetId,
			AppWidgetManager appWidgetManager, Context context) {
		SharedPreferences prefs = getPreferences(appWidgetId, context);
		if (null == prefs)
			return;
		
		RemoteViews views = new RemoteViews(context.getPackageName(),
				R.layout.capture_widget);
		
		String title = prefs.getString(TITLE, "???");
		views.setTextViewText(R.id.capture_widget_text,
				prefs.getString("name", title));

		Intent intent = getWidgetIntent(appWidgetId, context);
		
		PendingIntent pendingIntent = PendingIntent.getActivity(
				context, appWidgetId, intent, PendingIntent.FLAG_CANCEL_CURRENT);
		views.setOnClickPendingIntent(R.id.capture_widget_root,
				pendingIntent);
		
		appWidgetManager.updateAppWidget(appWidgetId, views);
	}
	
	private static Intent getWidgetIntent(int appWidgetId, Context context) {
		Intent intent = new Intent(context, EditActivity.class);
		intent.putExtra(EditActivityController.ACTIONMODE, EditActivityController.ACTIONMODE_ADDCHILD);

		SharedPreferences prefs = getPreferences(appWidgetId, context);
		String olpLocation = prefs.getString(LOCATION, "");
		intent.putExtra(EditActivityController.OLP_LOCATION, olpLocation);

		return intent;
	}
	
	public static void writeConfig(int appWidgetId, Context context, String locationOlp, String title) {
		Editor edit = getPreferences(appWidgetId, context).edit();
		edit.putString(LOCATION, locationOlp);
		edit.putString(TITLE, title);
		edit.commit();
	}
	
	public static SharedPreferences getPreferences(int appWidgetId, Context context) {
		SharedPreferences prefs = context.getSharedPreferences("widget_"
				+ appWidgetId, Context.MODE_PRIVATE);
		return prefs;
	}
}
