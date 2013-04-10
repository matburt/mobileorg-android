package com.matburt.mobileorg.test.Synchronizers;

import android.content.Context;

import com.matburt.mobileorg.Gui.SynchronizerNotificationCompat;

public class SynchronizerNotificationStub extends SynchronizerNotificationCompat {

	int errorNotificationNum = 0;
	
	public SynchronizerNotificationStub(Context context) {
		super(context);
	}

	@Override
	public void errorNotification(String errorMsg) {
		errorNotificationNum++;
	}

	@Override
	public void setupNotification() {
	}

	@Override
	public void updateNotification(int progress) {
	}

	@Override
	public void updateNotification(int progress, String message) {
	}

	@Override
	public void finalizeNotification() {
	}

	public void reset() {
		this.errorNotificationNum = 0;
	}
	
}
