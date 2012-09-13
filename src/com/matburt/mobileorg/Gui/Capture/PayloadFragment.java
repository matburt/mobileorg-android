package com.matburt.mobileorg.Gui.Capture;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Gui.ViewFragment;
import com.matburt.mobileorg.OrgData.OrgNode;

public class PayloadFragment extends ViewFragment {

	private RelativeLayout payloadView;
	private EditText payloadEdit;
	private OrgNode node;
	
	private ImageButton editButton;
	private ImageButton cancelButton;
	private ImageButton saveButton;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		this.payloadView = (RelativeLayout) inflater.inflate(
				R.layout.edit_payload, container, false);
		this.webView = (WebView) payloadView
				.findViewById(R.id.edit_payload_webview);
		this.webView.setBackgroundColor(0x00000000);
		this.webView.setWebViewClient(new InternalWebViewClient());

		this.payloadEdit = (EditText) payloadView
				.findViewById(R.id.edit_payload_edittext);
		
		this.editButton = (ImageButton) payloadView
				.findViewById(R.id.edit_payload_edit);
		editButton.setOnClickListener(editListener);
		
		this.cancelButton = (ImageButton) payloadView
				.findViewById(R.id.edit_payload_cancel);
		cancelButton.setOnClickListener(cancelListener);
		
		this.saveButton = (ImageButton) payloadView
				.findViewById(R.id.edit_payload_save);
		saveButton.setOnClickListener(saveListener);
		
		return payloadView;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		EditActivity editActivity = (EditActivity) getActivity();
		this.node = editActivity.getOrgNode();
		displayPayload(node);
		this.webView.setOnClickListener(editListener);
	}

	public String getPayload() {
		return this.node.getRawPayload();
	}
	
	private void switchToEdit() {
		webView.setVisibility(View.GONE);
		editButton.setVisibility(View.GONE);

		payloadEdit.setText(node.getRawPayload());
		payloadEdit.setVisibility(View.VISIBLE);
		cancelButton.setVisibility(View.VISIBLE);
		saveButton.setVisibility(View.VISIBLE);
	}
	
	private void switchToView() {
		payloadEdit.setVisibility(View.GONE);
		cancelButton.setVisibility(View.GONE);
		saveButton.setVisibility(View.GONE);

		webView.setVisibility(View.VISIBLE);
		editButton.setVisibility(View.VISIBLE);
	}
	
	private void savePayload(String payload) {
		this.node.setPayload(payload);
		displayPayload(node);
	}
	
	private OnClickListener editListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			switchToEdit();
		}
	};
	
	private OnClickListener saveListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			savePayload(payloadEdit.getText().toString());
			switchToView();
		}
	};
	
	private OnClickListener cancelListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			switchToView();
		}
	};
}
