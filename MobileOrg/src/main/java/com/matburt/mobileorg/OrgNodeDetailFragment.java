package com.matburt.mobileorg;

import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.matburt.mobileorg.orgdata.OrgContract;
import com.matburt.mobileorg.orgdata.OrgDatabase;
import com.matburt.mobileorg.orgdata.OrgNode;
import com.matburt.mobileorg.orgdata.OrgNodeTree;
import com.matburt.mobileorg.orgdata.OrgProviderUtils;
import com.matburt.mobileorg.util.OrgNodeNotFoundException;
import com.matburt.mobileorg.util.TodoDialog;

import java.util.NavigableMap;

/**
 * A fragment representing a single OrgNode detail screen.
 * This fragment is either contained in a {@link OrgNodeListActivity}
 * in two-pane mode (on tablets) or a {@link OrgNodeDetailActivity}
 * on handsets.
 */
public class OrgNodeDetailFragment extends Fragment {

    MainRecyclerViewAdapter adapter;
    Button insertNodeButton;
    RecyclerView recyclerView;
    TextView insertNodeText;
    private ContentResolver resolver;
    private long nodeId;
    private OrgNode selectedNode;
    private View highlightedView = null;
    private ActionMode mActionMode = null;
    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.agenda_entry, menu);
            return true;
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.agenda_entry_cancel:
//                    shareCurrentItem();
                    mode.finish(); // Action picked, so close the CAB
                    return true;
                default:
                    return false;
            }
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
        }
    };

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public OrgNodeDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.resolver = getActivity().getContentResolver();

        OrgNodeTree tree = null;

        if (getArguments().containsKey(OrgContract.NODE_ID)) {
            Bundle arguments = getArguments();
            this.nodeId = arguments.getLong(OrgContract.NODE_ID);


        }

        adapter = new MainRecyclerViewAdapter();

    }

    private OrgNodeTree getTree(){
        // Handling of the TODO file
        if (nodeId == OrgContract.TODO_ID) {
//                Cursor cursor = resolver.query(OrgContract.OrgData.CONTENT_URI,
//                        OrgContract.OrgData.DEFAULT_COLUMNS, OrgContract.Todos.ISDONE + "=1",
//                        null, OrgContract.OrgData.NAME_SORT);

            String todoQuery = "SELECT " +
                    OrgContract.formatColumns(
                            OrgDatabase.Tables.ORGDATA,
                            OrgContract.OrgData.DEFAULT_COLUMNS) +
                    " FROM orgdata JOIN todos " +
                    " ON todos.name = orgdata.todo WHERE todos.isdone=0";


            Cursor cursor = OrgDatabase.getInstance().getReadableDatabase().rawQuery(todoQuery, null);

            OrgNodeTree tree = new OrgNodeTree(OrgProviderUtils.orgDataCursorToArrayList(cursor));
            if(cursor != null) cursor.close();
            return tree;
        } else {
            try {
                return new OrgNodeTree(new OrgNode(nodeId, resolver), resolver);
            } catch (OrgNodeNotFoundException e) {
//                TODO: implement error
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.node_summary_recycler_fragment, container, false);


        recyclerView = (RecyclerView) rootView.findViewById(R.id.node_recycler_view);
        assert recyclerView != null;
//        recyclerView.addItemDecoration(new DividerItemDecoration(getContext()));

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);

        insertNodeButton = (Button) rootView.findViewById(R.id.empty_recycler);
        insertNodeText = (TextView) rootView.findViewById(R.id.empty_recycler_text);

        insertNodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditNodeFragment.createEditNodeFragment(
                        -1,
                        (int) OrgNodeDetailFragment.this.nodeId,
                        0,
                        getContext());
            }
        });

        refresh();

        int position = findCardViewContaining(getArguments().getLong(OrgContract.POSITION, -1));
        recyclerView.scrollToPosition(position);
        return rootView;
    }

    /**
     * Recreate the OrgNodeTree and refresh the Adapter
     */
    public void refresh() {
        adapter.tree = getTree();

        int size = adapter.getItemCount();

        if (size == 0) {
            recyclerView.setVisibility(View.GONE);
            int textId = (nodeId != OrgContract.TODO_ID) ? R.string.no_node : R.string.no_todo;
            insertNodeButton.setVisibility((nodeId != OrgContract.TODO_ID) ? View.VISIBLE : View.INVISIBLE);
            insertNodeText.setText(textId);
            insertNodeText.setVisibility(View.VISIBLE);
        } else {
            insertNodeButton.setVisibility(View.GONE);
            insertNodeText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }



    /**
     * Find the CardView containing the given node id
     *
     * @param id
     * @return the position of the CardView
     */
    int findCardViewContaining(long id) {
        if (id < 0) return 0;
        OrgNode node = null;
        long cardViewMainNode = -1;
        try {
            do {
                if (node != null) cardViewMainNode = node.id;
                node = new OrgNode(id, getContext().getContentResolver());
                id = node.parentId;
            } while (id > -1);
            for (int i = 0; i < adapter.tree.children.size(); i++) {
                if (adapter.tree.children.get(i).node.id == cardViewMainNode) {
                    return i;
                }
            }
        } catch (OrgNodeNotFoundException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public class MainRecyclerViewAdapter
            extends RecyclerView.Adapter<MultipleItemsViewHolder> {

        private OrgNodeTree tree;

        public MainRecyclerViewAdapter() {
        }

        @Override
        public MultipleItemsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.detail_recycler_item, parent, false);
            return new MultipleItemsViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final MultipleItemsViewHolder holder, final int position) {
            OrgNodeTree root = tree.children.get(position);

            SecondaryRecyclerViewAdapter secondaryAdapter = new SecondaryRecyclerViewAdapter(root, position);

            RecyclerView secondaryRecyclerView = (RecyclerView) holder.view.findViewById(R.id.node_recycler_view);
            secondaryRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            secondaryRecyclerView.setAdapter(secondaryAdapter);

            SimpleItemTouchHelperCallback callback = new SimpleItemTouchHelperCallback(secondaryAdapter);
            ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
            touchHelper.attachToRecyclerView(secondaryRecyclerView);
        }

        @Override
        public int getItemCount() {
            return tree.children.size();
        }

        class SimpleItemTouchHelperCallback extends ItemTouchHelper.Callback {
            private final RecyclerView.Adapter mAdapter;

            public SimpleItemTouchHelperCallback(RecyclerView.Adapter adapter) {
                mAdapter = adapter;
            }

            @Override
            public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
                int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
                return makeMovementFlags(dragFlags, swipeFlags);
            }


            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                long position = (long) viewHolder.getAdapterPosition();
//                OrgNode node = mAdapter.idTreeMap.get(position).node;
//                int id = (int)node.id;
//                int parentId = (int)node.parentId;

                OrgNodeViewHolder item = (OrgNodeViewHolder) viewHolder;
                EditNodeFragment.createEditNodeFragment((int)item.node.id, -1, -1, getContext());
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                return false;
            }
        }
    }

    public class SecondaryRecyclerViewAdapter
            extends RecyclerView.Adapter<OrgNodeViewHolder> {

        NavigableMap<Long, OrgNodeTree> items;
        private OrgNodeTree tree;
        private int parentPosition;

        public SecondaryRecyclerViewAdapter(OrgNodeTree root, int parentPosition) {
            tree = root;
            refreshVisibility();
            this.parentPosition = parentPosition;
        }

        void refreshVisibility() {
            items = tree.getVisibleNodesArray();
            notifyDataSetChanged();
        }

        @Override
        public OrgNodeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.subnode_layout, parent, false);
            return new OrgNodeViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final OrgNodeViewHolder item, final int position) {
            final OrgNodeTree tree = items.get((long)position);

            boolean isSelected = (tree.node == selectedNode);
            item.setup(tree, isSelected, getContext());

            item.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (selectedNode != null) {
                        closeInsertItem();
                    } else {
                        tree.toggleVisibility();
                    }
                    refreshVisibility();
                }
            });

            item.mView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
//                    ((AppCompatActivity)getActivity()).startSupportActionMode(mActionModeCallback);

                    if (highlightedView != null) {
                        closeInsertItem();
                    }

                    selectedNode = item.node;
                    highlightedView = item.mView;
//                    setItemModifiersVisibility(highlightedView, View.VISIBLE);
//                    item.mView.setSelected(true);

//                    notifyItemChanged(position);
//                    refreshVisibility();
//                    adapter.notifyItemChanged(parentPosition, SecondaryRecyclerViewAdapter.this);
                    adapter.notifyDataSetChanged();
                    return true;
                }
            });

            item.todoButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (selectedNode != null) {
                        closeInsertItem();
                        refreshVisibility();
                        return;
                    }
                    new TodoDialog(getContext(), tree.node, item.todoButton, true);
                    refreshVisibility();
                }
            });


            item.sameLevel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    OrgNode currentNode = item.node;
                    int parentId = (int) currentNode.parentId;

                    // Place the node right after this one in the adapter
                    int siblingPosition = currentNode.position + 1;
                    EditNodeFragment.createEditNodeFragment(-1, parentId, siblingPosition, getContext());
                }
            });

            item.childLevel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int parentId = (int) item.node.id;
                    EditNodeFragment.createEditNodeFragment(-1, parentId, 0, getContext());
                }
            });

            item.deleteNodeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(getActivity())
                            .setMessage(R.string.prompt_node_delete)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int whichButton) {
                                    item.node.deleteNode(getContext());
                                    refresh();
                                }
                            })
                            .setNegativeButton(android.R.string.no, null).show();
                }
            });
        }


        private void closeInsertItem() {
            selectedNode = null;
            if (highlightedView != null) {
                setItemModifiersVisibility(highlightedView, View.GONE);
                highlightedView = null;
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        void setItemModifiersVisibility(View view, int visibility){
            LinearLayout itemModifiers = (LinearLayout) view.findViewById(R.id.item_modifiers);
            if (itemModifiers != null) {
                itemModifiers.setVisibility(visibility);
            }
        }
    }

    public class DividerItemDecoration extends RecyclerView.ItemDecoration {

        private final int[] ATTRS = new int[]{android.R.attr.listDivider};

        private Drawable mDivider;

        /**
         * Default divider will be used
         */
        public DividerItemDecoration(Context context) {
            final TypedArray styledAttributes = context.obtainStyledAttributes(ATTRS);
            mDivider = styledAttributes.getDrawable(0);
            styledAttributes.recycle();
        }

        /**
         * Custom divider will be used
         */
        public DividerItemDecoration(Context context, int resId) {
            mDivider = ContextCompat.getDrawable(context, resId);
        }

        @Override
        public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
            int left = parent.getPaddingLeft();
            int right = parent.getWidth() - parent.getPaddingRight();

            int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = parent.getChildAt(i);

                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

                int top = child.getBottom() + params.bottomMargin;
                int bottom = top + mDivider.getIntrinsicHeight();

                mDivider.setBounds(left, top, right, bottom);
                mDivider.draw(c);
            }
        }
    }
}

