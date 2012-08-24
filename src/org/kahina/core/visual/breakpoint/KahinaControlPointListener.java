package org.kahina.core.visual.breakpoint;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.kahina.core.data.breakpoint.KahinaControlPoint;
import org.kahina.core.edit.breakpoint.BreakpointEditorEvent;
import org.kahina.core.io.util.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class KahinaControlPointListener implements ActionListener, KeyListener
{
    KahinaControlPointViewPanel viewPanel;
    
    public KahinaControlPointListener()
    {
        this.viewPanel = null;
    }
    
    public void setViewPanel(KahinaControlPointViewPanel viewPanel)
    {
        this.viewPanel = viewPanel;
    }
    
    public void actionPerformed(ActionEvent e)
    {
        String s = e.getActionCommand();
        if (s.equals("changeColor"))
        {
            Color newColor = JColorChooser.showDialog(viewPanel,"Choose Background Color",viewPanel.view.getModel().getSignalColor());
            //viewPanel.colorButton.setBackground(newColor);
            viewPanel.view.getModel().setSignalColor(newColor);
        }
        else if (s.equals("rename"))
        {
            String newName = JOptionPane.showInputDialog(viewPanel,
                    "Enter a new name for the control point.",
                    "Control Point Editor",
                    JOptionPane.PLAIN_MESSAGE);
            if (newName != null)
            {
                KahinaControlPoint point = viewPanel.view.getModel();
                point.setName(newName);
                viewPanel.processNameChange();
            }
        }
        else if (s.equals("suggestName"))
        {
            KahinaControlPoint point = viewPanel.view.getModel();
            point.setName(point.getSensor().getStepProperty().toString());
            viewPanel.processNameChange();
        }
        else if (s.equals("toggleActivation"))
        {
            KahinaControlPoint point = viewPanel.view.getModel();
            if (point.isActive())
            {
                point.deactivate();
            }
            else
            {
                point.activate();
            }
            viewPanel.adaptActivationButtonLabel();
        } 
        else if (s.equals("exportControlPoint"))
        {
            JFileChooser chooser = new JFileChooser(new File("."));
            chooser.setDialogTitle("Export control point");
            chooser.showSaveDialog(viewPanel);
            File outputFile = chooser.getSelectedFile();
            if (outputFile != null)
            {
                Document dom = XMLUtil.newEmptyDocument();
                Element pointElement = viewPanel.view.getModel().exportXML(dom);
                XMLUtil.writeXML(pointElement, outputFile.getAbsolutePath());
            }
        }     
    }
    
    public void keyPressed(KeyEvent e) 
    {
    }

    public void keyReleased(KeyEvent e) 
    {
        String val = viewPanel.nameEditLine.getText();
        viewPanel.view.getModel().setName(val);
        viewPanel.processNameChange();
    }

    public void keyTyped(KeyEvent e) 
    {

    }
}
