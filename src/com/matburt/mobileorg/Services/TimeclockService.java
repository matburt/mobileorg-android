package com.matburt.mobileorg.Services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Parsing.MobileOrgApplication;
import com.matburt.mobileorg.Parsing.NodeWrapper;
import com.matburt.mobileorg.Parsing.OrgDatabase;

public class TimeclockService extends Service {
	public static final String NODE_ID = "node_id";
	public static final String CLOCK_HOUR = "clock_hour";
	public static final String CLOCK_MINUTE = "clock_minute";

	public static final String TIMECLOCK_CANCEL = "com.matburt.mobileorg.Timeclock.action.TIMECLOCK_CANCEL";
	
	private final int notificationID = 1337;
	private NotificationManager mNM;
	private long node_id;

	private OrgDatabase db;
	private TimeclockReceiver receiver;
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		
		MobileOrgApplication appInst = (MobileOrgApplication) getApplication();
		this.db = appInst.getDB();
		this.receiver = new TimeclockReceiver();
		registerReceiver(receiver, new IntentFilter(TIMECLOCK_CANCEL));
	}

	@Override
	public void onDestroy() {
		this.mNM.cancel(notificationID);
		unregisterReceiver(receiver);
		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		this.node_id = intent.getLongExtra(NODE_ID, -1);
		showNotification(node_id);
		return 0;
	}
	
	private void showNotification(long node_id) {
		NodeWrapper node = new NodeWrapper(db.getNode(node_id));
		
		Notification notification = new Notification(R.drawable.icon, node.getName(),
				System.currentTimeMillis());

		Intent intent = new Intent(this, TimeclockDialog.class);
		intent.putExtra(NODE_ID, node_id);
		intent.putExtra(CLOCK_HOUR, 4);
		intent.putExtra(CLOCK_MINUTE, 2);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 1,
				intent, 0);

		notification.setLatestEventInfo(this, getText(R.string.app_name), node.getName(),
				contentIntent);

		node.close();
		mNM.notify(notificationID, notification);
	}
	
	private void cancelNotification() {
		mNM.cancel(notificationID);
	}
	
	private class TimeclockReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			cancelNotification();
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
