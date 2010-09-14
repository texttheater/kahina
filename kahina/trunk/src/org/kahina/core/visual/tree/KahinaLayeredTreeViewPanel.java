package org.kahina.core.visual.tree;

import java.awt.Color;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import org.kahina.core.visual.KahinaViewPanel;

public class KahinaLayeredTreeViewPanel extends KahinaViewPanel<KahinaLayeredTreeView>
{
	private static final long serialVersionUID = -1304882362038211887L;

	private static final boolean VERBOSE = false;

	private KahinaTreeViewPanel[] panels;

	public KahinaLayeredTreeViewPanel(int layers, KahinaTreeViewMarker marker)
	{
		if (VERBOSE)
		{
			System.err.println("new KahinaLayeredTreeViewPanel(" + layers + ", " + marker + ")");
		}
		panels = new KahinaTreeViewPanel[layers];
		for (int i = 0; i < panels.length; i++)
		{
			panels[i] = new KahinaTreeViewPanel(marker);
		}
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		if (layers > 1)
		{
			add(createSplitPane(0));
		} else
		{
			add(createPane(panels[0]));
		}
	}

	private JComponent createSplitPane(int index)
	{
		if (VERBOSE)
		{
			System.err.println(this + ".createSplitPane(" + index + ")");
		}
		JComponent top = createPane(panels[index]);
		JComponent bottom;
		index++;
		if (index + 1 == panels.length)
		{
			bottom = new JScrollPane(panels[index]);
			((JScrollPane) bottom).getViewport().setBackground(Color.WHITE);
		} else
		{
			bottom = createSplitPane(index);
		}

		return new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom);
	}

	private JComponent createPane(JComponent panel)
	{
		JScrollPane result = new JScrollPane(panel);
		result.getViewport().setBackground(Color.WHITE);
		return result;
	}

	@Override
	public void setView(KahinaLayeredTreeView view)
	{
		super.setView(view);
		for (int i = 0; i < panels.length; i++)
		{
			panels[i].setView(view.getView(i));
		}
	}

	@Override
	public void updateDisplay()
	{
		for (KahinaTreeViewPanel panel : panels)
		{
			panel.updateDisplay();
			panel.revalidate();
		}
	}

}
