package com.matburt.mobileorg;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Stack;
import java.util.EmptyStackException;
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

    ArrayList<String> orgPaths;
    ArrayList<Node> nodeList = new ArrayList<Node>();
    FileInputStream fstream;
    Node rootNode = new Node("MobileOrg", Node.NodeType.HEADING);
    public static final String LT = "MobileOrg";

    OrgFileParser(ArrayList<String> orgpaths) {
        this.orgPaths = orgpaths;
    }

    public void parse() {
        String thisLine;
        Stack<Node> nodeStack = new Stack();
        nodeStack.push(this.rootNode);
        int nodeDepth = 0;

        for (int jdx = 0; jdx < this.orgPaths.size(); jdx++) {
            try {
                BufferedReader breader = this.getHandle(this.orgPaths.get(jdx));
                Node fileNode = new Node(this.orgPaths.get(jdx),
                                         Node.NodeType.HEADING);
                if (nodeDepth > 0) {
                    for (;nodeDepth > 0; nodeDepth--) {
                        nodeStack.pop();
                    }
                    nodeStack.pop();
                }

                nodeStack.peek().addChildNode(fileNode);
                nodeStack.push(fileNode);
                while ((thisLine = breader.readLine()) != null) {
                    int numstars = 0;

                    if (thisLine.length() < 1 || thisLine.charAt(0) == '#') {
                        continue;
                    }

                    for (int idx = 0; idx < thisLine.length(); idx++) {
                        if (thisLine.charAt(idx) != '*') {
                            break;
                        }
                        numstars++;
                    }

                    if (numstars >= thisLine.length() || thisLine.charAt(numstars) != ' ') {
                        numstars = 0;
                    }

                    //headings
                    if (numstars > 0) {
                        String title = thisLine.substring(numstars+1);
                        Node newNode = new Node(title, Node.NodeType.HEADING);
                        if (numstars > nodeDepth) {
                            try {
                                Node lastNode = nodeStack.peek();
                                lastNode.addChildNode(newNode);
                            } catch (EmptyStackException e) {
                            }
                            nodeStack.push(newNode);
                            nodeDepth++;
                        }
                        else if (numstars == nodeDepth) {
                            nodeStack.pop();
                            nodeStack.peek().addChildNode(newNode);
                            nodeStack.push(newNode);
                        }
                        else if (numstars < nodeDepth) {
                            for (;numstars <= nodeDepth; nodeDepth--) {
                                nodeStack.pop();
                            }

                            Node lastNode = nodeStack.peek();
                            lastNode.addChildNode(newNode);
                            nodeStack.push(newNode);
                            nodeDepth++;
                        }
                    }
                    //content
                    else {
                        Node lastNode = nodeStack.peek();
                        lastNode.addPayload(thisLine);
                    }
                }
                nodeStack.pop();
                nodeDepth--;
                breader.close();
            }
            catch (IOException e) {
                Log.e(LT, "IO Exception on readerline: " + e.getMessage());
            }
        }
    }

    public BufferedReader getHandle(String filename) {
        BufferedReader breader = null;
        try {
            this.fstream = new FileInputStream("/data/data/com.matburt.mobileorg/files/" + filename);
            DataInputStream in = new DataInputStream(this.fstream);
            breader = new BufferedReader(new InputStreamReader(in));
        }
        catch (Exception e) {
            Log.e(LT, "Error: " + e.getMessage() + " in file " + filename);
        }
        return breader;
    }

}
