package com.matburt.mobileorg;

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
import com.matburt.mobileorg.Parsing.MobileOrgDatabase;
import com.matburt.mobileorg.Parsing.Node;
import com.matburt.mobileorg.Parsing.OrgFileParser;

public class MobileOrgWidget extends AppWidgetProvider {
    @SuppressWarnings("unused")
	private static final String LT = "MobileOrgWidget";

   @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {
        // To prevent any ANR timeouts, we perform the update in a service
       context.startService(new Intent(context,
                                       MobileOrgWidgetService.class));
    }

    public static class MobileOrgWidgetService extends Service {
        private MobileOrgDatabase appdb;
        @Override
        public void onStart(Intent intent, int startId) {
            this.appdb = new MobileOrgDatabase((Context)this);
            RemoteViews updateViews = this.genUpdateDisplay(this);
            ComponentName thisWidget = new ComponentName(this,
                                                         MobileOrgWidget.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            manager.updateAppWidget(thisWidget, updateViews);
        }

        @Override
        public void onDestroy() {
            this.appdb.close();
            super.onDestroy();
        }

        public RemoteViews genUpdateDisplay(Context context) {
            RemoteViews updateViews = null;
            updateViews = new RemoteViews(context.getPackageName(),
                                          R.layout.widget_mobileorg);

            MobileOrgApplication appInst = (MobileOrgApplication)this.getApplication();
            OrgFileParser ofp = new OrgFileParser(getBaseContext(), appInst);

            ofp.parse();
            Node agendaNode = ofp.rootNode.findChildNode("agendas.org");
            if (agendaNode != null) {
                Node todoNode = agendaNode.findChildNode("ToDo: ALL");
                if (todoNode != null) {
                    String widgetBuffer = "";
                    for (int idx = 0; idx < todoNode.children.size(); idx++) {
                        widgetBuffer = widgetBuffer + todoNode.children.get(idx).name + "\n";
                    }
                    updateViews.setTextViewText(R.id.message, widgetBuffer);
                }
            }
            return updateViews;
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