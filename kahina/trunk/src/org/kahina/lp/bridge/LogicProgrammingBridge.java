package org.kahina.lp.bridge;

import java.util.HashMap;

import org.kahina.core.KahinaInstance;
import org.kahina.core.bridge.KahinaBridge;
import org.kahina.core.control.KahinaController;
import org.kahina.core.data.source.KahinaSourceCodeLocation;
import org.kahina.core.event.KahinaControlEvent;
import org.kahina.core.event.KahinaTreeEvent;
import org.kahina.core.event.KahinaTreeEventType;
import org.kahina.core.gui.KahinaGUI;
import org.kahina.lp.LogicProgrammingStep;
import org.kahina.lp.LogicProgrammingStepType;
import org.kahina.lp.event.LogicProgrammingBridgeEvent;
import org.kahina.lp.event.LogicProgrammingBridgeEventType;

public class LogicProgrammingBridge extends KahinaBridge
{   
    //a dynamic map from external step IDs to most recent corresponding tree nodes
    protected HashMap<Integer,Integer> stepIDConv;
    
    //always contains the internal ID of the most recent step
    protected int currentID = -1;
    
    //store the state of the bridge, determining the next result of getPressedButton()
    char bridgeState;
    
    //in skip mode, this is the internal step ID of the step we are skipping
    int skipID = -1;
    
    
    public LogicProgrammingBridge(KahinaInstance kahina, KahinaGUI gui, KahinaController control)
    {
        super(kahina, gui, control);
        stepIDConv = new HashMap<Integer,Integer>();
    }
    
    /**
     * convert external step IDs to internal IDs corresponding to tree nodes
     * uses entries in stepIDConv table, extending it together with the tree if no entry was found
     * @return an internal step ID corresponding to the external ID
     */
    public int convertStepID(int extID)
    {
        Integer intID = stepIDConv.get(extID);
        if (intID == null)
        {
            LogicProgrammingStep newStep = new LogicProgrammingStep();
            intID = newStep.getID();
            newStep.setExternalID(extID);
            System.err.println(newStep);
            newStep.store();
            stepIDConv.put(extID, intID);
        }
        return intID;
    }
    
    public void registerStepInformation(int extID, String stepInfo)
    {
        System.err.println("registerStepInformation(" + extID + ",\"" + stepInfo + "\")");
        int stepID = convertStepID(extID);
        LogicProgrammingStep.get(stepID).setGoalDesc(stepInfo);
        control.processEvent(new LogicProgrammingBridgeEvent(LogicProgrammingBridgeEventType.SET_GOAL_DESC, stepID, stepInfo));
        currentID = stepID;
    }
    
    public void registerStepSourceCodeLocation(int extID, String absolutePath, int lineNumber)
    {
        System.err.println("registerStepSourceCodeLocation(" + extID + ",\"" + absolutePath + "\"," + lineNumber + ")");
        int stepID = convertStepID(extID);
        LogicProgrammingStep.get(stepID).setSourceCodeLocation(new KahinaSourceCodeLocation(absolutePath, lineNumber - 1));
        currentID = stepID;
    }
    
    public void registerStepLocation(int stepID, int parentID)
    {
        System.err.println("registerStepLocation(" + stepID + "," + parentID + ")");
        control.processEvent(new KahinaTreeEvent(KahinaTreeEventType.NEW_NODE, convertStepID(stepID), convertStepID(parentID)));
        currentID = convertStepID(stepID);
    }
    
    public void registerStepRedo(int extID)
    {
        System.err.println("registerStepRedo(" + extID + ")");
        int lastStepID = convertStepID(extID);
        int newStepID = kahina.getNewStepID();
        LogicProgrammingStep newStep = LogicProgrammingStep.get(lastStepID).copy();
        newStep.setType(LogicProgrammingStepType.REDO);
        stepIDConv.put(extID, newStepID);
        control.processEvent(new LogicProgrammingBridgeEvent(LogicProgrammingBridgeEventType.STEP_REDO, lastStepID));
        currentID = newStepID;
    }
    
    public void registerStepExit(int extID, boolean deterministic)
    {
        System.err.println("registerStepExit(" + extID + "," + deterministic + ")");
        LogicProgrammingStep step = LogicProgrammingStep.get(convertStepID(extID));
        if (deterministic)
        {
            step.setType(LogicProgrammingStepType.DET_EXIT);
            control.processEvent(new LogicProgrammingBridgeEvent(LogicProgrammingBridgeEventType.STEP_DET_EXIT, step.getID()));
        }
        else
        {
            control.processEvent(new LogicProgrammingBridgeEvent(LogicProgrammingBridgeEventType.STEP_NONDET_EXIT, step.getID()));
            step.setType(LogicProgrammingStepType.EXIT);
        }
        step.store();
        currentID = step.getID();
    }
    
    public void registerStepFinished(int extID)
    {
        System.err.println("registerStepFinished(" + extID + ")");
        int stepID = convertStepID(extID);
        control.processEvent(new LogicProgrammingBridgeEvent(LogicProgrammingBridgeEventType.STEP_FINISHED, stepID));
        currentID = stepID;
    }
    
    public void registerStepFailure(int extID)
    {
        System.err.println("registerStepFailure(" + extID + ")");
        int stepID = convertStepID(extID);
        LogicProgrammingStep.get(stepID).setType(LogicProgrammingStepType.FAIL);   
        LogicProgrammingStep.get(stepID).store();  
        control.processEvent(new LogicProgrammingBridgeEvent(LogicProgrammingBridgeEventType.STEP_FAIL, stepID));
        currentID = stepID;
    }
       
    public char getPressedButton()
    {
        switch (bridgeState)
        {
            case 'n':
            {
                return 'n';
            }
            case 'p':
            {
                return 'n';
            }
            case 'q':
            {
                return 'n';
            }
            case 'c':
            {
                bridgeState = 'n';
                return 'c';
            }
            case 'f':
            {
                bridgeState = 'n';
                return 'f';
            }
            case 'l':
            {
                bridgeState = 'l';
                return 'c';
            }
            case 't':
            {
                bridgeState = 's';
                return 'c';
            }
            case 's':
            {
                if (skipID == currentID)
                {
                    skipID = -1;
                    bridgeState = 'n';
                    return 'n';
                }
                else
                {
                    return 'c';
                }
            }
            default:
            {
                bridgeState = 'n';
                return 'n';
            }
        }
    }
    
    public void processEvent(KahinaControlEvent e)
    {
        String command = e.getCommand();
        if (command.equals("creep"))
        {
            if (bridgeState == 'n')
            {
                bridgeState = 'c';
            }
            else if (bridgeState == 'p')
            {
                skipID = -1;
                bridgeState = 'c';
            }
            else if (bridgeState == 'q')
            {
                skipID = -1;
                bridgeState = 'c';
            }
        }
        else if (command.equals("fail"))
        {
            if (bridgeState == 'n')
            {
                bridgeState = 'f';
            }
            else if (bridgeState == 'p')
            {
                skipID = -1;
                bridgeState = 'f';
            }
            else if (bridgeState == 'q')
            {
                skipID = -1;
                bridgeState = 'f';
            }
        }
        else if (command.equals("skip"))
        {
            if (bridgeState == 'n')
            {
                bridgeState = 't';
                skipID = currentID;
            }
            else if (bridgeState == 'p')
            {
                bridgeState = 't';
            }
            else if (bridgeState == 'q')
            {
                bridgeState = 't';
                skipID = currentID;
            }
        }
        else if (command.equals("leap"))
        {
            if (bridgeState == 'n')
            {
                bridgeState = 'l';
            }
            else if (bridgeState == 'p')
            {
                bridgeState = 'l';
                skipID = -1;
            }
            else if (bridgeState == 'q')
            {
                bridgeState = 'l';
                skipID = -1;
            }
        }
        else if (command.equals("(un)pause"))
        {
            if (bridgeState == 't')
            {
                bridgeState = 'p';
            }
            else if (bridgeState == 's')
            {
                bridgeState = 'q';
            }
            else if (bridgeState == 'p')
            {
                bridgeState = 't';
            }
            else if (bridgeState == 'q')
            {
                bridgeState = 's';
            }
        }
    }
}
