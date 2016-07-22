package com.matburt.mobileorg2.OrgData;

import android.content.ContentValues;
import android.content.Context;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.support.v4.util.Pair;
import android.util.Log;

import com.matburt.mobileorg2.OrgData.OrgContract.OrgData;

import java.util.HashMap;

public class OrgDatabase extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "MobileOrg.db";
	private static final int DATABASE_VERSION = 5;
	private static OrgDatabase mInstance = null;
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

	private OrgDatabase(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	/**
	 * Open the database at startup
	 *
	 * @param context
	 */
	static public void startDB(Context context) {
		Log.v("trace", "db started");
		mInstance = new OrgDatabase(context);
		Log.v("trace", "instance : " + mInstance);
	}

	public static OrgDatabase getInstance(){
		return mInstance;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE IF NOT EXISTS files("
				+ "_id integer primary key autoincrement,"
				+ "node_id integer,"
				+ "filename text,"
				+ "name text,"
				+ "comment text)");
		db.execSQL("CREATE TABLE IF NOT EXISTS todos("
				+ "_id integer primary key autoincrement,"
				+ "todogroup integer,"
				+ "name text,"
				+ "isdone integer default 0,"
				+ "UNIQUE(todogroup, name) ON CONFLICT IGNORE)");
		db.execSQL("CREATE TABLE IF NOT EXISTS priorities("
				+ "_id integer primary key autoincrement,"
				+ "name text)");
		db.execSQL("CREATE TABLE IF NOT EXISTS tags("
				+ "_id integer primary key autoincrement,"
				+ "taggroup integer,"
				+ "name text)");
		db.execSQL("CREATE TABLE IF NOT EXISTS edits("
				+ "_id integer primary key autoincrement,"
				+ "type text,"
				+ "title text,"
				+ "data_id integer,"
				+ "old_value text,"
				+ "new_value text,"
				+ "changed integer)");
		db.execSQL("CREATE TABLE IF NOT EXISTS orgdata ("
				+ "_id integer primary key autoincrement,"
				+ "parent_id integer default -1,"
				+ "file_id integer,"
				+ "level integer default 0,"
				+ "priority text,"
				+ "todo text,"
				+ "tags text,"
				+ "tags_inherited text,"
				+ "payload text,"
				+ "name text,"
                + "position integer,"
				+ "scheduled integer default -1,"
				+ "scheduled_date_only integer default 0,"
				+ "deadline integer default -1,"
				+ "deadline_date_only integer default 0)");

		ContentValues values = new ContentValues();
		values.put("_id", "0");
		values.put("todogroup", "0");
		values.put("name", "TODO");
		values.put("isdone","0");
		db.insert("todos", null, values);

		values.put("_id", "1");
		values.put("todogroup", "0");
		values.put("name", "DONE");
		values.put("isdone","1");
		db.insert("todos", null, values);

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

	public void fastInsertNodePayload(Long id, final String payload, final HashMap<OrgNodeTimeDate.TYPE, Pair<Long,Integer>> timestamps) {
		if (addPayloadStatement == null)
			addPayloadStatement = getWritableDatabase()
					.compileStatement("UPDATE orgdata SET payload=?, scheduled=?, deadline=?, scheduled_date_only=?, deadline_date_only=? WHERE _id=?");
		Log.v("time","payload : "+payload);
		Log.v("time","db time : "+timestamps.get(OrgNodeTimeDate.TYPE.Scheduled));

		addPayloadStatement.bindString(1, payload);
		addPayloadStatement.bindLong(2, timestamps.get(OrgNodeTimeDate.TYPE.Scheduled)!=null ? timestamps.get(OrgNodeTimeDate.TYPE.Scheduled).first: -1);
		addPayloadStatement.bindLong(3, timestamps.get(OrgNodeTimeDate.TYPE.Deadline)!=null ? timestamps.get(OrgNodeTimeDate.TYPE.Deadline).first: -1);
		addPayloadStatement.bindLong(4, timestamps.get(OrgNodeTimeDate.TYPE.Scheduled)!=null ? timestamps.get(OrgNodeTimeDate.TYPE.Scheduled).second: -1);
		addPayloadStatement.bindLong(5, timestamps.get(OrgNodeTimeDate.TYPE.Deadline)!=null ? timestamps.get(OrgNodeTimeDate.TYPE.Deadline).second: -1);
		addPayloadStatement.bindLong(6, id);
		addPayloadStatement.execute();
	}

	private void prepareOrgdataInsert() {
		if(this.orgdataInsertHelper == null) {
			this.orgdataInsertHelper = new InsertHelper(getWritableDatabase(), Tables.ORGDATA);
			this.orgdata_nameColumn = orgdataInsertHelper.getColumnIndex(OrgData.NAME);
			this.orgdata_todoColumn = orgdataInsertHelper.getColumnIndex(OrgData.TODO);
			this.orgdata_priorityColumn = orgdataInsertHelper.getColumnIndex(OrgData.PRIORITY);
			this.orgdata_parentidColumn = orgdataInsertHelper.getColumnIndex(OrgData.PARENT_ID);
			this.orgdata_fileidColumn = orgdataInsertHelper.getColumnIndex(OrgData.FILE_ID);
			this.orgdata_tagsColumn = orgdataInsertHelper.getColumnIndex(OrgData.TAGS);
			this.orgdata_tagsInheritedColumn = orgdataInsertHelper.getColumnIndex(OrgData.TAGS_INHERITED);
            this.orgdata_levelColumn = orgdataInsertHelper.getColumnIndex(OrgData.LEVEL);
            this.orgdata_positionColumn = orgdataInsertHelper.getColumnIndex(OrgData.POSITION);
		}
		orgdataInsertHelper.prepareForInsert();
	}

	public void beginTransaction() {
		getWritableDatabase().beginTransaction();
	}

	public void endTransaction() {
		getWritableDatabase().setTransactionSuccessful();
		getWritableDatabase().endTransaction();
	}

	public interface Tables {
		String EDITS = "edits";
		String FILES = "files";
		String PRIORITIES = "priorities";
		String TAGS = "tags";
		String TODOS = "todos";
		String ORGDATA = "orgdata";
	}
}
