package com.matburt.mobileorg;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class SimpleTextDisplay extends Activity
{
    private TextView orgDisplay;
    public static final String LT = "MobileOrg";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.simpletext);
        this.orgDisplay = (TextView)this.findViewById(R.id.orgTxt);
        this.poplateDisplay();
    }

    public void poplateDisplay() {
        Intent txtIntent = getIntent();
        String srcText = txtIntent.getStringExtra("txtValue");
        this.orgDisplay.setText(srcText);
    }
}