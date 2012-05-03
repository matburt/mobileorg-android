package com.matburt.mobileorg.provider;

import android.net.Uri;

public class OrgContracts {
	
	interface EditsColumns {
		String ID = "_id";
		String TYPE = "type";
		String DATA_ID = "data_id";
		String TITLE = "title";
		String OLD_VALUE = "old_value";
		String NEW_VALUE = "new_value";
	}
	
	interface OrgDataColumns {
		String ID = "_id";
		String NAME = "name";
		String TODO = "todo";
		String PARENT_ID = "parent_id";
		String FILE_ID = "file_id";
		String LEVEL = "level";
		String PRIORITY = "priority";
		String TAGS = "tags";
		String PAYLOAD = "payload";
	}
	
	interface FilesColumns {
		String ID = "_id";
		String NAME = "name";
		String FILENAME = "filename";
		String CHECKSUM = "checksum";
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

	public static final String CONTENT_AUTHORITY = "com.matburt.mobileorg.provider.OrgContentProvider";
	private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

	private static final String PATH_ORGDATA = "orgdata";
	private static final String PATH_EDITS = "edits";
	private static final String PATH_TODOS = "todos";
	private static final String PATH_TAGS = "tags";
	private static final String PATH_PRIORITIES = "priorities";
	private static final String PATH_FILES = "files";
	
	
	public static class OrgData implements OrgDataColumns {
		public static final Uri CONTENT_URI = 
				BASE_CONTENT_URI.buildUpon().appendPath(PATH_ORGDATA).build();
		
		public static String getId(Uri uri) {
			return uri.getPathSegments().get(1);
		}
	}
	
	public static class Edits implements EditsColumns {
		public static final Uri CONTENT_URI = 
				BASE_CONTENT_URI.buildUpon().appendPath(PATH_EDITS).build();
	}
	
	public static class Files implements FilesColumns {
		public static final Uri CONTENT_URI = 
				BASE_CONTENT_URI.buildUpon().appendPath(PATH_FILES).build();
	}
	
	public static class Todos implements TodosColumns {
		public static final Uri CONTENT_URI = 
				BASE_CONTENT_URI.buildUpon().appendPath(PATH_TODOS).build();
	}
	
	public static class Tags implements TagsColumns {
		public static final Uri CONTENT_URI = 
				BASE_CONTENT_URI.buildUpon().appendPath(PATH_TAGS).build();
	}
	
	public static class Priorities implements PrioritiesColumns {
		public static final Uri CONTENT_URI = 
				BASE_CONTENT_URI.buildUpon().appendPath(PATH_PRIORITIES).build();
	}
}
