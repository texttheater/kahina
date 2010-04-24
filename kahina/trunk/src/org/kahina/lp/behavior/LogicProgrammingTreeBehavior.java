package org.kahina.lp.behavior;

import java.util.HashSet;
import java.util.Set;

import org.kahina.core.KahinaInstance;
import org.kahina.core.behavior.KahinaTreeBehavior;
import org.kahina.core.control.KahinaController;
import org.kahina.core.data.tree.KahinaTree;
import org.kahina.core.event.KahinaEvent;
import org.kahina.core.event.KahinaTreeEvent;
import org.kahina.core.event.KahinaTreeEventType;
import org.kahina.lp.LogicProgrammingStep;
import org.kahina.lp.LogicProgrammingStepType;
import org.kahina.lp.event.LogicProgrammingBridgeEvent;
import org.kahina.lp.event.LogicProgrammingBridgeEventType;

public class LogicProgrammingTreeBehavior extends KahinaTreeBehavior
{
    private static final boolean verbose = false;
    
    //call dimension is always stored in a secondary tree structure
    protected KahinaTree secondaryTree;
    
    //memory for construction of the primary tree
    protected int lastActiveID;
    
    //store information whether steps exited or failed deterministically
    //TODO: this information must later be accessible to the drawing routine somehow
    protected Set<Integer> deterministicallyExited;
    protected Set<Integer> nonDetermBecauseOfRedo;
    
    public LogicProgrammingTreeBehavior(KahinaTree tree, KahinaController control, KahinaInstance kahina, KahinaTree secondaryTree)
    {
        super(tree, control, kahina);
        this.secondaryTree = secondaryTree;
        this.lastActiveID = -1;
        deterministicallyExited = new HashSet<Integer>();
        nonDetermBecauseOfRedo = new HashSet<Integer>();
        control.registerListener("logic programming bridge", this);
        if (verbose) System.err.println("new LogicProgrammingTreeBehavior(" + tree + "," + control + "," + kahina + "," + secondaryTree + ")");
    }
    
    /**
     * contains the logic by which the tree is formed out of callstacks
     * called by the event processing routine for a KahinaTreeEvent of type "new step"
     */
    public void integrateIncomingNode(int stepID, int ancestorID)
    {    
        if (verbose) System.err.println("LogicProgrammingTreeBehavior.integratingIncomingNode(" + stepID + "," + ancestorID + ")");
        if (verbose) System.err.println("\t object.addChild(" + lastActiveID + "," + stepID + ")");
        object.addChild(lastActiveID, stepID);
        //if (verbose) System.err.println(object.exportXML());
        lastActiveID = stepID;
        secondaryTree.addChild(ancestorID, stepID);
        //if (verbose) System.err.println(secondaryTree.exportXML());
    }
    
    /**
     * integrate incoming step detail information (usually goal descriptions) into tree
     * called by the event processing routine for a KahinaTreeEvent of type "new step"
     * @param externalID - the step ID in the monitored logic programming system
     * @param stepInfo - the step information to be associated with the step
     */
    public void processStepInformation(int stepID, String stepInfo)
    {
        if (verbose) System.err.println("LogicProgrammingTreeBehavior.processStepInformation(" + stepID + ",\"" + stepInfo + "\")");
        object.addNode(stepID, LogicProgrammingStep.get(stepID).getExternalID() + " " + stepInfo,"",LogicProgrammingStepType.CALL);
        //TODO: make this unnecessary => new structure for secondary tree, perhaps not a full tree model?
        secondaryTree.addNode(stepID, LogicProgrammingStep.get(stepID).getExternalID() + " " + stepInfo,"",LogicProgrammingStepType.CALL);
    }
    
    /**
     * register and react to an incoming redo operation
     * @param externalID - the ID of the step being redone in the monitored logic programming system
     */
    public void processStepRedo(int lastStepID)
    {
        if (verbose) System.err.println("LogicProgrammingTreeBehavior.processStepRedo(" + lastStepID + ")");
        
        nonDetermBecauseOfRedo.add(lastStepID);

        //generate a  new node corresponding to the new internal step
        int newStepID = object.addNode(object.getNodeCaption(lastStepID), "", LogicProgrammingStepType.REDO);
        //TODO: make this unnecessary if possible
        secondaryTree.addNode(object.getNodeCaption(lastStepID), "", LogicProgrammingStepType.REDO);
        
        object.setNodeStatus(lastStepID, LogicProgrammingStepType.DET_EXIT);

        //adapt call dimension
        int ancestorID = secondaryTree.getParent(lastStepID);
        secondaryTree.addChild(ancestorID, newStepID);

        //adapt control flow dimension
        int parentID = object.getParent(lastStepID);
        object.addChild(parentID, newStepID);
        
        lastActiveID = newStepID;
    }
    
    /**
     * register and react to an incoming exit operation
     * @param externalID - the ID of the step that exited in the monitored logic programming system
     * @param deterministic - whether the exit was deterministic
     */
    public void processStepExit(int stepID, boolean deterministic)
    {
        if (deterministic)
        {
            deterministicallyExited.add(stepID);
            object.setNodeStatus(stepID, LogicProgrammingStepType.DET_EXIT);
        }
        else
        {
            object.setNodeStatus(stepID, LogicProgrammingStepType.EXIT);
        }       
        //lastActiveID = stepID;
    }
    
    /**
     * registers and reacts to an incoming failed step
     * @param externalID - the ID of the step that failed in the monitored logic programming system
     */
    public void processStepFail(int stepID)
    {
        if (verbose) System.err.println("LogicProgrammingTreeBehavior.processStepFail(" + stepID + ")");      
        deterministicallyExited.add(stepID);
        object.setNodeStatus(stepID, LogicProgrammingStepType.FAIL);
        lastActiveID = object.getParent(stepID);
    }
    
    public void processEvent(KahinaEvent e)
    {
        if (verbose) System.err.println("LogicProgrammingTreeBehavior.processEvent(" + e + ")");
        if (e instanceof KahinaTreeEvent)
        {
            processEvent((KahinaTreeEvent) e);
        }
        else if (e instanceof LogicProgrammingBridgeEvent)
        {
            processEvent((LogicProgrammingBridgeEvent) e);
        }
    }
    
    public void processEvent(KahinaTreeEvent e)
    {
        switch (e.getTreeEventType())
        {
            case KahinaTreeEventType.NEW_NODE:
            {
                integrateIncomingNode(e.getFirstID(), e.getSecondID());
                break;
            }
        }
    }
    
    public void processEvent(LogicProgrammingBridgeEvent e)
    {
        switch (e.getEventType())
        {
            case LogicProgrammingBridgeEventType.SET_GOAL_DESC:
            {
                processStepInformation(e.getExternalID(), e.getStrContent());
                break;
            }
            case LogicProgrammingBridgeEventType.STEP_REDO:
            {
                processStepRedo(e.getExternalID());
                break;
            }
            case LogicProgrammingBridgeEventType.STEP_DET_EXIT:
            {
                processStepExit(e.getExternalID(), true);
                break;
            }
            case LogicProgrammingBridgeEventType.STEP_NONDET_EXIT:
            {
                processStepExit(e.getExternalID(), false);
                break;
            }
            case LogicProgrammingBridgeEventType.STEP_FAIL:
            {
                processStepFail(e.getExternalID());
                break;
            }
        }
    }
}
