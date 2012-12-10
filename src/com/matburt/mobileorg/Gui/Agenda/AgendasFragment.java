package com.matburt.mobileorg.Gui.Agenda;

import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.matburt.mobileorg.R;

public class AgendasFragment extends SherlockFragment {

	private ListView agendaList;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		this.agendaList = new ListView(getActivity());
		this.agendaList.setOnItemClickListener(agendaClickListener);
        this.agendaList.setOnCreateContextMenuListener(this);

		setHasOptionsMenu(true);
		
		return agendaList;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		refresh();
	}

	private void refresh() {
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
				android.R.layout.simple_list_item_1,
				OrgAgenda.getAgendasTitles(getActivity()));
		agendaList.setAdapter(adapter);
	}

	
	private OnItemClickListener agendaClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> adapter, View view,
				int position, long id) {
			showBlockAgendaFragment(position);
		}
	};
	
	private void showBlockAgendaFragment(int position) {
//		FragmentTransaction transaction = getFragmentManager().beginTransaction();
//		transaction.hide(this);
//		
//		AgendaFragment blockFragment = new AgendaFragment();
//		blockFragment.agendaPos = position;
//		//transaction.add("agendaFragment", blockFragment, "agendaBlockFragment");
//		//transaction.show(blockFragment);
//		transaction.addToBackStack(getTag());
//		
//		transaction.commit();
		
		Intent intent = new Intent(getActivity(), AgendaActivity.class);
		intent.putExtra(AgendaActivity.POSITION, position);
		startActivity(intent);
	}
	
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add("Remove");
		menu.add("Configure");
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		int position = info.position;
		
		if(item.getTitle().equals("Remove")) {
			OrgAgenda.removeAgenda(position, getActivity());
			refresh();
			return true;
		} else if(item.getTitle().equals("Configure")) {
			Intent intent = new Intent(getActivity(), AgendaSettings.class);
			intent.putExtra(AgendaSettings.AGENDA_NUMBER, position);
			startActivity(intent);
			return true;
		}
		else
			return super.onContextItemSelected(item);
	}


	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		
		MenuItem create = menu.add("Create");
		create.setIcon(R.drawable.ic_menu_add2);
		create.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		if(item.getTitle().equals("Create")) {
			Intent intent = new Intent(getActivity(), AgendaSettings.class);
			startActivity(intent);
			return true;
		}
		else
			return super.onOptionsItemSelected(item);
	}
}
