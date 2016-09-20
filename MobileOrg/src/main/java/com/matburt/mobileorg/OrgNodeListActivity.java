package com.matburt.mobileorg;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.matburt.mobileorg.gui.outline.OutlineAdapter;
import com.matburt.mobileorg.gui.SearchActivity;
import com.matburt.mobileorg.gui.wizard.WizardActivity;
import com.matburt.mobileorg.orgdata.OrgFile;
import com.matburt.mobileorg.services.SyncService;
import com.matburt.mobileorg.settings.SettingsActivity;
import com.matburt.mobileorg.synchronizers.AuthData;
import com.matburt.mobileorg.synchronizers.Synchronizer;
import com.matburt.mobileorg.util.OrgUtils;
import com.matburt.mobileorg.util.PreferenceUtils;

import java.io.File;


/**
 * An activity representing a list of OrgNodes. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link OrgNodeDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class OrgNodeListActivity extends AppCompatActivity {

    public final static String NODE_ID = "node_id";
    public final static String SYNC_FAILED = "com.matburt.mobileorg.SYNC_FAILED";
    static boolean passwordPrompt = true;
    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */

    private Long node_id;
    private SynchServiceReceiver syncReceiver;
    private MenuItem synchronizerMenuItem;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orgnode_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        Intent intent = getIntent();
        node_id = intent.getLongExtra(NODE_ID, -1);

        if (this.node_id == -1) displayNewUserDialogs();

        recyclerView = (RecyclerView) findViewById(R.id.orgnode_list);
        assert recyclerView != null;
        setupRecyclerView(recyclerView);

        if (findViewById(R.id.orgnode_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            ((OutlineAdapter) recyclerView.getAdapter()).setHasTwoPanes(true);
        }

        new Style(this);

        this.syncReceiver = new SynchServiceReceiver();
        registerReceiver(this.syncReceiver, new IntentFilter(
                Synchronizer.SYNC_UPDATE));

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null) fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder alert = new AlertDialog.Builder(OrgNodeListActivity.this);

                alert.setTitle(R.string.new_file);
                alert.setMessage(getResources().getString(R.string.filename) + ":");

                // Set an EditText view to get user input
                final EditText input = new EditText(OrgNodeListActivity.this);
                alert.setView(input);

                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String filename = input.getText().toString();
                        OrgFile newFile = new OrgFile(filename, filename);
                        File file = new File(newFile.getFilePath(OrgNodeListActivity.this));
                        if(file.exists()){
                            Toast.makeText(OrgNodeListActivity.this, R.string.file_exists, Toast.LENGTH_SHORT)
                                    .show();
                            return;
                        }
                        newFile.addFile(OrgNodeListActivity.this);
                        ((OutlineAdapter) recyclerView.getAdapter()).refresh();
                        Synchronizer.getInstance().addFile(filename);
                        connect();
                    }
                });

                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                    }
                });

                alert.show();
            }
        });

        connect();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.outline_menu, menu);
        synchronizerMenuItem = menu.findItem(R.id.menu_sync);

        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        MenuItem searchItem = menu.findItem(R.id.menu_search);

        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);

        ComponentName cn = new ComponentName(this, SearchActivity.class);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(cn));
//        searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default

        return true;
    }

    // TODO: Add onSaveInstanceState and onRestoreInstanceState

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                return true;

            case R.id.menu_sync:
                passwordPrompt = true;
                connect();
                return true;

            case R.id.menu_settings:
                runShowSettings(null);
                return true;

            case R.id.menu_search:
                onSearchRequested();
                return true;

            case R.id.menu_help:
                runHelp(null);
                return true;
        }
        return false;
    }

    public void runHelp(View view) {
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://github.com/matburt/mobileorg-android/wiki"));
        startActivity(intent);
    }

    public void runSynchronize() {
        startService(new Intent(this, SyncService.class));
    }

    public void runShowSettings(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    public void runShowWizard(View view) {
        startActivity(new Intent(this, WizardActivity.class));
    }

    private boolean runSearch() {
        return onSearchRequested();
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        recyclerView.setAdapter(new OutlineAdapter(this));
    }

    private void showUpgradePopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(OrgUtils.getStringFromResource(R.raw.upgrade, this));
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        builder.create().show();
    }


    private void displayNewUserDialogs() {
        if (!PreferenceUtils.isSyncConfigured())
            runShowWizard(null);

        if (PreferenceUtils.isUpgradedVersion())
            showUpgradePopup();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(this.syncReceiver);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        runSynchronize();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent.getAction().equals(SYNC_FAILED)) {
            Bundle extrasBundle = intent.getExtras();
            String errorMsg = extrasBundle.getString("ERROR_MESSAGE");
            showSyncFailPopup(errorMsg);
        }
    }

    private void showSyncFailPopup(String errorMsg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(errorMsg);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        builder.create().show();
    }

    /**
     * Run the synchronization if credentials are stored or prompt for credentials
     * if it has not yet been prompt
     */
    private void connect() {
        if (Synchronizer.getInstance().isCredentialsRequired() && passwordPrompt) {
            final AlertDialog.Builder alert = new AlertDialog.Builder(OrgNodeListActivity.this);
            final AuthData authData = AuthData.getInstance(OrgNodeListActivity.this);
            alert.setTitle(R.string.prompt_enter_password);

            // Set an EditText view to get user input
            final EditText input = new EditText(OrgNodeListActivity.this);

            input.setInputType(InputType.TYPE_CLASS_TEXT);
            alert.setView(input);
            input.setText(authData.getPassword());

            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    passwordPrompt = false;
                    runSynchronize();
                    dialog.dismiss();
                }
            });

            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    passwordPrompt = false;
                    dialog.dismiss();
                }
            });

            alert.show();
        } else {
            runSynchronize();
        }
    }

    private class SynchServiceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean syncStart = intent.getBooleanExtra(Synchronizer.SYNC_START, false);
            boolean syncDone = intent.getBooleanExtra(Synchronizer.SYNC_DONE, false);
            int progress = intent.getIntExtra(Synchronizer.SYNC_PROGRESS_UPDATE, -1);

            if (syncStart) {
                if (synchronizerMenuItem != null)
                    synchronizerMenuItem.setVisible(false);
            } else if (syncDone) {
                ((OutlineAdapter) recyclerView.getAdapter()).refresh();
                if (synchronizerMenuItem != null) synchronizerMenuItem.setVisible(true);

            } else if (progress >= 0 && progress <= 100) {
//                int normalizedProgress = (Window.PROGRESS_END - Window.PROGRESS_START) / 100 * progress;
            }
        }
    }
}
