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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.matburt.mobileorg.orgdata.OrgContract.Timestamps;
import com.matburt.mobileorg.orgdata.OrgNode;
import com.matburt.mobileorg.orgdata.OrgNodeTimeDate;
import com.matburt.mobileorg.util.OrgNodeNotFoundException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.TreeMap;


/**
 * A fragment representing a single OrgNode detail screen.
 * This fragment is either contained in a {@link OrgNodeListActivity}
 * in two-pane mode (on tablets) or a {@link OrgNodeDetailActivity}
 * on handsets.
 */
public class AgendaFragment extends Fragment {

    class AgendaItem {
        public OrgNodeTimeDate.TYPE type;
        public OrgNode node;
        public String text;
        long time;
        public AgendaItem(OrgNode node, OrgNodeTimeDate.TYPE type, long time){
            this.node = node;
            this.type = type;
            this.time = time;

            OrgNodeTimeDate date = new OrgNodeTimeDate(time);

            if(time < 0 || (node.getRangeInSec() > 86400 && date.isBetween(node.getScheduled(), node.getDeadline()))){
                text = getActivity().getResources().getString(R.string.all_day);
            } else {
                text = date.toString(false);
            }

        }
    }

    RecyclerViewAdapter adapter;
    RecyclerView recyclerView;
    ArrayList<AgendaItem> nodesList;
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

            while (cursor.moveToNext()) {
                long nodeId = cursor.getLong(cursor.getColumnIndexOrThrow(Timestamps.NODE_ID));

                if (orphanTimestampsNodes.contains(nodeId)) {
                    orphanTimestampsNodes.remove(nodeId);
                    rangedTimestampsNodes.add(nodeId);
                } else {
                    orphanTimestampsNodes.add(nodeId);
                }
            }
            cursor.close();
        }


        TreeMap<Long, ArrayList<AgendaItem>> nodeIdsForEachDay = new TreeMap<>();

        for (Long nodeId : orphanTimestampsNodes) {
            try {
                OrgNode node = new OrgNode(nodeId, resolver);
                Long day, time;
                OrgNodeTimeDate.TYPE type;
                if (node.getScheduled().getEpochTime() < 0) {
                    type = OrgNodeTimeDate.TYPE.Deadline;
                    time = node.getDeadline().getEpochTime();
                } else {
                    time = node.getScheduled().getEpochTime();
                    type = OrgNodeTimeDate.TYPE.Scheduled;
                }

                day = time / (24*3600);

                if (!nodeIdsForEachDay.containsKey(day))
                    nodeIdsForEachDay.put(day, new ArrayList<AgendaItem>());
                nodeIdsForEachDay.get(day).add(new AgendaItem(node, type, time));
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
                boolean scheduledBeforeDeadline = node.getScheduled().getEpochTime() < node.getDeadline().getEpochTime();

                long firstTime = scheduledBeforeDeadline ? node.getScheduled().getEpochTime() : node.getDeadline().getEpochTime();
                long lastTime  = scheduledBeforeDeadline ? node.getDeadline().getEpochTime()  : node.getScheduled().getEpochTime();
                long firstDay = firstTime / (24 * 3600);
                long lastDay = lastTime / (24 * 3600);
                OrgNodeTimeDate.TYPE type;
                long time;
                for (long day = firstDay; day <= lastDay; day++) {
                    if(firstDay == lastDay){
                        if (!nodeIdsForEachDay.containsKey(day))
                            nodeIdsForEachDay.put(day, new ArrayList<AgendaItem>());
                        nodeIdsForEachDay.get(day).add(new AgendaItem(node, scheduledBeforeDeadline ? OrgNodeTimeDate.TYPE.Scheduled : OrgNodeTimeDate.TYPE.Deadline, firstTime));
                        nodeIdsForEachDay.get(day).add(new AgendaItem(node, scheduledBeforeDeadline ? OrgNodeTimeDate.TYPE.Deadline : OrgNodeTimeDate.TYPE.Scheduled, lastTime));
                    } else {
                        if (day == firstDay) {
                            type = scheduledBeforeDeadline ? OrgNodeTimeDate.TYPE.Scheduled : OrgNodeTimeDate.TYPE.Deadline;
                            time = firstTime;
                        } else if (day == lastDay) {
                            type = scheduledBeforeDeadline ? OrgNodeTimeDate.TYPE.Deadline : OrgNodeTimeDate.TYPE.Scheduled;
                            time = lastTime;
                        } else {
                            type = OrgNodeTimeDate.TYPE.Timestamp;
                            time = -1;
                        }
                        if (!nodeIdsForEachDay.containsKey(day))
                            nodeIdsForEachDay.put(day, new ArrayList<AgendaItem>());
                        nodeIdsForEachDay.get(day).add(new AgendaItem(node, type, time));
                    }
                }
            } catch (OrgNodeNotFoundException e) {
                e.printStackTrace();
            }
        }

        int dayCursor = 0, nodeCursor = 0;
        for (long day : nodeIdsForEachDay.keySet()) {
            daysList.add(new OrgNodeTimeDate(day * 3600 * 24));
            items.add(new PositionHelper(dayCursor++, Type.kDate));
            Collections.sort(nodeIdsForEachDay.get(day), new TimeComparator());
            for (AgendaItem item: nodeIdsForEachDay.get(day)) {
                nodesList.add(item);
                items.add(new PositionHelper(nodeCursor++, Type.kNode));
            }
        }

        adapter = new RecyclerViewAdapter();
    }

    class TimeComparator implements Comparator<AgendaItem> {
        @Override
        public int compare(AgendaItem a, AgendaItem b) {
            return a.time < b.time ? -1 : a.time == b.time ? 0 : 1;
        }
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
            final AgendaItem item = nodesList.get(items.get(position).position);
            final OrgNode node = item.node;

            // The day associated with this item
            int dayPosition = position;
            while(items.get(dayPosition).type != Type.kDate && dayPosition > 0) dayPosition--;

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
            content.setText(item.text);

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


