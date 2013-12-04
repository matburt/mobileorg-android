package com.matburt.mobileorg.Gui.Agenda;

import java.io.Serializable;
import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.matburt.mobileorg.OrgData.OrgContract.OrgData;
import com.matburt.mobileorg.OrgData.OrgDatabase.Tables;
import com.matburt.mobileorg.OrgData.OrgFile;
import com.matburt.mobileorg.OrgData.OrgProviderUtils;
import com.matburt.mobileorg.util.OrgFileNotFoundException;
import com.matburt.mobileorg.util.SelectionBuilder;

public class OrgQueryBuilder implements Serializable {
	private static final long serialVersionUID = 2;
	
	public String title = "";
	
	public ArrayList<String> files = new ArrayList<String>();
	public ArrayList<String> todos = new ArrayList<String>();
	public ArrayList<String> tags = new ArrayList<String>();
	public ArrayList<String> priorities = new ArrayList<String>();
	public ArrayList<String> payloads = new ArrayList<String>();

	public boolean filterHabits = false;
	public boolean activeTodos = false;
	
	public OrgQueryBuilder(String title) {
		this.title = title;
	}
	
	public long[] getNodes(SQLiteDatabase db, Context context) {
		long[] result = null;
		
		Cursor cursor = getQuery(context).query(db, OrgData.DEFAULT_COLUMNS, OrgData.DEFAULT_SORT);
		
		result = new long[cursor.getCount()];
		cursor.moveToFirst();
		
		int i = 0;
		while(cursor.isAfterLast() == false) {
			result[i++] = cursor.getLong(cursor.getColumnIndex(OrgData.ID));
			cursor.moveToNext();
		}
		cursor.close();
		return result;
	}
	
	public SelectionBuilder getQuery(Context context) {
		final SelectionBuilder builder = new SelectionBuilder();
		builder.table(Tables.ORGDATA);

		getFileSelection(builder, context);
		
		if (activeTodos)
			builder.where(getSelection(OrgProviderUtils.getActiveTodos(context
					.getContentResolver()), OrgData.TODO));
		
		if(todos != null && todos.size() > 0)
			builder.where(getSelection(todos, OrgData.TODO));
		
		if(tags != null && tags.size() > 0)
			builder.where(getLikeSelection(tags, OrgData.TAGS) + " OR "
					+ getLikeSelection(tags, OrgData.TAGS_INHERITED));
		
		if(priorities != null && priorities.size() > 0)
			builder.where(getSelection(priorities, OrgData.PRIORITY));		
		
		if(payloads != null && payloads.size() > 0)
			builder.where(getLikeSelection(payloads, OrgData.PAYLOAD));
		
		if(filterHabits)
			builder.where("NOT " + OrgData.PAYLOAD + " LIKE ?", "%:STYLE: habit%");
		
		return builder;
	}
	
	private String getLikeSelection(ArrayList<String> values, String column) {
		StringBuilder builder = new StringBuilder();
		
		if(values == null)
			return "";
		
		for (String value: values) {
			builder.append(column + " LIKE '%" + value + "%'").append(" OR ");
		}
		
		builder.delete(builder.length() - " OR ".length(), builder.length() - 1);
		return builder.toString();
	}
	
	private String getSelection(ArrayList<String> values, String column) {
		StringBuilder builder = new StringBuilder();
		
		if(values == null || values.size() == 0)
			return "";
		
		for (String value: values) {
			builder.append(column + "='" + value + "'").append(" OR ");
		}
		
		builder.delete(builder.length() - " OR ".length(), builder.length() - 1);
		return builder.toString();
	}
	
	private void getFileSelection(SelectionBuilder builder, Context context) {		
		if(files == null || files.size() == 0) {
			builder.where("NOT " + OrgData.FILE_ID + "=?",
					Long.toString(getFileId(OrgFile.AGENDA_FILE,
							context.getContentResolver())));
			return;
		}
		
		StringBuilder stringBuilder = new StringBuilder();
		for (String filename: files) {
			long fileId = getFileId(filename, context.getContentResolver());
			stringBuilder.append(OrgData.FILE_ID + "=" + Long.toString(fileId)).append(" OR ");
		}
		stringBuilder.delete(stringBuilder.length() - " OR ".length(), stringBuilder.length() - 1);
		
		builder.where(stringBuilder.toString());
	}
	
	private long getFileId(String filename, ContentResolver resolver) {
		try {
			OrgFile agendaFile = new OrgFile(filename, resolver);
			return agendaFile.id;
		} catch (OrgFileNotFoundException e) { return -1;}
	}
}
