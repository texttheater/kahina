package org.kahina.logic.sat.insertionmus.visual;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.kahina.core.KahinaInstance;
import org.kahina.core.visual.KahinaView;
import org.kahina.logic.sat.insertionmus.MUCState;
import org.kahina.logic.sat.insertionmus.MUCStep;
import org.kahina.logic.sat.insertionmus.algorithms.AbstractAlgorithm;
import org.kahina.logic.sat.insertionmus.algorithms.AlgorithmData;
import org.kahina.logic.sat.insertionmus.algorithms.BasicAlgorithm;
import org.kahina.logic.sat.insertionmus.algorithms.MaarenWieringa.FasterAdvancedAlgorithm;
import org.kahina.logic.sat.muc.data.UCReducerList;

public class UCReducerListViewPanel extends KahinaView<UCReducerList>
{
	

	final String[] algorithmTypes = {"Default",
			"Advanced learn all",
			"Advanced only learn last",
			"Binary search related algorithm"};
	
	JPanel newReducerPanel;
    private JComboBox algorithmChooser;
    private JComboBox heuristicsChooser;
    protected JButton start;
    
    protected ActionListener btStartListener = new ActionListener(){
		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (kahina != null){
				MUCState state = (MUCState) kahina.getState();
				MUCStep step = state.getSelectedStep();
				
				AbstractAlgorithm alg = step.getAlgorithm();
				AlgorithmData data = step.getData();
				
				while (!data.isMus){
					while(data.instanceIDs.size() > 1){
						alg.nextStep(data);
					}
					alg.nextStep(data);

					
					MUCStep newStep = new MUCStep(data, alg);
					state.newStep(newStep, step.getID());
//					data = newStep.getData();
					step.reset();
					step = newStep;

				}
				System.out.println("Found a MUS");
			}
		}};
		
	protected ActionListener cbAlgorithmListener = new ActionListener(){
		@Override
		public void actionPerformed(ActionEvent arg0) {
			AbstractAlgorithm alg;

			if (algorithmTypes[0].equals(algorithmChooser.getSelectedItem())){
				alg = new BasicAlgorithm();
			}else if (algorithmTypes[1].equals(algorithmChooser.getSelectedItem())){
				alg = new FasterAdvancedAlgorithm();
			}else{
				alg = new BasicAlgorithm();
			}
//			kahina.getState()
			MUCState state = (MUCState) kahina.getState();
			MUCStep ucStep = state.getSelectedStep();
			ucStep.setAlgorithm(alg);
		}
	};
    
    public UCReducerListViewPanel(KahinaInstance<MUCState, ?, ?, ?> kahina) {
		super(kahina);
		// TODO Auto-generated constructor stub
		this.newReducerPanel = new JPanel();
		newReducerPanel.setLayout(new FlowLayout());
		algorithmChooser = new JComboBox();
		heuristicsChooser = new JComboBox();
		start = new JButton("Start");
		start.addActionListener(btStartListener);
		
		
		for (String str: algorithmTypes){
			algorithmChooser.addItem(str);
		}
		algorithmChooser.addActionListener(this.cbAlgorithmListener);
//		algorithmChooser.addItem("Default");
//		algorithmChooser.addItem("Advanced only learn last");
//		algorithmChooser.addItem("Advanced learn all");
//		algorithmChooser.addItem("Binary search related algorithm");
//		

		heuristicsChooser.addItem("Ascending");
		
		newReducerPanel.add(algorithmChooser);
		newReducerPanel.add(heuristicsChooser);
		newReducerPanel.add(start);
		
		
	}


	@Override
	public JComponent makePanel() {
		// TODO Auto-generated method stub
		return newReducerPanel;
	}
}
