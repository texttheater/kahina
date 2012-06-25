package org.kahina.core.edit.breakpoint;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;

import org.kahina.core.control.KahinaController;
import org.kahina.core.control.KahinaEvent;
import org.kahina.core.control.KahinaListener;
import org.kahina.core.data.breakpoint.patterns.TreeNodePattern;

public class BooleanConnectorPanel extends JPanel implements MouseListener, KahinaListener
{
    KahinaController control; 
    SingleNodeConstraintPanel nodeConstPanel;   
    Map<TreeNodePattern, Integer> xCoord; 
    Map<TreeNodePattern, Integer> yCoord;
    
    int xDim; 
    int yDim;
    
    TreeNodePattern markedPattern;
    
    // event system is responsible for synchronization with NodeConstraintPanel and TreeFragmentPanel
    private int nodeSelectionMode;
    
    public BooleanConnectorPanel(SingleNodeConstraintPanel nodeConstPanel, KahinaController control)
    {
        this.control = control;
        control.registerListener("breakpoint_editor", this);
        
        this.nodeConstPanel = nodeConstPanel;
        this.addMouseListener(this);
        
        xCoord = new HashMap<TreeNodePattern, Integer>();
        yCoord = new HashMap<TreeNodePattern, Integer>();
        
        markedPattern = null;
        
        nodeSelectionMode = -1;
    }
    
    public void adaptSize()
    {
        yDim = nodeConstPanel.getBasePatterns().size() * 24;
        
        setMinimumSize(new Dimension(xDim, yDim));
        setPreferredSize(new Dimension(xDim, yDim));
        setSize(new Dimension(xDim, yDim));
    }
    
    public void recalculateCoordinates()
    {
        xCoord = new HashMap<TreeNodePattern, Integer>();
        yCoord = new HashMap<TreeNodePattern, Integer>();
        xDim = 30;
        // first define the base nodes
        List<TreeNodePattern> basePattern = nodeConstPanel.getBasePatterns();
        for (int i = 0; i < basePattern.size(); i++)
        {
            TreeNodePattern pat = basePattern.get(i);
            xCoord.put(pat, 3);
            yCoord.put(pat, i * 24 + 12);
        }
        // then calculate the other positions based on these
        calculateCoordinatesFor(nodeConstPanel.getRootPattern());
    }
    
    private void calculateCoordinatesFor(TreeNodePattern p)
    {
        if (xCoord.get(p) == null)
        {
            switch (p.getType())
            {
                case TreeNodePattern.NEGATION:
                {
                    calculateCoordinatesFor(p.getLeftArgument());
                    yCoord.put(p, yCoord.get(p.getLeftArgument()));
                    int newX = xCoord.get(p.getLeftArgument()) + 30;
                    xCoord.put(p, newX);
                    if (newX > xDim)
                    {
                        xDim = newX + 20;
                    }
                    break;
                }
                // this only concerns boolean connectives with two arguments
                // (since atomic pattern should already have received their
                // coordinates)
                default:
                {
                    TreeNodePattern left = p.getLeftArgument();
                    TreeNodePattern right = p.getRightArgument();
                    calculateCoordinatesFor(left);
                    calculateCoordinatesFor(right);
                    yCoord.put(p, (yCoord.get(left) + yCoord.get(right)) / 2);
                    int xmax = xCoord.get(left);
                    if (xCoord.get(right) > xmax)
                    {
                        xmax = xCoord.get(right);
                    }
                    xCoord.put(p, xmax + 30);
                    if (xmax + 30 > xDim)
                    {
                        xDim = xmax + 50;
                    }
                }
            }
        }
    }
    
    private int getXfor(TreeNodePattern pat)
    {
        return xCoord.get(pat);
    }
    
    private int getYfor(TreeNodePattern pat)
    {
        return yCoord.get(pat);
    }
    
    public TreeNodePattern getMarkedPattern()
    {
        return markedPattern;
    }
    
    public void setMarkedPattern(TreeNodePattern markedPattern)
    {
        this.markedPattern = markedPattern;
    }
    
    @Override
	public void paintComponent(Graphics canvas)
    {
        Graphics2D cnv = (Graphics2D) canvas;
        cnv.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        cnv.setColor(Color.WHITE);
        cnv.fillRect(0, 0, xDim, yDim);
        cnv.setColor(Color.BLACK);
        cnv.drawRect(0, 0, xDim - 1, yDim - 1);
        for (TreeNodePattern pat : xCoord.keySet())
        {
            cnv.setColor(Color.BLACK);
            if (markedPattern == pat)
            {
                cnv.setColor(Color.RED);
            }
            int x = xDim - getXfor(pat);
            int y = getYfor(pat);
            switch (pat.getType())
            {
                case (TreeNodePattern.NEGATION):
                {
                    cnv.drawLine(x - 4, y, x + 4, y);
                    cnv.drawLine(x + 4, y, x + 4, y + 4);
                    break;
                }
                case (TreeNodePattern.CONJUNCTION):
                {
                    cnv.drawLine(x + 4, y + 4, x, y - 4);
                    cnv.drawLine(x - 4, y + 4, x, y - 4);
                    break;
                }
                case (TreeNodePattern.DISJUNCTION):
                {
                    cnv.drawLine(x + 5, y - 5, x, y + 5);
                    cnv.drawLine(x - 5, y - 5, x, y + 5);
                    break;
                }
                case (TreeNodePattern.IMPLICATION):
                {
                    int leftY = yCoord.get(pat.getLeftArgument());
                    int rightY = yCoord.get(pat.getRightArgument());
                    if (leftY < rightY)
                    {
                        cnv.drawLine(x - 4, y, x + 4, y);
                        cnv.drawLine(x, y - 4, x + 4, y);
                        cnv.drawLine(x, y + 4, x + 4, y);
                    }
                    else
                    {
                        cnv.drawLine(x - 4, y, x + 4, y);
                        cnv.drawLine(x, y - 4, x - 4, y);
                        cnv.drawLine(x, y + 4, x - 4, y);
                    }
                    break;
                }
            }
            drawLinesToArguments(pat, cnv);
            cnv.drawRect(x - 8, y - 8, 16, 16);
        }
    }
    
    private void drawLinesToArguments(TreeNodePattern pat, Graphics cnv)
    {
        if (pat.getLeftArgument() != null)
        {
            int x = xDim - xCoord.get(pat);
            int y = yCoord.get(pat);
            int leftX = xDim - xCoord.get(pat.getLeftArgument());
            int leftY = yCoord.get(pat.getLeftArgument());
            if (pat.getRightArgument() == null)
            {
                cnv.drawLine(x + 8, y, leftX - 8, leftY);
            }
            else
            {
                int rightX = xDim - xCoord.get(pat.getRightArgument());
                int rightY = yCoord.get(pat.getRightArgument());
                if (rightY < leftY)
                {
                    cnv.drawLine(x, y - 8, x, rightY);
                    cnv.drawLine(x, rightY, rightX - 8, rightY);
                    cnv.drawLine(x, y + 8, x, leftY);
                    cnv.drawLine(x, leftY, leftX - 8, leftY);
                }
                else
                {
                    cnv.drawLine(x, y - 8, x, leftY);
                    cnv.drawLine(x, leftY, leftX - 8, leftY);
                    cnv.drawLine(x, y + 8, x, rightY);
                    cnv.drawLine(x, rightY, rightX - 8, rightY);
                }
            }
        }
    }
    
    private TreeNodePattern getPatternByCoords(int x, int y)
    {
        ArrayList<TreeNodePattern> searchAgenda = new ArrayList<TreeNodePattern>();
        searchAgenda.add(nodeConstPanel.getRootPattern());
        while (searchAgenda.size() > 0)
        {
            TreeNodePattern curPat = searchAgenda.remove(0);
            if (x > (xDim - xCoord.get(curPat)) + 8)
            {
                TreeNodePattern left = curPat.getLeftArgument();
                TreeNodePattern right = curPat.getRightArgument();
                if (left != null)
                    searchAgenda.add(left);
                if (right != null)
                    searchAgenda.add(right);
            }
            else
            {
                if (x > (xDim - xCoord.get(curPat)) - 8
                        && y < yCoord.get(curPat) + 8
                        && y > yCoord.get(curPat) - 8)
                {
                    return curPat;
                }
            }
        }
        return null;
    }
    
    private void switchType(TreeNodePattern pat)
    {
        switch (pat.getType())
        {
            case TreeNodePattern.CONJUNCTION:
            {
                pat.setType(TreeNodePattern.DISJUNCTION);
                nodeConstPanel.hint("click on the same connective again to switch its type");
                break;
            }
            case TreeNodePattern.DISJUNCTION:
            {
                pat.setType(TreeNodePattern.IMPLICATION);
                nodeConstPanel.hint("click on the same connective again to switch its type");
                break;
            }
            case TreeNodePattern.IMPLICATION:
            {
                int leftY = yCoord.get(pat.getLeftArgument());
                int rightY = yCoord.get(pat.getRightArgument());
                System.err.println("LeftY: " + leftY + " RightY: " + rightY);
                if (rightY > leftY)
                {
                    pat.switchArguments();
                }
                else
                {
                    pat.setType(TreeNodePattern.CONJUNCTION);
                    pat.switchArguments();
                }
                nodeConstPanel.hint("click on the same connective again to switch its type");
                break;
            }
        }
    }
    
    public void mouseClicked(MouseEvent arg0)
    {
        int x = arg0.getX();
        int y = arg0.getY();
        TreeNodePattern selectedPattern = getPatternByCoords(x, y);
        if (markedPattern != null && selectedPattern == markedPattern)
        {
            switchType(markedPattern);
            control.processEvent(new BreakpointEditorEvent(BreakpointEditorEvent.CHANGE_NODE_SELECTION_MODE,KahinaBreakpointEditorPanel.NO_PENDING_OPERATION));
            nodeConstPanel.hint("click on the same connective again to switch its type");
            repaint();
        }
        else
        {
            if (nodeSelectionMode == KahinaBreakpointEditorPanel.NO_PENDING_OPERATION)
            {
                markedPattern = selectedPattern;
                if (markedPattern != null)
                {
                    if (selectedPattern.getRightArgument() != null)
                    {
                        nodeConstPanel.hint("click on the same connective again to switch its type");
                    }
                    else
                    {
                        nodeConstPanel.hint("Select a boolean operation to introduce another connective.");
                    }
                }
                else
                {
                    nodeConstPanel.hint("Add or a remove a constraint, or select a connective.");
                }
                control.processEvent(new BreakpointEditorEvent(BreakpointEditorEvent.TREE_NODE_UPDATE,nodeConstPanel));
                repaint();
            }
            else
            {
                if (markedPattern != null)
                {
                    if (nodeSelectionMode == KahinaBreakpointEditorPanel.PENDING_AND_OPERATION)
                    {
                        if (selectedPattern != null)
                        {
                            if (nodeConstPanel.introduceConjunction(markedPattern, selectedPattern))
                            {
                                nodeConstPanel.hint("Select a boolean operation to introduce another connective.");
                            }
                            else
                            {
                                nodeConstPanel.hint("Inconsistency check prevented operation! Neither node must dominate the other!",Color.RED);
                            }           
                        }
                        else
                        {
                            nodeConstPanel.hint("Add or a remove a constraint, or select a connective.");
                        }
                        control.processEvent(new BreakpointEditorEvent(BreakpointEditorEvent.CHANGE_NODE_SELECTION_MODE, KahinaBreakpointEditorPanel.NO_PENDING_OPERATION));
                    }
                    else if (nodeSelectionMode == KahinaBreakpointEditorPanel.PENDING_OR_OPERATION)
                    {
                        if (selectedPattern != null)
                        {
                            if (nodeConstPanel.introduceDisjunction(markedPattern, selectedPattern))
                            {
                                nodeConstPanel.hint("Select a boolean operation to introduce another connective.");
                            }
                            else
                            {
                                nodeConstPanel.hint("Inconsistency check prevented operation! Neither node must dominate the other!",Color.RED);
                            }        
                        }
                        else
                        {
                            nodeConstPanel.hint("Add or a remove a constraint, or select a connective.");
                        }
                        control.processEvent(new BreakpointEditorEvent(BreakpointEditorEvent.CHANGE_NODE_SELECTION_MODE,KahinaBreakpointEditorPanel.NO_PENDING_OPERATION));
                    }
                    else if (nodeSelectionMode == KahinaBreakpointEditorPanel.PENDING_IMPL_OPERATION)
                    {
                        if (selectedPattern != null)
                        {
                            if (nodeConstPanel.introduceImplication(markedPattern, selectedPattern))
                            {
                                nodeConstPanel.hint("Select a boolean operation to introduce another connective.");
                            }
                            else
                            {
                                nodeConstPanel.hint("Inconsistency check prevented operation! Neither node must dominate the other!",Color.RED);
                            }                           
                        }
                        else
                        {
                            nodeConstPanel.hint("Add or a remove a constraint, or select a connective.");
                        }
                        control.processEvent(new BreakpointEditorEvent(BreakpointEditorEvent.CHANGE_NODE_SELECTION_MODE,KahinaBreakpointEditorPanel.NO_PENDING_OPERATION));
                    }
                    control.processEvent(new BreakpointEditorEvent(BreakpointEditorEvent.TREE_NODE_UPDATE,nodeConstPanel));
                    repaint();
                }
                else
                {
                    nodeConstPanel.hint("Cannot establish connectives across nodes!",Color.RED);
                    control.processEvent(new BreakpointEditorEvent(BreakpointEditorEvent.CHANGE_NODE_SELECTION_MODE,KahinaBreakpointEditorPanel.NO_PENDING_OPERATION));
                }
            }          
        }     
    }
    
    public void mouseEntered(MouseEvent arg0)
    {
    }
    
    public void mouseExited(MouseEvent arg0)
    {
    }
    
    public void mousePressed(MouseEvent arg0)
    {
    }
    
    public void mouseReleased(MouseEvent arg0)
    {
    }
    
    public void setNodeSelectionMode(int selectionMode)
    {
        nodeSelectionMode = selectionMode;
    }
    
    public void processEvent(KahinaEvent event)
    {
        if (event.getType().equals("breakpoint_editor"))
        {
            processEvent((BreakpointEditorEvent) event);
        }
    }
    
    public void processEvent(BreakpointEditorEvent event)
    {
        if (event.getEditorEventType() == BreakpointEditorEvent.CHANGE_NODE_SELECTION_MODE)
        {
            nodeSelectionMode = event.getGoalID();
        }
    }
    
    public void informControl(BreakpointEditorEvent e)
    {
        control.processEvent(e);
    }
}