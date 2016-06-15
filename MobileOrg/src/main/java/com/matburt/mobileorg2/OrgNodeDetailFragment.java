package com.matburt.mobileorg2;

import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.matburt.mobileorg2.OrgData.OrgContract;
import com.matburt.mobileorg2.OrgData.OrgNode;
import com.matburt.mobileorg2.OrgData.OrgNodeTree;
import com.matburt.mobileorg2.OrgData.OrgProviderUtils;
import com.matburt.mobileorg2.util.OrgNodeNotFoundException;
import com.matburt.mobileorg2.util.TodoDialog;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.NavigableMap;

/**
 * A fragment representing a single OrgNode detail screen.
 * This fragment is either contained in a {@link OrgNodeListActivity}
 * in two-pane mode (on tablets) or a {@link OrgNodeDetailActivity}
 * on handsets.
 */
public class OrgNodeDetailFragment extends Fragment implements SwipeInterface{

    private ContentResolver resolver;

    private long nodeId;
    private OrgNode selectedNode;
    private View highlightedView = null;


    RecyclerViewAdapter adapter;
    Button insertNodeButton;
    RecyclerView recyclerView;
    TextView insertNodeText;

    ActivitySwipeDetector swipeDetector;

    @Override
    public void left2right(View v) {
        createEditNodeFragment(v.getId(), 0, 0);
    }

    @Override
    public void right2left(View v) {
        createEditNodeFragment(v.getId(), 0, 0);
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public OrgNodeDetailFragment() {
    }

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


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.resolver = getActivity().getContentResolver();

        OrgNodeTree tree = null;

        swipeDetector = new ActivitySwipeDetector(this);

        if (getArguments().containsKey(OrgContract.NODE_ID)) {
            this.nodeId = getArguments().getLong(OrgContract.NODE_ID);

            if(nodeId == OrgContract.TODO_ID){
                Cursor cursor = resolver.query(OrgContract.OrgData.CONTENT_URI,
                OrgContract.OrgData.DEFAULT_COLUMNS, "todo is not null and todo <> ''", null, OrgContract.OrgData.NAME_SORT);
                tree = new OrgNodeTree(OrgProviderUtils.orgDataCursorToArrayList(cursor));
                if(cursor != null) cursor.close();
            } else {
                try {
                    tree = new OrgNodeTree(new OrgNode(nodeId, resolver), resolver);
                } catch (OrgNodeNotFoundException e) {
//                displayError();
//                TODO: implement error
                }
            }
        }

        adapter = new RecyclerViewAdapter(tree);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.node_summary_recycler_fragment, container, false);



        recyclerView = (RecyclerView)rootView.findViewById(R.id.node_recycler_view);
        assert recyclerView != null;
//        recyclerView.addItemDecoration(new DividerItemDecoration(getContext()));

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);

        insertNodeButton = (Button)rootView.findViewById(R.id.empty_recycler);
        insertNodeText = (TextView)rootView.findViewById(R.id.empty_recycler_text);

        insertNodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createEditNodeFragment(-1, (int)OrgNodeDetailFragment.this.nodeId, 0);
            }
        });

        class SimpleItemTouchHelperCallback extends ItemTouchHelper.Callback {
            private final RecyclerViewAdapter mAdapter;
            public SimpleItemTouchHelperCallback(RecyclerViewAdapter adapter) {
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
                long position = (long)viewHolder.getAdapterPosition();
//                OrgNode node = mAdapter.idTreeMap.get(position).node;
//                int id = (int)node.id;
//                int parentId = (int)node.parentId;
//

            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                return false;
            }
        };

//        SimpleItemTouchHelperCallback callback = new SimpleItemTouchHelperCallback(adapter);
//        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
//        touchHelper.attachToRecyclerView(recyclerView);

        refresh();
        return rootView;
    }

    /**
     * Recreate the OrgNodeTree and refresh the Adapter
     */
    public void refresh(){
        try {
            adapter.tree = new OrgNodeTree(new OrgNode(nodeId, resolver), resolver);
        } catch (OrgNodeNotFoundException e) {
//                displayError();
//                TODO: implement error
        }

        adapter.closeInsertItem();

        int size = adapter.getItemCount();

        if(size == 0){
            recyclerView.setVisibility(View.GONE);
            insertNodeButton.setVisibility(View.VISIBLE);
            insertNodeText.setVisibility(View.VISIBLE);
        } else {
            insertNodeButton.setVisibility(View.GONE);
            insertNodeText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        refresh();
    }

    void createEditNodeFragment(int id, int parentId, int siblingPosition) {
        Bundle args = new Bundle();
        args.putLong(OrgContract.NODE_ID, id);
        args.putLong(OrgContract.PARENT_ID, parentId);
        args.putInt(OrgContract.OrgData.POSITION, siblingPosition);

        Intent intent = new Intent(getActivity(), EditNodeActivity.class);
        intent.putExtras(args);
        startActivity(intent);
    }

    public class RecyclerViewAdapter
            extends RecyclerView.Adapter<MultipleItemsViewHolder> {

        private OrgNodeTree tree;

        public RecyclerViewAdapter(OrgNodeTree root) {
            tree = root;
            refreshVisibility();
        }

        void refreshVisibility(){
            notifyDataSetChanged();
        }

        @Override
        public MultipleItemsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.detail_recycler_item, parent, false);
            return new MultipleItemsViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final MultipleItemsViewHolder holder, final int position) {
            OrgNodeTree root  = tree.children.get(position);

            LinearLayout rootView = (LinearLayout)holder.view.findViewById(R.id.layout_inside_cardview);
            rootView.removeAllViewsInLayout();

            ArrayList<OrgNodeTree> items = new ArrayList<>();
            items.add(root);
            for(OrgNodeTree child: root.children) {
                items.add(child);
            }

            for(final OrgNodeTree child: items) {
                final View view = LayoutInflater.from(getContext())
                    .inflate(R.layout.subnode_layout, null);

                view.setId((int)child.node.id);

                if(child == items.get(items.size()-1)){
                    view.findViewById(R.id.delimiter).setVisibility(View.GONE);
                }

                final ItemViewHolder item = new ItemViewHolder(view, child.node);

                boolean isSelected = (item.node == selectedNode);
                item.setup(child, isSelected, getContext());

                item.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (selectedNode != null) {
                            closeInsertItem();
                            return;
                        }

//                        item.mItem.toggleVisibility();
//                        refreshVisibility();
                    }
                });

                item.mView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
//                    ((AppCompatActivity)getActivity()).startSupportActionMode(mActionModeCallback);
                        Log.v("selection", "long click");
                        Log.v("selection", "selected node : "+selectedNode);
                        Log.v("selection", "highlighted : "+highlightedView);
                        if (selectedNode != null) {
                            closeInsertItem();
//                            setItemModifiersVisibility(highlightedView, View.GONE);
                        }

                        selectedNode = item.node;
                        highlightedView = view;

                        setItemModifiersVisibility(highlightedView, View.VISIBLE);
//                    item.mView.setSelected(true);

                        notifyItemChanged(position);
                        return true;
                    }
                });

                item.todoButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (selectedNode != null) {
                            closeInsertItem();
                            return;
                        }
                        new TodoDialog(getContext(), child.node, item.todoButton);
                    }
                });


                item.sameLevel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        OrgNode currentNode = item.node;
                        int parentId = (int) currentNode.parentId;

                        // Place the node right after this one in the adapter
                        int siblingPosition = currentNode.position + 1;
                        createEditNodeFragment(-1, parentId, siblingPosition);
                    }
                });

                item.childLevel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int parentId = (int) item.node.id;
                        createEditNodeFragment(-1, parentId, 0);
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
                                        item.node.deleteNode(resolver);
                                        refresh();
                                    }
                                })
                                .setNegativeButton(android.R.string.no, null).show();
                    }
                });
                view.setOnTouchListener(swipeDetector);
                rootView.addView(view);
            }
        }


        private void closeInsertItem(){
            selectedNode = null;
            if(highlightedView != null){
                setItemModifiersVisibility(highlightedView, View.GONE);
                highlightedView = null;
            }

            refreshVisibility();
        }

        @Override
        public int getItemCount() {
            return tree.children.size();
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

    static void setItemModifiersVisibility(View view, int visibility){
        LinearLayout itemModifiers = (LinearLayout) view.findViewById(R.id.item_modifiers);
        if(itemModifiers != null) itemModifiers.setVisibility(visibility);
    }
}


