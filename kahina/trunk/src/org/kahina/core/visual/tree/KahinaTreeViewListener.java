package org.kahina.core.visual.tree;

import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.kahina.core.KahinaRunner;
import org.kahina.core.data.tree.KahinaTree;
import org.kahina.core.gui.event.KahinaSelectionEvent;

public class KahinaTreeViewListener extends MouseAdapter implements ActionListener
{
    KahinaTreeViewPanel view;
    KahinaTreeViewMarker marker;
    MouseEvent lastMouseEvent;
    
    public KahinaTreeViewListener(KahinaTreeViewPanel view)
    {
        this.view = view;
        this.marker = new KahinaTreeViewMarker(view.view.getTreeModel());
        marker.registerTreeView(view);
        this.lastMouseEvent = null;
    }
    
    public KahinaTreeViewListener(KahinaTreeViewPanel view, KahinaTreeViewMarker marker)
    {
        this.view = view;
        this.marker = marker;
        marker.registerTreeView(view);
        this.lastMouseEvent = null;
    }
    
    @Override
	public void mouseClicked(MouseEvent e)
    {
        int clickedNode = view.view.nodeAtCoordinates(e.getX(), e.getY());
        if (lastMouseEvent != null && e.getWhen() - lastMouseEvent.getWhen() < 500)
        {
            if (view.view.getCollapsePolicy() == KahinaTreeView.COLLAPSE_SECONDARY)
            {
                view.view.secondaryTreeModel.toggleCollapse(clickedNode);
            }
            else if (view.view.getCollapsePolicy() == KahinaTreeView.COLLAPSE_PRIMARY)
            {
                view.view.getModel().toggleCollapse(clickedNode);
            }
            view.view.recalculate();
            view.updateDisplay();
            view.repaint();
        }
        else
        {
            KahinaRunner.processEvent(new KahinaSelectionEvent(clickedNode));
            lastMouseEvent = e;
        }
    }
    
    @Override
	public void mousePressed(MouseEvent e) 
    {
        maybeShowPopup(e);
    }

    @Override
	public void mouseReleased(MouseEvent e) 
    {
        maybeShowPopup(e);
    }

    private void maybeShowPopup(MouseEvent e) 
    {
        if (e.isPopupTrigger()) 
        {
            KahinaTreeViewContextMenu.getMenu(this, view.view).show(e.getComponent(),e.getX(), e.getY());
        }
    }
    
    public void actionPerformed(ActionEvent e)
    {
        String command = e.getActionCommand();
        if (command.equals("Zoom In"))
        {
            view.view.zoomIn();
            view.view.recalculate();
        }
        else if (command.equals("Zoom Out"))
        {
            view.view.zoomOut();
            view.view.recalculate();
        }
        else if (command.equals("Bottom Up"))
        {
            view.view.setDisplayOrientation(KahinaTreeView.BOTTOM_UP_DISPLAY);
            view.view.recalculate();
        }
        else if (command.equals("Top Down"))
        {
            view.view.setDisplayOrientation(KahinaTreeView.TOP_DOWN_DISPLAY);
            view.view.recalculate();
        }
        else if (command.equals("Increase vertical distance"))
        {
            view.view.increaseVerticalDistance();
            view.view.recalculate();
        }
        else if (command.equals("Decrease vertical distance"))
        {
            view.view.decreaseVerticalDistance();
            view.view.recalculate();
        }
        else if (command.equals("Increase horizontal distance"))
        {
            view.view.increaseHorizontalDistance();
            view.view.recalculate();
        }
        else if (command.equals("Decrease horizontal distance"))
        {
            view.view.decreaseHorizontalDistance();
            view.view.recalculate();
        }
        else if (command.equals("No special treatment"))
        {
            view.view.setTerminalsPolicy(KahinaTreeView.NO_SPECIAL_TREATMENT);
            view.view.recalculate();
        }
        else if (command.equals("On extra level"))
        {
            view.view.setTerminalsPolicy(KahinaTreeView.ON_EXTRA_LEVEL);
            view.view.recalculate();
        }
        else if (command.equals("Graphically separated"))
        {
            view.view.setTerminalsPolicy(KahinaTreeView.GRAPHICALLY_SEPARATED);
            view.view.recalculate();
        }
        else if (command.equals("Always"))
        {
            view.view.setNodeDisplayPolicy(KahinaTreeView.ALWAYS);
            view.view.recalculate();
        }
        else if (command.equals("Status decides, default: YES"))
        {
            view.view.setNodeDisplayPolicy(KahinaTreeView.STATUS_DEFAULT_YES);
            view.view.recalculate();
        }
        else if (command.equals("Status decides, default: NO"))
        {
            view.view.setNodeDisplayPolicy(KahinaTreeView.STATUS_DEFAULT_NO);
            view.view.recalculate();
        }
        else if (command.equals("Never"))
        {
            view.view.setNodeDisplayPolicy(KahinaTreeView.NEVER);
            view.view.recalculate();
        }
        else if (command.equals("External conditions"))
        {
            view.view.setNodeDisplayPolicy(KahinaTreeView.CONDITIONALLY);
            view.view.recalculate();
        }
        else if (command.equals("No collapsing"))
        {
            view.view.setCollapsePolicy(KahinaTreeView.NO_COLLAPSING);
            view.view.recalculate();
        }
        else if (command.equals("Collapse primary dimension"))
        {
            view.view.setCollapsePolicy(KahinaTreeView.COLLAPSE_PRIMARY);
            view.view.recalculate();
        }
        else if (command.equals("Collapse secondary dimension"))
        {
            view.view.setCollapsePolicy(KahinaTreeView.COLLAPSE_SECONDARY);
            view.view.recalculate();
        }
        else if (command.equals("Box nodes"))
        {
            view.view.setNodeShapePolicy(KahinaTreeView.BOX_SHAPE);
        }
        else if (command.equals("Oval nodes"))
        {
            view.view.setNodeShapePolicy(KahinaTreeView.OVAL_SHAPE);
        }
        else if (command.equals("Boxed edge labels"))
        {
            view.view.setEdgeShapePolicy(KahinaTreeView.BOX_SHAPE);
        }
        else if (command.equals("Oval edge labels"))
        {
            view.view.setEdgeShapePolicy(KahinaTreeView.OVAL_SHAPE);
        }
        else if (command.equals("Direct"))
        {
            view.view.setLineShapePolicy(KahinaTreeView.STRAIGHT_LINES);
        }
        else if (command.equals("Edgy"))
        {
            view.view.setLineShapePolicy(KahinaTreeView.EDGY_LINES);
        }
        else if (command.equals("Invisible"))
        {
            view.view.setLineShapePolicy(KahinaTreeView.INVISIBLE_LINES);
        }
        else if (command.equals("Secondary direct"))
        {
            view.view.setSecondaryLineShapePolicy(KahinaTreeView.STRAIGHT_LINES);
        }
        else if (command.equals("Secondary edgy"))
        {
            view.view.setSecondaryLineShapePolicy(KahinaTreeView.EDGY_LINES);
        }
        else if (command.equals("Secondary invisible"))
        {
            view.view.setSecondaryLineShapePolicy(KahinaTreeView.INVISIBLE_LINES);
        }
        else if (command.equals("Centered"))
        {
            view.view.setNodePositionPolicy(KahinaTreeView.CENTERED_NODES);
            view.view.recalculate();
        }
        else if (command.equals("Left alignment"))
        {
            view.view.setNodePositionPolicy(KahinaTreeView.LEFT_ALIGNED_NODES);
            view.view.recalculate();
        }
        else if (command.equals("Right alignment"))
        {
            view.view.setNodePositionPolicy(KahinaTreeView.RIGHT_ALIGNED_NODES);
            view.view.recalculate();
        }
        else if (command.equals("Antialiasing On"))
        {
            view.view.setAntialiasingPolicy(KahinaTreeView.ANTIALIASING);
        }
        else if (command.equals("Antialiasing Off"))
        {
            view.view.setAntialiasingPolicy(KahinaTreeView.NO_ANTIALIASING);
        }
        else if (command.endsWith("0 %"))
        {
            int zoomLevel = Integer.parseInt(command.substring(0, command.length() - 3));
            view.view.setZoomLevel(zoomLevel);
            view.view.recalculate();
        }
        else if (command.endsWith(" vertical distance"))
        {
            int vertDist = Integer.parseInt(command.substring(0, command.length() - 18));
            view.view.setVerticalDistance(vertDist);
            view.view.recalculate();
        }
        else if (command.endsWith(" horizontal distance"))
        {
            int horiDist = Integer.parseInt(command.substring(0, command.length() - 20));
            view.view.setHorizontalDistance(horiDist);
            view.view.recalculate();
        }
        else if (command.equals("Display second dimension"))
        {
            view.view.toggleSecondDimensionDisplay();
            view.view.recalculate();
        }
        else if (command.equals("Swap dimensions"))
        {
            view.view.swapDimensions();
            view.view.recalculate();
        }
        else if (command.equals("Save as PNG"))
        {
            JFileChooser chooser = new JFileChooser(new File("."));
            //FileNameExtensionFilter filter = new FileNameExtensionFilter("PNG Images", "png");
            //chooser.setFileFilter(filter);
            chooser.showSaveDialog(view);
            File outputFile = chooser.getSelectedFile();

            Graphics outputCanvas = view.image.getGraphics();
            view.paint(outputCanvas);
            try
            {
                ImageIO.write(view.image,"png",outputFile);
            }
            catch (IOException ioe)
            {
                JOptionPane.showMessageDialog(view, ioe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        view.updateDisplay();
        view.repaint();
    }
}
