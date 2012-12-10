package com.matburt.mobileorg.Services;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.RemoteViews;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.OrgData.MobileOrgApplication;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.util.OrgNodeNotFoundException;

public class TimeclockService extends Service {
	public static final String NODE_ID = "node_id";
	public static final String TIMECLOCK_UPDATE = "timeclock_update";
	public static final String TIMECLOCK_TIMEOUT = "timeclock_timeout";

	private final int notificationID = 1337;
	private NotificationManager mNM;
	private AlarmManager alarmManager;
	private Notification notification;
	
	private long node_id;
	private OrgNode node;
	private int estimatedMinute = -1;
	private int estimatedHour = -1;
	private MobileOrgApplication appInst;

	
	private static TimeclockService sInstance;
	private long startTime;
	private PendingIntent updateIntent;
	private PendingIntent timeoutIntent;
	private boolean hasTimedOut = false;
	
	public static TimeclockService getInstance() {
		return sInstance;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		sInstance = this;
		this.mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		this.alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		this.appInst = (MobileOrgApplication) getApplication();
	}

	@Override
	public void onDestroy() {
		cancelNotification();
		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		String action = intent.getStringExtra("action");
		Log.d("MobileOrg", "Called onStartCommand() with :" + action);
		if(action == null) {
			this.node_id = intent.getLongExtra(NODE_ID, -1);
			try {
				this.node = new OrgNode(node_id, getContentResolver());
			} catch (OrgNodeNotFoundException e) {}
			this.startTime = System.currentTimeMillis();
			
			getEstimated();
			showNotification(node_id);
			setUpdateAlarm();
			setTimeoutAlarm(this.estimatedHour, this.estimatedMinute);
		}		
		else if(action.equals(TIMECLOCK_UPDATE))
			updateTime();
		else if(action.equals(TIMECLOCK_TIMEOUT)){
			doTimeout();
		}
		
		return 0;
	}
	
	private void getEstimated() {
		String estimated = node.getOrgNodePayload().getProperty("Effort").trim();

		if (TextUtils.isEmpty(estimated) == false) {
			String[] split = estimated.split(":");
			try {
				if (split.length == 1)
					this.estimatedMinute = Integer.parseInt(split[0]);
				else if (split.length == 2) {
					this.estimatedHour = Integer.parseInt(split[0]);
					this.estimatedMinute = Integer.parseInt(split[1]);
				}
			} catch (NumberFormatException e) {
			}
		}
	}
	
	private void showNotification(long node_id) {
		
		PendingIntent contentIntent = PendingIntent.getActivity(this, 1,
				new Intent(this, TimeclockDialog.class), 0);
		
		Builder builder = new NotificationCompat.Builder(this);
		builder.setSmallIcon(R.drawable.timeclock_icon);
		builder.setContentTitle(node.name);
		builder.setContentIntent(contentIntent);
		builder.setOngoing(true);
		
		this.notification = builder.getNotification();

		notification.contentView = new RemoteViews(this.getPackageName(),
				R.layout.timeclock_notification);
		
		notification.contentView.setImageViewResource(R.id.timeclock_notification_icon,
				R.drawable.timeclock_icon);
		notification.contentView.setTextViewText(R.id.timeclock_notification_text,
				node.name);
		
		updateTime();

		mNM.notify(notificationID, notification);
	}
	
	private void setUpdateAlarm() {
		Intent intent = new Intent(this, TimeclockService.class);
		intent.putExtra("action", TIMECLOCK_UPDATE);
		this.updateIntent = PendingIntent.getService(appInst, 1,
				intent, 0);
		alarmManager.setRepeating(AlarmManager.RTC, System.currentTimeMillis()
				+ DateUtils.MINUTE_IN_MILLIS, DateUtils.MINUTE_IN_MILLIS, updateIntent);
	}
	
	private void setTimeoutAlarm(int hour, int minute) {
		if(hour <= 0 && minute <= 0)
			return;
		
		long time = (hour * DateUtils.HOUR_IN_MILLIS)
				+ (minute * DateUtils.MINUTE_IN_MILLIS);
		
		Intent intent = new Intent(this, TimeclockService.class);
		intent.putExtra("action", TIMECLOCK_TIMEOUT);
		this.timeoutIntent = PendingIntent.getService(appInst, 2,
				intent, 0);
		alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
				+ time, timeoutIntent);
	}
	
	private void unsetAlarms() {
		if(this.updateIntent != null) {
			alarmManager.cancel(this.updateIntent);
			this.updateIntent = null;
		}
		
		if(this.timeoutIntent != null) {
			alarmManager.cancel(this.timeoutIntent);
			this.timeoutIntent = null;
		}
	}
	
	private void doTimeout() {
		if(notification == null)
			return;
		notification.defaults = Notification.DEFAULT_ALL;
		mNM.notify(notificationID, notification);
		notification.defaults = 0;
		this.hasTimedOut = true;
		updateTime();
	}
	
	private void updateTime() {
		SpannableStringBuilder itemText = new SpannableStringBuilder(getElapsedTimeString());
		
		if(this.hasTimedOut)
			itemText.setSpan(new ForegroundColorSpan(Color.RED), 0,
					itemText.length(), 0);
		
		itemText.append(getEstimatedTimeString());
		
		notification.contentView.setTextViewText(
				R.id.timeclock_notification_time, itemText);
		mNM.notify(notificationID, notification);
	}
	
	public String getElapsedTimeString() {
		long difference = System.currentTimeMillis() - this.startTime;
		if(difference >= 0) {
			String elapsed = String.format("%d:%02d",
					(int) ((difference / (1000 * 60 * 60)) % 24),
					(int) ((difference / (1000 * 60)) % 60));
			
			return elapsed;
		}
		else
			return "0:00";
	}
	
	private String getEstimatedTimeString() {
		if (this.estimatedHour <= 0 && this.estimatedMinute <= 0)
			return "";
		else
			return "/"
					+ String.format("%d:%02d", this.estimatedHour,
							this.estimatedMinute);
	}
	
	public long getStartTime() {
		return this.startTime;
	}
	
	public long getEndTime() {
		return System.currentTimeMillis();
	}
	
	public long getNodeID() {
		return this.node_id;
	}
	
	public void cancelNotification() {
		unsetAlarms();
		mNM.cancel(notificationID);
		this.stopSelf();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
