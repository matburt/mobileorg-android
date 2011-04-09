package com.matburt.mobileorg.Parsing;

public class EditNode {
    public String editType;
    public String nodeId;
    public String title;
    public String oldVal = "";
    public String newVal = "";
    public EditNode() {
    }
    public EditNode(String editType, String nodeId, String title, String oldVal, String newVal) {
    	this.editType = editType;
    	this.nodeId = nodeId;
    	this.title = title;
    	this.oldVal = oldVal;
    	this.newVal = newVal;
    }
}
