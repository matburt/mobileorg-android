package com.matburt.mobileorg;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Stack;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import android.util.Log;

class OrgFileParser {

    String orgPath = "";
    FileInputStream fstream;
    ArrayList<Node> nodeList;
    public static final String LT = "MobileOrg";

    OrgFileParser(String orgpath) {
        orgPath = orgpath;
    }

    public Node generateNode(String heading, NodeType nodetype) {
        return new Node(heading, nodetype);
    }

    public void parse() {
        BufferedReader breader = this.getNewHandle();
        Log.d(LT, "Writing out each line from parse.");
        String thisLine;
        Stack<Node> nodeStack = new Stack();
        int nodeDepth = 0;

        try {
            while ((thisLine = breader.readLine()) != null) {
                Log.d(LT, thisLine);

                int numstars = 0;
                int lastnodedepth = 0;

                if (thisLine.charAt(0) == '#') {
                    continue;
                }

                for (int idx = 0; idx < thisLine.length(); idx++) {
                    if (thisLine.charAt(idx) != '*') {
                        break;
                    }
                    numstars++;
                }

                if (numstars >= thisLine.length() || thisLine.charAt(numstars) != ' ') {
                    numstars = 0
                }

                //headings
                if (numstars > 0) {
                    String title = thisLine.substr(numstars+1);
                    Node newNode = this.generateNode(title, Node.NodeType.HEADING);
                    if (numstars > nodeDepth) {
                        lastNode = nodeStack.peek();
                        lastNode.addChildNode(newNode);
                        nodeStack.push(newNode);
                        nodeDepth++;
                    }
                    else if (numstars < nodeDepth) {
                        for (;numstars < nodeDepth; nodeDepth--) {
                            nodeStack.pop();
                        }

                        if (nodeDepth == 1) {
                            nodeList.add(newNode);
                            nodeStack.push(newNode);
                        }
                        else {
                            lastNode = nodeStack.peek();
                            lastNode.addChildNode(newNode);
                        }
                    }
                }
                //content
                else {
                    lastNode = nodeStack.peek();
                    lastNode.addPayload(thisLine);
                }
            }
        }
        catch (IOException e) {
            Log.e(LT, "IO Exception on readerline: " + e.getMessage());
        }
    }

    public BufferedReader getNewHandle() {
        BufferedReader breader = null;
        try {
            fstream = new FileInputStream("/data/data/com.matburt.mobileorg/files/" + this.orgPath);
            DataInputStream in = new DataInputStream(fstream);
            breader = new BufferedReader(new InputStreamReader(in));
        }
        catch (Exception e) {
            Log.e(LT, "Error: " + e.getMessage());
        }
        return breader;
    }

}
