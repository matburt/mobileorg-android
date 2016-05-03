package com.matburt.mobileorg2.Gui.Outline;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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

import com.matburt.mobileorg2.Gui.Theme.DefaultTheme;
import com.matburt.mobileorg2.OrgData.OrgFile;
import com.matburt.mobileorg2.OrgData.OrgNode;
import com.matburt.mobileorg2.OrgData.OrgProviderUtils;
import com.matburt.mobileorg2.OrgNodeDetailActivity;
import com.matburt.mobileorg2.OrgNodeDetailFragment;
import com.matburt.mobileorg2.R;
import com.matburt.mobileorg2.util.OrgFileNotFoundException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OutlineAdapter extends RecyclerView.Adapter<OutlineItem> {

	private final AppCompatActivity activity;
	private ContentResolver resolver;
	private boolean mTwoPanes = false;
	public List<OrgNode> items = new ArrayList<>();
    private SparseBooleanArray selectedItems;
    ActionMode actionMode;


    private ActionMode.Callback mDeleteMode = new ActionMode.Callback() {
		@Override
		public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            Log.v("selection","onPrepare");
            String wordItem;
            int count = getSelectedItemCount();
            if(count == 1) wordItem = activity.getResources().getString(R.string.file);
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
            Log.v("selection","item clicked");
            switch (menuItem.getItemId()) {
                case R.id.item_delete:
                    List<Integer> selectedItems = getSelectedItems();
                    for(Integer num: selectedItems){
                        OrgNode node = items.get(num);
                        try {
                            OrgFile file = new OrgFile(node.fileId, resolver);
                            file.removeFile(resolver);
                        } catch (OrgFileNotFoundException e) {
                            e.printStackTrace();
                        }

                        node.deleteNode(activity.getContentResolver());
                        Log.v("selection","deleting : "+items.get(num).name);
                    }
                    refresh();
                    actionMode.finish();
                    return true;
            }
            return false;
        }
	};

	private DefaultTheme theme;

	public void setHasTwoPanes(boolean _hasTwoPanes){
		mTwoPanes = _hasTwoPanes;
	}

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
		return items.size();
	}

    @Override public OutlineItem onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.outline_item, parent, false);
        return new OutlineItem(v);
    }


    @Override
    public void onBindViewHolder(final OutlineItem holder, final int position) {
		holder.titleView.setText(items.get(position).name);
//		holder.mContentView.setText(items.get(position).levelView.getText());

        holder.mView.setActivated(selectedItems.get(position, false));

		final long itemId = getItemId(position);
		holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getSelectedItemCount() > 0) {
                    toggleSelection(position);
                } else {
                    if (mTwoPanes) {
                        Bundle arguments = new Bundle();
                        arguments.putLong(OrgNodeDetailFragment.NODE_ID, itemId);
                        OrgNodeDetailFragment fragment = new OrgNodeDetailFragment();
                        fragment.setArguments(arguments);

                        AppCompatActivity activity = (AppCompatActivity) v.getContext();
                        activity.getSupportFragmentManager().beginTransaction()
                                .replace(R.id.orgnode_detail_container, fragment)
                                .commit();
                    } else {
                        Context context = v.getContext();
                        Intent intent = new Intent(context, OrgNodeDetailActivity.class);
                        intent.putExtra(OrgNodeDetailFragment.NODE_ID, itemId);
                        context.startActivity(intent);
                    }
                }
            }
        });

        holder.mView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                toggleSelection(position);
                return true;
            }
        });

	}

	public void clear() {
        this.items.clear();
    }

    public void add(OrgNode node) {
		this.items.add(node);
	}

	public void insert(OrgNode node, int index) {
		this.items.add(index, node);
	}
	
	public void insertAll(ArrayList<OrgNode> nodes, int position) {
        Collections.reverse(nodes);
        for (OrgNode node: nodes)
			insert(node, position);
//		notifyDataSetInvalidated();
	}

	
	@Override
	public long getItemId(int position) {
		OrgNode node = items.get(position);
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
}
