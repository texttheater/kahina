package org.kahina.core.visual;

import java.lang.reflect.InvocationTargetException;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.kahina.core.KahinaException;
import org.kahina.core.control.KahinaEvent;
import org.kahina.core.control.KahinaListener;
import org.kahina.core.gui.KahinaProgressBar;
import org.kahina.core.gui.event.KahinaRedrawEvent;

public abstract class KahinaViewPanel<T extends KahinaView<?>> extends JPanel implements KahinaListener
{
	private static final long serialVersionUID = 5677332450070203832L;

	private static final boolean VERBOSE = false;

	public T view;
	
    protected KahinaProgressBar progressBar;
    JComponent progressBarParent;

	public void processEvent(KahinaEvent event)
	{
		if (VERBOSE)
		{
			System.err.println(this + " received " + event);
		}
		if (event instanceof KahinaRedrawEvent)
		{
		    if (view != null && view.needsRedraw())
		    {
		        updateDisplayAndRepaintFromEventDispatchThread();
		    }
		}
	}

	public void setView(T view)
	{
		this.view = view;
		updateDisplayAndRepaintFromEventDispatchThread();
	}

	public void updateDisplayAndRepaintFromEventDispatchThread()
	{
		try
		{
			if (SwingUtilities.isEventDispatchThread()) 
			{
				updateDisplay();
				revalidate();
				repaint();
			} 
			else 
			{
			    SwingUtilities.invokeAndWait(new Runnable() 
			    {
			        @Override
			        public void run() 
			        {
						updateDisplay();
						revalidate();
						repaint();
			        }
			    });
			}
		} 
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        catch (InvocationTargetException e)
        {
            e.printStackTrace();
        }
	}

	/**
	 * This method must be called from the Swing event dispatch thread.
	 */
	public abstract void updateDisplay();
	
    public void showProgressBar()
    {
        if (progressBarParent != null)
        {
            progressBarParent.add(progressBar);
            progressBarParent.revalidate();
        }
    }
    
    public void hideProgressBar()
    {
        if (progressBarParent != null)
        {
            progressBarParent.remove(progressBar);
            progressBarParent.revalidate();
        }
    }

    public void setProgressBar(KahinaProgressBar progressBar)
    {
        if (progressBar != null)
        {
            this.progressBar = progressBar;  
            this.progressBarParent = (JComponent) progressBar.getParent();
            hideProgressBar();
        }
    }
}
