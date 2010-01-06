package com.matburt.mobileorg;

import java.util.ArrayList;

class Node {

    public enum NodeType {
        HEADING, NONHEADING
    }

    String nodeName = "";
    NodeType nodeType;
    ArrayList<Node> subNodes;

    Node(String heading) {
        nodeName = heading
    }
}