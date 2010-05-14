package org.kahina.core.bridge;

import org.kahina.core.KahinaRunner;
import org.kahina.core.KahinaStep;
import org.kahina.core.breakpoint.KahinaBreakpoint;
import org.kahina.core.control.KahinaListener;
import org.kahina.core.event.KahinaAbortEvent;
import org.kahina.core.event.KahinaControlEvent;
import org.kahina.core.event.KahinaEvent;
import org.kahina.core.event.KahinaTreeMatchEvent;
import org.kahina.core.gui.event.KahinaSelectionEvent;

/**
 * policy: bridges may only operate directly on steps, not on complex structures (behaviors for that purpose)
 *
 * @author jdellert
 *
 */
public class KahinaBridge implements KahinaListener
{
	private static final boolean verbose = false;
    
    public KahinaBridge()
    {
        KahinaRunner.getControl().registerListener("control", this);
        KahinaRunner.getControl().registerListener("treeMatch", this);
    }
    
    public KahinaStep generateStep()
    {
    	if (verbose) System.err.println("KahinaBridge.generateStep()");
        return new KahinaStep();
    }
    
    public void processEvent(KahinaEvent e)
    {
    	if (e instanceof KahinaSelectionEvent)
    	{
    		processEvent((KahinaSelectionEvent) e);
    	} else if (e instanceof KahinaControlEvent)
        {
            processEvent((KahinaControlEvent) e);
        } else if (e instanceof KahinaAbortEvent)
        {
        	processEvent((KahinaAbortEvent) e);
        }
        else if (e instanceof KahinaTreeMatchEvent)
        {
            processEvent((KahinaTreeMatchEvent) e);
        }
    }
    
    //method stub to prevent infinite recursion; implemented by specialized bridges
    protected void processEvent(KahinaControlEvent e)
    {
        
    }
    
    protected void processEvent(KahinaAbortEvent e)
    {
        
    }
    
    protected void processEvent(KahinaSelectionEvent e)
    {
    	
    }
    
    protected void processEvent(KahinaTreeMatchEvent e)
    {
        String breakpointName = e.getBreakpoint().getName();
        int stepID = e.getNodeID();
        //TODO: base this decision on some means other than breakpoint naming
        if (breakpointName.startsWith("skip"))
        {
            processSkipPointMatch(e.getNodeID(), e.getBreakpoint());
        }
        else if (breakpointName.startsWith("break"))
        {
            processBreakPointMatch(e.getNodeID(), e.getBreakpoint());
        }
        else if (breakpointName.startsWith("prof"))
        {
            processProfilePointMatch(e.getNodeID(), e.getBreakpoint());
        }
    }
    
    protected void processSkipPointMatch(int nodeID, KahinaBreakpoint bp)
    {
        
    }
    
    protected void processBreakPointMatch(int nodeID, KahinaBreakpoint bp)
    {
        
    }
    
    protected void processProfilePointMatch(int nodeID, KahinaBreakpoint bp)
    {
        
    }
}
