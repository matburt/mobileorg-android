package com.matburt.mobileorg;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class ViewNodeDetailsActivity extends Activity implements OnClickListener {
	protected ArrayList<Integer> mNodePath;
	protected Button mParent;
	protected Button mUp;
	protected Button mDown;
	protected EditText mTitle;
	protected TextView mBody;
	protected EditText mPriority;
	protected EditText mTodoState;
	protected EditText mTags;
	protected Button mViewAsDocument;
	protected Node mNode;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
        setContentView(R.layout.node_details);
        Intent txtIntent = getIntent();
        this.mNodePath = txtIntent.getIntegerArrayListExtra("nodePath");
        this.mTitle = (EditText) this.findViewById(R.id.title);
        this.mBody = (TextView) this.findViewById(R.id.body);
        this.mPriority = (EditText) this.findViewById(R.id.priority);
        this.mTodoState = (EditText) this.findViewById(R.id.todo_state);
        this.mTags = (EditText) this.findViewById(R.id.tags);
        this.mViewAsDocument = (Button) this.findViewById(R.id.view_as_document);
        this.mParent = (Button) this.findViewById(R.id.parent);
        this.mUp = (Button) this.findViewById(R.id.previous_sibling);
        this.mDown = (Button) this.findViewById(R.id.next_sibling);
        this.populateDisplay();
        
        mViewAsDocument.setOnClickListener(this);
        mBody.setOnClickListener(this);
        mParent.setOnClickListener(this);
        mUp.setOnClickListener(this);
        mDown.setOnClickListener(this);
    }
	
	public void populateDisplay() {
		MobileOrgApplication appInst = (MobileOrgApplication)this.getApplication();
        mNode = appInst.getNode(mNodePath);
        Node parent = appInst.getParent(mNodePath);
        mParent.setText(parent.nodeName);
        mTitle.setText(mNode.nodeName);
        mBody.setText(mNode.nodePayload);
        mPriority.setText(mNode.priority);
        mTodoState.setText(mNode.todo);
        mTags.setText(mNode.tagString);
        mUp.setEnabled(mNodePath.get(mNodePath.size() - 1) > 0);
        mDown.setEnabled((mNode.parentNode != null) && 
        		mNode.parentNode.subNodes.size() - 1 > mNodePath.get(mNodePath.size() - 1));
    }

	@Override
	public void onClick(View v) {
		if (v.equals(mViewAsDocument)) {
			Intent intent = new Intent(this, SimpleTextDisplay.class);
			intent.putExtra("txtValue", mNode.nodePayload);
			this.startActivity(intent);
		}
		if (v.equals(mParent)) {
			save();
			// Finish the activity and return the path to the parent
			Intent intent = new Intent();
			ArrayList<Integer> newPath = 
				(ArrayList<Integer>) mNodePath.clone();
			newPath.remove(newPath.size()-1);
			intent.putIntegerArrayListExtra("nodePath", newPath);
			setResult(RESULT_OK, intent);
			finish();
		}
		if (v.equals(mUp)) {
			save();
			// Finish the activity and return the path to the previous sibling
			int newIndex = mNodePath.get(mNodePath.size() - 1) - 1;
			if (newIndex >= 0) {
				mNodePath.set(mNodePath.size() - 1, newIndex);
				populateDisplay();
				/*Intent intent = new Intent();
				intent.putIntegerArrayListExtra("nodePath", newPath);
				setResult(RESULT_OK, intent);
				finish();*/
			}
		}
		if (v.equals(mDown)) {
			save();
			// Finish the activity and return the path to the next sibling
			int newIndex = mNodePath.get(mNodePath.size() - 1) + 1;
			if (newIndex >= 0) {
				mNodePath.set(mNodePath.size() - 1, newIndex);
				populateDisplay();
				/*Intent intent = new Intent();
				intent.putIntegerArrayListExtra("nodePath", newPath);
				setResult(RESULT_OK, intent);
				finish();*/
			}
		}
		if (v.equals(mBody)) {
			save();
			Intent intent = new Intent(this, Capture.class);
			if (mNode.nodeId != null && mNode.nodeId.length() > 0) {
				intent.putExtra("nodeId", mNode.nodeId);	
			}
			intent.putExtra("editType", "body");
			intent.putExtra("txtValue", mNode.nodePayload);
			intent.putExtra("nodeTitle", mNode.nodeTitle);
			startActivityForResult(intent, EDIT_BODY);
		}
	}
	
	
	public void save() {
		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == EDIT_BODY) {
			String body = data.getStringExtra("text");
			mNode.nodePayload = body;
			populateDisplay();
		}
	}
	private static int EDIT_BODY = 1;
	
}
