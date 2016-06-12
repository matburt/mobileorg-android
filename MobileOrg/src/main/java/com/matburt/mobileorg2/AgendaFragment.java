package com.matburt.mobileorg2;

import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.matburt.mobileorg2.Gui.Outline.OutlineAdapter;
import com.matburt.mobileorg2.OrgData.OrgContract;
import com.matburt.mobileorg2.OrgData.OrgFileParser;
import com.matburt.mobileorg2.OrgData.OrgNode;
import com.matburt.mobileorg2.OrgData.OrgNodeTree;
import com.matburt.mobileorg2.OrgData.OrgProviderUtils;
import com.matburt.mobileorg2.util.OrgNodeNotFoundException;
import com.matburt.mobileorg2.util.PreferenceUtils;
import com.matburt.mobileorg2.util.TodoDialog;

import java.util.ArrayList;
import java.util.NavigableMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A fragment representing a single OrgNode detail screen.
 * This fragment is either contained in a {@link OrgNodeListActivity}
 * in two-pane mode (on tablets) or a {@link OrgNodeDetailActivity}
 * on handsets.
 */
public class AgendaFragment extends Fragment {

    private ContentResolver resolver;

    private long nodeId;
    private long lastEditedPosition;
    private long idHlightedPosition;
    private View highlightedView = null;


    RecyclerViewAdapter adapter;
    RecyclerView recyclerView;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public AgendaFragment() {
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.resolver = getActivity().getContentResolver();

        Log.v("scheduled","path : "+OrgContract.OrgData.CONTENT_URI);
        Cursor cursor = resolver.query(OrgContract.OrgData.CONTENT_URI,
                OrgContract.OrgData.DEFAULT_COLUMNS,
                "scheduled>0", null, null);
//        ArrayList<OrgNode> list = OrgProviderUtils.orgDataCursorToArrayList(cursor);
        cursor.moveToFirst();
        while(cursor.moveToNext()){
            Log.v("scheduled", "val : "+cursor.getInt(cursor.getColumnIndex("scheduled")));
        }
        cursor.close();


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

        class SimpleItemTouchHelperCallback extends ItemTouchHelper.Callback {
            private final RecyclerViewAdapter mAdapter;
            public SimpleItemTouchHelperCallback(RecyclerViewAdapter adapter) {
                mAdapter = adapter;
            }

            @Override
            public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
                int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
                return makeMovementFlags(dragFlags, swipeFlags);
            }


            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                return false;
            }
        };

        SimpleItemTouchHelperCallback callback = new SimpleItemTouchHelperCallback(adapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);

        return rootView;
    }

    @Override
    public void onResume(){
        super.onResume();
    }

    public class RecyclerViewAdapter
            extends RecyclerView.Adapter<RecyclerViewAdapter.ItemViewHolder> {

        public RecyclerViewAdapter() {

        }



        @Override
        public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.agenda_recycler_item, parent, false);
            return new ItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ItemViewHolder holder, final int position) {

            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });

            holder.mView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    return true;
                }
            });
        }

        @Override
        public int getItemCount() {
            return 0;
        }

        public class ItemViewHolder extends RecyclerView.ViewHolder {
            public final View mView;

            public long level;

            public ItemViewHolder(View view) {
                super(view);
                mView = view;

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

    static void setItemModifiersVisibility(View view, int visibility){
        LinearLayout itemModifiers = (LinearLayout) view.findViewById(R.id.item_modifiers);
        if(itemModifiers != null) itemModifiers.setVisibility(visibility);
    }
}


