package com.matburt.mobileorg;

import java.util.ArrayList;

class Node {

    public enum NodeType {
        HEADER, HEADING, COMMENT, DATA
    }

    String nodeName = "";
    NodeType nodeType;
    ArrayList<Node> subNodes;
    String nodePayload = "";

    Node(String heading, NodeType ntype) {
        nodeName = heading;
        nodeType = ntype;
    }

    void setPayload(String npayload) {
        this.nodePayload = npayload
    }

    void addChildNode(Node childNode) {
        this.subNodes.add(childNode);
    }

    void clearNodes() {
        this.subNodes.clear();
    }
}