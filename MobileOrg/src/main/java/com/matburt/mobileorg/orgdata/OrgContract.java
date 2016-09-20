package com.matburt.mobileorg.orgdata;

import android.net.Uri;

public class OrgContract {
	public static final String CONTENT_AUTHORITY = "com.matburt.mobileorg.orgdata.OrgProvider";
	private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
	private static final String PATH_ORGDATA = OrgDatabase.Tables.ORGDATA;
	private static final String PATH_TIMESTAMPS = OrgDatabase.Tables.TIMESTAMPS;
	private static final String PATH_TODOS = OrgDatabase.Tables.TODOS;
	private static final String PATH_TAGS = OrgDatabase.Tables.TAGS;
	private static final String PATH_PRIORITIES = OrgDatabase.Tables.PRIORITIES;
	private static final String PATH_FILES = OrgDatabase.Tables.FILES;
	private static final String PATH_SEARCH = "search";
	static public long TODO_ID = -2;
	static public long AGENDA_ID = -3;
	public static String NODE_ID = "node_id";
	public static String PARENT_ID = "parent_id";
	public static String POSITION = "position";

	/**
	 * @param tableName
	 * @param columns
	 * @return the list of column prefixed by the table name and separated by a ","
	 */
	public static String formatColumns(String tableName, String[] columns) {
		String result = "";
		for (String column : columns)
			result += tableName + "." + column + ", ";
		result = result.substring(0, result.length() - 2); // removing the last ", "
		return result;
	}
	interface TimestampsColumns {
		String NODE_ID = "node_id";
		String FILE_ID = "file_id";
		String TYPE = "type";
		String TIMESTAMP = "timestamp";
		String ALL_DAY = "all_day";
	}

	interface OrgDataColumns {
		String ID = "_id";
		String NAME = "name";
		String TODO = "todo";
		String PARENT_ID = "parent_id";
		String FILE_ID = "file_id";
		String LEVEL = "level";
		String POSITION = "position";
		String PRIORITY = "priority";
		String TAGS = "tags";
		String TAGS_INHERITED = "tags_inherited";
		String PAYLOAD = "payload";
		String DEADLINE = "deadline";
		String SCHEDULED = "scheduled";
		String DEADLINE_DATE_ONLY = "deadline_date_only";
		String SCHEDULED_DATE_ONLY = "scheduled_date_only";
	}
	interface FilesColumns {
		String ID = "_id";
		String NAME = "name";
		String FILENAME = "filename";
		String COMMENT = "comment";
		String NODE_ID = "node_id";
	}
	interface TodosColumns {
		String ID = "_id";
		String NAME = "name";
		String GROUP = "todogroup";
		String ISDONE = "isdone";
	}
	interface TagsColumns {
		String ID = "_id";
		String NAME = "name";
		String GROUP = "taggroup";
	}

	interface PrioritiesColumns {
		String ID = "_id";
		String NAME = "name";
	}
	
	public static class OrgData implements OrgDataColumns {
		public static final Uri CONTENT_URI =
				BASE_CONTENT_URI.buildUpon().appendPath(PATH_ORGDATA).build();

		public static final Uri CONTENT_URI_TODOS =
				BASE_CONTENT_URI.buildUpon().appendPath(PATH_TODOS).build();
		public static final String DEFAULT_SORT = ID + " ASC";
		public static final String NAME_SORT = NAME + " ASC";
		public static final String POSITION_SORT = POSITION + " ASC";
		public static final String[] DEFAULT_COLUMNS = {ID, NAME, TODO, TAGS, TAGS_INHERITED,
				PARENT_ID, PAYLOAD, LEVEL, PRIORITY, FILE_ID, POSITION, SCHEDULED, SCHEDULED_DATE_ONLY, DEADLINE, DEADLINE_DATE_ONLY};

		public static String getId(Uri uri) {
			return uri.getPathSegments().get(1);
		}

		public static Uri buildIdUri(String id) {
			return CONTENT_URI.buildUpon().appendPath(id).build();
		}

		public static Uri buildIdUri(Long id) {
			return buildIdUri(id.toString());
		}

		public static Uri buildChildrenUri(String parentId) {
			return CONTENT_URI.buildUpon().appendPath(parentId).appendPath("children").build();
		}

		public static Uri buildChildrenUri(long node_id) {
			return buildChildrenUri(Long.toString(node_id));
		}
	}
	
	public static class Timestamps implements TimestampsColumns {
		public static final Uri CONTENT_URI =
				BASE_CONTENT_URI.buildUpon().appendPath(PATH_TIMESTAMPS).build();
		public static final String[] DEFAULT_COLUMNS = {NODE_ID, FILE_ID, TYPE, TIMESTAMP, ALL_DAY};

		public static Uri buildIdUri(Long id) {
			return CONTENT_URI.buildUpon().appendPath(id.toString()).build();
		}

		public static String getId(Uri uri) {
			return uri.getLastPathSegment();
		}
	}
	
	public static class Files implements FilesColumns {
		public static final Uri CONTENT_URI =
				BASE_CONTENT_URI.buildUpon().appendPath(PATH_FILES).build();

		public static final String[] DEFAULT_COLUMNS = { ID, NAME, FILENAME,
				COMMENT, NODE_ID };
		public static final String DEFAULT_SORT = NAME + " ASC";

		public static String getId(Uri uri) {
			return uri.getLastPathSegment();
		}

		public static String getFilename(Uri uri) {
			return uri.getPathSegments().get(1);
		}

		public static Uri buildFilenameUri(String filename) {
			return CONTENT_URI.buildUpon().appendPath(filename)
					.appendPath("filename").build();
		}

		public static Uri buildIdUri(Long fileId) {
			return CONTENT_URI.buildUpon().appendPath(fileId.toString()).build();
		}
	}
	
	public static class Todos implements TodosColumns {
		public static final Uri CONTENT_URI =
				BASE_CONTENT_URI.buildUpon().appendPath(PATH_TODOS).build();

		public static final String[] DEFAULT_COLUMNS = {NAME, ID, GROUP, ISDONE};
	}
	
	public static class Tags implements TagsColumns {
		public static final Uri CONTENT_URI =
				BASE_CONTENT_URI.buildUpon().appendPath(PATH_TAGS).build();
	}
	
	public static class Priorities implements PrioritiesColumns {
		public static final Uri CONTENT_URI =
				BASE_CONTENT_URI.buildUpon().appendPath(PATH_PRIORITIES).build();
	}

	public static class Search implements OrgDataColumns {
		public static final Uri CONTENT_URI =
				BASE_CONTENT_URI.buildUpon().appendPath(PATH_SEARCH).build();

		public static String getSearchTerm(Uri uri) {
			return uri.getLastPathSegment();
		}
	}
}
