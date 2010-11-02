package com.matburt.mobileorg;
import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;
import android.widget.Button;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.preference.Preference;
import android.preference.ListPreference;
import android.widget.Toast;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageItemInfo;
import android.util.Log;
import java.util.ArrayList;

public class EditDetailsActivity extends Activity implements OnClickListener
{
    public static final String LT = "MobileOrg";
    private TableLayout mainLayout = null;
    private MobileOrgDatabase appdb;
    private ArrayList<Integer> npath;
    private ArrayList<Button> buttonList = null;
    private Node activeNode = null;
    private CreateEditNote noteEditor = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent txtIntent = getIntent();
        this.npath = txtIntent.getIntegerArrayListExtra("nodePath");
        this.noteEditor = new CreateEditNote(this);

        this.appdb = new MobileOrgDatabase((Context)this);
        ArrayList<ArrayList<String>> allTodos = this.appdb.getTodos();
        this.buttonList = new ArrayList<Button>();
        mainLayout = new TableLayout(this);
        mainLayout.setLayoutParams(
                     new TableLayout.LayoutParams(
                          LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        for (int idx = 0; idx < allTodos.size(); idx++) {
            for (int jdx = 0; jdx < allTodos.get(idx).size(); jdx++) {
                TableRow aTr = new TableRow(this);
                aTr.setLayoutParams(
                      new TableRow.LayoutParams(
                             TableRow.LayoutParams.FILL_PARENT,
                             TableRow.LayoutParams.WRAP_CONTENT));
                Button aButton = new Button(this);
                aButton.setText(allTodos.get(idx).get(jdx));
                aButton.setOnClickListener(this);
                aTr.addView(aButton);
                mainLayout.addView(aTr);
                this.buttonList.add(aButton);
            }
            TableRow nTr = new TableRow(this);
            nTr.setLayoutParams(
                      new TableRow.LayoutParams(
                             TableRow.LayoutParams.FILL_PARENT,
                             TableRow.LayoutParams.WRAP_CONTENT));
            mainLayout.addView(nTr);
        }
        setContentView(mainLayout);
        this.populateInfo();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.noteEditor != null) {
            this.noteEditor.close();
        }
    }

    public void saveEditTodo(String newTodo) {
        this.noteEditor.editNote("todo",
                                 this.activeNode.getProperty("ID"),
                                 this.activeNode.nodeName,
                                 this.activeNode.todo,
                                 newTodo);
    }

    public void populateInfo() {
        MobileOrgApplication appInst = (MobileOrgApplication)this.getApplication();
        Node thisNode = appInst.rootNode;
        Intent textIntent = new Intent();
        String displayBuffer = new String();
        for (int idx = 0; idx < this.npath.size(); idx++) {
            thisNode = thisNode.subNodes.get(
                                             this.npath.get(idx));
        }
        this.activeNode = thisNode;
    }

    public void onClick(View v) {
        for (int idx = 0; idx < this.buttonList.size(); idx++) {
            if (v == this.buttonList.get(idx)) {
                this.saveEditTodo(this.buttonList.get(idx).getText().toString());
                this.finish();
            }
        }
    }
}