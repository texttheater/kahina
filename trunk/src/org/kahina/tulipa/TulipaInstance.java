package org.kahina.tulipa;

import org.kahina.core.KahinaInstance;
import org.kahina.core.data.project.KahinaProject;
import org.kahina.core.gui.KahinaViewRegistry;
import org.kahina.tulipa.behavior.TulipaDAGBehavior;
import org.kahina.tulipa.bridge.TulipaBridge;
import org.kahina.tulipa.data.grammar.TulipaGrammar;
import org.kahina.tulipa.gui.TulipaGUI;
import org.kahina.tulipa.visual.grammar.TulipaGrammarView;

public class TulipaInstance extends KahinaInstance<TulipaState, TulipaGUI, TulipaBridge, KahinaProject>
{
    
    @Override
    protected void createTreeBehavior()
    {
        new TulipaDAGBehavior(state.getDAG(), this);
    }

    @Override
    protected TulipaBridge createBridge()
    {
        return new TulipaBridge(this);
    }

    @Override
    protected TulipaGUI createGUI()
    {
        return new TulipaGUI(TulipaStep.class, this);
    }

    @Override
    protected TulipaState createState()
    {
    	return new TulipaState();
    }

    @Override
    public TulipaState getState()
    {
        return state;
    }

    @Override
	protected void fillViewRegistry()
	{
		super.fillViewRegistry();
		KahinaViewRegistry.registerMapping(TulipaGrammar.class, TulipaGrammarView.class);
	}
}
