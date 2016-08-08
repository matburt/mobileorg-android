package com.matburt.mobileorg2;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.matburt.mobileorg2.OrgData.OrgContract.Timestamps;
import com.matburt.mobileorg2.OrgData.OrgNode;
import com.matburt.mobileorg2.util.OrgNodeNotFoundException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;


/**
 * A fragment representing a single OrgNode detail screen.
 * This fragment is either contained in a {@link OrgNodeListActivity}
 * in two-pane mode (on tablets) or a {@link OrgNodeDetailActivity}
 * on handsets.
 */
public class AgendaFragment extends Fragment {

    private ContentResolver resolver;

    RecyclerViewAdapter adapter;
    RecyclerView recyclerView;
    ArrayList<OrgNode> nodesList;
    ArrayList<String>  daysList;
    ArrayList<PositionHelper> items;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public AgendaFragment() {
    }

    enum Type {
        kNode,
        kDate
    };

    class PositionHelper {
        int position;
        Type type;

        PositionHelper(int position, Type type){
            this.position = position;
            this.type = type;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.resolver = getActivity().getContentResolver();

        Cursor cursor = resolver.query(Timestamps.CONTENT_URI,
                new String[]{Timestamps.NODE_ID, Timestamps.TIMESTAMP, Timestamps.TYPE},
                null, null, Timestamps.TIMESTAMP);

        nodesList = new ArrayList<>();

        if(cursor!=null) {
            Log.v("time","count : "+cursor.getCount());
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                long nodeId = cursor.getLong(cursor.getColumnIndexOrThrow(Timestamps.NODE_ID));
                Log.v("time", "cursor id, time, type : "+nodeId + ", " + cursor.getLong(cursor.getColumnIndexOrThrow(Timestamps.TIMESTAMP)) + ", " + cursor.getLong(cursor.getColumnIndexOrThrow(Timestamps.TYPE)));
                try {
                    nodesList.add(new OrgNode(nodeId, resolver));
                } catch (OrgNodeNotFoundException e) {
                    e.printStackTrace();
                }
                cursor.moveToNext();
            }
            cursor.close();
        }

        daysList = new ArrayList<>();
        items = new ArrayList<>();
        long prevDay = -1;
        long day;
        int nodeCursor = 0;
        int dayCursor = 0;

        for(OrgNode node : nodesList){
            day = node.getScheduled().getEpochTime()/(24*3600);

            if(day != prevDay) {
                daysList.add(SimpleDateFormat.getDateInstance().format(new Date(node.getScheduled().getEpochTime())));
                items.add(new PositionHelper(dayCursor++, Type.kDate));
                prevDay = day;
            }
            items.add(new PositionHelper(nodeCursor++, Type.kNode));
        }

        adapter = new RecyclerViewAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.node_summary_recycler_fragment, container, false);

        recyclerView = (RecyclerView)rootView.findViewById(R.id.node_recycler_view);
        assert recyclerView != null;
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext()));

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);

        return rootView;
    }

    @Override
    public void onResume(){
        super.onResume();
    }


    public class RecyclerViewAdapter
            extends RecyclerView.Adapter<ItemViewHolder> {

        /**
         * The view holder for the date
         */
        private class DateViewHolder extends ItemViewHolder{
            public DateViewHolder(View view) {
                super(view);
            }
        }

        /**
         * The view holder for the node items
         */
        private class OrgItemViewHolder extends ItemViewHolder{
            public OrgItemViewHolder(View view) {
                super(view);
            }
        }

        public RecyclerViewAdapter() {

        }



        @Override
        public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;


            if(viewType == Type.kDate.ordinal()){
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.days_view_holder, parent, false);
                return new DateViewHolder(view);
            } else {
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.agenda_recycler_item, parent, false);
                return new OrgItemViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(final ItemViewHolder holder, final int position) {
            int type = getItemViewType(position);

            if(type == Type.kDate.ordinal()) onBindDateHolder((DateViewHolder)holder, position);
            else onBindOrgItemHolder((OrgItemViewHolder)holder, position);
        }

        private void onBindOrgItemHolder(final OrgItemViewHolder holder, int position){
            final OrgNode node = nodesList.get(items.get(position).position);

            TextView title = (TextView) holder.itemView.findViewById(R.id.title);
            title.setText(node.name);

            TextView details = (TextView) holder.itemView.findViewById(R.id.details);
            details.setText(node.getPayload());

            TextView content = (TextView) holder.itemView.findViewById(R.id.date);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date(node.getScheduled().getEpochTime()));

            content.setText(SimpleDateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(node.getScheduled().getEpochTime())));

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditNodeFragment.createEditNodeFragment((int)node.id, -1, -1, getContext());
                }
            });

            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    return true;
                }
            });
        }

        private void onBindDateHolder(final DateViewHolder holder, int position){
            final String date = daysList.get(items.get(position).position);

            TextView title = (TextView) holder.itemView.findViewById(R.id.outline_item_title);
            title.setText(date);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });

            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    return true;
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public int getItemViewType(int position){
           return items.get(position).type.ordinal();
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


