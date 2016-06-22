package com.matburt.mobileorg2;

import android.support.v7.widget.RecyclerView;
import android.view.View;

public class ItemViewHolder extends RecyclerView.ViewHolder {
    public final View mView;

    public long level;

    public ItemViewHolder(View view) {
        super(view);
        mView = view;

    }
}