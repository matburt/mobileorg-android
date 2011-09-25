package com.matburt.mobileorg.Parsing;

public class EditNode {
    public String editType;
    public String nodeId;
    public String title;
    public String oldVal = "";
    public String newVal = "";

    public enum TYPE {TODO, PRIORITY, NAME, PAYLOAD}; 
    
    public EditNode() {
    }
    public EditNode(String editType, String nodeId, String title, String oldVal, String newVal) {
    	this.editType = editType;
    	this.nodeId = nodeId;
    	this.title = title;
    	this.oldVal = oldVal;
    	this.newVal = newVal;
    }
    
    public TYPE getType() {
	if (editType.equals("todo"))
		return TYPE.TODO;
	else if (editType.equals("priority"))
		return TYPE.PRIORITY;
	else if (editType.equals("heading"))
		return TYPE.NAME;
	else if (editType.equals("body"))
		return TYPE.PAYLOAD;
    return null;
    }
    
	
	public String getNodeId() {
		if (this.nodeId.indexOf("olp:") == 0)
			return "olp:" + this.nodeId;
		else
			return this.nodeId;
	}
    
	public String toString() {
		if (nodeId.indexOf("olp:") != 0)
			nodeId = "id:" + nodeId;
		
		String result = "* F(edit:" + this.editType + ") [[" + nodeId + "]["
				+ this.title.trim() + "]]\n";
		result += "** Old value\n" + this.oldVal.trim() + "\n";
		result += "** New value\n" + this.newVal.trim() + "\n";
		result += "** End of edit" + "\n";
		return result;
	}
}
