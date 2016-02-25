package com.matburt.mobileorg;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.matburt.mobileorg.Gui.Theme.DefaultTheme;
import com.matburt.mobileorg.OrgData.OrgFileParser;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.OrgData.OrgProviderUtils;
import com.matburt.mobileorg.util.OrgNodeNotFoundException;
import com.matburt.mobileorg.util.OrgUtils;
import com.matburt.mobileorg.util.PreferenceUtils;

import java.util.ArrayList;
import java.util.NavigableMap;
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
    private OrgNodeTree tree;
    private int gray, red, green, yellow, blue, foreground;

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

        gray = ContextCompat.getColor(getContext(), R.color.colorGray);
        red = ContextCompat.getColor(getContext(), R.color.colorRed);
        green = ContextCompat.getColor(getContext(), R.color.colorGreen);
        yellow = ContextCompat.getColor(getContext(), R.color.colorYellow);
        blue = ContextCompat.getColor(getContext(), R.color.colorBlue);
        foreground = ContextCompat.getColor(getContext(), R.color.colorPrimary);
        
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.view_fragment, container, false);

        if (tree ==null) return rootView;

        RecyclerView recyclerView = (RecyclerView)rootView.findViewById(R.id.node_recycler_view);
        assert recyclerView != null;
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext()));

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        setupRecyclerView(recyclerView);

        return rootView;
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        recyclerView.setAdapter(new SimpleItemRecyclerViewAdapter(tree));
    }


    public class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

        private NavigableMap<Integer, OrgNodeTree> idTreeMap;
        private final OrgNodeTree tree;

        public SimpleItemRecyclerViewAdapter(OrgNodeTree root) {
            tree = root;
            refresh();
        }

        void refresh(){
            idTreeMap = tree.getVisibleNodesArray();
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.detail_recycler_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.mItem = idTreeMap.get(position);
            OrgNode theNode = idTreeMap.get(position).node;
            holder.setup(holder.mItem.node);
            setupTodo(holder.todoButton, theNode.todo);
            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    holder.mItem.toggleVisibility();
                    refresh();
                }
            });
        }

        public void setupTodo(Button button, String todo) {
            if(!TextUtils.isEmpty(todo)) {
                Spannable todoSpan = new SpannableString(todo + " ");

                boolean active = OrgProviderUtils.isTodoActive(todo, resolver);

                int red = ContextCompat.getColor(getContext(), R.color.colorRed);
                int green = ContextCompat.getColor(getContext(), R.color.colorGreen);
                todoSpan.setSpan(new ForegroundColorSpan(active ? red : green), 0,
                        todo.length(), 0);
                button.setText(todoSpan);
                button.setVisibility(View.VISIBLE);
                button.setTextColor(active ? red : green);
            } else {
                button.setVisibility(View.GONE);
            }
        }


        
        @Override
        public int getItemCount() {
            return idTreeMap.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;

            public OrgNodeTree mItem;
            private TextView titleView;
            private TextView tagsView;
            private Button todoButton;
            private TextView levelView;
            private boolean levelFormatting = true;

            public ViewHolder(View view) {
                super(view);
                mView = view;

                titleView = (TextView) view.findViewById(R.id.outline_item_title);
                tagsView = (TextView) view.findViewById(R.id.outline_item_tags);
                todoButton = (Button) view.findViewById(R.id.outline_item_todo);
                levelView = (TextView) view.findViewById(R.id.outline_item_level);
                todoButton.setOnClickListener(todoClick);

                int fontSize = PreferenceUtils.getFontSize();
                tagsView.setTextSize(fontSize);
                todoButton.setTextSize(fontSize);
            }

            private View.OnClickListener todoClick = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    createTodoDialog().show();
                }
            };

            private Dialog createTodoDialog() {
                ArrayList<String> todos = PreferenceUtils.getSelectedTodos();

                if (todos.size() == 0)
                    todos = OrgProviderUtils.getTodos(getContext().getContentResolver());

                final ArrayList<String> todoList = todos;
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle(getResources().getString(R.string.todo_state))
				.setItems(todoList.toArray(new CharSequence[todoList.size()]),
                        new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								String selectedTodo = todoList.get(which);
								setNewTodo(selectedTodo);
							}
						});
                return builder.create();
            }



	private void setNewTodo(String selectedTodo) {
		if (selectedTodo.equals(mItem.node.todo))
			return;

		ContentResolver resolver = getContext().getContentResolver();

		OrgNode newNode;
		try {
			newNode = new OrgNode(mItem.node.id, resolver);
		} catch (OrgNodeNotFoundException e) {
			e.printStackTrace();
			return;
		}
		newNode.todo = selectedTodo;
		mItem.node.generateApplyWriteEdits(newNode, null, resolver);
		mItem.node.write(resolver);
		OrgUtils.announceSyncDone(getContext());
	}
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
                titleView.setTextSize(PreferenceUtils.getFontSize());

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

            public void setupTags(String tags, String tagsInherited) {
                if(!TextUtils.isEmpty(tags) || !TextUtils.isEmpty(tagsInherited)) {
                    if (!TextUtils.isEmpty(tagsInherited))
                        tagsView.setText(tags + "::" + tagsInherited);
                    else
                        tagsView.setText(tags);


                    tagsView.setTextColor(gray);
                    tagsView.setVisibility(View.VISIBLE);
                } else
                    tagsView.setVisibility(View.GONE);
            }

            public void setLevelFormating(boolean enabled) {
                this.levelFormatting = enabled;
            }

            public void setup(OrgNode node) {
                this.mItem.node = node;
                setupTags(node.tags, node.tags_inherited);

                SpannableStringBuilder titleSpan = new SpannableStringBuilder(node.name);

                if(node.name.startsWith(OrgFileParser.BLOCK_SEPARATOR_PREFIX)) {
                    setupAgendaBlock(titleSpan);
                    return;
                }

                if (levelFormatting)
                    applyLevelFormating(node.level, titleSpan);
                setupTitle(node.name, titleSpan);
                setupPriority(node.priority, titleSpan);
//                setupTodo(node.todo, theme, resolver);

                if (levelFormatting)
                    applyLevelIndentation(node.level, titleSpan);

                if(this.mItem.getVisibility()== OrgNodeTree.Visibility.folded)
                    setupChildrenIndicator(node, titleSpan);

                titleSpan.setSpan(new StyleSpan(Typeface.NORMAL), 0, titleSpan.length(), 0);
                titleView.setText(titleSpan);
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


