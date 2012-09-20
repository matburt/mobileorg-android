package com.matburt.mobileorg.Synchronizers;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Gui.Outline.OutlineActivity;

public class SynchronizerNotification {
	private NotificationManager notificationManager;
	private Notification notification;
	private int notifyRef = 1;
	private Context context;

	public SynchronizerNotification(Context context) {
		this.context = context;
	}
	
    public void errorNotification(String errorMsg) {
		this.notificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		Intent notifyIntent = new Intent(context, OutlineActivity.class);
		notifyIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
				notifyIntent, 0);

		notification = new Notification(R.drawable.icon,
				"Synchronization Failed", System.currentTimeMillis());
		
		notification.contentIntent = contentIntent;
		notification.flags = notification.flags;
		notification.contentView = new RemoteViews(context
				.getPackageName(), R.layout.sync_notification);
		
		notification.contentView.setImageViewResource(R.id.status_icon, R.drawable.icon);
        notification.contentView.setTextViewText(R.id.status_text, errorMsg);
        notification.contentView.setProgressBar(R.id.status_progress, 100, 100, false);
		notificationManager.notify(notifyRef, notification);
    }
	
	public void setupNotification() {
		this.notificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		Intent notifyIntent = new Intent(context, OutlineActivity.class);
		notifyIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
				| Intent.FLAG_ACTIVITY_SINGLE_TOP);

		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
				notifyIntent, 0);

		notification = new Notification(R.drawable.icon,
				"Started synchronization", System.currentTimeMillis());

		notification.contentIntent = contentIntent;
		notification.flags = notification.flags
				| Notification.FLAG_ONGOING_EVENT;
		notification.contentView = new RemoteViews(context.getPackageName(),
				R.layout.sync_notification);

		notification.contentView.setImageViewResource(R.id.status_icon,
				R.drawable.icon);
		notification.contentView.setTextViewText(R.id.status_text,
				"Synchronizing...");
		notification.contentView.setProgressBar(R.id.status_progress, 100, 0,
				false);
		notificationManager.notify(notifyRef, notification);
	}

	public void updateNotification(int progress) {
		notification.contentView.setProgressBar(R.id.status_progress, 100,
				progress, false);
		notificationManager.notify(notifyRef, notification);
	}

	public void updateNotification(int progress, String message) {
		notification.contentView.setTextViewText(R.id.status_text, message);
		notification.contentView.setProgressBar(R.id.status_progress, 100,
				progress, false);
		notificationManager.notify(notifyRef, notification);
	}

	public void updateNotification(int fileNumber, String message,
			int totalFiles) {
		int partialProgress = ((40 / totalFiles) * fileNumber);
		notification.contentView.setProgressBar(R.id.status_progress, 100,
				60 + partialProgress, false);
		notification.contentView.setTextViewText(R.id.status_text, message);
		notificationManager.notify(notifyRef, notification);
	}

	public void finalizeNotification() {
		notificationManager.cancel(notifyRef);
	}

}
