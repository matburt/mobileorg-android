package com.matburt.mobileorg2;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TimePicker;

import com.matburt.mobileorg2.OrgData.OrgContract;
import com.matburt.mobileorg2.OrgData.OrgEdit;
import com.matburt.mobileorg2.OrgData.OrgNode;
import com.matburt.mobileorg2.OrgData.OrgNodeTimeDate;
import com.matburt.mobileorg2.util.OrgNodeNotFoundException;
import com.matburt.mobileorg2.util.TodoDialog;

import java.util.Calendar;

public class EditNodeFragment extends Fragment {
    public static String NODE_ID = "node_id";
    public static String PARENT_ID = "parent_id";
    static public long nodeId = -1, parentId = -1;
    static Button schedule, deadline;
    static OrgNodeTimeDate.TYPE currentDateTimeDialog;
    static private OrgNode node;
    EditText title, content;
    private int position = 0;
    private Button todo, priority;

    static private void setupTimeStampButtons() {
        String scheduleText = node.getOrgNodePayload().getTimestamp(OrgNodeTimeDate.TYPE.Scheduled);
        String deadlineText = node.getOrgNodePayload().getTimestamp(OrgNodeTimeDate.TYPE.Deadline);
        if (scheduleText.length() > 0) schedule.setText(scheduleText);
        else schedule.setText(schedule.getResources().getString(R.string.scheduled));

        if (deadlineText.length() > 0) deadline.setText(deadlineText);
        else deadline.setText(deadline.getResources().getString(R.string.deadline));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.edit_node_entry, container, false);

        todo = (Button) rootView.findViewById(R.id.todo);
        priority = (Button) rootView.findViewById(R.id.priority);
        schedule = (Button) rootView.findViewById(R.id.scheduled);

        deadline = (Button) rootView.findViewById(R.id.deadline);

        title = (EditText)getActivity().findViewById(R.id.title);
        content = (EditText) getActivity().findViewById(R.id.content);

        Bundle bundle = getArguments();
        if(bundle!=null){
            nodeId = bundle.getLong(NODE_ID, -1);
            parentId = bundle.getLong(PARENT_ID, -1);
            position = bundle.getInt(OrgContract.OrgData.POSITION, 0);
            Log.v("position","position : "+position);
        }


        ContentResolver resolver = getActivity().getContentResolver();

        if(nodeId > -1) {
            // Editing already existing node
            try {
                node = new OrgNode(nodeId, resolver);
            } catch (OrgNodeNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            createNewNode(resolver);
        }

        TodoDialog.setupTodoButton(getContext(), node, todo, false);

        todo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentDateTimeDialog = OrgNodeTimeDate.TYPE.Scheduled;
                new TodoDialog(getContext(), node, todo);
            }
        });

        priority.setText(node.priority);

        title.setText(node.name);

        String payload = node.getCleanedPayload();
        if(payload.length()>0){
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

        final LinearLayout layout = (LinearLayout)rootView.findViewById(R.id.view_fragment_layout);
        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                layout.requestFocus();
                return true;
            }
        });

        schedule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentDateTimeDialog = OrgNodeTimeDate.TYPE.Scheduled;
                setupDateTimeDialog();
            }
        });

        deadline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentDateTimeDialog = OrgNodeTimeDate.TYPE.Deadline;
                setupDateTimeDialog();
            }
        });

        setupTimeStampButtons();

        // Add spinner for filename
//        addFileNameSpinner(rootView);

        getActivity().invalidateOptionsMenu();
        return rootView;
    }

    private void createNewNode(ContentResolver resolver){
        // Creating new node
        node = new OrgNode();
        node.parentId = parentId;
        node.position = position;
        Log.v("newNode","parentId : "+parentId);
        try {
            OrgNode parentNode = new OrgNode(parentId, resolver);
            node.level = parentNode.level + 1;
            node.fileId = parentNode.fileId;
            Log.v("newNode","fileId : "+node.fileId);
        } catch (OrgNodeNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Called by EditNodeActivity when the OK button from the menu bar is pressed
     * Triggers the update mechanism
     * First the new node is written to the DB
     * Then the file is written to disk
     */
    public void onOKPressed(){
        ContentResolver resolver = getContext().getContentResolver();
        String payload = "";

        payload+=content.getText().toString();

        node.name = title.getText().toString();
        node.setPayload(payload);
        if(nodeId > -1){
            try {
                Log.v("sync","hello");
                OrgNode original = new OrgNode(nodeId, resolver);
                node.write(resolver);
                OrgEdit.updateFile(original, node, getContext());
            } catch (OrgNodeNotFoundException e) {
                Log.v("sync","loser");
                e.printStackTrace();
            }
        } else {
            node.shiftNextSiblingNodes(resolver);
            node.write(resolver);
            OrgEdit.updateFile(null, node, getContext());
        }
    }

    /**
     * Called by EditNodeActivity when the Cancel button from the menu bar is pressed
     */
    public void onCancelPressed(){
    }

    private void setupDateTimeDialog(){
        DialogFragment newFragment = new DatePickerFragment();
        newFragment.show(getActivity().getSupportFragmentManager(), "datePicker");
    }

    public static class TimePickerFragment extends DialogFragment
            implements TimePickerDialog.OnTimeSetListener {
        static private int day = -1, month = -1, year = -1;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current time as the default values for the picker
            final Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            day = getArguments().getInt("day");
            month = getArguments().getInt("month");
            year = getArguments().getInt("year");

            // Create a new instance of TimePickerDialog and return it
            return new TimePickerDialog(getActivity(), this, hour, minute,
                    DateFormat.is24HourFormat(getActivity()));
        }

        public void onTimeSet(TimePicker view, int hourOfDay, int minuteOfDay) {
            ContentResolver resolver = getActivity().getContentResolver();

            node.getOrgNodePayload().insertOrReplaceDate(
                    new OrgNodeTimeDate(
                            EditNodeFragment.currentDateTimeDialog,
                            day,
                            month,
                            year,
                            hourOfDay,
                            minuteOfDay
                    )
            );
            Log.v("timestamp","test : "+node.getOrgNodePayload().getTimestamp(OrgNodeTimeDate.TYPE.Scheduled));

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

            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {
            node.addDate(
                    new OrgNodeTimeDate(
                            EditNodeFragment.currentDateTimeDialog,
                            day,
                            month,
                            year
                    )
            );

            setupTimeStampButtons();
//            Bundle bundle = new Bundle();
//            bundle.putInt("year",year);
//            bundle.putInt("month",month);
//            bundle.putInt("day",day);
//            TimePickerFragment newFragment = new TimePickerFragment();
//            newFragment.setArguments(bundle);
//            newFragment.show(getActivity().getSupportFragmentManager(), "timePicker");
        }
    }
}
