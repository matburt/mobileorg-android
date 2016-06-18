package com.matburt.mobileorg2.Gui.Outline;

import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.matburt.mobileorg2.Gui.Theme.DefaultTheme;
import com.matburt.mobileorg2.OrgData.OrgContract;
import com.matburt.mobileorg2.OrgData.OrgFile;
import com.matburt.mobileorg2.OrgData.OrgNode;
import com.matburt.mobileorg2.OrgData.OrgProviderUtils;
import com.matburt.mobileorg2.OrgNodeDetailActivity;
import com.matburt.mobileorg2.OrgNodeDetailFragment;
import com.matburt.mobileorg2.R;
import com.matburt.mobileorg2.util.OrgFileNotFoundException;

import java.util.ArrayList;
import java.util.List;

public class OutlineAdapter extends RecyclerView.Adapter<OutlineAdapter.OutlineItem> {
    private final AppCompatActivity activity;
    public List<OrgNode> items = new ArrayList<>();
    ActionMode actionMode;
    private ContentResolver resolver;
	private boolean mTwoPanes = false;
    private SparseBooleanArray selectedItems;
    // Number of added items. Here it is two: Agenda and Todos.
    private int numExtraItems = 2;
    private ActionMode.Callback mDeleteMode = new ActionMode.Callback() {
        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            Log.v("selection", "onPrepare");
            String wordItem;
            int count = getSelectedItemCount();
            if (count == 1) wordItem = activity.getResources().getString(R.string.file);
            else wordItem = activity.getResources().getString(R.string.files);
            menu.findItem(R.id.action_text).setTitle(count + " " + wordItem);
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            OutlineAdapter.this.clearSelections();
        }

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            MenuInflater inflater = activity.getMenuInflater();
            inflater.inflate(R.menu.main_context_action_bar, menu);

            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            Log.v("selection", "item clicked");
            switch (menuItem.getItemId()) {
                case R.id.item_delete:
                    String message;
                    int numSelectedItems = getSelectedItemCount();
                    if (numSelectedItems == 1)
                        message = activity.getResources().getString(R.string.prompt_delete_file);
                    else {
                        message = activity.getResources().getString(R.string.prompt_delete_files);
                        message = message.replace("#", String.valueOf(numSelectedItems));
                    }

                    new AlertDialog.Builder(activity)
                            .setMessage(message)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    deleteSelectedFiles();
                                }
                            })
                            .setNegativeButton(android.R.string.no, null).show();
                    return true;
            }
            return false;
        }
    };
    private DefaultTheme theme;

	public OutlineAdapter(AppCompatActivity activity) {
		super();
		this.activity = activity;
        this.resolver = activity.getContentResolver();

		this.theme = DefaultTheme.getTheme(activity);
        selectedItems = new SparseBooleanArray();
		refresh();
	}

	public void refresh() {
		clear();

		for (OrgNode node : OrgProviderUtils.getOrgNodeChildren(-1, resolver)){
			add(node);
		}


        notifyDataSetChanged();
	}

	@Override public int getItemCount() {
		return items.size() + numExtraItems;
	}

    @Override public OutlineItem onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.outline_item, parent, false);
        return new OutlineItem(v);
    }

    @Override
    public void onBindViewHolder(final OutlineItem holder, final int position) {
        int positionInItems = position - numExtraItems;

        String title;
        if(position == 0) {
            title = activity.getResources().getString(R.string.menu_todos);
        } else if (position == 1){
            title = activity.getResources().getString(R.string.menu_agenda);
        } else {
            title = items.get(positionInItems).name;
        }

        holder.titleView.setText(title);

        holder.mView.setActivated(selectedItems.get(positionInItems, false));

		final long itemId = getItemId(position);
		holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getSelectedItemCount() > 0) {
                    if(!isSelectableItem(position)) return;
                    toggleSelection(position);
                } else {
                    if (mTwoPanes) {
                        Bundle arguments = new Bundle();
                        arguments.putLong(OrgContract.NODE_ID, itemId);
                        OrgNodeDetailFragment fragment = new OrgNodeDetailFragment();
                        fragment.setArguments(arguments);

                        AppCompatActivity activity = (AppCompatActivity) v.getContext();
                        activity.getSupportFragmentManager().beginTransaction()
                                .replace(R.id.orgnode_detail_container, fragment)
                                .commit();
                        // TODO: add agenda fragment
                    } else {
                        Context context = v.getContext();
                        Intent intent = new Intent(context, OrgNodeDetailActivity.class);

                        if(position == 0){
                            intent.putExtra(OrgContract.NODE_ID, OrgContract.TODO_ID);
                        } else if (position == 1){
                            intent.putExtra(OrgContract.NODE_ID, OrgContract.AGENDA_ID);
                        } else {
                            intent.putExtra(OrgContract.NODE_ID, itemId);
                        }

                        context.startActivity(intent);
                    }
                }
            }
        });


        holder.mView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // Do nothing when user select Agenda.org or Todos.org

                if(!isSelectableItem(position)) return true;
                toggleSelection(position);
                return true;
            }
        });

	}

    private boolean isSelectableItem(int position){
        if(position < numExtraItems){
            String text = activity.getResources().getString(R.string.unselectable_item);
            Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

	public void clear() {
        this.items.clear();
    }

    public void add(OrgNode node) {
		this.items.add(node);
	}

	@Override
	public long getItemId(int position) {
        if(position < numExtraItems) return -1;
		OrgNode node = items.get(position - numExtraItems);
		return node.id;
	}

    public void toggleSelection(int pos) {
        Log.v("selection", "selection pos : " + pos);
        int countBefore = getSelectedItemCount();
        if (selectedItems.get(pos, false)) {
            selectedItems.delete(pos);
        }
        else {
            selectedItems.put(pos, true);
        }
        notifyItemChanged(pos);
        int countAfter = getSelectedItemCount();
        if(countBefore == 0 && countAfter > 0)
            actionMode = activity.startSupportActionMode(mDeleteMode);
        if(countAfter == 0 && actionMode != null)
            actionMode.finish();
        if(countAfter > 0 && actionMode != null){
            actionMode.invalidate();
        }

    }

    public void clearSelections() {
        selectedItems.clear();
        notifyDataSetChanged();
    }

    public int getSelectedItemCount() {
        return selectedItems.size();
    }

    public List<Integer> getSelectedItems() {
        List<Integer> items =
                new ArrayList<>(selectedItems.size());
        for (int i = 0; i < selectedItems.size(); i++) {
            items.add(selectedItems.keyAt(i));
        }
        return items;
    }

    private void deleteSelectedFiles(){
        List<Integer> selectedItems = getSelectedItems();
        for(Integer num: selectedItems){
            num -= numExtraItems;
            OrgNode node = items.get(num);
            try {
                OrgFile file = new OrgFile(node.fileId, resolver);
                file.removeFile(activity);
            } catch (OrgFileNotFoundException e) {
                e.printStackTrace();
            }

            node.deleteNode(activity);
            Log.v("selection","deleting : "+items.get(num).name);
        }
        refresh();
        actionMode.finish();
    }

    public void setHasTwoPanes(boolean _hasTwoPanes){
        mTwoPanes = _hasTwoPanes;
    }

    public class OutlineItem extends RecyclerView.ViewHolder {
        public final View mView;
        public TextView titleView;

        public OutlineItem(View view) {
            super(view);
            mView = view;
            titleView = (TextView) view.findViewById(R.id.title);

        }
    }



}
