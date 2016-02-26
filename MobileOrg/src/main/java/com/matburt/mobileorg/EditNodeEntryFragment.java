package com.matburt.mobileorg;

import android.content.ContentResolver;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.util.OrgNodeNotFoundException;
import com.matburt.mobileorg.util.TodoDialog;

public class EditNodeEntryFragment extends Fragment {
    public static String NODE_ID = "node_id";
    private OrgNode node;

    private TextView title, schedule, deadline;
    private Button todo;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
         View rootView = inflater.inflate(R.layout.edit_node_entry, container, false);

         long nodeId = getArguments().getLong(NODE_ID);

        ContentResolver resolver = getActivity().getContentResolver();
        Log.v("id", "nodeid : " + nodeId);
        try {
            node = new OrgNode(nodeId,resolver);
        } catch (OrgNodeNotFoundException e) {
            e.printStackTrace();
        }

        todo = (Button) rootView.findViewById(R.id.todo);

        TodoDialog.setupTodoButton(getContext(), node, todo, false);

        todo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TodoDialog(getContext(), node, todo);
            }
        });

        title = (TextView) rootView.findViewById(R.id.title);
        title.setText(node.name);

        schedule = (TextView) rootView.findViewById(R.id.scheduled);
//        schedule.setText(node.name);


        deadline = (TextView) rootView.findViewById(R.id.deadline);
//        deadline.setText(node.name);

        return rootView;
    }
}
