package com.matburt.mobileorg2.Gui;

import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.matburt.mobileorg2.OrgData.OrgNode;
import com.matburt.mobileorg2.OrgData.OrgProviderUtils;
import com.matburt.mobileorg2.R;

import java.util.ArrayList;

public class SearchActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    RecyclerViewAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search);

        Log.v("search", "hello world");

        recyclerView = (RecyclerView) findViewById(R.id.search_recycler_view);
        assert recyclerView != null;
//        recyclerView.addItemDecoration(new DividerItemDecoration(getContext()));

        adapter = new RecyclerViewAdapter();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);


        // Get the intent, verify the action and get the query
        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Log.v("search", "intent is ok");
            doSearch(query);
        }
    }

    private void doSearch(String query) {


        Cursor result = OrgProviderUtils.search("%" + query.trim() + "%",
                getContentResolver());
        adapter.items = OrgProviderUtils
                .orgDataCursorToArrayList(result);
        Log.v("search", "in adapter : " + adapter.items);
        adapter.notifyDataSetChanged();
    }

    public class RecyclerViewAdapter
            extends RecyclerView.Adapter<ViewHolder> {
        ArrayList<OrgNode> items;

        public RecyclerViewAdapter() {
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.search_item_layout, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            Log.v("search", "onbind");
            TextView title = (TextView) holder.itemView.findViewById(R.id.title);
            title.setText(items.get(position).name);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        OrgNode node;

        public ViewHolder(View itemView) {
            super(itemView);
        }
    }

}
