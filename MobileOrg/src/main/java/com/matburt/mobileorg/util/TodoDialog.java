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
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.matburt.mobileorg.OrgData.OrgNode;
import com.matburt.mobileorg.OrgData.OrgProviderUtils;
import com.matburt.mobileorg.R;

import java.util.ArrayList;

public class TodoDialog {
    private final Context context;
    private final OrgNode node;
    private final Button button;

    public TodoDialog(Context _context, OrgNode _node, Button _button) {
        this.context = _context;
        this.node = _node;
        this.button = _button;

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
                                if(node.id > -1) updateOrgNodeTodo(selectedTodo);
                                else node.todo = selectedTodo;
                                setupTodoButton(context,node,button, false);
                            }
                        });
        builder.create().show();
    }

    private void updateOrgNodeTodo(String selectedTodo) {
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
        Log.v("todo", "new todo : " + newNode.todo);
        node.generateApplyWriteEdits(newNode, null, resolver);
        node.write(resolver);
        OrgUtils.announceSyncDone(context);
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
            if(toggleVisibility) button.setVisibility(View.VISIBLE);
            button.setText(todoSpan);
            button.setTextColor(active ? red : green);
        } else {
//            if(toggleVisibility) button.setVisibility(View.INVISIBLE);
            button.setText("");
            button.setTextColor(ContextCompat.getColor(context, R.color.colorLightGray));
        }
    }
}
