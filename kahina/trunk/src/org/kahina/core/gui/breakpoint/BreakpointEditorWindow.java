package org.kahina.core.gui.breakpoint;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.kahina.core.breakpoint.KahinaBreakpoint;
import org.kahina.core.breakpoint.TreeAutomaton;
import org.kahina.core.control.KahinaController;
import org.kahina.core.control.KahinaListener;
import org.kahina.core.control.event.KahinaEvent;

public class BreakpointEditorWindow extends JFrame implements ActionListener, KahinaListener, ListSelectionListener
{
    KahinaController control;
    
    JButton newBreakpointButton;
    JButton activateBreakpointButton;
    JButton deactivateBreakpointButton;
    JButton removeBreakpointButton;
    JPanel breakpointListPanel;
    JList breakpointList;
    
    BreakpointEditPanel editPanel;
    
    List<KahinaBreakpoint> breakpoints;
    List<TreeAutomaton> compiledBreakpoints;
    int curID; //list ID of the breakpoint we are editing
    
    public BreakpointEditorWindow(KahinaController control)
    {       
        this.control = control;
        control.registerListener("breakpoint_editor", this);
        this.setTitle("Kahina Breakpoint Editor");
        this.setSize(800,600);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
       
        breakpoints = new ArrayList<KahinaBreakpoint>();
        compiledBreakpoints = new ArrayList<TreeAutomaton>();
        curID = -1;
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.LINE_AXIS));    
        mainPanel.add(buildLeftPanel()); 
        mainPanel.add(buildRightPanel());

        
        add(mainPanel);
        
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(new BreakpointEditorFileMenu(control));
        this.setJMenuBar(menuBar);
        
        adaptActivationStatus();
    }
    
    private JPanel buildLeftPanel()
    {
        breakpointListPanel = new JPanel();
        breakpointListPanel.setLayout(new BoxLayout(breakpointListPanel, BoxLayout.PAGE_AXIS));
        breakpointListPanel.setBorder(BorderFactory.createTitledBorder("Breakpoints"));
        
        newBreakpointButton = new JButton("New");
        newBreakpointButton.setActionCommand("newBreakpoint");
        newBreakpointButton.addActionListener(this);
        newBreakpointButton.setAlignmentX(CENTER_ALIGNMENT);
        breakpointListPanel.add(newBreakpointButton);
        breakpointListPanel.add(Box.createRigidArea(new Dimension(0,5)));
        
        activateBreakpointButton = new JButton("Activate");
        activateBreakpointButton.setActionCommand("activateBreakpoint");
        activateBreakpointButton.addActionListener(this);
        activateBreakpointButton.setAlignmentX(CENTER_ALIGNMENT);
        breakpointListPanel.add(activateBreakpointButton, BorderLayout.NORTH);
        breakpointListPanel.add(Box.createRigidArea(new Dimension(0,5)));
        
        breakpointList = new JList();
        breakpointList.setListData(breakpoints.toArray());
        breakpointList.addListSelectionListener(this);
        JScrollPane listScroller = new JScrollPane(breakpointList);
        listScroller.setPreferredSize(new Dimension(250, 80));
        listScroller.setMaximumSize(new Dimension(300, 1000));
        listScroller.setAlignmentX(CENTER_ALIGNMENT);
        breakpointListPanel.add(listScroller);
        
        breakpointListPanel.add(Box.createRigidArea(new Dimension(0,5)));
        
        deactivateBreakpointButton = new JButton("Deactivate");
        deactivateBreakpointButton.setActionCommand("deactivateBreakpoint");
        deactivateBreakpointButton.addActionListener(this);
        deactivateBreakpointButton.setAlignmentX(CENTER_ALIGNMENT);
        breakpointListPanel.add(deactivateBreakpointButton);
        breakpointListPanel.add(Box.createRigidArea(new Dimension(0,5)));
        
        removeBreakpointButton = new JButton("Remove");
        removeBreakpointButton.setActionCommand("removeBreakpoint");
        removeBreakpointButton.addActionListener(this);
        removeBreakpointButton.setAlignmentX(CENTER_ALIGNMENT);
        breakpointListPanel.add(removeBreakpointButton, BorderLayout.SOUTH);
        
        return breakpointListPanel;
    }
    
    private JPanel buildRightPanel()
    {
        editPanel = new BreakpointEditPanel(control);
        return editPanel;       
    }    
    
    public void actionPerformed(ActionEvent e)
    {
        String s = e.getActionCommand();
        if (s.equals("newBreakpoint"))
        {
            control.processEvent(new BreakpointEditorEvent(BreakpointEditorEvent.NEW_BREAKPOINT));
        }
        else if (s.equals("activateBreakpoint"))
        {
            control.processEvent(new BreakpointEditorEvent(BreakpointEditorEvent.ACTIVATE_BREAKPOINT, curID));
        }
        else if (s.equals("deactivateBreakpoint"))
        {
            control.processEvent(new BreakpointEditorEvent(BreakpointEditorEvent.DEACTIVATE_BREAKPOINT, curID));
        }
        else if (s.equals("removeBreakpoint"))
        {
            control.processEvent(new BreakpointEditorEvent(BreakpointEditorEvent.REMOVE_BREAKPOINT, curID));
        }
        adaptActivationStatus();
    }
    
    public void processEvent(KahinaEvent e)
    {
        if (e.getType().equals("breakpoint_editor"))
        {
            processBreakpointEvent((BreakpointEditorEvent) e);
        }
    }
    
    private void processBreakpointEvent(BreakpointEditorEvent e)
    {
        switch (e.getEditorEventType())
        {
            case BreakpointEditorEvent.NEW_BREAKPOINT:
            {
                KahinaBreakpoint newBreakpoint = new KahinaBreakpoint();
                breakpoints.add(newBreakpoint);
                compiledBreakpoints.add(new TreeAutomaton(newBreakpoint));
                breakpointList.setListData(breakpoints.toArray());
                breakpointList.setSelectedIndex(breakpoints.size() - 1);
                break;
            }
            case BreakpointEditorEvent.ACTIVATE_BREAKPOINT:
            {
                breakpoints.get(curID).activate();
                breakpointList.repaint();
                break;
            }
            case BreakpointEditorEvent.DEACTIVATE_BREAKPOINT:
            {
                breakpoints.get(curID).deactivate();
                breakpointList.repaint();
                break;
            }
            case BreakpointEditorEvent.REMOVE_BREAKPOINT:
            {
                breakpoints.remove(curID);
                compiledBreakpoints.remove(curID);
                curID = -1;
                breakpointList.setListData(breakpoints.toArray());
                break;
            }
            case BreakpointEditorEvent.TEST_BREAKPOINTS:
            {
                //compile the currently opened breakpoint once more
                if (curID != -1)
                {
                    editPanel.updateBreakpointPattern();
                    TreeAutomaton compiledBreakpoint = breakpoints.get(curID).compile();
                    compiledBreakpoints.set(curID, compiledBreakpoint);
                }
                BreakpointTestWindow w = new BreakpointTestWindow(compiledBreakpoints, control);
                w.setVisible(true);
                break;
            }
            case BreakpointEditorEvent.BREAKPOINT_NAME_UPDATE:
            {
                breakpointList.repaint();
                breakpointList.validate();
            }
        }
    }
    
    public void valueChanged(ListSelectionEvent e)
    {
        //compile the most recently edited breakpoint once more
        if (curID != -1)
        {
            editPanel.updateBreakpointPattern();
            TreeAutomaton compiledBreakpoint = breakpoints.get(curID).compile();
            compiledBreakpoints.set(curID, compiledBreakpoint);
        }
        curID = breakpointList.getSelectedIndex();
        if (curID == -1)
        {
            editPanel.setBreakpoint(null);
        }
        else
        {
            editPanel.setBreakpoint(breakpoints.get(curID));
        }
        adaptActivationStatus();
    }
    
    private void adaptActivationStatus()
    {
        if (curID == -1)
        {
            removeBreakpointButton.setEnabled(false);
            activateBreakpointButton.setEnabled(false);
            deactivateBreakpointButton.setEnabled(false);
            editPanel.setEnabled(false);
        }
        else
        {
            if (breakpoints.get(curID).isActive())
            {
                activateBreakpointButton.setEnabled(false);
                deactivateBreakpointButton.setEnabled(true);
            }
            else
            {
                activateBreakpointButton.setEnabled(true);
                deactivateBreakpointButton.setEnabled(false);
            }
            removeBreakpointButton.setEnabled(true);
            editPanel.setEnabled(true);
        }
    }
}
