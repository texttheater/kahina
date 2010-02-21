package org.kahina.data.tree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A simple Kahina tree implementation which does not support layers.
 */
public class KahinaUnlayeredMemTree extends KahinaTree
{    
    //empty tree has rootID = -1
    protected int rootID = -1;
    
    //encode properties of individual nodes
    protected Map<Integer, Integer> parents;
    protected Map<Integer, List<Integer>> children;
    protected Map<Integer, String> nodeCaptions; //captions are displayed on the nodes
    protected Map<Integer, String> edgeLabels; //labels are displayed on the edges to the parent
    protected Map<Integer, Integer> status; //appearance of nodes can be steered by appearance
    protected Set<Integer> collapsed; //node collapsing is stored in the model, not in individual views!
    
    HashSet<Integer> terminals; //terminals will be displayed on one level
    
    //store the ID of the next node that is going to be added
    private int nextID = 0;
    public KahinaUnlayeredMemTree()
    {
    	this(new DefaultLayerDecider());
    }
    
    public KahinaUnlayeredMemTree(LayerDecider decider)
    {        
    	super(decider);
        rootID = -1;
        
        parents = new HashMap<Integer, Integer>();
        children = new HashMap<Integer, List<Integer>>();
        nodeCaptions = new HashMap<Integer, String>();
        edgeLabels = new HashMap<Integer, String>();
        status = new HashMap<Integer, Integer>();
        collapsed = new HashSet<Integer>();
        
        terminals = new HashSet<Integer>();
    }
    
    @Override
    public int getRootID()
    {
    	return rootID;
    }
    
    @Override
	public int getRootID(int layerID)
    {
        return getRootID();
    }
    
    @Override
	public void setRootID(int rootID)
    {
        this.rootID = rootID;
    }
    
    @Override
	public void addChild(int parent, int child)
    {
        if (parent != -1)
        {
            List<Integer> childIDs = children.get(parent);
            if (childIDs == null)
            {
                childIDs = new ArrayList<Integer>();
                children.put(parent, childIDs);
            }
            childIDs.add(child);
        }
        parents.put(child, parent);
    }
    
    @Override
	public int getParent(int nodeID, int layerID)
    {
        Integer parent = parents.get(nodeID);
        if (parent == null) return -1;
        return parent;
    }
    
    @Override
	public String getNodeCaption(int nodeID)
    {
        String caption = nodeCaptions.get(nodeID);
        if (caption == null)
        {
            return null;
        }
        else
        {
        	System.err.println("Caption: " + caption); // TODO
            return caption;
        }
    }
    
    @Override
	public String getEdgeLabel(int nodeID)
    {
        String label = edgeLabels.get(nodeID);
        if (label == null)
        {
            return "";
        }
        else
        {
            return label;
        }
    }
    
    @Override
	public int getNodeStatus(int nodeID)
    {
        Integer st = status.get(nodeID);
        if (st == null)
        {
            return 0;
        }
        else
        {
            return st;
        }
    }
    
    @Override
	public List<Integer> getChildren(int nodeID, int layerID)
    {
        //System.err.print("KahinaTree.getChildren(" + nodeID + "," + layerID + ") = ");
        List<Integer> ids = children.get(nodeID);
        if (ids == null)
        {
            //System.err.println("[]");
            return new ArrayList<Integer>();
        }
        else
        {
            //System.err.println(ids);
            return ids;
        }
    }
    
    @Override
	public List<Integer> getLeaves()
    {
        List<Integer> leaves = new LinkedList<Integer>();
        collectLeaves(rootID, leaves);
        return leaves;
    }
    
    private void collectLeaves(int nodeID, List<Integer> leaves)
    {
        if (nodeID != -1)
        {
            List<Integer> nodeChildren = children.get(nodeID);
            if (nodeChildren == null)
            {
                leaves.add(nodeID);
            }
            else
            {
                for (int child : nodeChildren)
                {
                    collectLeaves(child, leaves);
                }
            }
        }
    }
    
    @Override
	public boolean isCollapsed(int nodeID)
    {
        return collapsed.contains(nodeID);
    }
    
    @Override
	public void collapse(int nodeID)
    {
        if (nodeID != -1)
        {
            collapsed.add(nodeID);
        }
    }
    
    @Override
	public void decollapse(int nodeID)
    {
        collapsed.remove(nodeID);
    }
    
    @Override
	public void decollapseAll()
    {
        collapsed = new HashSet<Integer>();
    }
    
    @Override
	public void toggleCollapse(int nodeID)
    {
        if (!isCollapsed(nodeID))
        {
            collapse(nodeID);
        }
        else
        {
            decollapse(nodeID);
        }
    }
    
    @Override
	public boolean hasCollapsedAncestor(int nodeID)
    {
        Integer parent = parents.get(nodeID);
        while (parent != null)
        {
            if (isCollapsed(parent))
            {
                return true;
            }
            parent = parents.get(parent);
        }
        return false;
    }
    
    @Override
	public int addNode(String caption, String label, int nodeStatus)
    {
        int nodeID = getNextFreeID();
        nodeCaptions.put(nodeID, caption);
        edgeLabels.put(nodeID, label);
        status.put(nodeID,nodeStatus);
        return nodeID;
    }

    protected int getNextFreeID()
    {
        int nextIDHyp = nextID;
        while (parents.get(nextIDHyp) != null)
        {
            nextIDHyp++;
        }
        nextID = nextIDHyp + 1;
        return nextIDHyp;
    }
    
    @Override
	public void clear()
    {
    	super.clear();
        rootID = -1;
        
        parents = new HashMap<Integer, Integer>();
        children = new HashMap<Integer, List<Integer>>();
        nodeCaptions = new HashMap<Integer, String>();
        edgeLabels = new HashMap<Integer, String>();
        status = new HashMap<Integer, Integer>();
        collapsed = new HashSet<Integer>();
        
        terminals = new HashSet<Integer>();
        
        nextID = 0;
    }

	@Override
	public int getSize()
	{
		return nodeCaptions.size();
	}
    
    // TODO find a good way to make this implementation-independent
    public static KahinaTree importXML(Document dom)
    {
        KahinaUnlayeredMemTree m = new KahinaUnlayeredMemTree();
        Element treeElement = dom.getDocumentElement();  
        NodeList childNodes = treeElement.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++)
        {
            Node n = childNodes.item(i);
            if (n.getNodeName().equals("node"))
            {
                importXMLNode(m, (Element) n, -1);
                //TODO: a little risky, root node could be assigned another ID
                m.rootID = 0;
                break;
            }
        }
        return m;
    }
    
    private static void importXMLNode(KahinaUnlayeredMemTree m, Element node, int parentID)
    {
        int nodeID = 0;
        if (node.getAttribute("id").length() > 0)
        {
            nodeID = Integer.parseInt(node.getAttribute("id"));
        }
        else
        {
            nodeID = m.getNextFreeID();
        }
        m.nodeCaptions.put(nodeID, node.getAttribute("caption"));
        m.edgeLabels.put(nodeID, node.getAttribute("label"));
        if (node.getAttribute("status").length() > 0)
        {
            m.status.put(nodeID, Integer.parseInt(node.getAttribute("status")));
        }
        m.addChild(parentID, nodeID);
        //go through children recursively
        NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++)
        {
            Node n = childNodes.item(i);
            if (n.getNodeName().equals("node"))
            {
                importXMLNode(m, (Element) n, nodeID);
            }
        }
    }
}
