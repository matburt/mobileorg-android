package com.matburt.mobileorg;

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
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.matburt.mobileorg.OrgData.OrgContract.Timestamps;
import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.OrgData.OrgNodeTimeDate;
import com.matburt.mobileorg.util.OrgNodeNotFoundException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.TimeZone;
import java.util.TreeMap;


/**
 * A fragment representing a single OrgNode detail screen.
 * This fragment is either contained in a {@link OrgNodeListActivity}
 * in two-pane mode (on tablets) or a {@link OrgNodeDetailActivity}
 * on handsets.
 */
public class AgendaFragment extends Fragment {

    RecyclerViewAdapter adapter;
    RecyclerView recyclerView;
    ArrayList<OrgNode> nodesList;
    ArrayList<OrgNodeTimeDate>  daysList;
    ArrayList<PositionHelper> items;
    private ContentResolver resolver;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public AgendaFragment() {
    }

    static void setItemModifiersVisibility(View view, int visibility) {
        LinearLayout itemModifiers = (LinearLayout) view.findViewById(R.id.item_modifiers);
        if (itemModifiers != null) itemModifiers.setVisibility(visibility);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.resolver = getActivity().getContentResolver();


        Cursor cursor = resolver.query(Timestamps.CONTENT_URI,
                new String[]{Timestamps.NODE_ID, Timestamps.TIMESTAMP, Timestamps.TYPE},
                null, null, Timestamps.TIMESTAMP);

        nodesList = new ArrayList<>();
        items = new ArrayList<>();
        daysList = new ArrayList<>();

        // Nodes with only a deadline or a scheduled but not both
        HashSet<Long> orphanTimestampsNodes = new HashSet<>();

        // Nodes with a deadline AND a scheduled date
        HashSet<Long> rangedTimestampsNodes = new HashSet<>();

        if (cursor != null) {
//            Log.v("time", "count : " + cursor.getCount());

            while (cursor.moveToNext()) {
                long nodeId = cursor.getLong(cursor.getColumnIndexOrThrow(Timestamps.NODE_ID));
//                Log.v("time", "nodeId agenda : " + nodeId);

                if (orphanTimestampsNodes.contains(nodeId)) {
                    orphanTimestampsNodes.remove(nodeId);
                    rangedTimestampsNodes.add(nodeId);
                } else {
                    orphanTimestampsNodes.add(nodeId);
                }
            }
            cursor.close();
        }


        TreeMap<Long, ArrayList<OrgNode>> nodeIdsForEachDay = new TreeMap<>();

        for (Long nodeId : orphanTimestampsNodes) {
            try {
                OrgNode node = new OrgNode(nodeId, resolver);
                Long day;
                if (node.getScheduled().getEpochTime() < 0) {
                    day = node.getDeadline().getEpochTime() / (24 * 3600);
//                    Log.v("deadline", "agenda deadline : " + day);
                } else {
                    day = node.getScheduled().getEpochTime() / (24 * 3600);
                    Log.v("timestamp", "agenda scheduled: " + day);
                }

                if (!nodeIdsForEachDay.containsKey(day))
                    nodeIdsForEachDay.put(day, new ArrayList<OrgNode>());
                nodeIdsForEachDay.get(day).add(node);
            } catch (OrgNodeNotFoundException e) {
                // If we are here, it means an OrgNode has been deleted but not the corresponding
                // timestamps from the Timestamp database. So we do the cleanup.
                OrgNodeTimeDate.deleteTimestamp(getActivity(), nodeId, null);
                e.printStackTrace();
            }
        }

        for (Long nodeId : rangedTimestampsNodes) {
            try {
                OrgNode node = new OrgNode(nodeId, resolver);

                long firstDay = node.getScheduled().getEpochTime() / (24 * 3600);
                long lastDay = node.getDeadline().getEpochTime() / (24 * 3600);

                for (long day = firstDay; day <= lastDay; day++) {
                    if (!nodeIdsForEachDay.containsKey(day))
                        nodeIdsForEachDay.put(day, new ArrayList<OrgNode>());
                    nodeIdsForEachDay.get(day).add(node);
                }
            } catch (OrgNodeNotFoundException e) {
                e.printStackTrace();
            }

        }

        int dayCursor = 0, nodeCursor = 0;
        for (long day : nodeIdsForEachDay.keySet()) {
            daysList.add(new OrgNodeTimeDate(day * 3600 * 24));
            items.add(new PositionHelper(dayCursor++, Type.kDate));
            for (OrgNode node : nodeIdsForEachDay.get(day)) {
                nodesList.add(node);
                items.add(new PositionHelper(nodeCursor++, Type.kNode));
            }
        }

        adapter = new RecyclerViewAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.node_summary_recycler_fragment, container, false);

        recyclerView = (RecyclerView) rootView.findViewById(R.id.node_recycler_view);
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

    enum Type {
        kNode,
        kDate
    }

    class PositionHelper {
        int position;
        Type type;

        PositionHelper(int position, Type type) {
            this.position = position;
            this.type = type;
        }
    }



    public class RecyclerViewAdapter
            extends RecyclerView.Adapter<ItemViewHolder> {

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

            // The day associated with this item
            int dayPosition = position;
            while(items.get(dayPosition).type != Type.kDate && dayPosition > 0) dayPosition--;
            OrgNodeTimeDate date = daysList.get(items.get(dayPosition).position);

            TextView title = (TextView) holder.itemView.findViewById(R.id.title);
            TextView details = (TextView) holder.itemView.findViewById(R.id.details);

            title.setText(node.name);

            String textDetails = node.getPayload();

            if (textDetails.equals("")) {
                RelativeLayout.LayoutParams layoutParams =
                        (RelativeLayout.LayoutParams) title.getLayoutParams();
                layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
                title.setLayoutParams(layoutParams);

                details.setVisibility(View.GONE);
            } else {
                details.setText(textDetails);
            }

            TextView content = (TextView) holder.itemView.findViewById(R.id.date);

            // If event spans on more than one day (=86499 sec), tag the enclosed days as 'all days'
            if(node.getRangeInSec() > 86400 && date.isBetween(node.getScheduled(), node.getDeadline())){
                content.setText(R.string.all_day);
            } else {
                content.setText(node.getScheduled().toString(false));
            }

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
            final OrgNodeTimeDate date = daysList.get(items.get(position).position);

            TextView title = (TextView) holder.itemView.findViewById(R.id.outline_item_title);
            title.setText(date.toString(true));

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

        /**
         * The view holder for the date
         */
        private class DateViewHolder extends ItemViewHolder {
            public DateViewHolder(View view) {
                super(view);
            }
        }

        /**
         * The view holder for the node items
         */
        private class OrgItemViewHolder extends ItemViewHolder {
            public OrgItemViewHolder(View view) {
                super(view);
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


