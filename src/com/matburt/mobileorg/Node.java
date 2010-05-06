package com.matburt.mobileorg;

import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


class Node {

    public enum NodeType {
        HEADER, HEADING, COMMENT, DATA
    }

    ArrayList<Node> subNodes = new ArrayList<Node>();
    ArrayList<String> tags = new ArrayList<String>();
    String nodeName = "";
    String todo = "";
    NodeType nodeType;
    String nodePayload = "";
    Date schedule = null;
    Date deadline = null;

    Node(String heading, NodeType ntype) {
        nodeName = heading;
        nodeType = ntype;
    }

    Node findChildNode(String regex) {
        Pattern findNodePattern = Pattern.compile(regex);
        for (int idx = 0; idx < this.subNodes.size(); idx++) {
            if (findNodePattern.matcher(this.subNodes.get(idx).nodeName).matches()) {
                return this.subNodes.get(idx);
            }
        }
        return null;
    }

    void addPayload(String npayload) {
        this.nodePayload += npayload + "\n";
    }

    void addChildNode(Node childNode) {
        this.subNodes.add(childNode);
    }

    void clearNodes() {
        this.subNodes.clear();
    }
}