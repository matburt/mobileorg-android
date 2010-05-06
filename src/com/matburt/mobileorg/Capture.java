package com.matburt.mobileorg;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.content.Intent;
import android.widget.EditText;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

public class Capture extends Activity
{
    private EditText orgEditDisplay;
    public static final String LT = "MobileOrg";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.simpleedittext);
        this.orgEditDisplay = (EditText)this.findViewById(R.id.orgEditTxt);
    }
}