package com.matburt.mobileorg.gui;

import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.matburt.mobileorg.orgdata.OrgContract;
import com.matburt.mobileorg.orgdata.OrgFile;
import com.matburt.mobileorg.orgdata.OrgNode;
import com.matburt.mobileorg.orgdata.OrgProviderUtils;
import com.matburt.mobileorg.OrgNodeDetailActivity;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.util.OrgFileNotFoundException;

import java.util.ArrayList;

public class SearchActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    RecyclerViewAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search);


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
            doSearch(query);
        }
    }

    private void doSearch(String query) {


        Cursor result = OrgProviderUtils.search("%" + query.trim() + "%",
                getContentResolver());
        adapter.items = OrgProviderUtils
                .orgDataCursorToArrayList(result);
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
            final OrgNode node = items.get(position);
            TextView title = (TextView) holder.itemView.findViewById(R.id.title);
            TextView payload = (TextView) holder.itemView.findViewById(R.id.payload);
            title.setText(node.name);
            if (node.getCleanedPayload().equals("")) payload.setVisibility(View.GONE);
            else {
                payload.setVisibility(View.VISIBLE);
                payload.setText(node.getCleanedPayload());
            }

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        OrgFile file = new OrgFile(node.fileId, getContentResolver());
                        Intent intent = new Intent(SearchActivity.this, OrgNodeDetailActivity.class);
                        intent.putExtra(OrgContract.NODE_ID, file.nodeId);
                        intent.putExtra(OrgContract.POSITION, node.id);
                        startActivity(intent);
                    } catch (OrgFileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            });
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
