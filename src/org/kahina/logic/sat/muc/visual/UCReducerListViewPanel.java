package org.kahina.logic.sat.muc.visual;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneLayout;

import org.kahina.core.data.dag.ColoredPathDAG;
import org.kahina.core.gui.event.KahinaRedrawEvent;
import org.kahina.core.io.color.ColorUtil;
import org.kahina.core.visual.KahinaViewPanel;
import org.kahina.logic.sat.muc.MUCInstance;
import org.kahina.logic.sat.muc.gui.WrapLayout;
import org.kahina.logic.sat.muc.heuristics.UCReductionHeuristics;
import org.kahina.logic.sat.muc.task.UCReducer;

public class UCReducerListViewPanel extends KahinaViewPanel<UCReducerListView> implements ActionListener
{
    JPanel newReducerPanel;
    private JComboBox heuristicsChooser;
    private JLabel colorLabel;
    private JButton signalColor;
    
    JPanel runningReducersPanel;
    
    MUCInstance kahina;
    
    public UCReducerListViewPanel(MUCInstance kahina, Map<String,Class<? extends UCReductionHeuristics>> heuristics)
    {
        this.kahina = kahina;
        
        this.setLayout(new BoxLayout(this,BoxLayout.LINE_AXIS));
        
        JPanel leftPanel = new JPanel();
        
        newReducerPanel = new JPanel();
        newReducerPanel.setBorder(BorderFactory.createTitledBorder("Start a new reducer at the selected node"));
        
        JLabel heuristicsLabel = new JLabel("Basic heuristics: ");
        newReducerPanel.add(heuristicsLabel);
        
        heuristicsChooser = new JComboBox();
        heuristicsChooser.setActionCommand("chooseHeuristics");
        heuristicsChooser.addActionListener(this);
        heuristicsChooser.setMaximumSize(new Dimension(300,30));
        for (String name : heuristics.keySet())
        {
            heuristicsChooser.addItem(name);
        }
        newReducerPanel.add(heuristicsChooser);
        
        colorLabel = new JLabel("Signal color: ");
        newReducerPanel.add(colorLabel);
        newReducerPanel.add(Box.createRigidArea(new Dimension(5,0)));
        
        signalColor = new JButton("Change");
        signalColor.setBackground(Color.RED);
        signalColor.setActionCommand("changeColor");
        signalColor.addActionListener(this);
        newReducerPanel.add(signalColor);
        newReducerPanel.add(Box.createRigidArea(new Dimension(5,0)));
        
        JButton startReducerButton = new JButton("Start");
        startReducerButton.setActionCommand("startReducer");
        startReducerButton.addActionListener(this);
        newReducerPanel.add(startReducerButton);
        
        GroupLayout layout = new GroupLayout(newReducerPanel);
        newReducerPanel.setLayout(layout);
        
        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(heuristicsLabel)
                    .addComponent(colorLabel)
                    .addComponent(startReducerButton))
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(heuristicsChooser)
                    .addComponent(signalColor)));
        
        layout.setVerticalGroup(
            layout.createSequentialGroup()
               .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                   .addComponent(heuristicsLabel)
                   .addComponent(heuristicsChooser))
               .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                   .addComponent(colorLabel)
                   .addComponent(signalColor))
               .addComponent(startReducerButton));     
        
        leftPanel.add(newReducerPanel);
        
        /*JButton shortestPathButton = new JButton("Shortest Path");
        shortestPathButton.setActionCommand("shortestPath");
        shortestPathButton.addActionListener(this);
        leftPanel.add(shortestPathButton);*/
        
        this.add(leftPanel);
        
        runningReducersPanel = new JPanel();
        runningReducersPanel.setLayout(new WrapLayout());
        runningReducersPanel.setBorder(BorderFactory.createTitledBorder("Reducer info"));
        JScrollPane scrollPane = new JScrollPane(runningReducersPanel);
        scrollPane.setLayout(new ScrollPaneLayout());
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        this.add(scrollPane);
    }

    @Override
    public void updateDisplay()
    {
        //System.err.println("UCReducerListViewPanel.updateDisplay()");
        kahina.getLogger().startMeasuring();
        runningReducersPanel.removeAll();
        //System.err.println(view.getModel());
        int size = view.getModel().size();
        if (size == 0)
        {
            runningReducersPanel.add(new JLabel("No candidate choices!"));
        }
        else
        {
            for (UCReducer reducer : view.getModel())
            {
                UCReducerPanel panel = new UCReducerPanel(this.view.kahina);
                reducer.setPanel(panel);
                runningReducersPanel.add(panel);
            }
        }
        runningReducersPanel.revalidate();
        kahina.getLogger().endMeasuring("for updating UCReducerListViewPanel");
    }

    @Override
    public void actionPerformed(ActionEvent event)
    {
        String s = event.getActionCommand();
        if (s.equals("changeColor"))
        {
            Color newColor = JColorChooser.showDialog(this,"Choose Background Color",signalColor.getBackground());
            signalColor.setBackground(newColor);
            view.newReducer.setSignalColor(newColor);
        }   
        else if (s.equals("startReducer"))
        {
            System.err.println("UCReducerListViewPanel.startReducer");
            UCReducer newReducer = new UCReducer(kahina.getState(), kahina.getState().getSelectedStepID(), kahina.getState().getFiles());
            try
            {
                newReducer.setHeuristics(view.heuristics.get(heuristicsChooser.getSelectedItem()).newInstance());
                newReducer.setSignalColor(signalColor.getBackground());
                signalColor.setBackground(ColorUtil.randomColor());
                view.getModel().add(newReducer);
                newReducer.start();
            }
            catch (IllegalAccessException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (InstantiationException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            updateDisplay();
            kahina.dispatchEvent(new KahinaRedrawEvent());
        }
        else if (s.equals("chooseHeuristics"))
        {
           //at the moment, the changed heuristic does not become relevant before a new reducer is constructed
           //nothing needs to be done in this scenario, because the selected heuristics are stored in the chooser
        }
        else if (s.equals("shortestPath"))
        {
           ColoredPathDAG dag = kahina.getState().getDecisionGraph();
           List<Integer> shortestPath = dag.findShortestPathFromRoot(kahina.getState().getSelectedStepID());
           System.err.println("Shortest path: " + shortestPath);
           
           UCReducer newReducer = new UCReducer(kahina.getState(), shortestPath.get(0), kahina.getState().getFiles());
           newReducer.setSignalColor(signalColor.getBackground());
           signalColor.setBackground(ColorUtil.randomColor());
           view.getModel().add(newReducer);
           
           updateDisplay();
           
           newReducer.getPath().getPath().clear();
           newReducer.getPath().getPath().addAll(shortestPath);
           newReducer.cancelTasks();
           newReducer.getPanel().displayStatus1("Shortest path to node " + kahina.getState().getSelectedStepID() + " (of length " + shortestPath.size() + ")");
           newReducer.getPanel().displayStatus2("");
           newReducer.getPanel().requestViewUpdate();
        }
    }
}