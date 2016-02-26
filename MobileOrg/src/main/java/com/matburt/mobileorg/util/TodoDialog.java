package com.matburt.mobileorg.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;

import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.OrgData.OrgProviderUtils;
import com.matburt.mobileorg.R;

import java.util.ArrayList;

public class TodoDialog {
    final private Context context;
    final private OrgNode node;
    final private Button button;

    public TodoDialog(Context context, OrgNode node, Button button) {
        this.context = context;
        this.node = node;
        this.button = button;

       ArrayList<String> todos = PreferenceUtils.getSelectedTodos();

        if (todos.size() == 0)
            todos = OrgProviderUtils.getTodos(context.getContentResolver());

        final ArrayList<String> todoList = todos;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getResources().getString(R.string.todo_state))
                .setItems(todoList.toArray(new CharSequence[todoList.size()]),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                String selectedTodo = todoList.get(which);
                                setNewTodo(selectedTodo);
                            }
                        });
        builder.create().show();
    }

    private void setNewTodo(String selectedTodo) {
        if (selectedTodo.equals(node.todo))
            return;

        ContentResolver resolver = context.getContentResolver();

        OrgNode newNode;
        try {
            newNode = new OrgNode(node.id, resolver);
        } catch (OrgNodeNotFoundException e) {
            e.printStackTrace();
            return;
        }
        newNode.todo = selectedTodo;
        node.generateApplyWriteEdits(newNode, null, resolver);
        node.write(resolver);
        OrgUtils.announceSyncDone(context);
        setupTodoButton(context,node,button, false);
    }

    static public void setupTodoButton(Context context, OrgNode node,
                                       Button button, boolean toggleVisibility) {
        String todoString = node.todo;
        if(!TextUtils.isEmpty(todoString)) {
            Spannable todoSpan = new SpannableString(todoString + " ");

            boolean active = OrgProviderUtils.isTodoActive(todoString, context.getContentResolver());

            int red = ContextCompat.getColor(context, R.color.colorRed);
            int green = ContextCompat.getColor(context, R.color.colorGreen);
            todoSpan.setSpan(new ForegroundColorSpan(active ? red : green), 0,
                    todoString.length(), 0);
            button.setText(todoSpan);
            button.setTextColor(active ? red : green);
        } else {
            if(toggleVisibility) button.setVisibility(View.GONE);
            else button.setText(context.getResources().getString(R.string.no_state));
        }
    }
}
