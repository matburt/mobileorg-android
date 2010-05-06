package com.matburt.mobileorg;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.Context;
import android.net.Uri;
import android.os.IBinder;
import android.text.format.Time;
import android.util.Log;
import android.widget.RemoteViews;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MobileOrgWidget extends AppWidgetProvider {
    private static final String LT = "MobileOrgWidget";

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
            // Build the widget update for today
            RemoteViews updateViews = this.genUpdateDisplay(this);
            
            // Push update for this widget to the home screen
            ComponentName thisWidget = new ComponentName(this,
                                                         MobileOrgWidget.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            manager.updateAppWidget(thisWidget, updateViews);
        }

        public RemoteViews genUpdateDisplay(Context context) {
            Resources res = context.getResources();
            RemoteViews updateViews = null;
            updateViews = new RemoteViews(context.getPackageName(),
                                          R.layout.widget_mobileorg);
            ArrayList<String> allOrgList = this.getOrgFiles(context);
            String storageMode = this.getStorageLocation(context);
            OrgFileParser ofp = new OrgFileParser(allOrgList, storageMode);
            ofp.parse();
            Node agendaNode = ofp.rootNode.findChildNode("agendas.org");
            Node todoNode = agendaNode.findChildNode("ToDo: ALL");
            String widgetBuffer = "";
            for (int idx = 0; idx < todoNode.subNodes.size(); idx++) {
                widgetBuffer = widgetBuffer + todoNode.subNodes.get(idx).nodeName + "\n";
            }
            updateViews.setTextViewText(R.id.message, widgetBuffer);
            return updateViews;
        }

        public String getStorageLocation(Context context) {
            SQLiteDatabase appdb = context.openOrCreateDatabase("MobileOrg",
                                                             0,
                                                             null);
            Cursor result = appdb.rawQuery("SELECT val from settings where key='storage'", null);
            String val = null;
            if (result != null) {
                if (result.getCount() > 0) {
                    result.moveToFirst();
                    val = result.getString(0);
                }
            }
            appdb.close();
            result.close();
            return val;
        }

        public ArrayList<String> getOrgFiles(Context context) {
            ArrayList<String> allFiles = new ArrayList<String>();
            SQLiteDatabase appdb = context.openOrCreateDatabase("MobileOrg",
                                                             0, null);
            Cursor result = appdb.rawQuery("SELECT file FROM files", null);
            if (result != null) {
                if (result.getCount() > 0) {
                    result.moveToFirst();
                    do {
                        Log.d(LT, "pulled " + result.getString(0));
                        allFiles.add(result.getString(0));
                    } while(result.moveToNext());
                }
            }
            appdb.close();
            result.close();
            return allFiles;
        }

        @Override
        public IBinder onBind(Intent intent) {
            // We don't need to bind to this service
            return null;
        }
    }
}