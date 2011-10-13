package org.kahina.lp.gui;

import org.kahina.core.KahinaInstance;
import org.kahina.core.gui.KahinaBreakpointMenu;
import org.kahina.core.gui.KahinaMainWindow;
import org.kahina.core.gui.KahinaWindowManager;
import org.kahina.core.gui.profiler.KahinaProfilerMenu;
import org.kahina.tralesld.gui.TraleSLDParseMenu;

public class LogicProgrammingMainWindow extends KahinaMainWindow
{
	private static final long serialVersionUID = -8044329699904664157L;

	public LogicProgrammingMainWindow(KahinaWindowManager windowManager)
	{
		super(windowManager);
	}
	
	public LogicProgrammingMainWindow(KahinaWindowManager windowManager, int winID)
	{
		super(windowManager, winID);
	}
	
	protected void addAdditionalMenus()
	{
		menuBar.add(new KahinaBreakpointMenu());
		menuBar.add(new KahinaProfilerMenu());
	}
}