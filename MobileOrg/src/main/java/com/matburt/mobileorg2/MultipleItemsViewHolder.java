package com.matburt.mobileorg2;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.matburt.mobileorg2.OrgData.OrgNode;
import com.matburt.mobileorg2.OrgData.OrgNodeTree;
import com.matburt.mobileorg2.util.PreferenceUtils;
import com.matburt.mobileorg2.util.TodoDialog;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MultipleItemsViewHolder extends RecyclerView.ViewHolder {
    View view;

    public MultipleItemsViewHolder(View view) {
        super(view);
        this.view = view;
    }

//            @Override
//            public void onViewRecycled(ItemViewHolder holder) {
//                holder.itemView.setOnLongClickListener(null);
//                super.onViewRecycled(holder);
//            }



//            @Override
//            public String toString() {
//                return super.toString() + " '" + mContentView.getText() + "'";
//            }



}