//package com.matburt.mobileorg;
//
//import android.support.v7.widget.RecyclerView;
//
//import android.annotation.SuppressLint;
//import android.support.v7.widget.RecyclerView;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.TextView;
//
//@SuppressLint("InflateParams")
//public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
//
//    private FruitModel[] fruitsData;
//
//    public MyAdapter(FruitModel[] fruitsData) {
//        this.fruitsData = fruitsData;
//
//    }
//
//    @Override
//    public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,int viewType) {
//
//        //create view and viewholder
//        View itemLayoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fruit, null);
//        ViewHolder viewHolder = new ViewHolder(itemLayoutView);
//        return viewHolder;
//    }
//
//    // Replace the contents of a view
//    @Override
//    public void onBindViewHolder(ViewHolder viewHolder, int position) {
//        viewHolder.txtViewTitle.setText(fruitsData[position].getFruitName());
//    }
//
//    // class to hold a reference to each item of RecyclerView
//    public static class ViewHolder extends RecyclerView.ViewHolder {
//
//        public TextView txtViewTitle;
//
//        public ViewHolder(View itemLayoutView) {
//            super(itemLayoutView);
//            txtViewTitle = (TextView) itemLayoutView.findViewById(R.id.fruit_name);
//        }
//    }
//
//    // Returns the size of the fruitsData
//    @Override
//    public int getItemCount() {
//        return fruitsData.length;
//    }
//}