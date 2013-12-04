package com.matburt.mobileorg.Plugin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.matburt.mobileorg.Services.SyncService;

public final class Synchronize extends BroadcastReceiver
{
    @Override
    public void onReceive(final Context context, final Intent intent) {
        context.startService(new Intent(context, SyncService.class));
    }
}