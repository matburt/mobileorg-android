package com.matburt.mobileorg.Parsing;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Application;

import com.matburt.mobileorg.Services.SyncService;

public class MobileOrgApplication extends Application {
    private Node rootNode = null;
    private ArrayList<Node> nodestack = new ArrayList<Node>();
    
    public ArrayList<EditNode> edits;
    private OrgDatabase appdb;
    private OrgFileParser parser;
    
    @Override
    public void onCreate() {
    	this.appdb = new OrgDatabase(this);
		this.rootNode = new Node("");
		clearNodestack();
		this.parser = new OrgFileParser(getBaseContext(), this);
		init();

		SyncService.startAlarm(this);
    }

    private void init() {
		if (this.appdb == null || this.appdb.getOrgFiles().isEmpty())
			return;
		
		this.rootNode = this.parser.prepareRootNode();
		clearNodestack();
		
		this.edits = this.parser.parseEdits();
	}
  
    public void pushNodestack(Node node) {
        nodestack.add(node);
    }

    public void popNodestack() {
        if (this.nodestack.size() > 0)
            this.nodestack.remove(nodestack.size()-1);
    }
       
	public void clearNodestack() {
		this.nodestack.clear();
		this.nodestack.add(this.rootNode);
	}
    
    public Node nodestackTop() {
        if (this.nodestack.size() > 0)
            return this.nodestack.get(nodestack.size()-1);
    	return null;
    }
    
    public int nodestackSize() {
    	return this.nodestack.size();
    }


	public boolean makeSureNodeIsParsed(Node node) {
		if (node == null)
			return false;

		if (node.parsed == false) {
			if (node.encrypted == false) {
				this.parser.parseFile(node.name, rootNode);
			} else {
			//	decryptNode(node);
				return true;
			}
		}
		return false;
	}

	public Node getFileNode(String filename) {
		Node node = this.rootNode.findChildNode(filename);
		makeSureNodeIsParsed(node);
		return node;
	}

	/**
	 * This function is called by the synchronizer or capture for each file that
	 * has changed. It will set the parsed flag of the file's node to false.
	 * Additionally it will try to update the node stack to point to the new
	 * nodes, which will cause the user display to be updated appropriately.
	 */
    public void addOrUpdateFile(String filename, String name, String checksum) {
    	appdb.addOrUpdateFile(filename, name, checksum);
    	
    	Node filenode = this.rootNode.getChild(filename);
    	
    	if(filenode == null) {
    		filenode = new Node(filename, this.rootNode);
    		rootNode.sortChildren();
    	}
    	
		filenode.parsed = false;
    	
    	refreshNodestack(filename);
    }

	private void refreshNodestack(String filename) {
		if (nodestack.size() >= 2 && nodestack.get(1).name.equals(filename)) {
			parser.parseFile(filename, this.rootNode);

			ArrayList<Node> newNodestack = new ArrayList<Node>();
			newNodestack.add(this.rootNode);

			this.nodestack.remove(0);

			Node newNode = this.rootNode;
			for (Node node : this.nodestack) {
				newNode = newNode.getChild(node.name);
				if (newNode != null) {
					newNodestack.add(newNode);
				}
				else
					break;
			}

			this.nodestack = newNodestack;
		}
	}

	public boolean removeFile(String filename) {
		OrgFile orgfile = new OrgFile(filename, this.getApplicationContext());
		orgfile.remove(this.appdb);
		this.rootNode.removeChild(filename);
		return true;
	}

    public HashMap<String, String> getOrgFiles() {
    	return appdb.getOrgFiles();
    }

    public ArrayList<EditNode> getNodeEdits() {
    	return this.edits;
    }
    
    public ArrayList<String> getPriorities() {
    	return appdb.getPriorities();
    }
    
    public ArrayList<HashMap<String, Integer>> getGroupedTodods() {
    	return appdb.getGroupedTodods();
    }
    
    public ArrayList<String> getTodods() {
    	return appdb.getTodods();
    }
}
