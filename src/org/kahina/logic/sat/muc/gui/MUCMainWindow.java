package org.kahina.logic.sat.muc.gui;

import org.kahina.core.gui.windows.KahinaMainWindow;
import org.kahina.logic.sat.muc.MUCInstance;

public class MUCMainWindow extends KahinaMainWindow
{
    
    public MUCMainWindow(MUCWindowManager windowManager, MUCInstance kahina)
    {
        super(windowManager, kahina);
    }
    
    public MUCMainWindow(MUCWindowManager windowManager, int winID, MUCInstance kahina)
    {
        super(windowManager, kahina, winID);
    }

    protected void addMenusInFront()
    {
        //menuBar.add(new MUCFileMenu((MUCInstance) kahina));
        menuBar.add(new MusticcaInstanceMenu((MUCInstance) kahina));
        menuBar.add(new MusticcaUSMenu((MUCInstance) kahina));
    }
    
    protected boolean showsViewMenu()
    {
        return false;
    }
    
    protected boolean showsProjectMenu()
    {
        return false;
    }
}
