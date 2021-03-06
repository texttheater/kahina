package org.kahina.logic.sat.muc.visual;

import java.util.List;

import org.kahina.core.control.KahinaEvent;
import org.kahina.core.gui.event.KahinaSelectionEvent;
import org.kahina.logic.sat.muc.MUCInstance;
import org.kahina.logic.sat.muc.gui.ClauseSelectionEvent;
import org.kahina.logic.sat.visual.cnf.list.KahinaSatInstanceListViewPanel;

public class MUCStepViewPanel extends KahinaSatInstanceListViewPanel
{
    MUCInstance kahina;
    MUCStepView view;
    
    public MUCStepViewPanel(MUCInstance kahina)
    {
        super();
        this.kahina = kahina;
        kahina.registerSessionListener("clauseSelection", this);
        getList().addMouseListener(new MUCStepViewListener(kahina, this));
    }
    
    public void processEvent(KahinaEvent e)
    {
        if (e instanceof ClauseSelectionEvent)
        {
            processEvent((ClauseSelectionEvent) e);
        }
        else
        {
            super.processEvent(e);
        }
    }
    
    public void processEvent(ClauseSelectionEvent e)
    {
        if (view.currentStep != null) 
        {
            List<Integer> selectedClauses = e.getClauseIDs();
            getList().getSelectionModel().clearSelection();
            for (int i = 0; i < view.currentStep.getUc().size(); i++)
            {
                if (selectedClauses.contains(view.currentStep.getUc().get(i)))
                {
                    getList().getSelectionModel().addSelectionInterval(i, i);
                }
            }
            getList().repaint();
        }
    }
    
    public void setView(MUCStepView view)
    {
        super.setView(view);
        this.view = view;
    }
    
    public void selectAll()
    {
        if (view.currentStep != null)
        {
            getList().setSelectionInterval(0, view.currentStep.getUc().size() - 1);
            getList().repaint();
        }
    }
}
