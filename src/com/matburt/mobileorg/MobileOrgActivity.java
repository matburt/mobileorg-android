package com.matburt.mobileorg;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import android.app.Activity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View.OnClickListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import com.matburt.mobileorg.Capture.Capture;
import com.matburt.mobileorg.Capture.ViewNodeDetailsActivity;
import com.matburt.mobileorg.Error.ErrorReporter;
import com.matburt.mobileorg.Error.ReportableError;
import com.matburt.mobileorg.Parsing.EditNode;
import com.matburt.mobileorg.Parsing.Node;
import com.matburt.mobileorg.Parsing.OrgFileParser;
import com.matburt.mobileorg.Settings.SettingsActivity;
import com.matburt.mobileorg.Settings.WizardActivity;
import com.matburt.mobileorg.Synchronizers.DropboxSynchronizer;
import com.matburt.mobileorg.Synchronizers.SDCardSynchronizer;
import com.matburt.mobileorg.Synchronizers.Synchronizer;
import com.matburt.mobileorg.Synchronizers.WebDAVSynchronizer;

public class MobileOrgActivity extends ListActivity
{
    private static class OrgViewAdapter extends BaseAdapter {

        public Node topNode;
        public Node thisNode;
        public ArrayList<Integer> nodeSelection;
        public ArrayList<EditNode> edits = new ArrayList<EditNode>();
        public ArrayList<HashMap<String, Integer>> allTodos = 
                             new ArrayList<HashMap<String, Integer>>();
        private Context context;
        private LayoutInflater lInflator;

        public OrgViewAdapter(Context context, Node ndx,
                              ArrayList<Integer> selection,
                              ArrayList<EditNode> edits,
                              ArrayList<HashMap<String, Integer>> allTodos) {
            this.topNode = ndx;
            this.thisNode = ndx;
            this.lInflator = LayoutInflater.from(context);
            this.nodeSelection = selection;
            this.edits = edits;
            this.context = context;
            this.allTodos = allTodos;

            Log.d("MobileOrg"+this,  "startup path="+nodeSelectionStr(selection));
            if (selection != null) {
                for (int idx = 0; idx < selection.size(); idx++) {
                    try {
                        this.thisNode = this.thisNode.subNodes.get(
                                            selection.get(idx));
                    }
                    catch (IndexOutOfBoundsException e) {
                        Log.d("MobileOrg"+this,  "IndexOutOfBounds on selection " +
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

        public ArrayList<EditNode> findEdits(String nodeId) {
            ArrayList<EditNode> thisEdits = new ArrayList<EditNode>();
            if (this.edits == null)
                return thisEdits;
            for (int idx = 0 ; idx < this.edits.size(); idx++)
                {
                    String compareS = "";
                    if (nodeId.indexOf("olp:") == 0)
                        compareS = "olp:" + this.edits.get(idx).nodeId;
                    else
                        compareS = this.edits.get(idx).nodeId;
                    if (compareS.equals(nodeId)) {
                        thisEdits.add(this.edits.get(idx));
                    }
                }
            return thisEdits;
        }

        public Integer findTodoState(String todoItem) {
            for (HashMap<String, Integer> group : this.allTodos) {
                for (String key : group.keySet()) {
                    if (key.equals(todoItem))
                        return group.get(key);
                }
            }
            return 0;
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
            ArrayList<EditNode> thisEdits = this.findEdits(
                                              this.thisNode.subNodes.get(position).nodeId);
            String todo = this.thisNode.subNodes.get(position).todo;
            String priority = this.thisNode.subNodes.get(position).priority;
            String dateInfo = "";
            thisView.setText(this.thisNode.subNodes.get(position).nodeName);

            for (EditNode e : thisEdits) {
                if (e.editType.equals("todo"))
                    todo = e.newVal;
                else if (e.editType.equals("priority"))
                    priority = e.newVal;
                else if (e.editType.equals("heading")) {
                    thisView.setText(e.newVal);
                }
            }

            if (this.thisNode.subNodes.get(position).altNodeTitle != null) {
                thisView.setText(this.thisNode.subNodes.get(position).altNodeTitle);
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

            tagsLayout.removeAllViews();
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
                Integer todoState = this.findTodoState(todo);
                if (todoState > 0)
                    todoView.setBackgroundColor(Color.GREEN);
                else
                    todoView.setBackgroundColor(Color.RED);
                todoView.setTextColor(Color.WHITE);
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

    private static final int RUN_PARSER = 3;

    private static final String LT = "MobileOrg";

    private int displayIndex;
    private ProgressDialog syncDialog;
    private MobileOrgDatabase appdb;
    private ReportableError syncError;
    private ListView lv;
    private Dialog newSetupDialog;
    private boolean newSetupDialog_shown = false;
    public SharedPreferences appSettings;
    final Handler syncHandler = new Handler();
    private ArrayList<Integer> origSelection = null;
    private boolean first = true;

    final Runnable syncUpdateResults = new Runnable() {
        public void run() {
            postSynchronize();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        lv = this.getListView();
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
	//If settings are empty, then app has been launched for the first time 
        if (this.appSettings.getString("syncSource","").equals("") ||
            (this.appSettings.getString("syncSource","").equals("webdav") &&
             this.appSettings.getString("webUrl","").equals("")) ||
            (this.appSettings.getString("syncSource","").equals("sdcard") &&
             this.appSettings.getString("indexFilePath","").equals(""))) {
            this.showWizard();
        }

	//Start the background sync service (if it isn't already) and if we have turned on background sync
        if (this.appSettings.getBoolean("doAutoSync", false)) {
            Intent serviceIntent = new Intent();
            serviceIntent.setAction("com.matburt.mobileorg.SYNC_SERVICE");
            this.startService(serviceIntent);
        }
    }

    @Override
    public void onDestroy() {
        this.appdb.close();
        super.onDestroy();
    }

    public void runParser() {
        MobileOrgApplication appInst = (MobileOrgApplication)this.getApplication();
        HashMap<String, String> allOrgList = this.appdb.getOrgFiles();
        if (allOrgList.isEmpty()) {
            return;
        }
        String storageMode = this.getStorageLocation();
        String userSynchro = this.appSettings.getString("syncSource","");
        String orgBasePath = "";

        if (userSynchro.equals("sdcard")) {
            String indexFile = this.appSettings.getString("indexFilePath","");
            File fIndexFile = new File(indexFile);
            orgBasePath = fIndexFile.getParent() + "/";
        }
        else {
            orgBasePath = Environment.getExternalStorageDirectory().getAbsolutePath() +
                          "/mobileorg/";
        }

        OrgFileParser ofp = new OrgFileParser(allOrgList,
                                              storageMode,
                                              userSynchro,
                                              this.appdb,
                                              orgBasePath);
        try {
        	ofp.parse();
        	appInst.rootNode = ofp.rootNode;
            appInst.edits = ofp.parseEdits();
			Collections.sort(appInst.rootNode.subNodes, Node.comparator);
        }
        catch(Throwable e) {
        	ErrorReporter.displayError(this, "An error occurred during parsing, try re-syncing: " + e.toString());
        }
    }

    public void showNewUserWindow() {
        if (this.newSetupDialog_shown) {
            this.newSetupDialog.cancel();
        }
        newSetupDialog = new Dialog(this);
        newSetupDialog.setContentView(R.layout.empty_main);
        Button syncButton = (Button)newSetupDialog.findViewById(R.id.dialog_run_sync);
        syncButton.setOnClickListener(new OnClickListener(){
                public void onClick(View v){
                    runSynchronizer();
                }});
        Button settingsButton = (Button)newSetupDialog.findViewById(R.id.dialog_show_settings);
        settingsButton.setOnClickListener(new OnClickListener(){
                public void onClick(View v){
                    onShowSettings();
                }});
        newSetupDialog.setTitle("Synchronize Org Files");
        newSetupDialog.show();
        this.newSetupDialog_shown = true;
    }

    @Override
    public void onResume() {
        Log.d("MobileOrg"+this,  "onResume" );
        super.onResume();
        Intent nodeIntent = getIntent();
    	MobileOrgApplication appInst = (MobileOrgApplication) this.getApplication();
        ArrayList<Integer> intentNodePath = nodeIntent.getIntegerArrayListExtra("nodePath");
        if (intentNodePath != null) {
            appInst.nodeSelection = copySelection(intentNodePath);
            nodeIntent.putIntegerArrayListExtra("nodePath", null);
            Log.d("MobileOrg"+this,  "resume first="+first+" had nodePath="+nodeSelectionStr(appInst.nodeSelection));
        }
        else {
            Log.d("MobileOrg"+this,  "resume first="+first+" restoring original selection"+nodeSelectionStr(this.origSelection));
            appInst.nodeSelection = copySelection(this.origSelection);
        }
        Log.d("MobileOrg"+this,  "afteResume appInst.nodeSelection="+nodeSelectionStr(appInst.nodeSelection));

        populateDisplay();
    }

    public void populateDisplay() {
    	MobileOrgApplication appInst = (MobileOrgApplication) this.getApplication();
        if (appInst.rootNode == null) {
            this.runParser();
        }

        HashMap<String, String> allOrgList = this.appdb.getOrgFiles();
        if (allOrgList.isEmpty()) {
            this.showNewUserWindow();
        }
        else if (this.newSetupDialog_shown) {
            newSetupDialog_shown = false;
            newSetupDialog.cancel();
        }

        if (first) {
            this.setListAdapter(new OrgViewAdapter(this,
                                                   appInst.rootNode,
                                                   appInst.nodeSelection,
                                                   appInst.edits,
                                                   this.appdb.getTodos()));
            if (appInst.nodeSelection != null) {
                this.origSelection = copySelection(appInst.nodeSelection);
            }
            else {
                this.origSelection = null;
            }
            Log.d("MobileOrg"+this,  " first redisplay, origSelection="+nodeSelectionStr(this.origSelection));

            getListView().setSelection( displayIndex );
            first = false;
        }
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
        Intent dispIntent = new Intent(this, OrgContextMenu.class);
        MobileOrgApplication appInst = (MobileOrgApplication)this.getApplication();

        appInst.pushSelection(pos);
        dispIntent.putIntegerArrayListExtra("nodePath", appInst.nodeSelection);
        startActivity(dispIntent);
    }

    static private ArrayList<Integer> copySelection(ArrayList<Integer> selection)
    {
        if (selection == null)
            return null;
        else
            return new ArrayList(selection);
    }

    static private String nodeSelectionStr(ArrayList<Integer> nodes)
    {
        if (nodes != null) {
            String tmp = "";

            for (Integer i : nodes) {
                if (tmp.length() > 0)
                    tmp += ",";
                tmp += i;
            }
            return tmp;
        }
        return "null";
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        MobileOrgApplication appInst = (MobileOrgApplication)this.getApplication();

        Log.d("MobileOrg"+this,  "onListItemClick position="+position);
        appInst.pushSelection(position);
        Node thisNode = appInst.getSelectedNode();
        Log.d("MobileOrg"+this, "appInst.nodeSelection="+nodeSelectionStr(appInst.nodeSelection));

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
                    orgBasePath = Environment.getExternalStorageDirectory().getAbsolutePath() +
                                  "/mobileorg/";
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
               displayIndex = appInst.lastIndex();
            Log.d("MobileOrg"+this,  "no subnodes, popped selection, displayIndex="+displayIndex);
            appInst.popSelection();
            if (thisNode.todo.equals("") &&
                thisNode.priority.equals("")) {
                Intent textIntent = new Intent(this, SimpleTextDisplay.class);
                String docBuffer = thisNode.nodeName + "\n\n" +
                    thisNode.nodePayload;

                textIntent.putExtra("txtValue", docBuffer);
                startActivity(textIntent);
            }
            else {
                Intent dispIntent = new Intent(this, ViewNodeDetailsActivity.class);

                dispIntent.putExtra("actionMode", "edit");
                Log.d("MobileOrg"+this, "Before edit appInst.nodeSelection="+nodeSelectionStr(appInst.nodeSelection));
                dispIntent.putIntegerArrayListExtra("nodePath", appInst.nodeSelection);
                Log.d("MobileOrg"+this, "After push appInst.nodeSelection="+nodeSelectionStr(appInst.nodeSelection));
                appInst.pushSelection(position);
                startActivity(dispIntent);
            }
        }
        else {
            expandSelection(appInst.nodeSelection);
        }
    }

    public void expandSelection(ArrayList<Integer> selection)
    {
        Intent dispIntent = new Intent(this, MobileOrgActivity.class);
        dispIntent.putIntegerArrayListExtra("nodePath", selection);
        startActivityForResult(dispIntent, 1);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("MobileOrg"+this,  "onActivityResult");
        MobileOrgApplication appInst = (MobileOrgApplication)this.getApplication();
        if (requestCode == RUN_PARSER) {
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
                orgBasePath = Environment.getExternalStorageDirectory().getAbsolutePath() +
                              "/mobileorg/";
            }
            String decryptedData = data.getStringExtra(Encryption.EXTRA_DECRYPTED_MESSAGE);
            OrgFileParser ofp = new OrgFileParser(appdb.getOrgFiles(),
                                                  getStorageLocation(),
                                                  userSynchro,
                                                  appdb,
                                                  orgBasePath);

            ofp.parse(thisNode, new BufferedReader(new StringReader(decryptedData)));
            expandSelection(appInst.nodeSelection);
        }
        else {
        	displayIndex = appInst.lastIndex();
            appInst.popSelection();
        }
    }

    public boolean onShowSettings() {
        Intent settingsIntent = new Intent(this, SettingsActivity.class);
        startActivity(settingsIntent);
        return true;
    }

    public void showWizard() {
        startActivity(new Intent(this, WizardActivity.class));
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
        else if (userSynchro.equals("dropbox")) {
            appSync = new DropboxSynchronizer(this);
        }
        else {
            this.onShowSettings();
            return;
        }

        if (!appSync.checkReady()) {
            Toast error = Toast.makeText((Context)this, "You have not fully configured the synchronizer.  Make sure you visit the 'Configure Synchronizer Settings' in the Settings menu", Toast.LENGTH_LONG);
            error.show();
            this.onShowSettings();
            return;
        }

        Thread syncThread = new Thread() {
                public void run() {
                	try {
                		syncError = null;
	                    appSync.pull();
	                    appSync.push();
                        Log.d("MobileOrg"+this,  "Finished parsing...");
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
        Intent captureIntent = new Intent(this, Capture.class);
        captureIntent.putExtra("actionMode", "create");
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
        Log.d("MobileOrg"+this,  "onOptionsItemSelected");
        switch (item.getItemId()) {
        case MobileOrgActivity.OP_MENU_SYNC:
            this.runSynchronizer();
            return true;
        case MobileOrgActivity.OP_MENU_SETTINGS:
            return this.onShowSettings();
        case MobileOrgActivity.OP_MENU_OUTLINE:
            Intent dispIntent = new Intent(this, MobileOrgActivity.class);
            dispIntent.putIntegerArrayListExtra( "nodePath", new ArrayList<Integer>() );
            startActivity(dispIntent);
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
