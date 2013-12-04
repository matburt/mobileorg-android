package com.matburt.mobileorg.Gui.Widget;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.widget.RemoteViews;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Gui.Capture.EditActivity;
import com.matburt.mobileorg.Gui.Capture.EditActivityController;
import com.matburt.mobileorg.Synchronizers.Synchronizer;

public class MobileOrgWidget extends AppWidgetProvider {

   @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {
        // To prevent any ANR timeouts, we perform the update in a service
       context.startService(new Intent(context,
                                       MobileOrgWidgetService.class));
    }

    public static class MobileOrgWidgetService extends Service {

    	private RemoteViews updateViews;
    	private AppWidgetManager manager;
    	private ComponentName thisWidget;
    	
    	@Override
        public void onStart(Intent intent, int startId) {
            RemoteViews updateViews = this.genUpdateDisplay(this);
            thisWidget = new ComponentName(this, MobileOrgWidget.class);
            manager = AppWidgetManager.getInstance(this);
            manager.updateAppWidget(thisWidget, updateViews);
            
            IntentFilter serviceFilter = new IntentFilter(Synchronizer.SYNC_UPDATE);
            registerReceiver(new SynchServiceReceiver(), serviceFilter);
        }

        private RemoteViews genUpdateDisplay(Context context) {
            updateViews = new RemoteViews(context.getPackageName(),
                                          R.layout.widget);
            
            Intent intent = new Intent(context, EditActivity.class);
            intent.setAction(EditActivityController.ACTIONMODE_CREATE);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            updateViews.setOnClickPendingIntent(R.id.widget, pendingIntent);

			updateViews.setTextViewText(R.id.message, getAgenda());
            return updateViews;
        }

        private String getAgenda() {
 //           MobileOrgApplication appInst = (MobileOrgApplication)this.getApplication();
            
//            Node agendaNode = appInst.getDB().getFileNode("agendas.org");
//            ArrayList<EditNode> parseEdits = appInst.getNodeEdits();
			
//            StringBuilder widgetBuffer = new StringBuilder();            
//			if (agendaNode != null) {
//				Node todoNode = agendaNode.findChildNode("Today");
//				if (todoNode != null) {
//					todoNode = todoNode.getChildren().get(0);
//					todoNode.applyEdits(parseEdits);
//					if (todoNode != null && !todoNode.todo.equals("DONE")) {
//						for (Node child : todoNode.getChildren()) {
//							widgetBuffer.append(child.payload.getTime());
//							widgetBuffer.append(" ");
//							widgetBuffer.append(child.name);
//							widgetBuffer.append("\n");
//						}		
//					}
//				}
//			}
			
//			return widgetBuffer.toString();
            return "";
        }
        
        private void refreshDisplay() {
        	updateViews.setTextViewText(R.id.message, getAgenda());
            manager.updateAppWidget(thisWidget, updateViews);
        }

        @Override
        public IBinder onBind(Intent intent) {
            // We don't need to bind to this service
            return null;
        }
        
    	private class SynchServiceReceiver extends BroadcastReceiver {
    		@Override
    		public void onReceive(Context context, Intent intent) {
    			if (intent.getBooleanExtra(Synchronizer.SYNC_DONE, false))
    				refreshDisplay();
    		}
    	}
    }
}