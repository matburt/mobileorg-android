package com.matburt.mobileorg2.OrgData;

import android.content.ContentResolver;
import android.content.Context;
import android.util.Log;

import com.matburt.mobileorg2.util.FileUtils;
import com.matburt.mobileorg2.util.OrgFileNotFoundException;
import com.matburt.mobileorg2.util.OrgNodeNotFoundException;

import java.util.ArrayList;

public class OrgEdit {

	public TYPE type = null;
	public String nodeId = "";
	public String title = "";
	public String oldValue = "";
	public String newValue = "";
	public OrgNode node;
	ContentResolver resolver;
	public OrgEdit() {
	}

	public OrgEdit(OrgNode node, TYPE type, ContentResolver resolver) {
		this.node = node;
		this.title = node.name;
		this.nodeId = node.getNodeId(resolver);
		this.type = type;

		setOldValue(node);
	}

	public OrgEdit(OrgNode node, TYPE type, String newValue, ContentResolver resolver) {
		this.title = node.name;
		this.nodeId = node.getNodeId(resolver);
		this.type = type;
		this.newValue = newValue;

		setOldValue(node);
	}

	/**
	 * Edit the org file on disk to incorporate new modifications
	 *
	 * @param oldNode :
	 *                the old version of the OrgNode
	 * @param newNode :
	 *                the new version of the OrgNode
	 */
	static public void updateFile(OrgNode oldNode, OrgNode newNode, Context context) {
		if (oldNode == null) return; // TODO: insertion of a new node
		if (newNode == null) return; // TODO: deletion of an old node

		ContentResolver resolver = context.getContentResolver();
		String content = "";
		try {
			OrgFile file = new OrgFile(oldNode.fileId, resolver);
			OrgNode root = new OrgNode(file.nodeId, resolver);


			OrgNodeTree tree = new OrgNodeTree(root, resolver);
			ArrayList<OrgNode> res = OrgNodeTree.getFullNodeArray(tree, true);
			for (OrgNode node : res) {
				Log.v("content","content");
				Log.v("content",node.toString());
				content += FileUtils.stripLastNewLine(node.toString()) + "\n";
			}

			file.updateFile(content, context);
		} catch (OrgFileNotFoundException e) {
			e.printStackTrace();
		} catch (OrgNodeNotFoundException e) {
			e.printStackTrace();
		}


	}

	private void setOldValue(OrgNode node) {
		switch (type) {
			case TODO:
				this.oldValue = node.todo;
				break;
			case HEADING:
				this.oldValue = node.name;
				break;
			case PRIORITY:
				this.oldValue = node.priority;
				break;
			case BODY:
				this.oldValue = node.getPayload();
				break;
			case TAGS:
				this.oldValue = node.tags;
				break;
			default:
				break;
		}
	}

	public String toString() {
		if (nodeId.indexOf("olp:") != 0)
			nodeId = "id:" + nodeId;

		StringBuilder result = new StringBuilder();

		result.append("** Old value\n" + oldValue.trim() + "\n");
		result.append("** New value\n" + newValue + "\n");
		result.append("** End of edit" + "\n\n");

		return result.toString().replace(":ORIGINAL_ID:", ":ID:");
	}

	public enum TYPE {
		HEADING,
		TODO,
		PRIORITY,
		BODY,
		TAGS,
		REFILE,
		ARCHIVE,
		ARCHIVE_SIBLING,
		DELETE,
		ADDHEADING
	}


}