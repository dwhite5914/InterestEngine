package com.nuwc.interestengine.data;

import java.util.LinkedList;
import java.util.List;

public class TreeNode
{
    public Object value;
    public TreeNode parent;
    public List<TreeNode> children;

    public TreeNode(Object value)
    {
        this.value = value;
        this.children = new LinkedList<>();
    }

    public TreeNode addChild(Object child)
    {
        TreeNode childNode = new TreeNode(child);
        childNode.parent = parent;
        this.children.add(childNode);

        return childNode;
    }
}
