package org.kahina.core.visual.tree;

import java.util.ArrayList;

public class WidthVector
{
    public ArrayList<Integer> start;
    public ArrayList<Integer> end;
    
    public static boolean VERBOSE = false;
    
    public WidthVector()
    {
        start = new ArrayList<Integer>();
        start.add(1);
        end = new ArrayList<Integer>();
        end.add(1);
    }
    
    public WidthVector(int start, int end)
    {
        this.start = new ArrayList<Integer>();
        this.start.add(start);
        this.end = new ArrayList<Integer>();
        this.end.add(end);
    }
    
    public static WidthVector adjoin(WidthVector w1, WidthVector w2, int horiDistance)
    {
        if (VERBOSE) System.err.println("Adjoining: " + w1 + " and " + w2);
        WidthVector w3 = new WidthVector();
        w3.start.clear();
        w3.end.clear();
        int w1size = w1.start.size();
        int w2size = w2.start.size();
        int minSize = w1size;
        if (w1size > w2size) minSize = w2size;
        int maxReqDistance = 0;
        int maxReqDistanceLevel = 0;
        for (int i = 0; i < minSize; i++)
        {
            int reqDistance = w1.end.get(i) + w2.start.get(i);
            if (reqDistance > maxReqDistance)
            {
                maxReqDistance = reqDistance;
                maxReqDistanceLevel = i;
            }
        }
        int leftAxisOffset = w1.start.get(maxReqDistanceLevel);
        int totalWidth = leftAxisOffset;
        totalWidth += horiDistance;
        totalWidth += w1.end.get(maxReqDistanceLevel);
        totalWidth += w2.start.get(maxReqDistanceLevel);
        totalWidth += w2.end.get(maxReqDistanceLevel);
        int leftAxisMovement = totalWidth / 2 - leftAxisOffset;
        int rightAxisMovement = (w1.end.get(maxReqDistanceLevel) + w2.start.get(maxReqDistanceLevel) - leftAxisMovement);
        /*int leftAxisMovement = maxReqDistance / 2;
        int rightAxisMovement = leftAxisMovement;*/
        if (VERBOSE) System.err.println("  leftAxisMovement: " + leftAxisMovement);
        if (VERBOSE) System.err.println("  rightAxisMovement: " + rightAxisMovement);
        for (int i = 0; i < minSize; i++)
        {
            w3.start.add(w1.start.get(i) + horiDistance + leftAxisMovement);
            w3.end.add(w2.end.get(i) + horiDistance + rightAxisMovement);
            if (VERBOSE) System.err.println("  Adding: [" + (w1.start.get(i) + leftAxisMovement) + "," + (w2.end.get(i) + rightAxisMovement) + "]");
        }
        for (int i = minSize; i < w1size; i++)
        {
            w3.start.add(w1.start.get(i) + leftAxisMovement);
            w3.end.add(w1.end.get(i) - leftAxisMovement);
            if (VERBOSE) System.err.println("  Adding: [" + (w1.start.get(i) + leftAxisMovement) + "," + (w1.end.get(i) - leftAxisMovement) + "]");
        }
        for (int i = minSize; i < w2size; i++)
        {
            w3.start.add(w2.start.get(i) - rightAxisMovement);
            w3.end.add(w2.end.get(i) + rightAxisMovement);
            if (VERBOSE) System.err.println("  Adding: [" + (w2.start.get(i) - rightAxisMovement) + "," + (w2.end.get(i) + rightAxisMovement) + "]");
        }
        if (VERBOSE) System.err.println("Result: " + w3);
        return w3;
    }
    
    public void add(WidthVector w2)
    {
        int w1size = start.size();
        int w2size = w2.start.size();
        int minSize = w1size;
        if (w1size > w2size) minSize = w2size;
        int maxReqDistance = 0;
        for (int i = 0; i < minSize; i++)
        {
            int reqDistance = end.get(i) + w2.start.get(i);
            if (reqDistance > maxReqDistance)
            {
                maxReqDistance = reqDistance;
            }
        }
        for (int i = 0; i < w2size && i < w1size; i++)
        {
            end.set(i, maxReqDistance + w2.getEnde(i));
        }
        for (int i = w1size; i < w2size; i++)
        {
            start.add(0);
            end.add(i, maxReqDistance + w2.getEnde(i));
        }
    }
    
    public void moveAxis(int axisMovement)
    {
        int size = start.size();
        for (int i = 0; i < size; i++)
        {
            start.set(i, start.get(i) + axisMovement);
            end.set(i, end.get(i) - axisMovement);
        }
    }
    
    public int maximumLeftDistance()
    {
        //System.err.println("Determine left distance for " + toString());
        int maximum = 1;
        int size = start.size();
        for (int i = 0; i < size; i++)
        {
            int val = start.get(i);
            if (val > maximum) maximum = val;
        }
        //System.err.println("Result: " + maximum);
        return maximum;
    }
    
    public int maximumRightDistance()
    {
        int maximum = 1;
        int size = end.size();
        for (int i = 0; i < size; i++)
        {
            int val = end.get(i);
            if (val > maximum) maximum = val;
        }
        return maximum;
    }
    
    public int getStart(int level)
    {
        if (level >= start.size())
        {
            return 0;
        }
        return start.get(level);
    }
    
    public int getEnde(int level)
    {
        if (level >= end.size())
        {
            return 0;
        }
        return end.get(level);
    }
    
    /**
     * 
     * @param w1 (may be null)
     * @param w2 (may NOT be null)
     * @return
     */
    public static int computeNecessaryDistance(WidthVector w1, WidthVector w2)
    {
        if (w1 == null)
        {
            return w2.maximumLeftDistance();
        }
        //System.err.print(" Distance between: " + w1 + " and " + w2 + ": ");
        int w1size = w1.start.size();
        int w2size = w2.start.size();
        int minSize = w1size;
        if (w1size > w2size) minSize = w2size;
        int maxReqDistance = 0;
        for (int i = 0; i < minSize; i++)
        {
            int reqDistance = w1.end.get(i) + w2.start.get(i);
            if (reqDistance > maxReqDistance)
            {
                maxReqDistance = reqDistance;
            }
        }
        //System.err.print(maxReqDistance + " ");
        return maxReqDistance;
    }
    
    public WidthVector copy()
    {
        WidthVector copy = new WidthVector();
        copy.start.clear();
        copy.end.clear();
        copy.start.addAll(start);
        copy.end.addAll(end);
        return copy;
    }
    
    @Override
	public String toString()
    {
        int size = start.size();
        String str = "";
        for (int i = 0; i < size; i++)
        {
            str += "["+ start.get(i) + "," + end.get(i) + "]";
        }
        return str;
    }
}
