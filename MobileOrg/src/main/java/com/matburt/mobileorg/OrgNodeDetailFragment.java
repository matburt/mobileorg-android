package com.matburt.mobileorg;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
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
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

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

    private ContentResolver resolver;

    private long nodeId;
    private long lastEditedPosition;
    private long idHlightedPosition;

    private OrgNodeTree tree;
    private int gray, red, green, yellow, blue, foreground, foregroundDark, black;
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
        
        Log.i("commit", "received with : " + getArguments().getLong(NODE_ID));
        if (getArguments().containsKey(NODE_ID)) {
            this.nodeId = getArguments().getLong(NODE_ID);
            try {
                this.tree = new OrgNodeTree(new OrgNode(nodeId, resolver), resolver);
            } catch (OrgNodeNotFoundException e) {
//                displayError();
//                TODO: implement error
            }

            Activity activity = this.getActivity();
            CollapsingToolbarLayout appBarLayout = (CollapsingToolbarLayout) activity.findViewById(R.id.toolbar_layout);
            if (appBarLayout != null && tree != null) {
                appBarLayout.setTitle(tree.node.getCleanedName());
            }
        }
        adapter = new RecyclerViewAdapter(tree);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.node_summary_recycler_fragment, container, false);

        if (tree ==null) return rootView;

        RecyclerView recyclerView = (RecyclerView)rootView.findViewById(R.id.node_recycler_view);
        assert recyclerView != null;
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext()));

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);

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
                // Create new fragment and transaction
                Fragment newFragment = new EditNodeEntryFragment();
                Bundle args = new Bundle();
                long position = (long)viewHolder.getAdapterPosition();
                long id = mAdapter.idTreeMap.get(position).node.id;
                lastEditedPosition = position;
                args.putLong(NODE_ID, id);
                newFragment.setArguments(args);
                FragmentTransaction transaction = getFragmentManager().beginTransaction();

                // Replace whatever is in the fragment_container view with this fragment,
                // and add the transaction to the back stack
                transaction.replace(R.id.orgnode_detail_container, newFragment);
                transaction.addToBackStack(null);

// Commit the transaction
                transaction.commit();
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

    @Override
    public void onResume(){
        super.onResume();
        if(lastEditedPosition > -1) {
            adapter.refreshItem(lastEditedPosition);
            adapter.notifyDataSetChanged();
            lastEditedPosition = -1;
        }
    }

    public class RecyclerViewAdapter
            extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private NavigableMap<Long, OrgNodeTree> idTreeMap;
        private final OrgNodeTree tree;

        public RecyclerViewAdapter(OrgNodeTree root) {
            tree = root;
            refresh();
        }

        void refresh(){
            idTreeMap = tree.getVisibleNodesArray();
//            holder.setup(holder.mItem.node);
            notifyDataSetChanged();
        }

        public void refreshItem(long position){
            OrgNodeTree tree = idTreeMap.get(position);
            if(tree != null) tree.node.updateAllNodes(getContext().getContentResolver());
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch(viewType){
                case 0:
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.detail_recycler_item, parent, false);
                    return new ViewHolder(view);
                case 1:
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.insert_node_before, parent, false);
                    return new InsertNodeViewHolder(view);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder _holder, final int position) {
            if(idHlightedPosition> -1 && (position==idHlightedPosition || position==idHlightedPosition+2)) return;
            final ViewHolder holder = (ViewHolder)_holder;
            holder.mItem = idTreeMap.get((long)position);
            holder.level = holder.mItem.node.level;
            holder.setup(holder.mItem.node);

            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    holder.mItem.toggleVisibility();
                    refresh();
                }
            });

            holder.mView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    idHlightedPosition = position;
                    insertItem(position);
                    notifyDataSetChanged();
                    return false;
                }
            });
        }

        /**
         * Add an item before and after position
         * @param position
         */
        private void insertItem(int position){
            NavigableMap<Long, OrgNodeTree> newIdTreeMap = new TreeMap<>();
            long newId = 0, oldId = 0;
            while(oldId<idTreeMap.size()) {
                if(oldId==(long)position || oldId==(long)position+1) newId++;
                newIdTreeMap.put(newId++, idTreeMap.get(oldId++));
            }
            idTreeMap = newIdTreeMap;
        }

        @Override
        public int getItemCount() {
            return idTreeMap.size();
        }

        @Override
        public int getItemViewType(int position) {
            if(idHlightedPosition>-1) {
                if (position == idHlightedPosition) return 1;
                if (position == idHlightedPosition + 2) return 1;
            }
            return 0;
        }

        public class InsertNodeViewHolder extends RecyclerView.ViewHolder {
             public InsertNodeViewHolder(View view) {
                 super(view);
             }
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;

            public OrgNodeTree mItem;
            private TextView titleView;
            private Button todoButton;
            private TextView levelView;
            private boolean levelFormatting = true;
            public long level;

            public ViewHolder(View view) {
                super(view);
                mView = view;

                titleView = (TextView) view.findViewById(R.id.outline_item_title);
                todoButton = (Button) view.findViewById(R.id.outline_item_todo);
                levelView = (TextView) view.findViewById(R.id.outline_item_level);
                todoButton.setOnClickListener(todoClick);

                int fontSize = PreferenceUtils.getFontSize();
                todoButton.setTextSize(fontSize);
            }

            private View.OnClickListener todoClick = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
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



            public void setup(OrgNode node) {
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
}


