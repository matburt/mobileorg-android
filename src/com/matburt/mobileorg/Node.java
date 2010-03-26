package com.matburt.mobileorg;

import java.util.ArrayList;
import java.util.Date;


class Node {

    public enum NodeType {
        HEADER, HEADING, COMMENT, DATA
    }

    String nodeName = "";
    String todo = "";
    NodeType nodeType;
    ArrayList<Node> subNodes = new ArrayList<Node>();
    String nodePayload = "";
    ArrayList<String> tags = new ArrayList<String>();
    Date schedule = null;
    Date deadline = null;

    Node(String heading, NodeType ntype) {
        nodeName = heading;
        nodeType = ntype;
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