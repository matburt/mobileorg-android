package com.matburt.mobileorg.Gui.Agenda;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.matburt.mobileorg.R;

public class AgendaSettings extends SherlockActivity {
	public static final String AGENDA_NUMBER = "agenda_number";
	private static final String AGENDA_TITLE = "agenda_title";

	private int agendaPos;

	private TextView titleView;
	private ListView agendaList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.agenda_settings);
        this.titleView = (TextView) findViewById(R.id.agenda_title);
        this.agendaList = (ListView) findViewById(R.id.agenda_list);
        
        Button createButton = (Button) findViewById(R.id.agenda_create);
        createButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				createAgendaBlockEntry();
			}
		});
        
        agendaList.setOnItemClickListener(agendaItemClick);
        agendaList.setOnCreateContextMenuListener(this);
        registerForContextMenu(agendaList);
        
        this.agendaPos = getIntent().getIntExtra(AGENDA_NUMBER, -1);
        
        if(this.agendaPos == -1)
        	this.agendaPos = OrgAgenda.addAgenda(this);
        
        
    	if (savedInstanceState != null) {
    		String title = savedInstanceState.getString(AGENDA_TITLE);
    		titleView.setText(title);
    	} else {
    		String agendaTitle = OrgAgenda.getAgenda(agendaPos, this).title;
    		this.titleView.setText(agendaTitle);
    	}
    }


	@Override
    protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	outState.putString(AGENDA_TITLE, titleView.getText().toString());
		Log.d("MobileOrg", "onSaveInstanceState saved : "
				+ titleView.getText().toString());
    }


	@Override
	protected void onResume() {
		super.onResume();
		refresh();
	}
    
	public void refresh() {
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1,
				OrgAgenda.getAgendaQueryTitles(agendaPos, this));
		agendaList.setAdapter(adapter);
		getSupportActionBar().setTitle("Block settings");
	}


	private OnItemClickListener agendaItemClick = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> adapter, View view, int position,
				long id) {
			Intent intent = new Intent(AgendaSettings.this, AgendaEntrySetting.class);
			intent.putExtra(AgendaEntrySetting.AGENDA_NUMBER, agendaPos);
			intent.putExtra(AgendaEntrySetting.ENTRY_NUMBER, position);
			startActivity(intent);
		}
	};
	

    private void createAgendaBlockEntry() {
		Intent intent = new Intent(AgendaSettings.this, AgendaEntrySetting.class);
		intent.putExtra(AgendaEntrySetting.AGENDA_NUMBER, this.agendaPos);
		startActivity(intent);
    }
    
    private void saveAgendaBlock() {
    	OrgAgenda blockAgenda = OrgAgenda.getAgenda(agendaPos, this);
    	blockAgenda.title = this.titleView.getText().toString();
    	OrgAgenda.replaceAgenda(blockAgenda, agendaPos, this);
    }
    
	private void removeAgendaBlockEntry(int blockPos) {
		OrgAgenda.removeAgendaEntry(agendaPos, blockPos, this);
		refresh();
	}
    
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getSupportMenuInflater().inflate(R.menu.agenda_block, menu);
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch(item.getItemId()) {
		case R.id.agenda_block_save:
			saveAgendaBlock();
			finish();
			break;
			
		case R.id.agenda_block_cancel:
			finish();
			break;
			
		default:
			return super.onOptionsItemSelected(item);
		}
		
		return true;
	}
	

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add("Remove");
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		if(item.getTitle().equals("Remove")) {
		    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
			removeAgendaBlockEntry(info.position);
			return true;
		}
		else
			return super.onContextItemSelected(item);
	}
}
