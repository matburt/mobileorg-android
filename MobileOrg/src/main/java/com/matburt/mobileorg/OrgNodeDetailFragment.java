package com.matburt.mobileorg;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.matburt.mobileorg.OrgData.OrgContract;
import com.matburt.mobileorg.OrgData.OrgFileParser;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.OrgData.OrgNodeTree;
import com.matburt.mobileorg.util.OrgNodeNotFoundException;
import com.matburt.mobileorg.util.PreferenceUtils;
import com.matburt.mobileorg.util.TodoDialog;

import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A fragment representing a single OrgNode detail screen.
 * This fragment is either contained in a {@link OrgNodeListActivity}
 * in two-pane mode (on tablets) or a {@link OrgNodeDetailActivity}
 * on handsets.
 */
public class OrgNodeDetailFragment extends Fragment {
    public static String NODE_ID = "node_id";
    public static String PARENT_ID = "parent_id";

    private ContentResolver resolver;

    private long nodeId;
    private long lastEditedPosition;
    private long idHlightedPosition;
    private View highlightedView = null;

    private int gray, red, green, yellow, blue, foreground, foregroundDark, black, background, highlightedBackground;
    private static int nTitleColors = 3;
    private int[] titleColor;
    private int[] titleFontSize;
    RecyclerViewAdapter adapter;


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
        lastEditedPosition = -1;
        idHlightedPosition = -1;

        this.resolver = getActivity().getContentResolver();

        gray = ContextCompat.getColor(getContext(), R.color.colorGray);
        red = ContextCompat.getColor(getContext(), R.color.colorRed);
        green = ContextCompat.getColor(getContext(), R.color.colorGreen);
        yellow = ContextCompat.getColor(getContext(), R.color.colorYellow);
        blue = ContextCompat.getColor(getContext(), R.color.colorBlue);
        foreground = ContextCompat.getColor(getContext(), R.color.colorPrimary);
        foregroundDark = ContextCompat.getColor(getContext(), R.color.colorPrimaryDark);
        black = ContextCompat.getColor(getContext(), R.color.colorBlack);

        titleColor = new int[nTitleColors];
        titleColor[0] = foregroundDark;
        titleColor[1] = foreground;
        titleColor[2] = black;

        titleFontSize = new int[nTitleColors];
        titleFontSize[0] = 25;
        titleFontSize[1] = 20;
        titleFontSize[2] = 16;

        OrgNodeTree tree = null;

        if (getArguments().containsKey(NODE_ID)) {
            this.nodeId = getArguments().getLong(NODE_ID);
            try {
                tree = new OrgNodeTree(new OrgNode(nodeId, resolver), resolver);
            } catch (OrgNodeNotFoundException e) {
//                displayError();
//                TODO: implement error
            }

            Activity activity = this.getActivity();
//            AppBarLayout appBarLayout = (AppBarLayout) activity.findViewById(R.id.app_bar);
//            if (appBarLayout != null && tree != null) {
//                appBarLayout.setTitle(tree.node.getCleanedName());
//            }
        }
        adapter = new RecyclerViewAdapter(tree);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.node_summary_recycler_fragment, container, false);

        RecyclerView recyclerView = (RecyclerView)rootView.findViewById(R.id.node_recycler_view);
        assert recyclerView != null;
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext()));

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);

//        registerForContextMenu(recyclerView);

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
                OrgNode node = mAdapter.idTreeMap.get(position).node;
                int id = (int)node.id;
                int parentId = (int)node.parentId;
                lastEditedPosition = position;

                createEditNodeFragment(id, parentId, 0);
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                return false;
            }
        };

        SimpleItemTouchHelperCallback callback = new SimpleItemTouchHelperCallback(adapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);

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

        lastEditedPosition = -1;
        adapter.closeInsertItem();
    }

    @Override
    public void onResume(){
        super.onResume();
        Log.v("newNode", "last : " + lastEditedPosition);
        if(lastEditedPosition > -1) {
            refresh();
        }
    }

    void createEditNodeFragment(int id, int parentId, int siblingPosition) {
        Bundle args = new Bundle();
        args.putLong(NODE_ID, id);
        args.putLong(PARENT_ID, parentId);
        args.putInt(OrgContract.OrgData.POSITION, siblingPosition);

        Intent intent = new Intent(getActivity(), EditNodeActivity.class);
        intent.putExtras(args);
        startActivity(intent);
    }

    public class RecyclerViewAdapter
            extends RecyclerView.Adapter<RecyclerViewAdapter.ItemViewHolder> {

        private NavigableMap<Long, OrgNodeTree> idTreeMap;
        private OrgNodeTree tree;

        public RecyclerViewAdapter(OrgNodeTree root) {
            tree = root;
            refreshVisibility();
        }

        void refreshVisibility(){
            idTreeMap = tree.getVisibleNodesArray();
            notifyDataSetChanged();
        }

        @Override
        public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.detail_recycler_item, parent, false);
            return new ItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ItemViewHolder holder, final int position) {
            holder.mItem = idTreeMap.get((long) position);
            holder.level = holder.mItem.node.level;
            boolean isSelected = (position == idHlightedPosition);
            holder.setup(holder.mItem.node, isSelected);

            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(idHlightedPosition>-1){
                        closeInsertItem();
                        return;
                    }

                    holder.mItem.toggleVisibility();
                    refreshVisibility();
                }
            });

            holder.mView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
//                    ((AppCompatActivity)getActivity()).startSupportActionMode(mActionModeCallback);
                    Log.v("selection","long click");
                    if(highlightedView != null){
                        setItemModifiersVisibility(highlightedView, View.GONE);
                    }
                    idHlightedPosition = position;
                    highlightedView = holder.mView;

                    setItemModifiersVisibility(highlightedView, View.VISIBLE);
//                    holder.mView.setSelected(true);



//                    insertItem((int)idHlightedPosition);
//                    notifyDataSetChanged();
                    return false;
                }
            });

            holder.sameLevel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    OrgNode currentNode = idTreeMap.get(idHlightedPosition).node;
                    int parentId = (int)currentNode.parentId;
                    lastEditedPosition = position;

                    // Place the node right after this one in the adapter
                    int siblingPosition = currentNode.position + 1;
                    createEditNodeFragment(-1, parentId, siblingPosition);
                }
            });

            holder.childLevel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int parentId = (int) idTreeMap.get(idHlightedPosition).node.id;
                    lastEditedPosition = position;
                    createEditNodeFragment(-1, parentId, 0);
                }
            });

            holder.deleteNodeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(getActivity())
                            .setMessage(R.string.prompt_node_delete)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int whichButton) {
                                    OrgNode currentNode = idTreeMap.get(idHlightedPosition).node;
                                    currentNode.deleteNode(resolver);
                                    refresh();
                                }})
                            .setNegativeButton(android.R.string.no, null).show();
                }
            });
        }


        private void closeInsertItem(){
            idHlightedPosition = -1;
            highlightedView = null;
            refreshVisibility();
        }

        @Override
        public int getItemCount() {
            return idTreeMap.size();
        }

        public class ItemViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            private Button sameLevel, childLevel, deleteNodeButton;
            public OrgNodeTree mItem;
            private TextView titleView;
            private Button todoButton;

            private TextView levelView;
            private boolean levelFormatting = true;
            public long level;

            public ItemViewHolder(View view) {
                super(view);
                mView = view;

                titleView = (TextView) view.findViewById(R.id.outline_item_title);
                todoButton = (Button) view.findViewById(R.id.outline_item_todo);
                levelView = (TextView) view.findViewById(R.id.outline_item_level);
                todoButton.setOnClickListener(todoClick);
                sameLevel = (Button) view.findViewById(R.id.insert_same_level);
                childLevel = (Button) view.findViewById(R.id.insert_neighbourg_level);
                deleteNodeButton = (Button) view.findViewById(R.id.delete_node);

                int fontSize = PreferenceUtils.getFontSize();
                todoButton.setTextSize(fontSize);
            }

//            @Override
//            public void onViewRecycled(ItemViewHolder holder) {
//                holder.itemView.setOnLongClickListener(null);
//                super.onViewRecycled(holder);
//            }

            private View.OnClickListener todoClick = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(idHlightedPosition>-1){
                        closeInsertItem();
                        return;
                    }
                    new TodoDialog(getContext(),mItem.node, todoButton);
                }
            };


//            @Override
//            public String toString() {
//                return super.toString() + " '" + mContentView.getText() + "'";
//            }


            public void setupPriority(String priority, SpannableStringBuilder titleSpan) {
                if (priority != null && !TextUtils.isEmpty(priority)) {
                    Spannable prioritySpan = new SpannableString(priority + " ");
                    int yellow = ContextCompat.getColor(getContext(), R.color.colorYellow);
                    prioritySpan.setSpan(new ForegroundColorSpan(yellow), 0,
                            priority.length(), 0);
                    titleSpan.insert(0, prioritySpan);
                }
            }

            public void applyLevelIndentation(long level, SpannableStringBuilder item) {
                String indentString = "";
                for(int i = 0; i < level; i++)
                    indentString += "   ";

                this.levelView.setText(indentString);
            }

            public void applyLevelFormating(long level, SpannableStringBuilder item) {
//                item.setSpan(
//                        new ForegroundColorSpan(theme.levelColors[(int) Math
//                                .abs((level) % theme.levelColors.length)]), 0, item
//                                .length(), 0);
            }

            public void setupTitle(String name, SpannableStringBuilder titleSpan) {
                titleView.setGravity(Gravity.LEFT);
                titleView.setTextSize(titleFontSize[Math.min((int)level-1, nTitleColors)]);
                if(level==1) titleView.setTypeface(null, Typeface.BOLD);
                else titleView.setTypeface(null,Typeface.NORMAL);

                if (name.startsWith("COMMENT"))
                    titleSpan.setSpan(new ForegroundColorSpan(gray), 0,
                            "COMMENT".length(), 0);
                else if (name.equals("Archive"))
                    titleSpan.setSpan(new ForegroundColorSpan(gray), 0,
                            "Archive".length(), 0);

                formatLinks(titleSpan);
            }

            public void setupAgendaBlock(SpannableStringBuilder titleSpan) {
                titleSpan.delete(0, OrgFileParser.BLOCK_SEPARATOR_PREFIX.length());

                titleSpan.setSpan(new ForegroundColorSpan(foreground), 0,
                        titleSpan.length(), 0);
                titleSpan.setSpan(new StyleSpan(Typeface.BOLD), 0,
                        titleSpan.length(), 0);

                titleView.setTextSize(PreferenceUtils.getFontSize() + 4);
                //titleView.setBackgroundColor(theme.c4Blue);
                titleView.setGravity(Gravity.CENTER_VERTICAL
                        | Gravity.CENTER_HORIZONTAL);

                titleView.setText(titleSpan);
            }

            public final Pattern urlPattern = Pattern.compile("\\[\\[[^\\]]*\\]\\[([^\\]]*)\\]\\]");
            private void formatLinks(SpannableStringBuilder titleSpan) {
                Matcher matcher = urlPattern.matcher(titleSpan);
                while(matcher.find()) {
                    titleSpan.delete(matcher.start(), matcher.end());
                    titleSpan.insert(matcher.start(), matcher.group(1));

                    titleSpan.setSpan(new ForegroundColorSpan(blue),
                            matcher.start(), matcher.start() + matcher.group(1).length(), 0);

                    matcher = urlPattern.matcher(titleSpan);
                }
            }

            public void setLevelFormating(boolean enabled) {
                this.levelFormatting = enabled;
            }



            public void setup(OrgNode node, boolean isSelected) {
                this.mItem.node = node;
                SpannableStringBuilder titleSpan = new SpannableStringBuilder(node.name);

                if(node.name.startsWith(OrgFileParser.BLOCK_SEPARATOR_PREFIX)) {
                    setupAgendaBlock(titleSpan);
                    return;
                }

                if (levelFormatting)
                    applyLevelFormating(node.level, titleSpan);
                setupTitle(node.name, titleSpan);
                setupPriority(node.priority, titleSpan);
                TodoDialog.setupTodoButton(getContext(), node, todoButton, true);

                if (levelFormatting)
                    applyLevelIndentation(node.level, titleSpan);

                if(this.mItem.getVisibility()== OrgNodeTree.Visibility.folded)
                    setupChildrenIndicator(node, titleSpan);

//                titleSpan.setSpan(new StyleSpan(Typeface.NORMAL), 0, titleSpan.length(), 0);
                titleView.setText(titleSpan);
                int colorId = (int) Math.min(level-1,nTitleColors-1);
                titleView.setTextColor(titleColor[colorId]);
                mView.setSelected(isSelected);
            }

            public void setupChildrenIndicator(OrgNode node, SpannableStringBuilder titleSpan) {
                if (node.hasChildren(resolver)) {
                    titleSpan.append("...");
                    titleSpan.setSpan(new ForegroundColorSpan(foreground),
                            titleSpan.length() - "...".length(), titleSpan.length(), 0);
                }
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

    static void setItemModifiersVisibility(View view, int visibility){
        LinearLayout itemModifiers = (LinearLayout) view.findViewById(R.id.item_modifiers);
        if(itemModifiers != null) itemModifiers.setVisibility(visibility);
    }
}


