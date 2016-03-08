package com.matburt.mobileorg;

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

import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.OrgData.OrgNodeTimeDate;
import com.matburt.mobileorg.util.OrgNodeNotFoundException;
import com.matburt.mobileorg.util.TodoDialog;

import java.util.Calendar;

public class EditNodeFragment extends Fragment {
    public static String NODE_ID = "node_id";
    static public long nodeId;
    static private OrgNode node;

    EditText title, content;
    static Button schedule, deadline;
    private Button todo;

    static OrgNodeTimeDate.TYPE currentDateTimeDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.edit_node_entry, container, false);

        if(getArguments()!=null) nodeId = getArguments().getLong(NODE_ID, -1);
        else nodeId = -1;

        if(nodeId > -1) {
            ContentResolver resolver = getActivity().getContentResolver();
            try {
                node = new OrgNode(nodeId, resolver);
            } catch (OrgNodeNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            node = new OrgNode();
            node.write(getContext().getContentResolver());
            nodeId = node.id;
        }

        todo = (Button) rootView.findViewById(R.id.todo);

        TodoDialog.setupTodoButton(getContext(), node, todo, false);

        todo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentDateTimeDialog = OrgNodeTimeDate.TYPE.Scheduled;
                new TodoDialog(getContext(), node, todo);
            }
        });

        title = (EditText) rootView.findViewById(R.id.title);
        title.setText(node.name);

        content = (EditText) rootView.findViewById(R.id.content);
        String payload = node.getCleanedPayload();
        if(payload.length()>0){
            content.setText(payload);
            content.setTextColor(getResources().getColor(R.color.colorBlack));
        }
//
//        content.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                content.setFocusable(true);
//                content.requestFocus();
//                return false;
//            }
//        });
//
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

        schedule = (Button) rootView.findViewById(R.id.scheduled);
        schedule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentDateTimeDialog = OrgNodeTimeDate.TYPE.Scheduled;
                setupDateTimeDialog();
            }
        });

        deadline = (Button) rootView.findViewById(R.id.deadline);
        deadline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentDateTimeDialog = OrgNodeTimeDate.TYPE.Deadline;
                setupDateTimeDialog();
            }
        });


        setupTimeStampButtons();

        getActivity().invalidateOptionsMenu();
        return rootView;
    }

    static private void setupTimeStampButtons() {
        String scheduleText = node.getOrgNodePayload().getScheduled();
        String deadlineText = node.getOrgNodePayload().getDeadline();
        if(scheduleText.length() > 0) schedule.setText(scheduleText);
        else schedule.setText(schedule.getResources().getString(R.string.scheduled));

        if(deadlineText.length() > 0) deadline.setText(deadlineText);
        else deadline.setText(deadline.getResources().getString(R.string.deadline));

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
            OrgNode node = null;
            try {
                node = new OrgNode(nodeId, resolver);
            } catch (OrgNodeNotFoundException e) {
                e.printStackTrace();
                return;
            }

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
            Log.v("timestamp","test : "+node.getOrgNodePayload().getScheduled());

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
            node.getOrgNodePayload().insertOrReplaceDate(
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
