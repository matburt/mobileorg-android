package com.matburt.mobileorg2.OrgData;

import android.content.Context;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import com.matburt.mobileorg2.OrgData.OrgContract.OrgData;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

public class OrgDatabase extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "MobileOrg.db";
	private static final int DATABASE_VERSION = 5;

	private int orgdata_nameColumn;
	private int orgdata_todoColumn;
	private int orgdata_tagsColumn;
	private int orgdata_tagsInheritedColumn;
	private int orgdata_priorityColumn;
	private int orgdata_parentidColumn;
	private int orgdata_fileidColumn;
	private int orgdata_levelColumn;
	private int orgdata_positionColumn;

	private InsertHelper orgdataInsertHelper;
	private SQLiteStatement addPayloadStatement;
	private SQLiteStatement addTimestampsStatement;



	public interface PrioritiesTable{
		String tableName = "priorities";
		String[] id = {"_id", "integer primary key autoincrement"};
		String[] name = {"name", "text"};
	}

	public interface FilesTable{
		String tableName = "files";
		String[] id = {"_id", "integer primary key autoincrement"};
		String[] name = {"name", "text"};
		String[] node_id = {"node_id", "integer"};
		String[] filename = {"filename", "text"};
		String[] comment = {"comment", "text)"};
	}


	public interface TodoTable{
		String tableName = "todos";
		String[] id = {"_id", "integer primary key autoincrement"};
		String[] name = {"name", "text"};
		String[] todogroup = {"todogroup", "integer"};
		String[] isdone = {"isdone", "integer", "default 0"};
		String[] todoConstrain = {"UNIQUE(todogroup, name) ON CONFLICT IGNORE"};
	}

	public interface TagsTable{
		String tableName = "files";
		String[] id = {"_id", "integer primary key autoincrement"};
		String[] name = {"name", "text"};
		String[] taggroup = {"taggroup", "integer"};
	}

	public interface EditsTable{
		String tableName = "edits";
		String[] id = {"_id", "integer primary key autoincrement"};
		String[] type = {"type", "text"};
		String[] title = {"title", "text"};
		String[] data_id = {"data_id", "integer"};
		String[] old_value = {"old_value", "text"};
		String[] new_value = {"new_value", "text"};
		String[] changed = {"changed", "integer)"};
	}

	public interface OrgDataTable{
		String tableName = "orgdata";
		String[] id = {"_id", "integer primary key autoincrement"};
		String[] name = {"name", "text"};
		String[] parent_id = {"parent_id", "integer", "default -1"};
		String[] file_id = {"file_id", "integer"};
		String[] level = {"level", "integer", "default 0"};
		String[] priority = {"priority", "text"};
		String[] todo = {"todo", "text"};
		String[] tags = {"tags", "text"};
		String[] tags_inherited = {"tags_inherited", "text"};
		String[] payload = {"payload", "text"};
		String[] position = {"position", "integer"};
		String[] scheduled = {"scheduled", "integer", "default -1"};
		String[] deadline = {"deadline", "integer", "default -1"};
	}
	private static OrgDatabase mInstance = null;

	public static OrgDatabase getInstance(Context context){
		if(mInstance == null) mInstance = new OrgDatabase(context);
		return mInstance;
	}

	private OrgDatabase(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.v("swag","megaswag : " + new Table<OrgDatabase>().createDB());

	}




	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		switch (newVersion) {
			case 4:
				db.execSQL("DROP TABLE IF EXISTS priorities");
				db.execSQL("DROP TABLE IF EXISTS files");
				db.execSQL("DROP TABLE IF EXISTS todos");
				db.execSQL("DROP TABLE IF EXISTS edits");
				db.execSQL("DROP TABLE IF EXISTS orgdata");
				break;

			case 5:
				db.execSQL("alter table orgdata add tags_inherited text");
				break;
		}
		onCreate(db);
	}

	public long fastInsertNode(OrgNode node) {
		prepareOrgdataInsert();
		orgdataInsertHelper.bind(orgdata_parentidColumn, node.parentId);
		orgdataInsertHelper.bind(orgdata_nameColumn, node.name);
		orgdataInsertHelper.bind(orgdata_todoColumn, node.todo);
		orgdataInsertHelper.bind(orgdata_priorityColumn, node.priority);
		orgdataInsertHelper.bind(orgdata_fileidColumn, node.fileId);
		orgdataInsertHelper.bind(orgdata_tagsColumn, node.tags);
		orgdataInsertHelper.bind(orgdata_tagsInheritedColumn, node.tags_inherited);
		orgdataInsertHelper.bind(orgdata_levelColumn, node.level);
		orgdataInsertHelper.bind(orgdata_positionColumn, node.position);
		return orgdataInsertHelper.execute();
	}

	public void fastInsertNodePayload(Long id, final String payload, final HashMap<OrgNodeTimeDate.TYPE, Long> timestamps) {
		if (addPayloadStatement == null)
			addPayloadStatement = getWritableDatabase()
					.compileStatement("UPDATE orgdata SET payload=?, scheduled=?, deadline=? WHERE _id=?");

		addPayloadStatement.bindString(1, payload);
		addPayloadStatement.bindLong(2, timestamps.get(OrgNodeTimeDate.TYPE.Scheduled)!=null ? timestamps.get(OrgNodeTimeDate.TYPE.Scheduled): -1);
		addPayloadStatement.bindLong(3, timestamps.get(OrgNodeTimeDate.TYPE.Deadline)!=null ? timestamps.get(OrgNodeTimeDate.TYPE.Deadline): -1);
		addPayloadStatement.bindLong(4, id);
		addPayloadStatement.execute();
	}



	private void prepareOrgdataInsert() {
//		if(this.orgdataInsertHelper == null) {
//			this.orgdataInsertHelper = new InsertHelper(getWritableDatabase(), Tables.ORGDATA);
//			this.orgdata_nameColumn = orgdataInsertHelper.getColumnIndex(OrgData.NAME);
//			this.orgdata_todoColumn = orgdataInsertHelper.getColumnIndex(OrgData.TODO);
//			this.orgdata_priorityColumn = orgdataInsertHelper.getColumnIndex(OrgData.PRIORITY);
//			this.orgdata_parentidColumn = orgdataInsertHelper.getColumnIndex(OrgData.PARENT_ID);
//			this.orgdata_fileidColumn = orgdataInsertHelper.getColumnIndex(OrgData.FILE_ID);
//			this.orgdata_tagsColumn = orgdataInsertHelper.getColumnIndex(OrgData.TAGS);
//			this.orgdata_tagsInheritedColumn = orgdataInsertHelper.getColumnIndex(OrgData.TAGS_INHERITED);
//			this.orgdata_levelColumn = orgdataInsertHelper.getColumnIndex(OrgData.LEVEL);
//			this.orgdata_positionColumn = orgdataInsertHelper.getColumnIndex(OrgData.POSITION);
//		}
//		orgdataInsertHelper.prepareForInsert();
	}

	public void beginTransaction() {
		getWritableDatabase().beginTransaction();
	}

	public void endTransaction() {
		getWritableDatabase().setTransactionSuccessful();
		getWritableDatabase().endTransaction();
	}
}
