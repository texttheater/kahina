package org.kahina.core.data.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A simple Kahina tree implementation which does not support layers.
 */
public class KahinaUnlayeredMemTree extends KahinaTree
{
	private static final long serialVersionUID = -2918960685520857350L;
	
	private static final boolean VERBOSE = false;
	
	// encode properties of individual nodes
	protected Map<Integer, Integer> parents;
	protected Map<Integer, List<Integer>> children;
	protected Map<Integer, String> nodeCaptions; // captions are displayed on
													// the nodes
	protected Map<Integer, String> edgeLabels; // labels are displayed on the
												// edges to the parent
	protected Map<Integer, Integer> status; // appearance of nodes can be
											// steered by appearance
	protected Set<Integer> collapsed; // node collapsing is stored in the model,
										// not in individual views!
	protected Map<Integer, Integer> layers; // layer by node, whether this is
											// used depends on the application

	// store the ID of the next node that is going to be added
	private int nextID = 0;

	private final List<KahinaTreeChildAddListener> childAddListeners;

	public KahinaUnlayeredMemTree()
	{
		this(new DefaultLayerDecider());
	}

	public KahinaUnlayeredMemTree(LayerDecider decider)
	{
		super(decider);

		parents = new TreeMap<Integer, Integer>();
		children = new TreeMap<Integer, List<Integer>>();
		nodeCaptions = new TreeMap<Integer, String>();
		edgeLabels = new TreeMap<Integer, String>();
		status = new TreeMap<Integer, Integer>();
		collapsed = new TreeSet<Integer>();
		layers = new TreeMap<Integer, Integer>();
		childAddListeners = new ArrayList<KahinaTreeChildAddListener>();
	}
	
	/**
	 * Gets all the node IDs in ascending order, useful for bottom-up traversal.
	 * @return all the node IDs in a tree set, in descending order
	 */
	public Set<Integer> getAllNodeIDs()
	{
	    return nodeCaptions.keySet();
	}

	@Override
	public int getRootID(int layerID)
	{
		return getRootID();
	}

	@Override
	public int getRootID(int layerID, int referenceNode)
	{
		return getRootID(layerID);
	}

	@Override
	public void addChild(int parent, int child)
	{
		// System.err.println("KahinaUnlayeredMemTree.addChild(" + parent + ","
		// + child + ")");
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
		notifyChildAddListeners(child);
		needsUpdate = true;
	}

	private void notifyChildAddListeners(int child)
	{
		if (VERBOSE)
		{
			System.err.println("KahinaUnlayeredMemTree.notifyChildAddListeners(" + child + ")");
		}
		for (KahinaTreeChildAddListener listener : childAddListeners)
		{
			listener.childAdded(child);
		}
	}

	@Override
	public void addChildAddListener(KahinaTreeChildAddListener listener)
	{
		childAddListeners.add(listener);
	}

	@Override
	public void removeChildAddListener(KahinaTreeChildAddListener listener)
	{
		childAddListeners.remove(listener);
	}

	@Override
	public int getParent(int nodeID, int layerID)
	{
		return getParent(nodeID);
	}

	@Override
	public int getBestEquivalent(int nodeID, int layerID)
	{
		return nodeID;
	}

	@Override
	public int getLayer(int nodeID)
	{
		Integer result = layers.get(nodeID);
		if (result == null)
		{
			result = -1;
		}
		return result;
	}

	@Override
	public void setLayer(int nodeID, int layer)
	{
		layers.put(nodeID, layer);
		needsUpdate = true;
	}

	@Override
	public int getParent(int nodeID)
	{
		Integer parent = parents.get(nodeID);
		if (parent == null)
			return -1;
		return parent;
	}

	@Override
	public String getNodeCaption(int nodeID)
	{
		String caption = nodeCaptions.get(nodeID);
		if (caption == null)
		{
			return "";
		} else
		{
			return caption;
		}
	}

	@Override
	public void setNodeCaption(int nodeID, String caption)
	{
		nodeCaptions.put(nodeID, caption);
		needsUpdate = true;
	}

	@Override
	public String getEdgeLabel(int nodeID)
	{
		String label = edgeLabels.get(nodeID);
		if (label == null)
		{
			return "";
		} else
		{
			return label;
		}
	}

	@Override
	public void setEdgeLabel(int nodeID, String label)
	{
		edgeLabels.put(nodeID, label);
		needsUpdate = true;
	}

	@Override
	public int getNodeStatus(int nodeID)
	{
		Integer st = status.get(nodeID);
		if (st == null)
		{
			return 0;
		} else
		{
			return st;
		}
	}

	@Override
	public void setNodeStatus(int nodeID, int newStatus)
	{
		status.put(nodeID, newStatus);
	}

	@Override
	public List<Integer> getChildren(int nodeID)
	{
		// System.err.print("KahinaTree.getChildren(" + nodeID + "," + layerID +
		// ") = ");
		List<Integer> ids = children.get(nodeID);
		if (ids == null)
		{
			// System.err.println("[]");
			return Collections.emptyList();
		} 
		else
		{
			// System.err.println(ids);
			return Collections.unmodifiableList(ids);
		}
	}

	@Override
	public List<Integer> getChildren(int nodeID, int layerID, boolean stopAtCornerstones)
	{
		// System.err.print("KahinaTree.getChildren(" + nodeID + "," + layerID +
		// ") = ");
		List<Integer> ids = children.get(nodeID);
		if (ids == null)
		{
			// System.err.println("[]");
			return Collections.emptyList();
		} 
		else
		{
			// System.err.println(ids);
			return Collections.unmodifiableList(ids);
		}
	}

	@Override
	public List<Integer> getLeaves()
	{
		List<Integer> leaves = new LinkedList<Integer>();
		collectLeaves(getRootID(), leaves);
		return leaves;
	}

	@Override
	protected void collectLeaves(int nodeID, List<Integer> leaves)
	{
		if (nodeID != -1)
		{
			List<Integer> nodeChildren = children.get(nodeID);
			if (nodeChildren == null)
			{
				leaves.add(nodeID);
			} else
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
			needsUpdate = true;
		}
	}

	@Override
	public void decollapse(int nodeID)
	{
		collapsed.remove(nodeID);
        needsUpdate = true;
	}

	@Override
	public void decollapseAll()
	{
		collapsed = new HashSet<Integer>();
        needsUpdate = true;
	}

	@Override
	public int addNode(String caption, String label, int nodeStatus)
	{
		int nodeID = getNextFreeID();
		nodeCaptions.put(nodeID, caption);
		edgeLabels.put(nodeID, label);
		status.put(nodeID, nodeStatus);
        needsUpdate = true;
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

		parents = new HashMap<Integer, Integer>();
		children = new HashMap<Integer, List<Integer>>();
		nodeCaptions = new HashMap<Integer, String>();
		edgeLabels = new HashMap<Integer, String>();
		status = new HashMap<Integer, Integer>();
		collapsed = new HashSet<Integer>();

		nextID = 0;
        needsUpdate = true;
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
				// TODO: a little risky, root node could be assigned another ID
				m.setRootID(0);
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
		} else
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
		// go through children recursively
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

	@Override
	public void addNode(int id, String caption, String label, int nodeStatus)
	{
		setNodeCaption(id, caption);
		setEdgeLabel(id, label);
		setNodeStatus(id, nodeStatus);
		nextID = Math.max(id + 1, nextID);
        needsUpdate = true;
	}

	@Override
	public String toString()
	{
		// ad-hoc
		return nodeCaptions.toString();
	}

	@Override
	public boolean removeLeaf(int id)
	{
		if (children.get(id) == null || children.get(id).size() == 0)
		{
			children.get(parents.get(id)).remove(new Integer(id));
			parents.remove(id);
			nodeCaptions.remove(id);
			edgeLabels.remove(id);
			status.remove(id);
			collapsed.remove(id);
			layers.remove(id);
	        needsUpdate = true;
			return true;
		} 
		else
		{
			return false;
		}
	}
}
