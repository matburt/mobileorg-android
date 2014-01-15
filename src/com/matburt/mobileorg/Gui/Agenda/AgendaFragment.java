package com.matburt.mobileorg.Gui.Agenda;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.commonsware.cwac.merge.MergeAdapter;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Gui.Outline.OutlineActionMode;
import com.matburt.mobileorg.Gui.Outline.OutlineAdapter;
import com.matburt.mobileorg.OrgData.OrgDatabase;

public class AgendaFragment extends SherlockFragment {

	public int agendaPos = 0;
	private ListView agendaList;
	private MergeAdapter mergeAdapter;
	private SQLiteDatabase db;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		this.db = new OrgDatabase(getActivity()).getReadableDatabase();

		this.agendaList = new ListView(getActivity());
		this.agendaList.setOnItemClickListener(agendaClickListener);
		
		return agendaList;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		showBlockAgenda(agendaPos);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		db.close();
	}
	
	public void showBlockAgenda(int agendaPos) {
		this.mergeAdapter = new MergeAdapter();
		
		OrgAgenda blockAgenda = OrgAgenda.getAgenda(agendaPos, getActivity());
		
		for (OrgQueryBuilder agenda : blockAgenda.queries)
			addAgenda(agenda);

		this.agendaList.setAdapter(mergeAdapter);
		this.agendaList.setOnItemClickListener(agendaClickListener);
		
		((AgendaActivity) getActivity()).getSupportActionBar().setTitle("Agenda: " +
				blockAgenda.title);
	}
	
	public void addAgenda(OrgQueryBuilder query) {
		TextView titleView = (TextView) View.inflate(getActivity(), R.layout.agenda_header, null);
		titleView.setText(query.title);
		mergeAdapter.addView(titleView);

		OutlineAdapter adapter = new OutlineAdapter(getActivity());
		adapter.setLevelIndentation(false);
		adapter.setState(query.getNodes(db, getActivity()));
		mergeAdapter.addAdapter(adapter);
	}
	
	private OnItemClickListener agendaClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View v, int position,
				long id) {
			long nodeId = mergeAdapter.getItemId(position);
			OutlineActionMode.runEditNodeActivity(nodeId, getActivity());
		}
	};
}
