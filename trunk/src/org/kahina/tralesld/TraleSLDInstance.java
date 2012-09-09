package org.kahina.tralesld;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.kahina.core.control.KahinaControlEvent;
import org.kahina.core.control.KahinaEvent;
import org.kahina.core.control.KahinaEventTypes;
import org.kahina.core.control.KahinaSystemEvent;
import org.kahina.core.data.project.KahinaProject;
import org.kahina.core.data.project.KahinaProjectStatus;
import org.kahina.core.data.source.KahinaSourceCodeLocation;
import org.kahina.core.gui.KahinaDialogEvent;
import org.kahina.core.gui.KahinaViewRegistry;
import org.kahina.core.gui.event.KahinaChartUpdateEvent;
import org.kahina.core.gui.event.KahinaEdgeSelectionEvent;
import org.kahina.core.gui.event.KahinaSelectionEvent;
import org.kahina.core.gui.event.KahinaUpdateEvent;
import org.kahina.core.io.util.XMLUtil;
import org.kahina.core.util.ListUtil;
import org.kahina.lp.LogicProgrammingInstance;
import org.kahina.lp.profiler.LogicProgrammingProfiler;
import org.kahina.lp.visual.source.PrologJEditSourceCodeView;
import org.kahina.prolog.util.PrologUtil;
import org.kahina.tralesld.behavior.TraleSLDTreeBehavior;
import org.kahina.tralesld.bridge.TraleSLDBridge;
import org.kahina.tralesld.control.TraleSLDControlEventCommands;
import org.kahina.tralesld.data.fs.TraleSLDFS;
import org.kahina.tralesld.data.fs.TraleSLDVariableBindingSet;
import org.kahina.tralesld.data.project.TraleProject;
import org.kahina.tralesld.gui.TraleSLDGUI;
import org.kahina.tralesld.profiler.TraleSLDProfiler;
import org.kahina.tralesld.visual.fs.TraleSLDFeatureStructureView;
import org.kahina.tralesld.visual.fs.TraleSLDVariableBindingSetView;
import org.w3c.dom.Document;

public class TraleSLDInstance extends LogicProgrammingInstance<TraleSLDState, TraleSLDGUI, TraleSLDBridge, TraleProject>
{
	boolean withAuxiliaryInstance = false;
	
	// TODO extract a generic commander superclass from QTypeCommander and move
	// the command stuff in TraleSLDInstance to a TraleSLDCommander. QTypeCommander
	// already avoids the ugly hack used below to set the bridge to abort.

	private static final boolean VERBOSE = false;

	Queue<String> traleCommands = new ArrayDeque<String>();

	private boolean commanding = false;

	public final Action COMPILE_ACTION = new AbstractAction("Compile")
	{

		private static final long serialVersionUID = -3829326193202814557L;

		@Override
		public void actionPerformed(ActionEvent e)
		{
			dispatchEvent(new KahinaControlEvent(TraleSLDControlEventCommands.COMPILE));
		}

	};

	public final Action PARSE_ACTION = new AbstractAction("Parse")
	{

		private static final long serialVersionUID = -3829326193202814557L;

		@Override
		public void actionPerformed(ActionEvent e)
		{
			dispatchEvent(new KahinaControlEvent(TraleSLDControlEventCommands.PARSE));
		}

	};

	public final Action RESTART_ACTION = new AbstractAction("Restart parse")
	{

		private static final long serialVersionUID = -3829326193202814557L;

		@Override
		public void actionPerformed(ActionEvent e)
		{
			dispatchEvent(new KahinaControlEvent(TraleSLDControlEventCommands.RESTART));
		}

	};

	private TraleSLDProfiler profiler;

	private String grammar;

	private List<String> sentence = Collections.emptyList();

	
   public TraleSLDInstance()
    {
        this(false);
    }
	   
	public TraleSLDInstance(boolean withWorkbench)
	{
	    this.withAuxiliaryInstance = withWorkbench;
	    
		COMPILE_ACTION.setEnabled(false);
		PARSE_ACTION.setEnabled(false); // need grammar first
		RESTART_ACTION.setEnabled(false); // need grammar and sentence first
	}

	@Override
	public TraleSLDBridge startNewSession()
	{
		try
		{
			super.startNewSession();
			profiler = new TraleSLDProfiler(this, state.getFullProfile());
			control.registerListener("edge select", this);
			control.registerListener("update", this);
			control.registerListener(KahinaEventTypes.CONTROL, this);
			return bridge;
		}
		catch (NullPointerException e)
		{
			System.err.println("NULL POINTER EXCEPTION at the following stack position:");
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}

	public String getCommand()
	{
		synchronized (traleCommands)
		{
			if (traleCommands.isEmpty())
			{
				commanding = true;
				updateActions();
				return "";
			}

			String traleCommand = traleCommands.remove();
			commanding = !"quit".equals(traleCommand);
			updateActions();
			if (VERBOSE)
			{
				System.err.println(this + ".getCommand()=" + traleCommand + "(Queue: " + traleCommands + ")");
			}
			return traleCommand;
		}
	}

	private void updateActions()
	{
		COMPILE_ACTION.setEnabled(commanding);
		PARSE_ACTION.setEnabled(commanding && grammar != null);
		RESTART_ACTION.setEnabled(commanding && grammar != null && !sentence.isEmpty());
	}

	@Override
	protected void createTreeBehavior()
	{
		new TraleSLDTreeBehavior(state.getStepTree(), this, state.getSecondaryStepTree());
	}

	@Override
	protected TraleSLDBridge createBridge()
	{
		return new TraleSLDBridge(this);
	}

	@Override
	protected TraleSLDGUI createGUI()
	{
		return new TraleSLDGUI(TraleSLDStep.class, this, withAuxiliaryInstance);
	}

	@Override
	protected TraleSLDState createState()
	{
		return new TraleSLDState(control, withAuxiliaryInstance);
	}

	@Override
	protected void fillViewRegistry()
	{
		super.fillViewRegistry();
		KahinaViewRegistry.registerMapping(TraleSLDFS.class, TraleSLDFeatureStructureView.class);
		KahinaViewRegistry.registerMapping(TraleSLDVariableBindingSet.class, TraleSLDVariableBindingSetView.class);
		KahinaViewRegistry.registerMapping(KahinaSourceCodeLocation.class, PrologJEditSourceCodeView.class);
	}

	//@Override
	public void processEvent(KahinaEvent e)
	{
		super.processEvent(e);
		if (e instanceof KahinaEdgeSelectionEvent)
		{
			processEdgeSelectionEvent((KahinaEdgeSelectionEvent) e);
		} else if (e instanceof KahinaUpdateEvent)
		{
			processUpdateEvent((KahinaUpdateEvent) e);
		} else if (e instanceof KahinaControlEvent)
		{
			processControlEvent((KahinaControlEvent) e);
		} else if (e instanceof KahinaSystemEvent)
		{
			processSystemEvent((KahinaSystemEvent) e);
		}
	}

	private void processSystemEvent(KahinaSystemEvent e)
	{
		if (e.getSystemEventType() == KahinaSystemEvent.QUIT)
		{
			synchronized (traleCommands)
			{
				if (commanding)
				{
					traleCommands.add("quit");
				}
			}
		}
	}

	private void processControlEvent(KahinaControlEvent event)
	{
		String command = event.getCommand();

		if (TraleSLDControlEventCommands.REGISTER_SENTENCE.equals(command))
		{
			sentence = castToStringList(event.getArguments()[0]);
			updateActions();
			if (VERBOSE)
			{
				System.err.println("Sentence registered.");
			}
		} 
		else if (TraleSLDControlEventCommands.REGISTER_GRAMMAR.equals(command))
		{
			grammar = (String) event.getArguments()[0];
			PARSE_ACTION.setEnabled(commanding);
			updateActions();
			if (VERBOSE)
			{
				System.err.println("Grammar registered.");
			}
		} 
		else if (TraleSLDControlEventCommands.COMPILE.equals(command))
		{
			if (event.getArguments() == null || event.getArguments().length == 0)
			{
				dispatchEvent(new KahinaDialogEvent(KahinaDialogEvent.COMPILE, new Object[] { grammar }));
			} 
			else
			{
				// Lazy hack: set bridge to abort - if we go through the controller,
				// KahinaRunner will deinitialize and thwart subsequent eventing
				bridge.processEvent(new KahinaSystemEvent(KahinaSystemEvent.QUIT));
				compile((String) event.getArguments()[0]);
			}
		} 
		else if (TraleSLDControlEventCommands.PARSE.equals(command))
		{
			if (event.getArguments() == null || event.getArguments().length == 0)
			{
				dispatchEvent(new KahinaDialogEvent(KahinaDialogEvent.PARSE, new Object[] { ListUtil.join(" ", sentence) }));
			} 
			else
			{
				// Lazy hack: see above
				bridge.processEvent(new KahinaSystemEvent(KahinaSystemEvent.QUIT));
				parse(castToStringList(event.getArguments()[0]));
			}
		} 
		else if (TraleSLDControlEventCommands.RESTART.equals(command))
		{
			// Lazy hack: see above
			bridge.processEvent(new KahinaSystemEvent(KahinaSystemEvent.QUIT));
			compile(grammar);
			parse(sentence);
		}
		else if (TraleSLDControlEventCommands.REBUILD_SIGNATURE_INFO.equals(command))
		{
			gui.signatureUpdate();
		}
	}

	@SuppressWarnings("unchecked")
	private List<String> castToStringList(Object object)
	{
		return (List<String>) object;
	}

	protected void compile(String absolutePath)
	{
		if (VERBOSE)
		{
			System.err.println(this + ".compile(" + absolutePath + ")");
		}
		synchronized (traleCommands)
		{
			traleCommands.add("query dcompile_gram(" + PrologUtil.stringToAtomLiteral(absolutePath) + ").");
			traleCommands.add("query send_signature.");
		}
	}

	protected void parse(List<String> words)
	{
		synchronized (traleCommands)
		{
			//TODO: make sure the signature is retained across parses; requires major restructuring
			traleCommands.add("query send_signature.");
			traleCommands.add("query drec[" + ListUtil.join(",", words) + "].");
		}
	}

	private void processEdgeSelectionEvent(KahinaEdgeSelectionEvent e)
	{
		int nodeID = state.getNodeForEdge(e.getSelectedEdge());
		if (nodeID != -1)
		{
			processEvent(new KahinaSelectionEvent(nodeID));
		}
	}

	private void processUpdateEvent(KahinaUpdateEvent e)
	{
		int edgeID = state.getEdgeForNode(e.getSelectedStep());
		if (edgeID != -1)
		{
			processEvent(new KahinaChartUpdateEvent(edgeID));
		}
	}

	@Override
	public LogicProgrammingProfiler getProfiler()
	{
		return profiler;
	}

	public static void main(String[] args)
	{
		(new TraleSLDInstance(false)).start(args);
	}

    @Override
    protected TraleProject createNewProject()
    {
        return new TraleProject();
    }

    public void loadProject(File projectFile)
    {
        Document dom;
        try
        {
            dom = XMLUtil.parseXMLStream(new FileInputStream(projectFile), false);
            project = TraleProject.importXML(dom.getDocumentElement());
            setProjectStatus(KahinaProjectStatus.PROGRAM_UNCOMPILED);
        }
        catch (FileNotFoundException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
