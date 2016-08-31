package com.matburt.mobileorg;

import android.support.v7.widget.RecyclerView;
import android.view.View;

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