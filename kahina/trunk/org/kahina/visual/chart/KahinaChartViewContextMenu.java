package org.kahina.visual.chart;

import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

public class KahinaChartViewContextMenu extends JPopupMenu
{
    KahinaChartView v;
    
    public KahinaChartViewContextMenu(ActionListener l, KahinaChartView v)
    {
        super();
        this.v = v;
        JMenu zoomSubmenu = new JMenu("Zoom");
        JMenuItem zoomInItem = new JMenuItem("Zoom In");
        zoomInItem.addActionListener(l);
        zoomSubmenu.add(zoomInItem);
        JMenuItem zoomOutItem = new JMenuItem("Zoom Out");
        zoomOutItem.addActionListener(l);
        zoomSubmenu.add(zoomOutItem);
        zoomSubmenu.addSeparator();
        ButtonGroup sizeGroup = new ButtonGroup();
        //size buttons visible depend on current size
        for (int i = 4; i <= 20; i += 1)
        {
            JRadioButtonMenuItem sizeItem = new JRadioButtonMenuItem(i + "0 %");
            if (i == v.getZoomLevel()) sizeItem.setSelected(true);
            sizeItem.addActionListener(l);
            sizeGroup.add(sizeItem);
            zoomSubmenu.add(sizeItem);
        }
        add(zoomSubmenu);
        
        addSeparator();
        
        JMenu cellWidthPolicySubmenu = new JMenu("Cell width policy");
        ButtonGroup cellWidthPolicyGroup = new ButtonGroup();
        JRadioButtonMenuItem minimalNecessaryWidthItem = new JRadioButtonMenuItem("Minimal necessary width");
        minimalNecessaryWidthItem.addActionListener(l);
        cellWidthPolicyGroup.add(minimalNecessaryWidthItem);
        cellWidthPolicySubmenu.add(minimalNecessaryWidthItem);
        JRadioButtonMenuItem maximalNecessaryWidthItem = new JRadioButtonMenuItem("Maximal necessary width");
        maximalNecessaryWidthItem.addActionListener(l);
        cellWidthPolicyGroup.add(maximalNecessaryWidthItem);
        cellWidthPolicySubmenu.add(maximalNecessaryWidthItem);
        JRadioButtonMenuItem fixedWidthItem = new JRadioButtonMenuItem("Fixed width");
        fixedWidthItem.addActionListener(l);
        cellWidthPolicyGroup.add(fixedWidthItem);
        cellWidthPolicySubmenu.add(fixedWidthItem);
        switch (v.getCellWidthPolicy())
        {
            case 0:
            {
                fixedWidthItem.setSelected(true); break;
            }
            case 1:
            {
                minimalNecessaryWidthItem.setSelected(true); break;
            }
            case 2:
            {
                maximalNecessaryWidthItem.setSelected(true);
            }
        }
        add(cellWidthPolicySubmenu);
        
        JMenu edgeStackingPolicySubmenu = new JMenu("Edge stacking policy");
        ButtonGroup edgeStackingPolicyGroup = new ButtonGroup();
        JRadioButtonMenuItem fillSpaceCompactlyItem = new JRadioButtonMenuItem("Fill space compactly");
        fillSpaceCompactlyItem.addActionListener(l);
        edgeStackingPolicyGroup.add(fillSpaceCompactlyItem);
        edgeStackingPolicySubmenu.add(fillSpaceCompactlyItem);
        JRadioButtonMenuItem maintainChronologicalOrderItem = new JRadioButtonMenuItem("Maintain chronological order");
        maintainChronologicalOrderItem.addActionListener(l);
        edgeStackingPolicyGroup.add(maintainChronologicalOrderItem);
        edgeStackingPolicySubmenu.add(maintainChronologicalOrderItem);
        switch (v.getEdgeStackingPolicy())
        {
            case 0:
            {
                fillSpaceCompactlyItem.setSelected(true); break;
            }
            case 1:
            {
                maintainChronologicalOrderItem.setSelected(true);
            }
        }
        add(edgeStackingPolicySubmenu);
        
        JMenu displayOrientationSubmenu = new JMenu("Display orientation");
        ButtonGroup displayOrientationGroup = new ButtonGroup();
        JRadioButtonMenuItem bottomUpItem = new JRadioButtonMenuItem("Bottom Up");
        bottomUpItem.addActionListener(l);
        displayOrientationGroup.add(bottomUpItem);
        displayOrientationSubmenu.add(bottomUpItem);
        JRadioButtonMenuItem topDownItem = new JRadioButtonMenuItem("Top Down");
        topDownItem.addActionListener(l);
        displayOrientationGroup.add(topDownItem);
        displayOrientationSubmenu.add(topDownItem);
        switch (v.getDisplayOrientation())
        {
            case 0:
            {
                bottomUpItem.setSelected(true); break;
            }
            case 1:
            {
                topDownItem.setSelected(true);
            }
        }
        add(displayOrientationSubmenu);
        
        JMenu displayRangePolicySubmenu = new JMenu("Display range policy");
        ButtonGroup displayRangePolicyGroup = new ButtonGroup();
        JRadioButtonMenuItem usedRangeOnlyItem = new JRadioButtonMenuItem("Used range only");
        usedRangeOnlyItem.addActionListener(l);
        displayRangePolicyGroup.add(usedRangeOnlyItem);
        displayRangePolicySubmenu.add(usedRangeOnlyItem);
        JRadioButtonMenuItem usedOrDefinedRangeItem = new JRadioButtonMenuItem("Defined range");
        usedOrDefinedRangeItem.addActionListener(l);
        displayRangePolicyGroup.add(usedOrDefinedRangeItem);
        displayRangePolicySubmenu.add(usedOrDefinedRangeItem);
        JRadioButtonMenuItem completeRangeItem = new JRadioButtonMenuItem("Complete range");
        completeRangeItem.addActionListener(l);
        displayRangePolicyGroup.add(completeRangeItem);
        displayRangePolicySubmenu.add(completeRangeItem);
        switch (v.getDisplayRangePolicy())
        {
            case 0:
            {
                usedOrDefinedRangeItem.setSelected(true); break;
            }
            case 1:
            {
                usedRangeOnlyItem.setSelected(true); break;
            }
            case 2:
            {
               completeRangeItem.setSelected(true);
            }
        }
        add(displayRangePolicySubmenu);
        
        JMenu antialiasingSubmenu = new JMenu("Antialiasing");
        ButtonGroup antialiasingGroup = new ButtonGroup();
        JRadioButtonMenuItem antialiasingOnItem = new JRadioButtonMenuItem("On");
        antialiasingOnItem.setActionCommand("Antialiasing On");
        antialiasingOnItem.addActionListener(l);
        antialiasingGroup.add(antialiasingOnItem);
        antialiasingSubmenu.add(antialiasingOnItem);
        JRadioButtonMenuItem antialiasingOffItem = new JRadioButtonMenuItem("Off");
        antialiasingOffItem.setActionCommand("Antialiasing Off");
        antialiasingOffItem.addActionListener(l);
        antialiasingGroup.add(antialiasingOffItem);
        antialiasingSubmenu.add(antialiasingOffItem);
        switch (v.getAntialiasingPolicy())
        {
            case 0:
            {
                antialiasingOnItem.setSelected(true); break;
            }
            case 1:
            {
                antialiasingOffItem.setSelected(true);
            }
        }
        add(antialiasingSubmenu);
        
        addSeparator();
        
        JMenuItem exportPNGItem = new JMenuItem("Save as PNG");
        exportPNGItem.addActionListener(l);
        add(exportPNGItem);
    }
    
    public static JPopupMenu getMenu(ActionListener l, KahinaChartView v)
    {
        return new KahinaChartViewContextMenu(l, v);
    }
}
