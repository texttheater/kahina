package org.kahina.logic.sat.visual.cnf.list;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseListener;

import javax.swing.JList;
import javax.swing.JScrollPane;

import org.kahina.core.KahinaInstance;
import org.kahina.core.visual.KahinaViewPanel;
import org.kahina.core.visual.text.KahinaTextViewListener;

public class KahinaSatInstanceListViewPanel extends KahinaViewPanel<KahinaSatInstanceListView>
{
    private JList list;
    JScrollPane listScrollPane;
    
    public KahinaSatInstanceListViewPanel()
    {
        this.setLayout(new GridLayout());
        view = null;
        list = new JList();
        list.setSelectionBackground(Color.YELLOW);
        list.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        list.setFixedCellHeight(16);
        list.setCellRenderer(new KahinaSatInstanceListCellRenderer(this));
        
        listScrollPane = new JScrollPane(getList());
        this.add(listScrollPane);          
    }
    
    public void setView(KahinaSatInstanceListView view)
    {
        this.view = view;
        getList().setModel(view.getListModel());
        //list.setSelectionModel(view.getSelectionModel());
        /*for (MouseListener mouseListener : list.getMouseListeners())
        {
            list.removeMouseListener(mouseListener);
        }
        list.addMouseListener(new KahinaTextViewListener(this, kahina));*/
        this.updateDisplayAndRepaintFromEventDispatchThread();
    }
    
    @Override
    public void updateDisplay()
    {
        //System.err.println(this + ".updateDisplay()");
        revalidate();
        repaint();
    }

    public JList getList()
    {
        return list;
    }
}
