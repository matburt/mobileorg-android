package com.matburt.mobileorg.Gui.Agenda;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Synchronizers.Synchronizer;

public class AgendaActivity extends SherlockFragmentActivity {
	public static final String POSITION = "position";
	private int position;
	private SynchServiceReceiver syncReceiver;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.agenda);
		
		position = getIntent().getIntExtra(POSITION, 0);
		
		syncReceiver = new SynchServiceReceiver();
		registerReceiver(syncReceiver, new IntentFilter(
				Synchronizer.SYNC_UPDATE));
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(syncReceiver);
		super.onDestroy();
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		refresh();
	}
	
	public void refresh() {
		AgendaFragment agendaFragment = (AgendaFragment) getSupportFragmentManager()
				.findFragmentByTag("agenda_fragment");
		
		agendaFragment.agendaPos = position;
		agendaFragment.showBlockAgenda(position);
	}
	
	private class SynchServiceReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			boolean syncDone = intent.getBooleanExtra(Synchronizer.SYNC_DONE, false);
			
			if (syncDone)
				refresh();
		}
	}
}
