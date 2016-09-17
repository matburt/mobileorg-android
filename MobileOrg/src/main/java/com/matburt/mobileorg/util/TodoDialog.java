package com.matburt.mobileorg.util;

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

import com.matburt.mobileorg.orgdata.OrgFile;
import com.matburt.mobileorg.orgdata.OrgNode;
import com.matburt.mobileorg.orgdata.OrgProviderUtils;
import com.matburt.mobileorg.R;

import java.util.ArrayList;

public class TodoDialog {
    private final Context context;
    private final OrgNode node;
    private final Button button;

    public TodoDialog(Context _context, OrgNode _node, Button _button, final boolean writeChangeOnTodoChanged) {
        this.context = _context;
        this.node = _node;
        this.button = _button;

       ArrayList<String> todos = PreferenceUtils.getSelectedTodos();

        if (todos.size() == 0)
            todos = OrgProviderUtils.getTodos(context.getContentResolver());

        ArrayList<String> result = new ArrayList<>();
        result.add("---");
        result.addAll(todos);

        final ArrayList<String> todoList = result;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.todo_state)
                .setItems(todoList.toArray(new CharSequence[todoList.size()]),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                String selectedTodo = todoList.get(which);
                                if(which == 0) selectedTodo = "";
                                node.todo = selectedTodo;
                                setupTodoButton(context,node,button, false);
                                if(writeChangeOnTodoChanged){
                                    node.write(context);
                                    OrgFile.updateFile(node, context);
                                }
                            }
                        });
        builder.create().show();
    }

    static public void setupTodoButton(Context context, OrgNode node,
                                       Button button, boolean toggleVisibility) {
        String todoString = node.todo;
        if(!TextUtils.isEmpty(todoString)) {
            Spannable todoSpan = new SpannableString(todoString + " ");

            boolean active = OrgProviderUtils.isTodoActive(todoString, context.getContentResolver());

            int red = ContextCompat.getColor(context, R.color.colorRed);
            int green = ContextCompat.getColor(context, R.color.colorGreen);
//            int gray = ContextCompat.getColor(context, R.color.colorGray);
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
