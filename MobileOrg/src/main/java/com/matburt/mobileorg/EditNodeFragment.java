package com.matburt.mobileorg;

import android.content.ContentResolver;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.util.OrgNodeNotFoundException;
import com.matburt.mobileorg.util.TodoDialog;

public class EditNodeFragment extends Fragment {
    public static String NODE_ID = "node_id";
    private OrgNode node;

    EditText title, content, schedule, deadline;
    private Button todo;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.edit_node_entry, container, false);

        long nodeId;
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
        }

        todo = (Button) rootView.findViewById(R.id.todo);

        TodoDialog.setupTodoButton(getContext(), node, todo, false);

        todo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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

        schedule = (EditText) rootView.findViewById(R.id.scheduled);
//        schedule.setText(node.name);


        deadline = (EditText) rootView.findViewById(R.id.deadline);
//        deadline.setText(node.name);

        return rootView;
    }
}
