package com.matburt.mobileorg.Gui.Outline;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.widget.ListView;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Gui.ViewActivity;
import com.matburt.mobileorg.Gui.Capture.EditActivity;
import com.matburt.mobileorg.Gui.Capture.EditActivityController;
import com.matburt.mobileorg.OrgData.OrgFile;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.Services.CalendarSyncService;
import com.matburt.mobileorg.Services.TimeclockService;
import com.matburt.mobileorg.util.OrgFileNotFoundException;
import com.matburt.mobileorg.util.OrgUtils;
import com.matburt.mobileorg.util.PreferenceUtils;

public class OutlineActionMode implements ActionMode.Callback {

	private Context context;
	private ContentResolver resolver;
	
	private ListView list;
	private OutlineAdapter adapter;
	private int listPosition;
	private OrgNode node;

	public OutlineActionMode(Context context) {
		super();
		this.context = context;
		this.resolver = context.getContentResolver();
	}
	
	public void initActionMode(ListView list, int position, int restorePosition) {
		initActionMode(list, position);
		this.listPosition = restorePosition;
	}
	
	public void initActionMode(ListView list, int position) {
		list.setItemChecked(position, true);
		this.list = list;
		this.adapter = (OutlineAdapter) list.getAdapter();
		this.listPosition = position;
		this.node = adapter.getItem(position);
	}
	
	@Override
	public void onDestroyActionMode(ActionMode mode) {
		this.list.setItemChecked(this.listPosition, true);
	}
	
	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
		
		if (this.node != null && this.node.id >= 0 && node.isNodeEditable(resolver)) {
	        inflater.inflate(R.menu.outline_node, menu);
		}
		else if(this.node != null && this.node.isFilenode(resolver)) {
			if(this.node.name.equals(OrgFile.AGENDA_FILE_ALIAS))
		        inflater.inflate(R.menu.outline_file_uneditable, menu);
			else
				inflater.inflate(R.menu.outline_file, menu);
		} else
	        inflater.inflate(R.menu.outline_node_uneditable, menu);
        
        return true;
	}
	
	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		return false;
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		switch (item.getItemId()) {

		case R.id.menu_edit:
			runEditNodeActivity(node.id, context);
			break;
		case R.id.menu_delete:
			runDeleteNode();
			break;
		case R.id.menu_delete_file:
			runDeleteFileNode();
			break;
		case R.id.menu_clockin:
			runTimeClockingService();
			break;
		case R.id.menu_archive:
			runArchiveNode(false);
			break;
		case R.id.menu_archive_tosibling:
			runArchiveNode(true);
			break;
		case R.id.menu_view:
			runViewNodeActivity();
			break;
		case R.id.menu_recover:
			runRecover();
			break;

		case R.id.menu_capturechild:
			runCaptureActivity(node.id, context);
			break;
			
		default:
			mode.finish();
			return false;
		}

		mode.finish();
		return true;
	}

	
	public static void runEditNodeActivity(long nodeId, Context context) {
		Intent intent = new Intent(context, EditActivity.class);
		intent.putExtra(EditActivityController.ACTIONMODE, EditActivityController.ACTIONMODE_EDIT);
		intent.putExtra(EditActivityController.NODE_ID, nodeId);
		context.startActivity(intent);
	}
	
	public static  void runCaptureActivity(long id, Context context) {
		Intent intent = new Intent(context, EditActivity.class);
		
		String captureMode = EditActivityController.ACTIONMODE_CREATE;
		if (PreferenceUtils.useAdvancedCapturing()) {
			captureMode = EditActivityController.ACTIONMODE_ADDCHILD;
		}
		
		intent.putExtra(EditActivityController.ACTIONMODE, captureMode);
		intent.putExtra(EditActivityController.NODE_ID, id);
		context.startActivity(intent);
	}
	
	private void runDeleteNode() {	
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(R.string.prompt_node_delete)
				.setCancelable(false)
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								node.deleteNode(resolver);
								OrgUtils.announceSyncDone(context);
							}
						})
				.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		builder.create().show();
	}
	
	private void runArchiveNode(final boolean archiveToSibling) {	
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(R.string.prompt_node_archive)
				.setCancelable(false)
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								archiveNode(archiveToSibling);
							}
						})
				.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		builder.create().show();
	}

	private void archiveNode(boolean archiveToSibling) {		
		if(archiveToSibling)
			node.archiveNodeToSibling(resolver);
		else
			node.archiveNode(resolver);
		OrgUtils.announceSyncDone(context);
	}
	
	private void runDeleteFileNode() {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(R.string.prompt_delete_file)
				.setCancelable(false)
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								deleteFileNode();
							}
						})
				.setNegativeButton(R.string.no,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
		builder.create().show();
	}
	
	private void deleteFileNode() {
		try {
			OrgFile file = new OrgFile(node.fileId, resolver);
			file.removeFile(resolver);
			
			Intent calDeleteIntent = new Intent(context, CalendarSyncService.class);
			calDeleteIntent.putExtra(CalendarSyncService.CLEARDB, true);
			calDeleteIntent.putExtra(CalendarSyncService.FILELIST, new String[] {file.filename});
			context.startService(calDeleteIntent);
			
			OrgUtils.announceSyncDone(context);
		} catch (OrgFileNotFoundException e) {}
	}
	
	public static void runViewNodeActivity(long nodeId, Context context) {
		Intent intent = new Intent(context, ViewActivity.class);
		intent.putExtra(ViewActivity.NODE_ID, nodeId);
		context.startActivity(intent);
	}
	
	private void runViewNodeActivity() {		
		runViewNodeActivity(node.id, context);
	}
	
	private void runTimeClockingService() {
		Intent intent = new Intent(context, TimeclockService.class);
		intent.putExtra(TimeclockService.NODE_ID, node.id);
		context.startService(intent);
	}
	
	private void runRecover() {
		try {
			OrgFile orgFile = this.node.getOrgFile(resolver);
			Log.d("MobileOrg", orgFile.toString(resolver));
		} catch (OrgFileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
