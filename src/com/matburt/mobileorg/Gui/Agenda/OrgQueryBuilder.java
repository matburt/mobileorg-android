package com.matburt.mobileorg.Gui.Agenda;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.matburt.mobileorg.OrgData.OrgContract.OrgData;
import com.matburt.mobileorg.OrgData.OrgDatabase.Tables;
import com.matburt.mobileorg.OrgData.OrgFile;
import com.matburt.mobileorg.OrgData.OrgProviderUtils;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.OrgData.OrgNodeDate;
import com.matburt.mobileorg.OrgData.OrgNodePayload;
import com.matburt.mobileorg.util.OrgFileNotFoundException;
import com.matburt.mobileorg.util.OrgNodeNotFoundException;
import com.matburt.mobileorg.util.SelectionBuilder;

public class OrgQueryBuilder implements Serializable {
	private static final long serialVersionUID = 2;

	public enum Type {
		ALL, AGENDA;
	}
	public Type type = Type.ALL;
	
	public String title = "";
	
	public String span = "Day";
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
	
	/**
	 * Returns an array of DB IDs for the nodes matching the query.
	 */
	public long[] getNodes(SQLiteDatabase db, Context context) {
		long[] result = null;
		
		Cursor cursor = getQuery(context).query(db, OrgData.DEFAULT_COLUMNS, OrgData.DEFAULT_SORT);
		ContentResolver resolver = context.getContentResolver();
		
		result = new long[cursor.getCount()];
		cursor.moveToFirst();
		
		int i = 0;
		while(cursor.isAfterLast() == false) {
			long id = cursor.getLong(cursor.getColumnIndex(OrgData.ID));
			switch (type) {
			case ALL:
				result[i++] = id;
				break;
			case AGENDA:
				try {
					OrgNode node = new OrgNode(id, resolver);

					if (matchesAgendaQuery(node)) {
						result[i++] = id;
					}
				} catch (OrgNodeNotFoundException e) {
				}
				break;
			}
			cursor.moveToNext();
		}
		cursor.close();
		return Arrays.copyOf(result, i);
	}

	// http://stackoverflow.com/a/237204/165039
	private static boolean isInteger(String str) {
		if (str == null) {
			return false;
		}
		int length = str.length();
		if (length == 0) {
			return false;
		}
		int i = 0;
		if (str.charAt(0) == '-') {
			if (length == 1) {
				return false;
			}
			i = 1;
		}
		for (; i < length; i++) {
			char c = str.charAt(i);
			if (c <= '/' || c >= ':') {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns the number of days from the start date.
	 */
	private int spanToNDays(Calendar startDay) {
		if (isInteger(span)) {
			return Integer.parseInt(span);
		} else if (span.equalsIgnoreCase("Day")) {
			return 1;
		} else if (span.equalsIgnoreCase("Week")) {
			return 7;
		} else if (span.equalsIgnoreCase("Fortnight")) {
			return 14;
		} else if (span.equalsIgnoreCase("Month")) {
			int daysInMonth = startDay.getActualMaximum(Calendar.DAY_OF_MONTH);
			return startDay.get(Calendar.DAY_OF_MONTH) - daysInMonth;
		} else if (span.equalsIgnoreCase("Year")) {
			int daysInYear = startDay.getActualMaximum(Calendar.DAY_OF_YEAR);
			return startDay.get(Calendar.DAY_OF_YEAR) - daysInYear;
		}
		return 1;
	}

	private static OrgNodeDate getDate(String dateStr) {
		try {
			return new OrgNodeDate(dateStr);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	/**
	 * Returns whether the node has a date in the half-open interval.
	 */
	private static boolean hasDateTimeInRange(OrgNode node,
	                                          Calendar start, Calendar end) {
		OrgNodePayload payload = node.getOrgNodePayload();
		long startTime = start.getTimeInMillis();
		long endTime = end.getTimeInMillis();
		OrgNodeDate scheduled = getDate(payload.getScheduled());
		OrgNodeDate deadline = getDate(payload.getDeadline());
		OrgNodeDate timestamp = getDate(payload.getTimestamp());

		return ((scheduled != null && scheduled.beginTime < endTime)
		        || (deadline != null && deadline.beginTime < endTime)
		        || (timestamp != null && timestamp.beginTime >= startTime
		            && timestamp.beginTime < endTime));
	}

	/**
	 * Returns whether the node matches the Agenda query criteria.
	 */
	private boolean matchesAgendaQuery(OrgNode node) {
		int nDays;
		Calendar startDay = Calendar.getInstance();
		Calendar nextDay;
		Calendar end;

		if (span.equalsIgnoreCase("Month")) {
			startDay.set(Calendar.DAY_OF_MONTH, 0);
		} else if (span.equalsIgnoreCase("Year")) {
			startDay.set(Calendar.DAY_OF_YEAR, 0);
		}
		nDays = Math.max(1, spanToNDays(startDay));
		nextDay = (Calendar) startDay.clone();
		nextDay.set(Calendar.HOUR_OF_DAY, 0);
		nextDay.set(Calendar.MINUTE, 0);
		nextDay.set(Calendar.SECOND, 0);
		nextDay.set(Calendar.MILLISECOND, 0);
		nextDay.add(Calendar.DATE, 1);
		end = (Calendar) nextDay.clone();
		end.add(Calendar.DATE, nDays);
		return (hasDateTimeInRange(node, startDay, nextDay)
		        || (!node.isHabit() && hasDateTimeInRange(node, nextDay, end)));
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
