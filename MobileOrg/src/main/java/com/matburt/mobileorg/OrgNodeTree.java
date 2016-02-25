package com.matburt.mobileorg;

import android.content.ContentResolver;
import android.util.Log;

import com.matburt.mobileorg.OrgData.OrgNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Created by bcoste on 25/02/16.
 */
public class OrgNodeTree {
    public OrgNode node;
    public int id;
    public enum Visibility {
        folded,
        children,
        subtree,
    }

    private Visibility visibility;
    private ArrayList<OrgNodeTree> children;
    private static int idConstructor;

    OrgNodeTree(OrgNode root, ContentResolver resolver){
        if(root == null) return;
        node = root;
        children = new ArrayList<>();
        visibility = Visibility.subtree;
        for(OrgNode child: root.getChildren(resolver)) children.add(new OrgNodeTree(child, resolver));
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
    NavigableMap<Integer,OrgNodeTree> getVisibleNodesArray(){
        TreeMap<Integer,OrgNodeTree> result = new TreeMap<>();
        idConstructor = -1;
        fillMap(result, this);
        return result;
    }

    private static void fillMap(TreeMap<Integer,OrgNodeTree> map, OrgNodeTree tree){
        // The root node is the filename node
        // It must not be added
        if(idConstructor > -1) map.put(idConstructor++, tree);
        else idConstructor++;

        if(tree.visibility == Visibility.folded) return;

        for(OrgNodeTree child: tree.children) fillMap(map, child);
    }
}
