package org.kahina.logic.sat.visual.cnf.list;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.ListModel;

import org.kahina.core.KahinaInstance;
import org.kahina.core.visual.KahinaView;
import org.kahina.logic.sat.data.cnf.CnfSatInstance;

public class KahinaSatInstanceListView extends KahinaView<CnfSatInstance>
{
    protected DefaultListModel listModel;
    
    // mapping from status values to display properties
    HashMap<Integer, Color> statusColorEncoding;
    
    public KahinaSatInstanceListView(KahinaInstance<?, ?, ?, ?> kahina)
    {
        super(kahina);
        listModel = new DefaultListModel();
        statusColorEncoding = new HashMap<Integer, Color>();
    }
    
    public void doDisplay()
    {
        listModel.clear();
        recalculate();
    }
    
    public void setStatusColorEncoding(int status, Color color)
    {
        statusColorEncoding.put(status, color);
    }
    
    public int getLineStatus(int lineID)
    {
        return 0;
    }
    
    public Color getLineColor(int lineID)
    {
        int status = getLineStatus(lineID);
        Color col = statusColorEncoding.get(status);
        if (col == null)
        {
            return Color.BLACK;
        } 
        else
        {
            return col;
        }
    }
    
    public ListModel getListModel()
    {
        return listModel;
    }

    @Override
    public JComponent makePanel()
    {
        KahinaSatInstanceListViewPanel panel = new KahinaSatInstanceListViewPanel(kahina);
        kahina.registerInstanceListener("redraw", panel);
        panel.setView(this);
        return panel;
    }

    public void displayText(String string)
    {
        listModel = new DefaultListModel();
        listModel.addElement(string);
    }
    
    public void recalculate()
    {
        List<List<Integer>> clauses = model.getClauses();
        listModel.clear();
        for (int i = 0; i < clauses.size(); i++)
        {
            StringBuilder s = new StringBuilder();
            s.append(i + 1);
            s.append(": {");
            for (Integer literal : clauses.get(i))
            {
                s.append(literal);
                s.append(',');
            }
            s.deleteCharAt(s.length() - 1);
            s.append('}');
            listModel.addElement(s.toString());
        }
        //this was the efficient way, before clauses could be modified!
        /*List<List<Integer>> clauses = model.getClauses();
        if (clauses.size() > listModel.getSize())
        {
            for (int i = listModel.getSize(); i < clauses.size(); i++)
            {
                StringBuilder s = new StringBuilder();
                s.append(i + 1);
                s.append(": {");
                for (Integer literal : clauses.get(i))
                {
                    s.append(literal);
                    s.append(',');
                }
                s.deleteCharAt(s.length() - 1);
                s.append('}');
                listModel.addElement(s.toString());
            }
        }*/
    }
}
