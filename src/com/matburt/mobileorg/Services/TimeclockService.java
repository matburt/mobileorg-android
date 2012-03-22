package com.matburt.mobileorg.Services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Parsing.MobileOrgApplication;
import com.matburt.mobileorg.Parsing.NodeWrapper;
import com.matburt.mobileorg.Parsing.OrgDatabase;

public class TimeclockService extends Service {
	public static final String NODE_ID = "node_id";
	
	private final int notificationID = 1337;
	private NotificationManager mNM;
	private long node_id;

	private OrgDatabase db;
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		
		MobileOrgApplication appInst = (MobileOrgApplication) getApplication();
		this.db = appInst.getDB();
	}

	@Override
	public void onDestroy() {
		this.mNM.cancel(notificationID);
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
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				intent, 0);

		notification.setLatestEventInfo(this, getText(R.string.app_name), node.getName(),
				contentIntent);

		node.close();
		mNM.notify(notificationID, notification);
	}
	
	@SuppressWarnings("unused")
	private void cancelNotification() {
		mNM.cancel(notificationID);
	}
	

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
