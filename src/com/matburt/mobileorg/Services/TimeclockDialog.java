package com.matburt.mobileorg.Services;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Parsing.MobileOrgApplication;
import com.matburt.mobileorg.Parsing.NodeWrapper;
import com.matburt.mobileorg.Parsing.OrgDatabase;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

public class TimeclockDialog extends Activity {

	private long node_id;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        requestWindowFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.timeclock_dialog);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, 
                android.R.drawable.ic_dialog_alert);
        
        // TODO Pass proper node id from TimeclockService
        Intent intent = getIntent();
        this.node_id = intent.getLongExtra(TimeclockService.NODE_ID, -1);
        
        MobileOrgApplication appInst = (MobileOrgApplication) getApplication();
        OrgDatabase db = appInst.getDB();
        Log.d("MobileOrg", "Node id: " + node_id);
		NodeWrapper node = new NodeWrapper(db.getNode(node_id));

        
        setTitle("MobileOrg Timeclock");
        TextView textView = (TextView) findViewById(R.id.timeclock_text);
        textView.setText(node.getName());
        
        Button button = (Button) findViewById(R.id.timeclock_cancel);
        button.setOnClickListener(cancelListener);
        node.close();
	}

	private View.OnClickListener cancelListener = new View.OnClickListener() {
		public void onClick(View v) {
			finish();
		}
	};
}
