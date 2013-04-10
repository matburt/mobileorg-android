package com.matburt.mobileorg.Gui.Agenda;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

import android.content.Context;

import com.matburt.mobileorg.util.OrgUtils;

public class OrgAgenda implements Serializable {
	private static final long serialVersionUID = 2;
	private static final String AGENDA_CONFIG_FILE = "agendas.conf";

	public String title = "";
	
	public ArrayList<OrgQueryBuilder> queries = new ArrayList<OrgQueryBuilder>();

	public static ArrayList<OrgAgenda> readAgendas(Context context)
			throws IOException {
		try {
			FileInputStream fis = context.openFileInput(AGENDA_CONFIG_FILE);
			byte[] serializedObject = new byte[fis.available()];
			fis.read(serializedObject);
			fis.close();

			@SuppressWarnings("unchecked")
			ArrayList<OrgAgenda> result = (ArrayList<OrgAgenda>) OrgUtils
					.deserializeObject(serializedObject);
			return result;
		} catch (FileNotFoundException e) {
			return new ArrayList<OrgAgenda>();
		}
	}
	
	public static void writeAgendas(ArrayList<OrgAgenda> agendas, Context context) throws IOException {
		byte[] serializeObject = OrgUtils.serializeObject(agendas);
		FileOutputStream fos = context.openFileOutput(AGENDA_CONFIG_FILE, Context.MODE_PRIVATE);
		fos.write(serializeObject);
		fos.flush();
		fos.close();
	}

    public static ArrayList<String> getAgendasTitles(Context context) {
    	ArrayList<String> result = new ArrayList<String>();
		try {
			ArrayList<OrgAgenda> agendas = readAgendas(context);
			
			if(agendas == null)
				return result;
			
			for(OrgAgenda agenda: agendas)
				result.add(agenda.title);
			
			return result;
		} catch (IOException e) {
			e.printStackTrace();
			return result;
		}
    }
	
	public static void removeAgenda(int position, Context context) {
		try {
			ArrayList<OrgAgenda> agendas = readAgendas(context);
			agendas.remove(position);
			writeAgendas(agendas, context);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void replaceAgenda(OrgAgenda agenda, int position, Context context) {
		try {
			ArrayList<OrgAgenda> agendas = readAgendas(context);
			agendas.remove(position);
			agendas.add(position, agenda);
			writeAgendas(agendas, context);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static OrgAgenda getAgenda(int agendaPos, Context context) {
		try {
			ArrayList<OrgAgenda> agendas = readAgendas(context);
			
			if(agendaPos >= 0 && agendaPos < agendas.size())
				return agendas.get(agendaPos);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new OrgAgenda();
	}


	public static int addAgenda(Context context) {
		try {
			ArrayList<OrgAgenda> agendas = readAgendas(context);
			OrgAgenda agenda = new OrgAgenda();
			agendas.add(agenda);
			writeAgendas(agendas, context);
			return agendas.size() - 1;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return -1;
	}
	
	public static ArrayList<String> getAgendaQueryTitles(int agendaPos, Context context) {
		ArrayList<String> result = new ArrayList<String>();
		OrgAgenda agenda = getAgenda(agendaPos, context);

		for (OrgQueryBuilder query : agenda.queries)
			result.add(query.title);

		return result;
	}


/** Agenda Query **/
	
	public static OrgQueryBuilder getAgendaEntry(int agendaPos, int entryPos, Context context) {
		OrgAgenda agenda = getAgenda(agendaPos, context);
		if(entryPos >= 0 && entryPos < agenda.queries.size())
			return agenda.queries.get(entryPos);
		else
			return new OrgQueryBuilder("");
	}
	
	public static void removeAgendaEntry(int agendaPos, int entryPos, Context context) {
		try {
			ArrayList<OrgAgenda> agendas = readAgendas(context);
			
			OrgAgenda agenda = agendas.get(agendaPos);
			agenda.queries.remove(entryPos);
			
			writeAgendas(agendas, context);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public static void writeAgendaEntry(OrgQueryBuilder query, int agendaPos, int entryPos, Context context) {
		try {
			ArrayList<OrgAgenda> agendas = readAgendas(context);
			
			if(agendaPos < 0 || agendaPos >= agendas.size())
				return;
			
			OrgAgenda agenda = agendas.get(agendaPos);
			
			if(entryPos >= 0 && entryPos < agenda.queries.size()) {
				agenda.queries.remove(entryPos);
				agenda.queries.add(entryPos, query);
			} else
				agenda.queries.add(query);
			
			writeAgendas(agendas, context);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
