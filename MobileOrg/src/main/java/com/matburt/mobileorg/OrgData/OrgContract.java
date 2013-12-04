package com.matburt.mobileorg.OrgData;

import android.net.Uri;

public class OrgContract {
	
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
		String TAGS_INHERITED = "tags_inherited";
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

	public static final String CONTENT_AUTHORITY = "com.matburt.mobileorg.OrgData.OrgProvider";
	private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

	private static final String PATH_ORGDATA = "orgdata";
	private static final String PATH_EDITS = "edits";
	private static final String PATH_TODOS = "todos";
	private static final String PATH_TAGS = "tags";
	private static final String PATH_PRIORITIES = "priorities";
	private static final String PATH_FILES = "files";
	private static final String PATH_SEARCH = "search";
	
	
	public static class OrgData implements OrgDataColumns {
		public static final Uri CONTENT_URI = 
				BASE_CONTENT_URI.buildUpon().appendPath(PATH_ORGDATA).build();
		
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
		public static final String DEFAULT_SORT = ID + " ASC";
		public static final String NAME_SORT = NAME + " ASC";

		
		public static final String[] DEFAULT_COLUMNS = { ID, NAME, TODO, TAGS, TAGS_INHERITED,
				PARENT_ID, PAYLOAD, LEVEL, PRIORITY, FILE_ID };
	}
	
	public static class Edits implements EditsColumns {
		public static final Uri CONTENT_URI = 
				BASE_CONTENT_URI.buildUpon().appendPath(PATH_EDITS).build();
		
		public static final String[] DEFAULT_COLUMNS = { ID, DATA_ID, TITLE,
				TYPE, OLD_VALUE, NEW_VALUE };
		
		public static String getId(Uri uri) {
			return uri.getPathSegments().get(1);
		}
		
		public static Uri buildIdUri(String id) {
			return CONTENT_URI.buildUpon().appendPath(id).build();
		}
		
		public static Uri buildIdUri(Long id) {
			return buildIdUri(id.toString());
		}
	}
	
	public static class Files implements FilesColumns {
		public static final Uri CONTENT_URI = 
				BASE_CONTENT_URI.buildUpon().appendPath(PATH_FILES).build();

		public static final String[] DEFAULT_COLUMNS = { ID, NAME, FILENAME,
				CHECKSUM, NODE_ID };
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
