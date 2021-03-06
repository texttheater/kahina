package org.kahina.swi.bridge;

import org.kahina.core.gui.event.KahinaSelectionEvent;
import org.kahina.lp.LogicProgrammingInstance;
import org.kahina.prolog.bridge.PrologBridge;
import org.kahina.swi.SWIPrologStep;

public class SWIPrologBridge extends PrologBridge
{

	public SWIPrologBridge(LogicProgrammingInstance kahina)
	{
		super(kahina);
	}
	
	@Override
	protected SWIPrologStep generateStep()
	{
		return new SWIPrologStep();
	}
	
	/**
	 * Called to register variable bindings for a step.
	 * @param externalStepID
	 * @param direction any string, normally either {@code "in"} (for call, redo) or {@code "out"} (for exit)
	 * @param keys argument numbers or variable names
	 * @param values string representations of Prolog terms
	 */
	public void registerBindings(int externalStepID, String direction, String[] keys, String[] values)
	{
		try
		{
			int internalStepID = convertStepID(externalStepID);
			SWIPrologStep step = state.retrieve(SWIPrologStep.class, internalStepID);
			
			if ("in".equals(direction))
			{
				step.setInBindings(keys, values);
			} else
			{
				step.setOutBindings(keys, values);
			}

			state.store(internalStepID, step);

			if (getBridgeState() == 'n')
			{
				kahina.dispatchEvent(new KahinaSelectionEvent(internalStepID));
			}
		} catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}

}
