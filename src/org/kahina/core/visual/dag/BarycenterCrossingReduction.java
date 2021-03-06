package org.kahina.core.visual.dag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.kahina.core.data.dag.KahinaDAG;

public class BarycenterCrossingReduction extends CrossingReduction
{
	static KahinaDAG currentDAG = null;
	
	public static ArrayList<List<Integer>> minimizeCrossings(KahinaDAG dag, ArrayList<List<Integer>> nodeLevels)
	{
		ArrayList<List<Integer>> reorderedLayers = new ArrayList<List<Integer>>();
		currentDAG = dag;
		//remember initial positions of all nodes on left-right-scale
		HashMap<Integer,Integer> nodePos = new HashMap<Integer,Integer>();
		for (int i = 0; i < nodeLevels.size(); i++)
		{
			List<Integer> level = nodeLevels.get(i);
			for (int j = 0; j < level.size(); j++)
			{
				nodePos.put(level.get(j), j);
			}
		}
		//top-down variant
		if (nodeLevels.size() > 0)
		{
    		List<Integer> topLayer = new LinkedList<Integer>();
    		topLayer.addAll(nodeLevels.get(0));
    		reorderedLayers.add(topLayer);
		}
		for (int i = 1; i < nodeLevels.size(); i++)
        {
            List<NodeAvPosPair> pairs = new ArrayList<NodeAvPosPair>();
            //System.err.println("Node level " + i + ": " + nodeLevels.get(i));
            for (int node : nodeLevels.get(i))
            {
                double avPos = 0.0;
                List<Integer> parents = dag.getVisibleParents(node);
                //System.err.println("Visible parents: " + parents);
                double inDegree = parents.size() + 0.0;
                for (int parent : parents)
                {
                    avPos += nodePos.get(parent) / inDegree;
                }
                //System.err.println("  " + node + ".avPos = " + avPos);
                pairs.add(new NodeAvPosPair(node, avPos));
            }
            Collections.sort(pairs);
            List<Integer> reorderedLevel = new LinkedList<Integer>();
            int j = 0;
            for (NodeAvPosPair pair : pairs)
            {
                reorderedLevel.add(pair.nodeID);
                nodePos.put(pair.nodeID, j);
                j++;
            }
            //System.err.println("Reordered " + i + ": " + reorderedLevel);
            reorderedLayers.add(reorderedLevel);
        }
        return reorderedLayers;
		//bottom-up variant
		/*for (int i = nodeLevels.size() - 1; i >= 0; i--)
		{
			List<NodeAvPosPair> pairs = new ArrayList<NodeAvPosPair>();
			for (int node : nodeLevels.get(i))
			{
				double avPos = 0.0;
				List<Integer> parents = dag.getVisibleParents(node);
				//System.err.println("Visible parents: " + parents);
				double inDegree = parents.size() + 0.0;
				for (int parent : parents)
				{
					avPos += nodePos.get(parent) / inDegree;
				}
				pairs.add(new NodeAvPosPair(node, avPos));
			}
			Collections.sort(pairs);
			List<Integer> reorderedLevel = new LinkedList<Integer>();
			for (NodeAvPosPair pair : pairs)
			{
				reorderedLevel.add(pair.nodeID);
			}
			reorderedLayers.add(0,reorderedLevel);
		}
		return reorderedLayers;*/
	}
	

}
