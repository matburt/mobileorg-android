package com.matburt.mobileorg;

import android.app.ListActivity;
import android.app.Application;
import android.app.ProgressDialog;
import android.app.AlertDialog;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.AdapterView;
import android.content.Intent;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import java.util.ArrayList;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.lang.Runnable;
import java.io.BufferedReader;
import java.io.StringReader;
import java.io.File;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.content.SharedPreferences;

public class MobileOrgActivity extends ListActivity
{
    private static class OrgViewAdapter extends BaseAdapter {

        public Node topNode;
        public Node thisNode;
        public ArrayList<Integer> nodeSelection;
        public ArrayList<EditNode> edits = new ArrayList<EditNode>();
        private Context context;
        private LayoutInflater lInflator;

        public OrgViewAdapter(Context context, Node ndx,
                              ArrayList<Integer> selection,
                              ArrayList<EditNode> edits) {
            this.topNode = ndx;
            this.thisNode = ndx;
            this.lInflator = LayoutInflater.from(context);
            this.nodeSelection = selection;
            this.edits = edits;
            this.context = context;
            if (selection != null) {
                for (int idx = 0; idx < selection.size(); idx++) {
                    try {
                        this.thisNode = this.thisNode.subNodes.get(
                                            selection.get(idx));
                    }
                    catch (IndexOutOfBoundsException e) {
                        Log.d("MobileOrg", "IndexOutOfBounds on selection " +
                              selection.get(idx).toString() + " in node " +
                              this.thisNode.nodeName);
                        return;
                    }
                }
            }
        }

        public int getCount() {
            if (this.thisNode == null ||
                this.thisNode.subNodes == null)
                return 0;
            return this.thisNode.subNodes.size();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public EditNode findEdit(String nodeId) {
            if (this.edits == null)
                return null;
            for (int idx = 0 ; idx < this.edits.size(); idx++)
                {
                    if (this.edits.get(idx).nodeId.equals(nodeId)) {
                        return this.edits.get(idx);
                    }
                }
            return null;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = this.lInflator.inflate(R.layout.main, null);
            }
            TextView thisView = (TextView)convertView.findViewById(R.id.orgItem);
            TextView todoView = (TextView)convertView.findViewById(R.id.todoState);
            TextView priorityView = (TextView)convertView.findViewById(R.id.priorityState);
            LinearLayout tagsLayout = (LinearLayout)convertView.findViewById(R.id.tagsLayout);
            TextView dateView = (TextView)convertView.findViewById(R.id.dateInfo);
            EditNode thisEdit = this.findEdit(this.thisNode.subNodes.get(position).getProperty("ID"));
            String todo = this.thisNode.subNodes.get(position).todo;
            String priority = this.thisNode.subNodes.get(position).priority;
            String dateInfo = "";
            thisView.setText(this.thisNode.subNodes.get(position).nodeName);

            if (thisEdit != null) {
                if (thisEdit.editType.equals("todo"))
                    todo = thisEdit.newVal;
                else if (thisEdit.editType.equals("priority"))
                    priority = thisEdit.newVal;
                else if (thisEdit.editType.equals("heading"))
                    thisView.setText(thisEdit.newVal);
            }

            SimpleDateFormat formatter = new SimpleDateFormat("<yyyy-MM-dd EEE>");
            if (this.thisNode.subNodes.get(position).deadline != null) {
                dateInfo += "DEADLINE: " + formatter.format(
                                this.thisNode.subNodes.get(position).deadline) + " ";
            }
            
            if (this.thisNode.subNodes.get(position).schedule != null) {
                dateInfo += "SCHEDULED: " + formatter.format(
                                this.thisNode.subNodes.get(position).schedule) + " ";
            }

            for (String tag : this.thisNode.subNodes.get(position).tags) {
				TextView tagView = new TextView(this.context);
				tagView.setText(tag);
                tagView.setTextColor(Color.LTGRAY);
				tagView.setPadding(0, 0, 5, 0);
				tagsLayout.addView(tagView);
			}

            if (TextUtils.isEmpty(todo)) {
            	todoView.setVisibility(View.GONE);
            }
            else {
            	todoView.setText(todo);
            	todoView.setVisibility(View.VISIBLE);
            }

            if (TextUtils.isEmpty(priority)) {
                priorityView.setVisibility(View.GONE);
            }
            else {
                priorityView.setText(priority);
                priorityView.setVisibility(View.VISIBLE);
            }

            if (TextUtils.isEmpty(dateInfo)) {
                dateView.setVisibility(View.GONE);
            }
            else {
                dateView.setText(dateInfo);
                dateView.setVisibility(View.VISIBLE);
            }

            convertView.setTag(thisView);
            return convertView;
        }
    }

    private static final int OP_MENU_SETTINGS = 1;
    private static final int OP_MENU_SYNC = 2;
    private static final int OP_MENU_OUTLINE = 3;
    private static final int OP_MENU_CAPTURE = 4;
    private static final String LT = "MobileOrg";
    private ProgressDialog syncDialog;
    private MobileOrgDatabase appdb;
    private ReportableError syncError;
    public SharedPreferences appSettings;
    final Handler syncHandler = new Handler();
    final Runnable syncUpdateResults = new Runnable() {
        public void run() {
            postSynchronize();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        ListView lv = this.getListView();
        this.appdb = new MobileOrgDatabase((Context)this);
        appSettings = PreferenceManager.getDefaultSharedPreferences(
                                       getBaseContext());
        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener
                                      (){
                    public boolean onItemLongClick(AdapterView<?> av, View v,
                                                   int pos, long id) {
                    onLongListItemClick(v,pos,id);
                    return true;
                }
            });
        if (this.appSettings.getString("syncSource","").equals("") ||
            (this.appSettings.getString("syncSource","").equals("webdav") &&
             this.appSettings.getString("webUrl","").equals("")) ||
            (this.appSettings.getString("syncSource","").equals("sdcard") &&
             this.appSettings.getString("indexFilePath","").equals(""))) {
            this.onShowSettings();
        }
    }

    @Override
    public void onDestroy() {
        this.appdb.close();
        super.onDestroy();
    }

    public void runParser() {
        MobileOrgApplication appInst = (MobileOrgApplication)this.getApplication();
        ArrayList<String> allOrgList = this.appdb.getOrgFiles();
        String storageMode = this.getStorageLocation();
        String userSynchro = this.appSettings.getString("syncSource","");
        String orgBasePath = "";

        if (userSynchro.equals("sdcard")) {
            String indexFile = this.appSettings.getString("indexFilePath","");
            File fIndexFile = new File(indexFile);
            orgBasePath = fIndexFile.getParent() + "/";
        }
        else {
            orgBasePath = "/sdcard/mobileorg/";
        }

        OrgFileParser ofp = new OrgFileParser(allOrgList,
                                              storageMode,
                                              this.appdb,
                                              orgBasePath);
        try {
        	ofp.parse();
        	appInst.rootNode = ofp.rootNode;
            appInst.edits = ofp.parseEdits();
        }
        catch(Throwable e) {
        	ErrorReporter.displayError(this, "An error occurred during parsing: " + e.toString());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        MobileOrgApplication appInst = (MobileOrgApplication)this.getApplication();
        if (appInst.rootNode == null) {
            this.runParser();
        }

        Intent nodeIntent = getIntent();
        appInst.nodeSelection = nodeIntent.getIntegerArrayListExtra("nodePath");
        this.setListAdapter(new OrgViewAdapter(this,
                                               appInst.rootNode,
                                               appInst.nodeSelection,
                                               appInst.edits));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MobileOrgActivity.OP_MENU_OUTLINE, 0, R.string.menu_outline);
        menu.add(0, MobileOrgActivity.OP_MENU_CAPTURE, 0, R.string.menu_capture);
        menu.add(0, MobileOrgActivity.OP_MENU_SYNC, 0, R.string.menu_sync);
        menu.add(0, MobileOrgActivity.OP_MENU_SETTINGS, 0, R.string.menu_settings);
        return true;
    }

    protected void onLongListItemClick(View av, int pos, long id) {
        Intent dispIntent = new Intent();
        MobileOrgApplication appInst = (MobileOrgApplication)this.getApplication();
        dispIntent.setClassName("com.matburt.mobileorg",
                                "com.matburt.mobileorg.OrgContextMenu");

        appInst.pushSelection(pos);

        dispIntent.putIntegerArrayListExtra("nodePath", appInst.nodeSelection);
        startActivity(dispIntent);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        MobileOrgApplication appInst = (MobileOrgApplication)this.getApplication();

        appInst.pushSelection(position);
        Node thisNode = appInst.getSelectedNode();

        if(thisNode.encrypted && !thisNode.parsed)
        {
            //if suitable APG version is installed
            if(Encryption.isAvailable((Context)this))
            {
                //retrieve the encrypted file data
                String userSynchro = this.appSettings.getString("syncSource","");
                String orgBasePath = "";
                if (userSynchro.equals("sdcard")) {
                    String indexFile = this.appSettings.getString("indexFilePath","");
                    File fIndexFile = new File(indexFile);
                    orgBasePath = fIndexFile.getParent() + "/";
                }
                else {
                    orgBasePath = "/sdcard/mobileorg/";
                }

                byte[] rawData = OrgFileParser.getRawFileData(orgBasePath, thisNode.nodeName);
                //and send it to APG for decryption
                Encryption.decrypt(this, rawData);
            }
            else
            {
                appInst.popSelection();
            }
            return;
        }

        if (thisNode.subNodes.size() < 1) {
            appInst.popSelection();
            Intent textIntent = new Intent();

            String docBuffer = thisNode.nodeName + "\n\n" +
                thisNode.nodePayload;
            textIntent.setClassName("com.matburt.mobileorg",
                                    "com.matburt.mobileorg.SimpleTextDisplay");
            textIntent.putExtra("txtValue", docBuffer);
            startActivity(textIntent);
        }
        else {
            expandSelection(appInst.nodeSelection);
        }
    }

    public void expandSelection(ArrayList<Integer> selection)
    {
        Intent dispIntent = new Intent();
        dispIntent.setClassName("com.matburt.mobileorg",
                                "com.matburt.mobileorg.MobileOrgActivity");
        dispIntent.putIntegerArrayListExtra("nodePath", selection);
        startActivityForResult(dispIntent, 1);        
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        MobileOrgApplication appInst = (MobileOrgApplication)this.getApplication();
        if (requestCode == 3) {
            this.runParser();
        }
        else if(requestCode == Encryption.DECRYPT_MESSAGE)
        {
            if (resultCode != Activity.RESULT_OK || data == null)
            {
                appInst.popSelection();
                return;
            }
            
            Node thisNode = appInst.getSelectedNode();
            String userSynchro = this.appSettings.getString("syncSource","");
            String orgBasePath = "";
            if (userSynchro.equals("sdcard")) {
                String indexFile = this.appSettings.getString("indexFilePath","");
                File fIndexFile = new File(indexFile);
                orgBasePath = fIndexFile.getParent() + "/";
            }
            else {
                orgBasePath = "/sdcard/mobileorg/";
            }
            String decryptedData = data.getStringExtra(Encryption.EXTRA_DECRYPTED_MESSAGE);
            OrgFileParser ofp = new OrgFileParser(appdb.getOrgFiles(),
                                                  getStorageLocation(),
                                                  appdb,
                                                  orgBasePath);

            ofp.parse(thisNode, new BufferedReader(new StringReader(decryptedData)));
            expandSelection(appInst.nodeSelection);
        }
        else {
            appInst.popSelection();
        }
    }

    public boolean onShowSettings() {
        Intent settingsIntent = new Intent();
        settingsIntent.setClassName("com.matburt.mobileorg",
                                    "com.matburt.mobileorg.SettingsActivity");
        startActivity(settingsIntent);
        return true;
    }

    public void runSynchronizer() {
        String userSynchro = this.appSettings.getString("syncSource","");
        final Synchronizer appSync;
        if (userSynchro.equals("webdav")) {
            appSync = new WebDAVSynchronizer(this);
        }
        else if (userSynchro.equals("sdcard")) {
            appSync = new SDCardSynchronizer(this);
        }
        else {
            this.onShowSettings();
            return;
        }
        Thread syncThread = new Thread() {
                public void run() {
                	try {
                		syncError = null;
	                    appSync.pull();
	                    appSync.push();
                	}
                	catch(ReportableError e) {
                		syncError = e;
                	}
                    finally {
                        appSync.close();
                    }
                    syncHandler.post(syncUpdateResults);
            }
        };
        syncThread.start();
        
        syncDialog = ProgressDialog.show(this, "",getString(R.string.sync_wait), true);
    }

    public boolean runCapture() {
        Intent captureIntent = new Intent();
        captureIntent.setClassName("com.matburt.mobileorg",
                                   "com.matburt.mobileorg.Capture");
        startActivityForResult(captureIntent, 3);
        return true;
    }

    public void postSynchronize() {
        syncDialog.dismiss();
        if(this.syncError != null) {
            ErrorReporter.displayError(this, this.syncError);
        }
        else {
            this.runParser();
            this.onResume();
        }
    }

    /* Handles item selections */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MobileOrgActivity.OP_MENU_SYNC:
            this.runSynchronizer();
            return true;
        case MobileOrgActivity.OP_MENU_SETTINGS:
            return this.onShowSettings();
        case MobileOrgActivity.OP_MENU_OUTLINE:
            return true;
        case MobileOrgActivity.OP_MENU_CAPTURE:
            return this.runCapture();
        }
        return false;
    }

    public String getStorageLocation() {
        return this.appSettings.getString("storageMode", "");
    }
}
