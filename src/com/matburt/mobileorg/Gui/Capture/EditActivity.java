package com.matburt.mobileorg.Gui.Capture;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.view.WindowManager;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.actionbarsherlock.view.Window;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Services.TimeclockService;
import com.matburt.mobileorg.Synchronizers.Synchronizer;
import com.matburt.mobileorg.provider.OrgNode;
import com.matburt.mobileorg.provider.OrgProviderUtil;
import com.matburt.mobileorg.util.OrgUtils;

public class EditActivity extends SherlockFragmentActivity {
	public final static String NODE_ID = "node_id";
	public final static String ACTIONMODE = "actionMode";
	public final static String ACTIONMODE_CREATE = "create";
	public final static String ACTIONMODE_EDIT = "edit";
	public final static String ACTIONMODE_ADDCHILD = "add_child";

	private OrgNode node;
	private String actionMode;
	
	public final static String FRAGMENT_DETAILS_TAG = "details";
	public final static String FRAGMENT_PAYLOAD_TAG = "payload";
	
	private EditDetailsFragment detailsFragment;
	private EditPayloadFragment payloadFragment;
	private EditPayloadFragment rawPayloadFragment;
	private ContentResolver resolver;
	private EditTabListener editTabListener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.edit);
		this.resolver = getContentResolver();
		
		restoreInstanceState(savedInstanceState);
		initState();
		setupActionbarTabs(savedInstanceState);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		getSupportFragmentManager().putFragment(outState,
				EditPayloadFragment.class.getName(), payloadFragment);
		getSupportFragmentManager().putFragment(outState,
				EditPayloadFragment.class.getName() + "raw", rawPayloadFragment);
		getSupportFragmentManager().putFragment(outState,
				EditDetailsFragment.class.getName(), detailsFragment);
        outState.putInt("tab", getSupportActionBar().getSelectedNavigationIndex());
	}
	
	private void restoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			this.detailsFragment = (EditDetailsFragment) getSupportFragmentManager()
					.getFragment(savedInstanceState,
							EditDetailsFragment.class.getName());
			this.payloadFragment = (EditPayloadFragment) getSupportFragmentManager()
					.getFragment(savedInstanceState,
							EditPayloadFragment.class.getName());
			this.rawPayloadFragment = (EditPayloadFragment) getSupportFragmentManager()
					.getFragment(savedInstanceState,
							EditPayloadFragment.class.getName() + "raw");
		}		
		
		if (this.detailsFragment == null)
			this.detailsFragment = new EditDetailsFragment();
		if (this.payloadFragment == null)
			this.payloadFragment = new EditPayloadFragment();
		if (this.rawPayloadFragment == null)
			this.rawPayloadFragment = new EditPayloadFragment();
	}
	
	public void setupActionbarTabs(Bundle savedInstanceState) {
		ActionBar actionbar = getSupportActionBar();
		actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		actionbar.removeAllTabs();

		ActionBar.Tab detailsTab = actionbar.newTab().setText("Details");
		//editTabListener = new EditTabListener(detailsFragment, "details", getSupportFragmentManager());
		detailsTab.setTabListener(new EditTabListener(detailsFragment, "details", getSupportFragmentManager()));
		actionbar.addTab(detailsTab);
	    
		ActionBar.Tab payloadTab = actionbar.newTab().setText("Payload");
		payloadTab.setTabListener(new EditTabListener(payloadFragment, "payload", getSupportFragmentManager()));
		actionbar.addTab(payloadTab);

	    ActionBar.Tab rawPayloadTab = actionbar.newTab().setText("Raw Payload");
	    rawPayloadTab.setTabListener(new EditTabListener(rawPayloadFragment, "raw_payload", getSupportFragmentManager()));
	    actionbar.addTab(rawPayloadTab);
	    
		if (savedInstanceState != null) {
            actionbar.setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
        }
	}
	
	private void initState() {
		Intent intent = getIntent();
		this.actionMode = intent.getStringExtra("actionMode");
		long node_id = intent.getLongExtra("node_id", -1);	

		String defaultTodo = PreferenceManager
				.getDefaultSharedPreferences(this).getString("defaultTodo", "");
		
		if (this.actionMode == null) {
			this.node = getCaptureIntentContents(intent);
			this.actionMode = ACTIONMODE_CREATE;
		} else if (this.actionMode.equals(ACTIONMODE_CREATE)) {
			this.node = new OrgNode();
		} else if (this.actionMode.equals(ACTIONMODE_EDIT)) {
			this.node = new OrgNode(node_id, getContentResolver());
		} else if (this.actionMode.equals(ACTIONMODE_ADDCHILD)) {
			this.node = new OrgNode(node_id, getContentResolver());
		}

		setOrgNode(this.node, defaultTodo);
	}
	
	public void setOrgNode(OrgNode node, String defaultTodo) {
		
		EditDetailsFragment detailsFragment = new EditDetailsFragment();
		detailsFragment.init(node, defaultTodo, resolver);
		
		FragmentTransaction beginTransaction = getSupportFragmentManager().beginTransaction();
		beginTransaction.replace(R.id.editnode_fragment_container, detailsFragment);
		beginTransaction.commit();
		this.detailsFragment = detailsFragment;
		
		this.payloadFragment.init(node.getCleanedPayload(), true);
		this.rawPayloadFragment.init(node.getRawPayload(), false);
	}
	
	public OrgNode getOrgNode() {
		return this.detailsFragment.getEditedOrgNode();
	}


	private static OrgNode getCaptureIntentContents(Intent intent) {
		String subject = intent
				.getStringExtra("android.intent.extra.SUBJECT");
		String text = intent.getStringExtra("android.intent.extra.TEXT");

		if(text != null && subject != null) {
			subject = "[[" + text + "][" + subject + "]]";
			text = "";
		}
		
		if(subject == null)
			subject = "";
		if(text == null)
			text = "";

		OrgNode node = new OrgNode();
		node.name = subject;
		node.payload = text;
		return node;
	}

	
	
	
    @Override
	public boolean onPrepareOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getSupportMenuInflater();
	    inflater.inflate(R.menu.edit, menu);
	    
		if (this.node.id > -1) {
			SubMenu subMenu = menu.addSubMenu(R.string.menu_advanced);
			MenuItem item = subMenu.add(R.string.menu_advanced,
					R.string.menu_delete, 0, R.string.menu_delete);
			item.setIcon(R.drawable.ic_input_delete);

			item = subMenu.add(R.string.menu_advanced, R.string.menu_clockin,
					1, R.string.menu_clockin);
			item.setIcon(R.drawable.ic_menu_today);
			
			item = subMenu.add(R.string.menu_advanced, R.string.menu_archive,
					1, R.string.menu_archive);
			
			item = subMenu.add(R.string.menu_advanced, R.string.menu_archive_tosibling,
					1, R.string.menu_archive_tosibling);

			MenuItem subMenuItem = subMenu.getItem();
			subMenuItem.setIcon(R.drawable.ic_menu_moreoverflow);
			subMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		}
		return true;	
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			doCancel();
			return true;
			
		case R.id.nodeedit_save:
			save();
			setResult(RESULT_OK);
			finish();
			return true;
			
		case R.id.nodeedit_cancel:
			doCancel();
			return true;
			
		case R.string.menu_delete:
			runDeleteNode();
			return true;
			
		case R.string.menu_clockin:
			startTimeClockingService();
			return true;
			
		case R.string.menu_archive:
			runArchiveNode("archive");
			return true;
			
		case R.string.menu_archive_tosibling:
			runArchiveNode("archive-sibling");
			return true;		
		}
		return false;
	}
	
	private void runDeleteNode() {	
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.outline_delete_prompt)
				.setCancelable(false)
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								node.deleteNode(resolver);
								finish();
							}
						})
				.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		builder.create().show();
	}
	
	private void runArchiveNode(final String editString) {	
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.outline_archive_prompt)
				.setCancelable(false)
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								if(editString.equals("archive"))
									node.archiveNode(resolver);
								else
									node.archiveNodeToSibling(resolver);
								finish();
							}
						})
				.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		builder.create().show();
	}

	private void startTimeClockingService() {
		Intent intent = new Intent(EditActivity.this, TimeclockService.class);
		intent.putExtra(TimeclockService.NODE_ID, node.id);
		startService(intent);
	}
	
	@Override
	public void onBackPressed() {
		doCancel();
	}
	
	private void doCancel() {
		if(this.detailsFragment.hasEdits() == false &&
                   this.payloadFragment.hasEdits() == false) {
			setResult(RESULT_CANCELED);
			finish();
			return;
		}
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.node_edit_prompt)
				.setCancelable(false)
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								setResult(RESULT_CANCELED);
								finish();
							}
						})
				.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		builder.create().show();
	}
	
	
	private void insertChangesIntoPayloadResidue() {
		node.getPayload().insertOrReplace("SCHEDULED:", detailsFragment.getScheduled());
		node.getPayload().insertOrReplace("DEADLINE:", detailsFragment.getDeadline());
	}
	
	private void save() {
		OrgNode newNode = this.detailsFragment.getEditedOrgNode();

		if (this.actionMode.equals(ACTIONMODE_CREATE) || this.actionMode.equals(ACTIONMODE_ADDCHILD))
			createNewNode(newNode);
		else if (this.actionMode.equals(ACTIONMODE_EDIT))
			node.generateAndApplyEdits(newNode, resolver);

		announceUpdate();
	}
	
	private void createNewNode(OrgNode newNode) {
		OrgNode newParent = this.detailsFragment.getLocation();

		StringBuilder newCleanedPayload = new StringBuilder(this.payloadFragment.getText());
		insertChangesIntoPayloadResidue();
		String newPayloadResidue = node.getPayload().getNewPayloadResidue();
		String newPayload = newCleanedPayload.toString() + newPayloadResidue;

		boolean addTimestamp = PreferenceManager.getDefaultSharedPreferences(
				this).getBoolean("captureWithTimestamp", false);
		if(addTimestamp)
			newCleanedPayload.append("\n").append(OrgUtils.getTimestamp()).append("\n");
		
		OrgProviderUtil.createNodeWithNewheadingEditnode(newNode, newParent, newPayload, resolver);
	}

	
	private void announceUpdate() {
		Intent intent = new Intent(Synchronizer.SYNC_UPDATE);
		intent.putExtra(Synchronizer.SYNC_DONE, true);
		intent.putExtra("showToast", false);
		sendBroadcast(intent);
	}

}
