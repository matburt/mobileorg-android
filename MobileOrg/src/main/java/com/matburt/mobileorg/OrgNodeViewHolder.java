package com.matburt.mobileorg;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.matburt.mobileorg.orgdata.OrgNode;
import com.matburt.mobileorg.orgdata.OrgNodeTree;
import com.matburt.mobileorg.util.PreferenceUtils;
import com.matburt.mobileorg.util.TodoDialog;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrgNodeViewHolder extends ItemViewHolder {
    public final View mView;
    public final Pattern urlPattern = Pattern.compile("\\[\\[[^\\]]*\\]\\[([^\\]]*)\\]\\]");
    public Button sameLevel, childLevel, deleteNodeButton;
    public Button todoButton, priorityButton;
    LinearLayout itemModifiers;
    OrgNode node;
    private TextView titleView, contentView;
    private TextView levelView;

//            @Override
//            public void onViewRecycled(OrgNodeViewHolder holder) {
//                holder.itemView.setOnLongClickListener(null);
//                super.onViewRecycled(holder);
//            }


//            @Override
//            public String toString() {
//                return super.toString() + " '" + mContentView.getText() + "'";
//            }


    public OrgNodeViewHolder(View view) {
        super(view);
        mView = view;

        titleView = (TextView) view.findViewById(R.id.outline_item_title);
        contentView = (TextView) view.findViewById(R.id.outline_item_content);
        todoButton = (Button) view.findViewById(R.id.outline_item_todo);
        priorityButton = (Button) view.findViewById(R.id.outline_item_priority);
        levelView = (TextView) view.findViewById(R.id.outline_item_level);

        itemModifiers = (LinearLayout) view.findViewById(R.id.item_modifiers);
        sameLevel = (Button) view.findViewById(R.id.insert_same_level);
        childLevel = (Button) view.findViewById(R.id.insert_neighbourg_level);
        deleteNodeButton = (Button) view.findViewById(R.id.delete_node);

        int fontSize = PreferenceUtils.getFontSize();
        todoButton.setTextSize(fontSize);
    }

    public void setupPriority(String priority) {
        if (TextUtils.isEmpty(priority)) {
            priorityButton.setVisibility(View.GONE);
        } else {
            priorityButton.setText(priority);
        }
    }

    public void applyLevelIndentation(long level, SpannableStringBuilder item) {
        String indentString = "";
        for(int i = 0; i < level; i++)
            indentString += "   ";

        this.levelView.setText(indentString);
    }

    public void setupTitle(String name, SpannableStringBuilder titleSpan) {
        titleView.setGravity(Gravity.LEFT);
        titleView.setTextSize(Style.titleFontSize[Math.min((int)getLevel()-1, Style.nTitleColors-1)]);
        if(getLevel()==1) titleView.setTypeface(null, Typeface.BOLD);
        else titleView.setTypeface(null, Typeface.NORMAL);

        if (name.startsWith("COMMENT"))
            titleSpan.setSpan(new ForegroundColorSpan(Style.gray), 0,
                    "COMMENT".length(), 0);
        else if (name.equals("Archive"))
            titleSpan.setSpan(new ForegroundColorSpan(Style.gray), 0,
                    "Archive".length(), 0);

        formatLinks(titleSpan);
    }

    private void formatLinks(SpannableStringBuilder titleSpan) {
        Matcher matcher = urlPattern.matcher(titleSpan);
        while(matcher.find()) {
            titleSpan.delete(matcher.start(), matcher.end());
            titleSpan.insert(matcher.start(), matcher.group(1));

            titleSpan.setSpan(new ForegroundColorSpan(Style.blue),
                    matcher.start(), matcher.start() + matcher.group(1).length(), 0);

            matcher = urlPattern.matcher(titleSpan);
        }
    }

    public void setup(OrgNodeTree root, boolean isSelected, Context context) {
        this.node = root.node;
        SpannableStringBuilder titleSpan = new SpannableStringBuilder(node.name);

        setupTitle(node.name, titleSpan);
        setupPriority(node.priority);
        TodoDialog.setupTodoButton(context, node, todoButton, true);

        if(root.getVisibility()== OrgNodeTree.Visibility.folded)
            setupChildrenIndicator(node, titleSpan, context);

//                titleSpan.setSpan(new StyleSpan(Typeface.NORMAL), 0, titleSpan.length(), 0);
        titleView.setText(titleSpan);
        int colorId = (int) Math.min(getLevel()-1,Style.nTitleColors-1);
        titleView.setTextColor(Style.titleColor[colorId]);
        mView.setSelected(isSelected);
        if (isSelected) itemModifiers.setVisibility(View.VISIBLE);
        else itemModifiers.setVisibility(View.GONE);
        String cleanedPayload = node.getCleanedPayload();
        contentView.setText(cleanedPayload);


        if(cleanedPayload.equals("")){
            RelativeLayout.LayoutParams layoutParams =
                    (RelativeLayout.LayoutParams)titleView.getLayoutParams();
            layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
            titleView.setLayoutParams(layoutParams);

            layoutParams =
                    (RelativeLayout.LayoutParams)todoButton.getLayoutParams();
            layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
            todoButton.setLayoutParams(layoutParams);

            contentView.setVisibility(View.GONE);
        } else {
            RelativeLayout.LayoutParams layoutParams =
                    (RelativeLayout.LayoutParams)titleView.getLayoutParams();
            layoutParams.removeRule(RelativeLayout.CENTER_VERTICAL);
            titleView.setLayoutParams(layoutParams);

            layoutParams =
                    (RelativeLayout.LayoutParams)todoButton.getLayoutParams();
            layoutParams.removeRule(RelativeLayout.CENTER_VERTICAL);
            todoButton.setLayoutParams(layoutParams);


            contentView.setVisibility(View.VISIBLE);
        }
    }

    public void setupChildrenIndicator(OrgNode node, SpannableStringBuilder titleSpan, Context context) {
        if (node.hasChildren(context.getContentResolver())) {
            titleSpan.append("...");
            titleSpan.setSpan(new ForegroundColorSpan(Style.foreground),
                    titleSpan.length() - "...".length(), titleSpan.length(), 0);
        }
    }

    private long getLevel(){
        if(node != null) return node.level;
        return -1;
    }
}