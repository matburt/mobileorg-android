package com.matburt.mobileorg;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TimePicker;
import android.widget.Toast;

import com.matburt.mobileorg.orgdata.OrgContract;
import com.matburt.mobileorg.orgdata.OrgFile;
import com.matburt.mobileorg.orgdata.OrgNode;
import com.matburt.mobileorg.orgdata.OrgNodeTimeDate;
import com.matburt.mobileorg.util.OrgNodeNotFoundException;
import com.matburt.mobileorg.util.TodoDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class EditNodeFragment extends Fragment {
    public static String NODE_ID = "node_id";
    public static String PARENT_ID = "parent_id";
    static public long nodeId = -1, parentId = -1;
    static Button schedule_date, deadline_date, schedule_time, deadline_time;
    static OrgNodeTimeDate timeDate;
    static private OrgNode node;
    EditText title, content;
    Context context;
    private int position = 0;
    private Button todo, priority;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.edit_node_entry, container, false);
        context = getContext();


        todo = (Button) rootView.findViewById(R.id.todo);
        priority = (Button) rootView.findViewById(R.id.priority);
        schedule_date = (Button) rootView.findViewById(R.id.scheduled_date);
        deadline_date = (Button) rootView.findViewById(R.id.deadline_date);

        schedule_time = (Button) rootView.findViewById(R.id.scheduled_time);
        deadline_time = (Button) rootView.findViewById(R.id.deadline_time);

        title = (EditText) getActivity().findViewById(R.id.title);
        content = (EditText) getActivity().findViewById(R.id.content);

        Bundle bundle = getArguments();
        if (bundle != null) {
            nodeId = bundle.getLong(NODE_ID, -1);
            parentId = bundle.getLong(PARENT_ID, -1);
            position = bundle.getInt(OrgContract.OrgData.POSITION, 0);
        }

        ContentResolver resolver = getActivity().getContentResolver();

        if (nodeId > -1) {
            // Editing already existing node
            try {
                node = new OrgNode(nodeId, resolver);
            } catch (OrgNodeNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            createNewNode(resolver);
        }


        /**
         * Save user changes (scheduled and time values) if a configuration change occured
         * (like screen rotation or call)
         */
        if(savedInstanceState != null) {
            node.getScheduled().year = (int) savedInstanceState.getLong("year_schedule");
            node.getScheduled().monthOfYear = (int) savedInstanceState.getLong("month_schedule");
            node.getScheduled().dayOfMonth = (int) savedInstanceState.getLong("day_schedule");
            node.getScheduled().startTimeOfDay = (int) savedInstanceState.getLong("hour_schedule");
            node.getScheduled().startMinute = (int) savedInstanceState.getLong("minute_schedule");

            node.getDeadline().year = (int) savedInstanceState.getLong("year_deadline");
            node.getDeadline().monthOfYear = (int) savedInstanceState.getLong("month_deadline");
            node.getDeadline().dayOfMonth = (int) savedInstanceState.getLong("day_deadline");
            node.getDeadline().startTimeOfDay = (int) savedInstanceState.getLong("hour_deadline");
            node.getDeadline().startMinute = (int) savedInstanceState.getLong("minute_deadline");

            node.todo = savedInstanceState.getString("todo");
            node.priority = savedInstanceState.getString("priority");
        }

        TodoDialog.setupTodoButton(getContext(), node, todo, false);


        todo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TodoDialog(getContext(), node, todo, false);
            }
        });

        priority.setText(node.priority);
        priority.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPriorityDialog();
            }
        });

        title.setText(node.name);
        String payload = node.getCleanedPayload();
        if (payload.length() > 0) {
            content.setText(payload);
        }

        title.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                title.setFocusable(true);
                title.requestFocus();
                return false;
            }
        });

        final LinearLayout layout = (LinearLayout) rootView.findViewById(R.id.view_fragment_layout);
        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                layout.requestFocus();
                return true;
            }
        });

        schedule_date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timeDate = node.getScheduled();
                setupDateDialog();
            }
        });

        deadline_date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timeDate = node.getDeadline();
                setupDateDialog();
            }
        });


        schedule_time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timeDate = node.getScheduled();
                setupTimeDialog();
            }
        });

        deadline_time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timeDate = node.getDeadline();
                setupTimeDialog();
            }
        });


        setupTimeStampButtons();

        getActivity().invalidateOptionsMenu();
        return rootView;
    }

    static private void setupTimeStampButtons() {
        String scheduleText = node.getScheduled().getDate();
        String deadlineText = node.getDeadline().getDate();
        if (scheduleText.length() > 0) schedule_date.setText(scheduleText);
        if (deadlineText.length() > 0) deadline_date.setText(deadlineText);

        String scheduleTimeText = node.getScheduled().getStartTime();
        String deadlineTimeText = node.getDeadline().getStartTime();
        if (scheduleTimeText.length() > 0) schedule_time.setText(scheduleTimeText);
        if (deadlineTimeText.length() > 0) deadline_time.setText(deadlineTimeText);
    }

    static public void createEditNodeFragment(int id, int parentId, int siblingPosition, Context context) {
        Bundle args = new Bundle();
        args.putLong(OrgContract.NODE_ID, id);
        args.putLong(OrgContract.PARENT_ID, parentId);
        args.putInt(OrgContract.OrgData.POSITION, siblingPosition);

        Intent intent = new Intent(context, EditNodeActivity.class);
        intent.putExtras(args);
        context.startActivity(intent);
    }


    private void createNewNode(ContentResolver resolver){
        // Creating new node
        node = new OrgNode();
        node.parentId = parentId;
        node.position = position;
        try {
            OrgNode parentNode = new OrgNode(parentId, resolver);
            node.level = parentNode.level + 1;
            node.fileId = parentNode.fileId;
        } catch (OrgNodeNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("year_schedule", node.getScheduled().year);
        outState.putLong("month_schedule", node.getScheduled().monthOfYear);
        outState.putLong("day_schedule", node.getScheduled().dayOfMonth);
        outState.putLong("hour_schedule", node.getScheduled().startTimeOfDay);
        outState.putLong("minute_schedule", node.getScheduled().startMinute);

        outState.putLong("year_deadline", node.getDeadline().year);
        outState.putLong("month_deadline", node.getDeadline().monthOfYear);
        outState.putLong("day_deadline", node.getDeadline().dayOfMonth);
        outState.putLong("hour_deadline", node.getDeadline().startTimeOfDay);
        outState.putLong("minute_deadline", node.getDeadline().startMinute);

        outState.putString("todo", node.todo);
        outState.putString("priority", node.priority);
    }

    /**
     * Called by EditNodeActivity when the OK button from the menu bar is pressed
     * Triggers the update mechanism
     * First the new node is written to the DB
     * Then the file is written to disk
     * @return : whether or not, the fragment must finish
     */
    public boolean onOKPressed(){
        List<OrgNodeTimeDate> timedates = Arrays.asList(node.getDeadline(), node.getScheduled());
        for(OrgNodeTimeDate timedate: timedates){
            if(     (timedate.startMinute >= 0 || timedate.startTimeOfDay >= 0) &&
                    (timedate.dayOfMonth < 0 || timedate.monthOfYear < 0 || timedate.year < 0)){
                Toast.makeText(context,R.string.pick_a_date,Toast.LENGTH_LONG).show();
                return false;
            }
        }

        ContentResolver resolver = getContext().getContentResolver();
        String payload = "";

        payload+=content.getText().toString();

        node.name = title.getText().toString();
        node.setPayload(payload);

        if(nodeId <0 ) node.shiftNextSiblingNodes(context);

        node.write(getContext());
        OrgFile.updateFile(node, context);
        return true;
    }

    /**
     * Called by EditNodeActivity when the Cancel button from the menu bar is pressed
     */
    public void onCancelPressed(){
    }

    private void setupDateDialog(){
        DialogFragment newFragment = new DatePickerFragment();
        newFragment.show(getActivity().getSupportFragmentManager(), "datePicker");
    }

    private void setupTimeDialog(){
        DialogFragment newFragment = new TimePickerFragment();
        newFragment.show(getActivity().getSupportFragmentManager(), "timePicker");
    }

    private void showPriorityDialog() {
        final ArrayList<String> priorityList = new ArrayList<>();
        priorityList.add("A");
        priorityList.add("B");
        priorityList.add("C");
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.priority)
                .setItems(priorityList.toArray(new CharSequence[priorityList.size()]),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                String selectedPriority = priorityList.get(which);
                                node.priority = selectedPriority;
//                                setupTodoButton(context,node,button, false);
                                priority.setText(selectedPriority);
                            }
                        });
        builder.create().show();
    }

    public static class TimePickerFragment extends DialogFragment
            implements TimePickerDialog.OnTimeSetListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current time as the default values for the picker
            final Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            // Create a new instance of TimePickerDialog and return it
            return new TimePickerDialog(getActivity(), this, hour, minute,
                    DateFormat.is24HourFormat(getActivity()));
        }

        public void onTimeSet(TimePicker view, int hourOfDay, int minuteOfDay) {
            timeDate.startTimeOfDay = hourOfDay;
            timeDate.startMinute = minuteOfDay;
            setupTimeStampButtons();
        }
    }

    public static class DatePickerFragment extends DialogFragment
            implements DatePickerDialog.OnDateSetListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current date as the default date in the picker
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            if(timeDate.year > -1 && timeDate.monthOfYear > -1 && timeDate.dayOfMonth > -1){
                year = timeDate.year;
                month = timeDate.monthOfYear;
                day = timeDate.dayOfMonth;
            }

            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {
            timeDate.year = year;
            timeDate.monthOfYear = month;
            timeDate.dayOfMonth = day;
            setupTimeStampButtons();
        }
    }
}
