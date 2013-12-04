package com.matburt.mobileorg.test.Gui;

import java.io.IOException;
import java.util.ArrayList;

import com.matburt.mobileorg.Gui.Agenda.OrgQueryBuilder;
import com.matburt.mobileorg.Gui.Agenda.OrgAgenda;

import android.content.Context;
import android.test.AndroidTestCase;

public class AgendaTests extends AndroidTestCase {

	private Context context;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.context = getContext();
	}
	
	public void testBlockSerialization() throws IOException {
		ArrayList<OrgAgenda> agendas = new ArrayList<OrgAgenda>();
		OrgAgenda blockAgenda = new OrgAgenda();
		blockAgenda.title = "test";
		agendas.add(blockAgenda);
		
		OrgAgenda.writeAgendas(agendas, context);
		ArrayList<OrgAgenda> readAgendas = OrgAgenda.readAgendas(context);
		
		assertEquals(agendas.size(), readAgendas.size());
		OrgAgenda readBlockAgenda = readAgendas.get(0);
		assertEquals(blockAgenda.title, readBlockAgenda.title);
	}
	
	public void testQuerySerialization() throws IOException {
		ArrayList<OrgAgenda> agendas = new ArrayList<OrgAgenda>();
		OrgAgenda blockAgenda = new OrgAgenda();
		agendas.add(blockAgenda);

		OrgQueryBuilder builder = new OrgQueryBuilder("test");
		blockAgenda.queries.add(builder);
		
		OrgAgenda.writeAgendas(agendas, context);
		ArrayList<OrgAgenda> readAgendas = OrgAgenda.readAgendas(context);
		
		OrgAgenda readBlockAgenda = readAgendas.get(0);
		assertEquals(blockAgenda.queries.size(), readBlockAgenda.queries.size());
		assertEquals(blockAgenda.queries.get(0).title, readBlockAgenda.queries.get(0).title);
	}
}
