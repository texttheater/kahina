package org.kahina.core.gui;

import java.awt.Toolkit;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.JFrame;

import org.kahina.core.KahinaInstance;
import org.kahina.core.control.KahinaController;
import org.kahina.core.control.KahinaListener;
import org.kahina.core.event.KahinaEvent;
import org.kahina.core.event.KahinaEventTypes;
import org.kahina.core.event.KahinaPerspectiveEvent;
import org.kahina.core.event.KahinaWindowEvent;
import org.kahina.core.event.KahinaWindowEventType;
import org.kahina.core.io.util.XMLUtilities;
import org.kahina.core.visual.KahinaEmptyView;
import org.kahina.core.visual.KahinaView;
import org.w3c.dom.Node;

public class KahinaWindowManager implements KahinaListener
{
    public KahinaMainWindow mainWindow;
    
    KahinaPerspective psp;
    KahinaArrangement arr;
    
    //main registry for windows: access windows by their windowID
    private HashMap<Integer,KahinaWindow> windowByID;
    
    KahinaGUI gui;
    
    KahinaController control;
    
    public KahinaWindowManager(KahinaGUI gui, KahinaController control)
    {
        this.gui = gui;  
        this.control = control;
		control.registerListener(KahinaEventTypes.PERSPECTIVE, this);
		control.registerListener(KahinaEventTypes.WINDOW, this);
		
        this.windowByID = new HashMap<Integer,KahinaWindow>();
    }
    
    /**
     * Builds the windows according to some perspective. Must be called before display.
     */
    public void createWindows(KahinaPerspective psp)
    {
        this.psp = psp;     
        this.arr = psp.getArrangement();
        
        //create and register main window
        mainWindow = createMainWindow(this, control, gui.kahina, arr.getPrimaryWinIDForName("main"));
        arr.setPrimaryWindow("main", mainWindow.getID());
        
		//create windows for all the other views (TODO: produce all the dynamic clones as well)
        for (String name : gui.varNameToView.keySet())
        {
        	KahinaView<?> view = gui.varNameToView.get(name);
        	System.err.println("Generating view: " + name);
            KahinaWindow viewWindow = new KahinaDefaultWindow(view, this, arr.getPrimaryWinIDForName(name));
            viewWindow.setTitle(view.getTitle());
            
            //TODO: for now, build the arrangement from within (this should be done in the perspective's default constructor)
            arr.topLevelWindows.add(viewWindow.getID());
        }
        
        for (int winID : arr.topLevelWindows)
        {
        	KahinaWindow w = getWindowByID(winID);
            w.setSize(arr.getWidth(w.getID()), arr.getHeight(w.getID()));
            w.setLocation(arr.getXPos(w.getID()), arr.getYPos(w.getID()));
            
        	//apply configuration as defined by the perspective to the view
            gui.varNameToView.get(arr.getBindingForWinID(w.getID())).setConfig(psp.getConfiguration(w.getID()));
        }
    }
    
    /**
     * Discards the current perspective and applies a newly provided one.
     * @param psp the perspective to be applied
     */
    public void setAndApplyPerspective(KahinaPerspective psp)
    {
        this.psp = psp;     
        this.arr = psp.getArrangement();
        
        //TODO: does not handle the embedding structure so far; just the window positions
        //it will require a major and systematic effort to correctly implement all of this
        
        for (int winID : windowByID.keySet())
        {
        	System.err.println("Applying new layout to window with ID " + winID);
        	
        	KahinaWindow w = getWindowByID(winID);
            w.setSize(arr.getWidth(w.getID()), arr.getHeight(w.getID()));
            w.setLocation(arr.getXPos(w.getID()), arr.getYPos(w.getID()));
    
        	//apply configuration as defined by the perspective to the view
            //TODO: also define the main window as a "view" for a more unified treatment
            String binding = arr.getBindingForWinID(w.getID());
            if (!binding.equals("main"))
            {
            	gui.varNameToView.get(binding).setConfig(psp.getConfiguration(w.getID()));
            }
            w.validate();
            w.repaint();
        }
    }
    
    public void registerWindow(KahinaWindow window)
    {
    	 windowByID.put(window.getID(),window);
    	 arr.setWindowType(window.getID(), window.getWindowType());
    }
    
    public KahinaWindow getWindowByID(int winID)
    {
    	return windowByID.get(winID);
    }
    
    protected KahinaMainWindow createMainWindow(KahinaWindowManager kahinaWindowManager, KahinaController control, KahinaInstance<?, ?, ?> kahina)
	{
		return new KahinaMainWindow(this, control, gui.kahina);
	}
    
    protected KahinaMainWindow createMainWindow(KahinaWindowManager kahinaWindowManager, KahinaController control, KahinaInstance<?, ?, ?> kahina, int winID)
	{
		return new KahinaMainWindow(this, control, gui.kahina, winID);
	}

	public void disposeAllWindows()
    {
        for (int windowID : arr.topLevelWindows)
        {
            getWindowByID(windowID).dispose();
        }
        mainWindow.dispose();
    }
	
	public boolean isTopLevelWindow(KahinaWindow w)
	{
		return arr.topLevelWindows.contains(w.getID());
	}
    
    public KahinaWindow integrateInDefaultWindow(KahinaView<?> view)
    {
        KahinaWindow viewWindow = new KahinaDefaultWindow(view, this);
        viewWindow.setTitle(view.getTitle());
        arr.topLevelWindows.add(viewWindow.getID());
        return viewWindow;
    }
    
    public void integrateInVerticallySplitWindow(int window1ID, int window2ID, String newTitle, KahinaController control)
    {
        KahinaWindow wrapperWindow1 = windowByID.get(window1ID);
        if (wrapperWindow1 == null)
        {
            wrapperWindow1 =  new KahinaDefaultWindow(new KahinaEmptyView(control), this);
            System.err.println("WARNING: split window could not access window \"" + window1ID + "\"");
        }
        KahinaWindow wrapperWindow2 = windowByID.get(window2ID);
        if (wrapperWindow2 == null)
        {
        	wrapperWindow2 =  new KahinaDefaultWindow(new KahinaEmptyView(control), this);
            System.err.println("WARNING: split window could not access window \"" + window1ID + "\"");
        }
        control.processEvent(new KahinaWindowEvent(KahinaWindowEventType.NEW_VERT_SPLIT, -1, newTitle));
        KahinaVerticallySplitWindow splitWindow = (KahinaVerticallySplitWindow) windowByID.get(newTitle);
        splitWindow.setUpperWindow(wrapperWindow1);
        splitWindow.setLowerWindow(wrapperWindow2);
        control.processEvent(new KahinaWindowEvent(KahinaWindowEventType.REMOVE, window1ID));
        control.processEvent(new KahinaWindowEvent(KahinaWindowEventType.REMOVE, window2ID));
    }
    
    public void integrateInHorizontallySplitWindow(int window1ID, int window2ID, String newTitle, KahinaController control)
    {
        KahinaWindow wrapperWindow1 = windowByID.get(window1ID);
        if (wrapperWindow1 == null)
        {
            wrapperWindow1 =  new KahinaDefaultWindow(new KahinaEmptyView(control), this);
            System.err.println("WARNING: split window could not access window \"" + window1ID + "\"");
        }
        KahinaWindow wrapperWindow2 = windowByID.get(window2ID);
        if (wrapperWindow2 == null)
        {
        	wrapperWindow2 =  new KahinaDefaultWindow(new KahinaEmptyView(control), this);
            System.err.println("WARNING: split window could not access window \"" + window2ID + "\"");
        }
        control.processEvent(new KahinaWindowEvent(KahinaWindowEventType.NEW_HORI_SPLIT, -1, newTitle));
        KahinaHorizontallySplitWindow splitWindow = (KahinaHorizontallySplitWindow) windowByID.get(newTitle);
        splitWindow.setLeftWindow(wrapperWindow1);
        splitWindow.setRightWindow(wrapperWindow2);
        control.processEvent(new KahinaWindowEvent(KahinaWindowEventType.REMOVE, window1ID));
        control.processEvent(new KahinaWindowEvent(KahinaWindowEventType.REMOVE, window2ID));
    }
    
    public void displayWindows()
    {
    	if (psp == null)
    	{
    		System.err.println("No perspective defined, createWindows() was probably not called.");
    		System.err.println("A default perspective can be generated via KahinaPerspective.generateDefaultPerspective()");
    		System.err.println("Unable to display windows, quitting.");
    		System.exit(1);
    	}
    	else
    	{
    		mainWindow.setVisible(true);
    		for (int winID : arr.topLevelWindows)
    		{
    			getWindowByID(winID).setVisible(psp.isVisible(winID));
    		}
    	}
    }
    
	@Override
	public void processEvent(KahinaEvent e)
	{
		if (e instanceof KahinaPerspectiveEvent)
		{
			processPerspectiveEvent((KahinaPerspectiveEvent) e);
		}
		else if (e instanceof KahinaWindowEvent)
		{
			processWindowEvent((KahinaWindowEvent) e);
		}
	}
    
	private void processPerspectiveEvent(KahinaPerspectiveEvent e)
	{
		int type = e.getPerspectiveEventType();
		if (type == KahinaPerspectiveEvent.SAVE_PERSPECTIVE)
		{
			savePerspectiveAs(e.getFile());
		} 
		else if (type == KahinaPerspectiveEvent.LOAD_PERSPECTIVE)
		{
			setAndApplyPerspective(loadPerspective(e.getFile()));
		}
	}
	
	private void processWindowEvent(KahinaWindowEvent e)
	{
		int type = e.getWindowEventType();
		if (type == KahinaWindowEventType.NEW_DEFAULT)
		{
	        KahinaWindow viewWindow = new KahinaDummyWindow(this);
	        arr.topLevelWindows.add(viewWindow.getID());
	        viewWindow.setTitle(e.getStringContent());
            viewWindow.setSize(300,100);
            viewWindow.setLocation(200,200);
	        viewWindow.setVisible(true);
	        control.processEvent(new KahinaWindowEvent(KahinaWindowEventType.ADD_VIEW_MENU_ENTRY, viewWindow.getID()));
		} 
		else if (type == KahinaWindowEventType.NEW_HORI_SPLIT)
		{
	        KahinaHorizontallySplitWindow splitWindow = new KahinaHorizontallySplitWindow(this);
	        arr.topLevelWindows.add(splitWindow.getID());
	        splitWindow.setTitle(e.getStringContent());
	        splitWindow.setLeftWindow(new KahinaDummyWindow(this));
	        splitWindow.setRightWindow(new KahinaDummyWindow(this));
            splitWindow.setSize(600,150);
            splitWindow.setLocation(200,200);
	        splitWindow.setVisible(true);
	        control.processEvent(new KahinaWindowEvent(KahinaWindowEventType.ADD_VIEW_MENU_ENTRY, splitWindow.getID()));
		} 
		else if (type == KahinaWindowEventType.NEW_VERT_SPLIT)
		{
	        KahinaVerticallySplitWindow splitWindow = new KahinaVerticallySplitWindow(this);
	        arr.topLevelWindows.add(splitWindow.getID());
	        splitWindow.setTitle(e.getStringContent());
	        splitWindow.setUpperWindow(new KahinaDummyWindow(this));
	        splitWindow.setLowerWindow(new KahinaDummyWindow(this));
            splitWindow.setSize(300,250);
            splitWindow.setLocation(200,200);
	        splitWindow.setVisible(true);
	        control.processEvent(new KahinaWindowEvent(KahinaWindowEventType.ADD_VIEW_MENU_ENTRY, splitWindow.getID()));
		} 
		else if (type == KahinaWindowEventType.NEW_TABBED)
		{
	        KahinaTabbedWindow tabbedWindow = new KahinaTabbedWindow(this);
	        arr.topLevelWindows.add(tabbedWindow.getID());
	        tabbedWindow.setTitle(e.getStringContent());
	        tabbedWindow.addWindow(new KahinaDummyWindow(this));
            tabbedWindow.setSize(300,250);
            tabbedWindow.setLocation(200,200);
	        tabbedWindow.setVisible(true);
	        control.processEvent(new KahinaWindowEvent(KahinaWindowEventType.ADD_VIEW_MENU_ENTRY, tabbedWindow.getID()));
		} 
		else if (type == KahinaWindowEventType.TOGGLE_VISIBLE)
		{
			if (arr.topLevelWindows.contains(e.getWindowID()))
			{
				psp.toggleVisibility(e.getWindowID());
				KahinaWindow window = windowByID.get(e.getWindowID());
				if (window == null)
				{
					System.err.println("WARNING: could not find window \"" + e.getWindowID() + "\"");
				}
				else
				{
					window.setVisible(psp.isVisible(e.getWindowID()));
					if (!window.isVisible()) window.dispose();
				}
			}
			else
			{
				System.err.println("WARNING: cannot hide/show non-top-level window \"" + e.getWindowID() + "\"");
			}
		} 
		else if (type == KahinaWindowEventType.REMOVE)
		{
			if (arr.topLevelWindows.contains(e.getWindowID()))
			{
				psp.setVisibility(e.getWindowID(), false);
				arr.topLevelWindows.remove(e.getWindowID());
				KahinaWindow window = windowByID.get(e.getWindowID());
				window.dispose();
			}
			else
			{
				System.err.println("WARNING: Removal only possible for top-level windows! Undock first!");
			}
		} 
		else if (type == KahinaWindowEventType.DISPOSE)
		{
			control.processEvent(new KahinaWindowEvent(KahinaWindowEventType.UNDOCK, e.getWindowID()));
			control.processEvent(new KahinaWindowEvent(KahinaWindowEventType.REMOVE, e.getWindowID()));
			windowByID.remove(e.getWindowID());
		} 
		else if (type == KahinaWindowEventType.RENAME)
		{
			KahinaWindow window = windowByID.get(e.getWindowID());
			if (window == null)
			{
				System.err.println("WARNING: Could not find window \"" + e.getWindowID() + "\".");
			}
			else
			{
				//TODO: switch titles of clones as well; let clones always have identical title + " (clone)"
				window.setTitle(e.getStringContent());
				window.mainPanel.repaint();
			}
		} 
		else if (type == KahinaWindowEventType.FLIP)
		{
			KahinaWindow window = windowByID.get(e.getWindowID());
			if (window == null)
			{
				System.err.println("WARNING: Could not find window \"" + e.getWindowID() + "\".");
			}
			else
			{
				if (window.isFlippableWindow())
				{
					window.flipSubwindows();
					window.mainPanel.repaint();
				}
				else
				{
					System.err.println("WARNING: Window \"" + e.getWindowID() + "\" is not flippable. Ignored.");
				}
			}
		} 
		else if (type == KahinaWindowEventType.DYNAMIC_CLONE)
		{
			KahinaWindow window = windowByID.get(e.getWindowID());
			if (window == null)
			{
				System.err.println("WARNING: Could not find window \"" + e.getWindowID() + "\".");
			}
			else
			{
				KahinaWindow cloneWindow = window.createDynamicClone();
		        arr.topLevelWindows.add(cloneWindow.getID());
		        cloneWindow.setVisible(true);
		        control.processEvent(new KahinaWindowEvent(KahinaWindowEventType.ADD_VIEW_MENU_ENTRY, cloneWindow.getID()));
			}
		} 
		else if (type == KahinaWindowEventType.SNAPSHOT_CLONE)
		{
			KahinaWindow window = windowByID.get(e.getWindowID());
			if (window == null)
			{
				System.err.println("WARNING: Could not find window \"" + e.getWindowID() + "\".");
			}
			else
			{
				KahinaWindow cloneWindow = window.createSnapshotClone();
		        arr.topLevelWindows.add(cloneWindow.getID());
		        cloneWindow.setVisible(true);
		        control.processEvent(new KahinaWindowEvent(KahinaWindowEventType.ADD_VIEW_MENU_ENTRY, cloneWindow.getID()));
			}
		} 
		else if (type == KahinaWindowEventType.UNDOCK)
		{
			KahinaWindow window = windowByID.get(e.getWindowID());
			if (window == null)
			{
				System.err.println("WARNING: Could not find window \"" + e.getWindowID() + "\".");
			}
			else
			{
				KahinaWindow embeddingWindow = window.getEmbeddingWindow();
				if (embeddingWindow == null)
				{
					//warning deactivated because undocking is used generically for drag & drop functionality
					//System.err.println("WARNING: Window \"" + e.getWindowID() + "\" cannot be undocked, as is not embedded.");
				}
				//now comes the interesting case
				else
				{
					//let the embeddingWindow release the window and provide an appropriate replacement
					KahinaWindow replacementWindow = embeddingWindow.getReplacementAfterRelease(window);
					//simpler case: embeddingWindow was embedded
					if (!embeddingWindow.isTopLevelWindow())
					{
						KahinaWindow embEmbeddingWindow = embeddingWindow.getEmbeddingWindow();
						embEmbeddingWindow.replaceSubwindow(embeddingWindow,replacementWindow);
						embEmbeddingWindow.validate();
						embEmbeddingWindow.repaint();
					}
					//complicated case: embeddingWindow was top level window
					else
					{
						control.processEvent(new KahinaWindowEvent(KahinaWindowEventType.REMOVE, embeddingWindow.getID()));
						arr.topLevelWindows.add(replacementWindow.getID());
						replacementWindow.setVisible(true);
						control.processEvent(new KahinaWindowEvent(KahinaWindowEventType.ADD_VIEW_MENU_ENTRY, replacementWindow.getID()));
					}
					windowByID.remove(embeddingWindow.getID());
					//register and display the undocked window
					psp.setVisibility(e.getWindowID(), true);
					arr.topLevelWindows.add(e.getWindowID());
					window.setVisible(true);
				}
			}
		} 
		else if (type == KahinaWindowEventType.VERT_SPLIT)
		{
			KahinaWindow window = windowByID.get(e.getWindowID());
			if (window == null)
			{
				System.err.println("WARNING: Could not find window \"" + e.getWindowID() + "\".");
			}
			else
			{
				KahinaWindow oldEmbeddingWindow = window.getEmbeddingWindow();
				KahinaVerticallySplitWindow splitWindow = new KahinaVerticallySplitWindow(this);
		        splitWindow.setTitle(e.getStringContent());
		        splitWindow.setUpperWindow(window);
		        splitWindow.setLowerWindow(new KahinaDummyWindow(this));
	            splitWindow.setSize(window.getWidth(),window.getHeight());
	            splitWindow.setLocation(window.getLocation());
	            window.setSize(window.getWidth(),window.getHeight() / 2);
				if (oldEmbeddingWindow != null)
				{
					oldEmbeddingWindow.replaceSubwindow(window,splitWindow);
					arr.setEmbeddingWindowID(window.getID(),splitWindow.getID());
					oldEmbeddingWindow.validate();
					oldEmbeddingWindow.repaint();
				}
				else
				{
					control.processEvent(new KahinaWindowEvent(KahinaWindowEventType.REMOVE, e.getWindowID()));
		            arr.topLevelWindows.remove(e.getWindowID());
					arr.topLevelWindows.add(splitWindow.getID());
					splitWindow.setVisible(true);
					control.processEvent(new KahinaWindowEvent(KahinaWindowEventType.ADD_VIEW_MENU_ENTRY, splitWindow.getID()));
				}
			}
		} 
		else if (type == KahinaWindowEventType.HORI_SPLIT)
		{
			KahinaWindow window = windowByID.get(e.getWindowID());
			if (window == null)
			{
				System.err.println("WARNING: Could not find window \"" + e.getWindowID() + "\".");
			}
			else
			{
				KahinaWindow oldEmbeddingWindow = window.getEmbeddingWindow();
				KahinaHorizontallySplitWindow splitWindow = new KahinaHorizontallySplitWindow(this);
		        splitWindow.setTitle(e.getStringContent());
		        splitWindow.setLeftWindow(window);
		        splitWindow.setRightWindow(new KahinaDummyWindow(this));
	            splitWindow.setSize(window.getWidth(),window.getHeight());
	            splitWindow.setLocation(window.getLocation());
	            window.setSize(window.getWidth(),window.getHeight() / 2);
				if (oldEmbeddingWindow != null)
				{
					oldEmbeddingWindow.replaceSubwindow(window,splitWindow);
					arr.setEmbeddingWindowID(window.getID(),splitWindow.getID());
					oldEmbeddingWindow.validate();
					oldEmbeddingWindow.repaint();
				}
				else
				{
					control.processEvent(new KahinaWindowEvent(KahinaWindowEventType.REMOVE, e.getWindowID()));
		            arr.topLevelWindows.remove(e.getWindowID());
					arr.topLevelWindows.add(splitWindow.getID());
					splitWindow.setVisible(true);
					control.processEvent(new KahinaWindowEvent(KahinaWindowEventType.ADD_VIEW_MENU_ENTRY, splitWindow.getID()));
				}
			}
		}
	}
	
	private KahinaPerspective loadPerspective(File file)
	{
		return KahinaPerspective.importXML(XMLUtilities.parseXMLFile(file, false).getDocumentElement());
	}
	
	private void savePerspectiveAs(File file)
	{
		Node node = psp.exportXML(XMLUtilities.newEmptyDocument());
		XMLUtilities.writeXML(node,file.getAbsolutePath());
	}
}
