package com.matburt.mobileorg.OrgData;

import android.content.Context;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import com.matburt.mobileorg.OrgData.OrgContract.OrgData;

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
	
	private InsertHelper orgdataInsertHelper;
	private SQLiteStatement addPayloadStatement;
	
	public interface Tables {
		String EDITS = "edits";
		String FILES = "files";
		String PRIORITIES = "priorities";
		String TAGS = "tags";
		String TODOS = "todos";
		String ORGDATA = "orgdata";
	}
	
	public OrgDatabase(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE IF NOT EXISTS files("
				+ "_id integer primary key autoincrement,"
				+ "node_id integer,"
				+ "filename text,"
				+ "name text,"
				+ "checksum text)");
		db.execSQL("CREATE TABLE IF NOT EXISTS todos("
				+ "_id integer primary key autoincrement,"
				+ "todogroup integer,"
				+ "name text,"
				+ "isdone integer default 0)");
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
				+ "name text)");
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
		return orgdataInsertHelper.execute();
	}
		
	public void fastInsertNodePayload(Long id, final String payload) {
		if(addPayloadStatement == null)
			addPayloadStatement = getWritableDatabase()
					.compileStatement("UPDATE orgdata SET payload=? WHERE _id=?");
		
		addPayloadStatement.bindString(1, payload);
		addPayloadStatement.bindLong(2, id);
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
}
