package com.matburt.mobileorg2.OrgData;

import android.content.ContentResolver;
import android.util.Log;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Created by bcoste on 25/02/16.
 */
public class OrgNodeTree {
    public OrgNode node;
    public enum Visibility {
        folded,
        children,
        subtree,
    }

    private Visibility visibility;
    public ArrayList<OrgNodeTree> children;

    private OrgNodeTree(OrgNode root, ContentResolver resolver, boolean isRecursive){
        node = root;
        children = new ArrayList<>();
        visibility = Visibility.subtree;

        if(isRecursive && root != null) {
            for (OrgNode child : root.getChildren(resolver)) {
                Log.v("newNode", "child : " + child.name);
                children.add(new OrgNodeTree(child, resolver));
            }
        }
    }

    /**
     * Create a tree with only the root
     * @param root
     */
    public OrgNodeTree(OrgNode root){
        this(root,null, false);
    }

    /**
     * Create a tree and all sub-trees
     * @param root
     * @param resolver
     */
    public OrgNodeTree(OrgNode root, ContentResolver resolver){
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
            for(OrgNodeTree child: children) child.visibility = Visibility.folded;
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
        long idConstructor = 0;
        fillMap(result, this, idConstructor);
        return result;
    }

    private static void fillMap(TreeMap<Long,OrgNodeTree> map, OrgNodeTree tree, long idConstructor){
        // The root node is the filename node
        // It must not be added
        map.put(idConstructor++, tree);

        if(tree.visibility == Visibility.folded) return;

//        for(OrgNodeTree child: tree.children) fillMap(map, child);
        for(OrgNodeTree child: tree.children) map.put(idConstructor++, child);
    }
}
