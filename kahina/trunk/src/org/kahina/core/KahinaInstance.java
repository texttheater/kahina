package org.kahina.core;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Set;

import javax.swing.JOptionPane;

import org.kahina.core.bridge.KahinaBridge;
import org.kahina.core.control.KahinaListener;
import org.kahina.core.data.DataManager;
import org.kahina.core.data.KahinaObject;
import org.kahina.core.data.source.KahinaSourceCodeLocation;
import org.kahina.core.data.text.KahinaLineReference;
import org.kahina.core.data.tree.KahinaTree;
import org.kahina.core.event.KahinaEvent;
import org.kahina.core.event.KahinaEventTypes;
import org.kahina.core.event.KahinaSessionEvent;
import org.kahina.core.event.KahinaSystemEvent;
import org.kahina.core.gui.KahinaGUI;
import org.kahina.core.gui.KahinaViewRegistry;
import org.kahina.core.gui.event.KahinaConsoleLineEvent;
import org.kahina.core.gui.event.KahinaUpdateEvent;
import org.kahina.core.util.FileUtilities;
import org.kahina.core.util.ProgressMonitorWrapper;
import org.kahina.core.util.SwingUtilities;
import org.kahina.core.visual.KahinaDefaultView;
import org.kahina.core.visual.source.KahinaSourceCodeView;
import org.kahina.core.visual.tree.KahinaTreeView;

public abstract class KahinaInstance<S extends KahinaState, G extends KahinaGUI, B extends KahinaBridge> implements KahinaListener
{
	protected S state;
	protected G gui;
	protected B bridge;

	public KahinaInstance()
	{
		fillViewRegistry();
		state = createState();
		gui = createGUI();
		bridge = createBridge();
		KahinaRunner.getControl().registerListener(KahinaEventTypes.UPDATE, this);
		KahinaRunner.getControl().registerListener(KahinaEventTypes.SESSION, this);
		KahinaRunner.getControl().registerListener(KahinaEventTypes.SYSTEM, this);
	}

	public KahinaInstance(S state)
	{
		fillViewRegistry();
		this.state = state;
		gui = createGUI();
		// TODO (re)create bridge (when adding support for resuming live
		// sessions)
		KahinaRunner.getControl().registerListener(KahinaEventTypes.UPDATE, this);
		KahinaRunner.getControl().registerListener(KahinaEventTypes.SESSION, this);
		KahinaRunner.getControl().registerListener(KahinaEventTypes.SYSTEM, this);
	}

	protected abstract S createState();

	protected abstract G createGUI();

	protected abstract B createBridge();

	public G getGUI()
	{
		return gui;
	}

	public B getBridge()
	{
		return bridge;
	}

	public S getState()
	{
		return state;
	}

	/**
	 * overwrite this to register views for user-defined datatypes MUST register
	 * views for all data types use super.fillViewRegistry() in implementations
	 * to register most basic views
	 */
	protected void fillViewRegistry()
	{
		KahinaViewRegistry.registerMapping(KahinaObject.class, KahinaDefaultView.class);
		KahinaViewRegistry.registerMapping(KahinaTree.class, KahinaTreeView.class);
		KahinaViewRegistry.registerMapping(KahinaSourceCodeLocation.class, KahinaSourceCodeView.class);
	}

	@Override
	public void processEvent(KahinaEvent e)
	{
		if (e instanceof KahinaUpdateEvent)
		{
			processUpdateEvent((KahinaUpdateEvent) e);
		} else if (e instanceof KahinaSessionEvent)
		{
			processSessionEvent((KahinaSessionEvent) e);
		} else if (e instanceof KahinaSystemEvent)
		{
			processSystemEvent((KahinaSystemEvent) e);
		}
	}

	private void processSystemEvent(KahinaSystemEvent e)
	{
		if (e.getSystemEventType() == KahinaSystemEvent.QUIT)
		{
			KahinaRunner.deinitialize();
		}
	}

	private void processSessionEvent(KahinaSessionEvent e)
	{
		int type = e.getSessionEventType();
		if (type == KahinaSessionEvent.SAVE_SESSION)
		{
			saveSessionAs(e.getFile());
		}
	}

	private void saveSessionAs(File zipFile)
	{
		File directory;
		try
		{
			directory = FileUtilities.createTemporaryDirectory();
		} catch (IOException e)
		{
			gui.showMessageDialog(SwingUtilities.visualError("State could not be saved due to the following problem:", e), "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		if (zipFile.exists())
		{
			int answer = gui.showConfirmDialog("File " + zipFile + " exists. Overwrite it?", "Confirm overwrite", JOptionPane.YES_NO_OPTION);
			if (answer != JOptionPane.YES_OPTION)
			{
				return;
			}
		}
		File stepFolder = new File(directory, "steps");
		if (!stepFolder.mkdir())
		{
			gui.showMessageDialog("Failed to create directory " + directory + ". State not saved.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		DataManager dm = KahinaRunner.getDataManager();
		ProgressMonitorWrapper monitor = gui.createProgressMonitorWrapper("Saving state", null, 0, dm.persistSteps() * 2 + 2);
		ObjectOutputStream out = null;
		try
		{
			synchronized (dm)
			{
				dm.persist(stepFolder, monitor);
			}
			synchronized (state)
			{
				out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(new File(directory, "state"))));
				out.writeObject(state);
				monitor.increment();
			}
			FileUtilities.zipDirectory(directory, zipFile, monitor);
			FileUtilities.deleteRecursively(directory);
			monitor.close();
		} catch (Exception e)
		{
			monitor.close();
			gui.showMessageDialog(SwingUtilities.visualError("State could not be saved due to the following problem: ", e), "Error", JOptionPane.ERROR_MESSAGE);
		} finally
		{
			if (out != null)
			{
				try
				{
					out.close();
				} catch (IOException e)
				{
					gui.showMessageDialog(SwingUtilities.visualError("State could not be saved due to the following problem: ", e), "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		}
	}

	private void processUpdateEvent(KahinaUpdateEvent e)
	{
		Set<KahinaLineReference> refs = state.getLineReferencesForStep(e.getSelectedStep());
		if (refs != null)
		{
			KahinaRunner.processEvent(new KahinaConsoleLineEvent(refs));
		}
	}
}
