package org.kahina.tralesld.visual.fs;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.kahina.core.visual.KahinaViewPanel;

public class TraleSLDFeatureStructureViewPanel extends KahinaViewPanel<TraleSLDFeatureStructureView>
{
	private static final long serialVersionUID = -8507986910087886388L;

	private JPanel innerPanel;

	private VisualizationUtility util;

	private static final boolean verbose = true;

	public TraleSLDFeatureStructureViewPanel()
	{
		util = VisualizationUtility.getDefault();
		innerPanel = new JPanel();
		innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		JScrollPane scrollPane = new JScrollPane(innerPanel);
		add(scrollPane);
	}

	@Override
	public void updateDisplay()
	{
		if (verbose)
		{
			System.err.println("TraleSLDFeatureStructureViewPanel.updateDisplay()");
		}
		innerPanel.removeAll();
		String grisuMessage;
		if (view == null || (grisuMessage = view.getGrisuMessage()) == null)
		{
			innerPanel.add(new JLabel("No feature structures (yet) at this port."));
		} else
		{
			innerPanel.add(util.visualize(grisuMessage));
			// TODO should use the asynchronous method instead, but currently
			// leads to weird behavior - some updates then happen only on the
			// second click
			// util.visualize(grisuMessage, innerPanel);
		}
		innerPanel.repaint();
	}

}
