package org.kahina.core.visual.breakpoint;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.kahina.core.data.breakpoint.KahinaBreakpoint;
import org.kahina.core.data.breakpoint.KahinaControlPoint;
import org.kahina.core.data.breakpoint.patterns.TreeAutomaton;
import org.kahina.core.edit.breakpoint.BreakpointEditorEvent;

public class KahinaControlPointProfileListener implements ActionListener, ListSelectionListener
{
    KahinaControlPointProfileViewPanel profilePanel;
    
    public KahinaControlPointProfileListener(KahinaControlPointProfileViewPanel profilePanel)
    {
        this.profilePanel = profilePanel;
    }

    public void actionPerformed(ActionEvent e)
    {
        String s = e.getActionCommand();
        if (s.equals("newControlPoint"))
        {
            //TODO: adapt type argument, 0 must not be the default value in all cases!
            KahinaControlPoint newControlPoint = new KahinaControlPoint(0);
            profilePanel.view.getModel().addControlPoint(newControlPoint);
            profilePanel.pointList.setListData(profilePanel.view.getModel().getControlPoints());
            profilePanel.pointList.setSelectedIndex(profilePanel.view.getModel().getSize() - 1);
        } 
        else if (s.equals("removeControlPoint"))
        {
            profilePanel.removeCurrentControlPoint();
        }
        //TODO: new profile, import profile, save profile etc.
        //TODO: deal with the activation status!
        //adaptActivationStatus();
    }

    public void valueChanged(ListSelectionEvent arg0)
    {
        int curID = profilePanel.pointList.getSelectedIndex();
        if (curID == -1)
        {
            profilePanel.pointPanel.view.display(null);
        } 
        else
        {
            profilePanel.pointPanel.view.display(profilePanel.view.getModel().getControlPoint(curID));
        }
        profilePanel.pointPanel.updateDisplay();
        profilePanel.pointPanel.revalidate();
        //TODO: deal with the activation status!
        //adaptActivationStatus();
    }  
}