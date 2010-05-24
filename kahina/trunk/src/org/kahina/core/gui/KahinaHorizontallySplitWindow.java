package org.kahina.core.gui;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.border.TitledBorder;

public class KahinaHorizontallySplitWindow extends KahinaWindow
{
    KahinaWindow leftWindow;
    KahinaWindow rightWindow;
    
    JPanel leftPanel;
    JPanel rightPanel;
    
    public KahinaHorizontallySplitWindow()
    {
        leftPanel = new JPanel();
        leftPanel.setBorder(BorderFactory.createTitledBorder("Drag window 1 here!"));
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        rightPanel = new JPanel();
        rightPanel.setBorder(BorderFactory.createTitledBorder("Drag window 2 here!"));
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        getContentPane().add(new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel));
    }
    
    public void setLeftWindow(KahinaWindow w)
    {
        ((TitledBorder) leftPanel.getBorder()).setTitle(w.getTitle());
        leftPanel.add(w.getContentPane());
    }
    
    public void setRightWindow(KahinaWindow w)
    {
        ((TitledBorder) rightPanel.getBorder()).setTitle(w.getTitle());
        rightPanel.add(w.getContentPane());
    }
}
