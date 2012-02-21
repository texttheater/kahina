package org.kahina.core.gui.profiler;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenu;

import org.kahina.core.KahinaRunner;
import org.kahina.core.gui.KahinaDialogEvent;
import org.kahina.core.util.SwingUtil;

public class KahinaProfilerMenu extends JMenu implements ActionListener
{
	
	private static final long serialVersionUID = -7484775954027995992L;

	public KahinaProfilerMenu()
	{
		super("Profiler");
		add(SwingUtil.createMenuItem("Full profile", "fullProfile", this));
		add(SwingUtil.createMenuItem("Profile call subtree", "callSubtreeProfile", this));
		add(SwingUtil.createMenuItem("Profile search subtree", "searchSubtreeProfile", this));
		addSeparator();
		add(SwingUtil.createMenuItem("Edit warnings", "editWarnings", this));
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		String command = e.getActionCommand();
		if (command.equals("fullProfile"))
		{
			KahinaRunner.processEvent(new KahinaDialogEvent(KahinaDialogEvent.FULL_PROFILE));
		} else if (command.equals("callSubtreeProfile"))
		{
			KahinaRunner.processEvent(new KahinaDialogEvent(KahinaDialogEvent.CALL_SUBTREE_PROFILE));
		} else if (command.equals("searchSubtreeProfile"))
		{
			KahinaRunner.processEvent(new KahinaDialogEvent(KahinaDialogEvent.SEARCH_SUBTREE_PROFILE));
		} else if (command.equals("editWarnings"))
		{
			KahinaRunner.processEvent(new KahinaDialogEvent(KahinaDialogEvent.EDIT_WARNINGS));
		}
	}

}
