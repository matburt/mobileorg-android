package com.matburt.mobileorg.Services;

import java.util.ArrayList;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Gui.NodeEditActivity;
import com.matburt.mobileorg.Parsing.EditNode;
import com.matburt.mobileorg.Parsing.MobileOrgApplication;
import com.matburt.mobileorg.Parsing.Node;
import com.matburt.mobileorg.Parsing.OrgFileParser;

public class MobileOrgWidget extends AppWidgetProvider {
    public static String WIDGET_CAPTURE = "WIDGET_CAPTURE";

   @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {
        // To prevent any ANR timeouts, we perform the update in a service
       context.startService(new Intent(context,
                                       MobileOrgWidgetService.class));
    }

    public static class MobileOrgWidgetService extends Service {

    	@Override
        public void onStart(Intent intent, int startId) {
            RemoteViews updateViews = this.genUpdateDisplay(this);
            ComponentName thisWidget = new ComponentName(this,
                                                         MobileOrgWidget.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            manager.updateAppWidget(thisWidget, updateViews);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
        }

        public RemoteViews genUpdateDisplay(Context context) {
            RemoteViews updateViews = null;
            updateViews = new RemoteViews(context.getPackageName(),
                                          R.layout.widget);
            
            Intent intent = new Intent(context, NodeEditActivity.class);
            intent.setAction(NodeEditActivity.ACTIONMODE_CREATE);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            updateViews.setOnClickPendingIntent(R.id.widget, pendingIntent);


			updateViews.setTextViewText(R.id.message, getAgenda());
            return updateViews;
        }

        private String getAgenda() {
            MobileOrgApplication appInst = (MobileOrgApplication)this.getApplication();
            
            OrgFileParser ofp = new OrgFileParser(getBaseContext(), appInst);
			Node agendaNode = ofp.parseFile("agendas.org", null);
			ArrayList<EditNode> parseEdits = ofp.parseEdits();
			
			
            StringBuilder widgetBuffer = new StringBuilder();            
			if (agendaNode != null) {
				Node todoNode = agendaNode.findChildNode("Today");
				if (todoNode != null) {
					todoNode = todoNode.getChildren().get(0);
					todoNode.applyEdits(parseEdits);
					if (todoNode != null && !todoNode.todo.equals("DONE")) {
						for (Node child : todoNode.getChildren()) {
							widgetBuffer.append(child.payload.getTime());
							widgetBuffer.append(" ");
							widgetBuffer.append(child.name);
							widgetBuffer.append("\n");
						}		
					}
				}
			}
			
			return widgetBuffer.toString();
        }

        public String getStorageLocation(Context context) {
            SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            return appPrefs.getString("storageMode", "");
        }

        @Override
        public IBinder onBind(Intent intent) {
            // We don't need to bind to this service
            return null;
        }
    }
}