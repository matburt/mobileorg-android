package com.matburt.mobileorg.orgdata;

import android.content.ContentResolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.NavigableMap;
import java.util.Stack;
import java.util.TreeMap;

public class OrgNodeTree {
    private static long idConstructor;
    public OrgNode node;
    public ArrayList<OrgNodeTree> children;
    private Visibility visibility;

    private OrgNodeTree(OrgNode root, ContentResolver resolver, boolean isRecursive){
        node = root;
        children = new ArrayList<>();
        visibility = Visibility.subtree;

        if(isRecursive && root != null) {
            for (OrgNode child : root.getChildren(resolver)) {
                children.add(new OrgNodeTree(child, resolver));
            }
        }
    }

    /**
     * Create a tree with only the root
     *
     * @param root
     */
    public OrgNodeTree(OrgNode root) {
        this(root, null, false);
    }

    /**
     * Create a tree and all sub-trees
     * @param root
     * @param resolver
     */
    public OrgNodeTree(OrgNode root, ContentResolver resolver) {
        this(root, resolver, true);
    }

    /**
     * Create a flat tree from an ArrayList
     * @param arrayList
     */
    public OrgNodeTree(ArrayList<OrgNode> arrayList){
        this((OrgNode)null);
        for(OrgNode node: arrayList) children.add(new OrgNodeTree(node));
    }

    static public ArrayList<OrgNode> getFullNodeArray(OrgNodeTree root) {
        return getFullNodeArray(root, false);
    }

    static public ArrayList<OrgNode> getFullNodeArray(OrgNodeTree root, boolean excludeRoot) {
        Stack<OrgNodeTree> stack = new Stack<>();
        ArrayList<OrgNode> result = new ArrayList<>();
        stack.push(root);
        int count = 0;
        while (!stack.empty()) {
            OrgNodeTree tree = stack.pop();

            Collections.sort(tree.children, new Comparator<OrgNodeTree>() {
                @Override
                public int compare(OrgNodeTree a, OrgNodeTree b) {
                    if (a.node.position < b.node.position) return 1;
                    if (a.node.position > b.node.position) return -1;
                    return 0;
                }
            });

            for (OrgNodeTree child : tree.children) stack.push(child);
            if (!excludeRoot || count > 0) result.add(tree.node);
            count++;
        }

        return result;
    }

    private static void fillMap(TreeMap<Long, OrgNodeTree> map, OrgNodeTree tree) {
        // The root node is the filename node
        // It must not be added
        map.put(++idConstructor, tree);


        if (tree.visibility == Visibility.folded) return;

        for (OrgNodeTree child : tree.children) fillMap(map, child);

    }

    public Visibility getVisibility(){
        return visibility;
    }

    /**
     * Cycle through the visibility states.
     * Special care for subtree visibility because it propagates to child nodes
     */
    public void toggleVisibility(){
        if(visibility==Visibility.folded){
            visibility = Visibility.children;
            for (OrgNodeTree child : children) {
                child.visibility = Visibility.folded;
            }
        } else if(visibility==Visibility.children){
            visibility = Visibility.subtree;
            for(OrgNodeTree child: children) child.cascadeVisibility(Visibility.subtree);
        } else if(visibility==Visibility.subtree) visibility = Visibility.folded;
    }

    private void cascadeVisibility(Visibility _visibility){
        visibility = _visibility;
        for(OrgNodeTree child: children) child.cascadeVisibility(_visibility);
    }

    /**
     * Generate a mapping between an OrgNode from the tree and its index in the tree
     */
    public NavigableMap<Long,OrgNodeTree> getVisibleNodesArray(){
        TreeMap<Long,OrgNodeTree> result = new TreeMap<>();
        idConstructor = -1;
        fillMap(result, this);
        return result;
    }

    public enum Visibility {
        folded,
        children,
        subtree,
    }
}
