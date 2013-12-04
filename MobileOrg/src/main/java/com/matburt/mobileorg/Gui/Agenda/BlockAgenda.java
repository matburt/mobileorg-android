package com.matburt.mobileorg.Gui.Agenda;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

import android.content.Context;
import android.util.Log;

import com.matburt.mobileorg.util.OrgUtils;

public class BlockAgenda implements Serializable {
	private static final long serialVersionUID = 2;
	private static final String AGENDA_CONFIG_FILE = "agendas.conf";

	public String title = "";
	
	public ArrayList<OrgQueryBuilder> queries = new ArrayList<OrgQueryBuilder>();
	

	public static ArrayList<BlockAgenda> readAgendas(Context context) throws IOException {
		FileInputStream fis = context.openFileInput(AGENDA_CONFIG_FILE);
		byte[] serializedObject = new byte[fis.available()];
		fis.read(serializedObject);
		fis.close();
		
		@SuppressWarnings("unchecked")
		ArrayList<BlockAgenda> result = (ArrayList<BlockAgenda>) OrgUtils
		.deserializeObject(serializedObject);
		return result;
	}
	
	public static void writeAgendas(ArrayList<BlockAgenda> agendas, Context context) throws IOException {
		byte[] serializeObject = OrgUtils.serializeObject(agendas);
		FileOutputStream fos = context.openFileOutput(AGENDA_CONFIG_FILE, Context.MODE_PRIVATE);
		fos.write(serializeObject);
		fos.close();
	}

/** Agendas  **/	
    public static ArrayList<String> getAgendasTitles(Context context) {
    	ArrayList<String> result = new ArrayList<String>();
		try {
			ArrayList<BlockAgenda> agendas = readAgendas(context);
			
			for(BlockAgenda agenda: agendas)
				result.add(agenda.title);
			
			return result;
		} catch (IOException e) {
			return result;
		}
    }
	
	public static void removeAgenda(int position, Context context) {
		try {
			ArrayList<BlockAgenda> agendas = readAgendas(context);
			agendas.remove(position);
			writeAgendas(agendas, context);
		} catch (IOException e) {}
	}
	
	public static void replaceAgenda(BlockAgenda agenda, int position, Context context) {
		try {
			ArrayList<BlockAgenda> agendas = readAgendas(context);
			agendas.remove(position);
			agendas.add(position, agenda);
			writeAgendas(agendas, context);
		} catch (IOException e) {}
	}

	
/** Block Agendas **/

	public static BlockAgenda getBlockAgenda(int agendaPos, Context context) {
		try {
			ArrayList<BlockAgenda> agendas = readAgendas(context);
			
			if(agendaPos >= 0 && agendaPos < agendas.size())
				return agendas.get(agendaPos);
		} catch (IOException e) {}
		return new BlockAgenda();
	}


	public static int addBlockAgenda(Context context, String title) {
		try {
			ArrayList<BlockAgenda> agendas = readAgendas(context);
			BlockAgenda blockAgenda = new BlockAgenda();
			blockAgenda.title = title;
			agendas.add(blockAgenda);
			writeAgendas(agendas, context);
			return agendas.size() - 1;
		} catch (IOException e) {}
		return -1;
	}
	
	public static ArrayList<String> getBlockAgendaQueryTitles(int agendaPos, Context context) {
		ArrayList<String> result = new ArrayList<String>();
		BlockAgenda blockAgenda = getBlockAgenda(agendaPos, context);

		for (OrgQueryBuilder query : blockAgenda.queries)
			result.add(query.title);

		return result;
	}


/** Block Agenda Query **/
	
	public static OrgQueryBuilder getAgendaBlockEntry(int agendaPos, int blockPos, Context context) {
		BlockAgenda blockAgenda = getBlockAgenda(agendaPos, context);
		if(blockPos >= 0 && blockPos < blockAgenda.queries.size())
			return blockAgenda.queries.get(blockPos);
		else
			return new OrgQueryBuilder("");
	}
	
	public static void removeBlockAgendaEntry(int agendaPos, int blockPos, Context context) {
		try {
			ArrayList<BlockAgenda> agendas = readAgendas(context);
			
			BlockAgenda blockAgenda = agendas.get(agendaPos);
			blockAgenda.queries.remove(blockPos);
			
			writeAgendas(agendas, context);
		} catch (IOException e) {}
	}
	
	
	public static void writeAgendaBlockEntry(OrgQueryBuilder query, int agendaPos, int blockPos, Context context) {
		try {
			ArrayList<BlockAgenda> agendas = readAgendas(context);
			
			if(agendaPos < 0 || agendaPos >= agendas.size())
				return;
			
			BlockAgenda blockAgenda = agendas.get(agendaPos);
			
			if(blockPos >= 0 && blockPos < blockAgenda.queries.size()) {
				blockAgenda.queries.remove(blockPos);
				blockAgenda.queries.add(blockPos, query);
			} else
				blockAgenda.queries.add(query);
			
			writeAgendas(agendas, context);
			
		} catch (IOException e) {
			Log.e("MobileOrg", e.getLocalizedMessage());
		}
	}
}
