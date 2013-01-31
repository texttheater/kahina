package org.kahina.logic.sat.muc.task;

import java.awt.Color;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import javax.swing.SwingUtilities;

import org.kahina.core.data.dag.ColoredPath;
import org.kahina.core.gui.event.KahinaRedrawEvent;
import org.kahina.core.gui.event.KahinaUpdateEvent;
import org.kahina.core.io.color.ColorUtil;
import org.kahina.core.task.KahinaTask;
import org.kahina.core.task.KahinaTaskManager;
import org.kahina.logic.sat.io.minisat.MiniSAT;
import org.kahina.logic.sat.io.minisat.MiniSATFiles;
import org.kahina.logic.sat.io.proof.ResolutionProofParser;
import org.kahina.logic.sat.muc.MUCState;
import org.kahina.logic.sat.muc.MUCStep;
import org.kahina.logic.sat.muc.data.Overlap;
import org.kahina.logic.sat.muc.heuristics.ReductionHeuristic;
import org.kahina.logic.sat.muc.visual.UCReducerPanel;

public class ReductionAgent extends KahinaTaskManager
{
    //provides access to the decision DAG that is constructed by multiple UCReducers
    MUCState state;
    
    //remember at which step ID we started
    int startID;
    int startSize;
    int numSATReductions;
    int numUNSATReductions;  
    
    //store the current UC and its ID after each (attempted) reduction step
    MUCStep uc;
    int ucID = -1;
    
    boolean stopped = false;
    
    MiniSATFiles files;
    
    ReductionHeuristic heuristics;
    private Color signalColor;
    
    private UCReducerPanel panel;
    
    private ColoredPath path;
    
    public ReductionAgent(MUCState state, int startID, MiniSATFiles files)
    {
        this.state = state;
        
        System.err.println("Retrieving start MUCStep with ID " + startID);
        this.uc = state.retrieve(MUCStep.class, startID);
        this.ucID = startID;
        this.startID = startID;
        
        path = new ColoredPath(signalColor);
        getPath().append(startID);
        state.getDecisionGraph().addColorPath(getPath());
        
        this.files = files.copyWithoutTmpFiles();
        this.setSignalColor(ColorUtil.randomColor());
        this.setPanel(null);
    }
    
    public void start()
    {
        this.numSATReductions = 0;
        this.numUNSATReductions = 0;
        this.startSize = uc.getUc().size();
        startNextReduction();
    }
    
    public void taskFinished(KahinaTask task)
    {
        try
        {
            if (task instanceof ReductionTask)
            {
                ReductionTask ucTask = (ReductionTask) task;
                System.err.print("ReductionAgent " + this.hashCode() + ": Reduction #" + ucTask.reductionID + " of clauses " + ucTask.candidates + " at step " + ucID );
                MUCStep result = ucTask.getResult();
                //attempt was unsuccessful
                if (uc == result)
                {
                    System.err.println(" without success!");
                    //uc and ucID just stay the same
                    numSATReductions++;
                    if (!state.usesMetaLearning())
                    {
                        if (ucTask.candidates.size() == 1)
                        {
                            state.addAndDistributeIrreducibilityInfo(ucTask.ucID, ucTask.candidates.get(0));
                        }
                    }
                    else
                    {
                        //we learn that the current selector variables cannot be 1 together
                        TreeSet<Integer> metaClause = new TreeSet<Integer>();
                        int numClauses = state.getStatistics().numClausesOrGroups;
                        for (int i = 1; i <= numClauses; i++)
                        {
                            if (!result.getUc().contains(i) || ucTask.candidates.contains(i))
                            {
                                metaClause.add(i);
                            }
                        }
                        //state.learnMetaBlock(metaClause);
                        state.learnMetaClause(metaClause);
                    }
                    //model rotation if only one candidate was reduced, and the task was configured to apply MR
                    if (ucTask.usesModelRotation() && ucTask.candidates.size() == 1)
                    {
                        TreeSet<Integer> derivedCritical = new TreeSet<Integer>();
                        state.modelRotation(ucTask.getModel(), ucID, ucTask.candidates.get(0), derivedCritical);
                        System.err.println("  model rotation yields additional critical clauses " + derivedCritical);
                    }
                    //System.err.println(this + ": Reduction #" + ucTask.reductionID + " of clause " + ucTask.candidate + " at step " + ucID + " led to satisfiable instance! No change!");
                }
                //attempt was successful, we might have arrived at a new UC
                else
                {
                    System.err.println(" successful!");
                    numUNSATReductions++;
                    int stepID = state.registerMUC(result, ucID, ucTask.candidates);
                    getPath().append(stepID);
                    Overlap overlap = new Overlap(uc.getUc(),result.getUc());
                    for (int candidate : overlap.aMinusB)
                    {
                        if (ucTask.uc.getRemovalLink(candidate) == null)
                        {
                            state.addAndDistributeReducibilityInfo(ucTask.ucID, candidate, -2);
                        }
                    }
                    if (ucTask.candidates.size() == 1)
                    {
                        ucTask.uc.setRemovalLink(ucTask.candidates.get(0), stepID);
                    }
                    uc = state.retrieve(MUCStep.class, stepID);
                    ucID = stepID;
                    heuristics.setNewUC(uc);
                    if (heuristics.needsProof())
                    {
                        //heuristics.deliverProof(MiniSAT.getVarRelevanceOrdering(files.tmpProofFile));
                        heuristics.deliverProof(ResolutionProofParser.createResolutionProofTree(files.tmpProofFile.toString(), state.getSatInstance()));
                    }
                    files.deleteTempFiles();
                    if (state.usesMetaLearning())
                    {
                        state.learnMetaUnits(uc);
                        if (state.usesBlocks())
                        {
                            //we ensure that the meta instance can compactly represent the new UC
                            TreeSet<Integer> metaBlock = new TreeSet<Integer>();
                            int numClauses = state.getStatistics().numClausesOrGroups;
                            for (int i = 1; i <= numClauses; i++)
                            {
                                if (!result.getUc().contains(i))
                                {
                                    metaBlock.add(i);
                                }
                            }
                            state.learnMetaBlock(metaBlock);
                        }
                    }
                    if (getPanel() != null) getPanel().requestViewUpdate();
                }
                state.updateDecisionNode(ucTask.ucID);
                //TODO: optionally select the new step in case of a successful reduction
                if (state.usesBlocks())
                {
                    state.getKahina().getGUI().getViewByID("currentUCBlocks").getModel().requireUpdate();
                }
                state.getKahina().getGUI().getViewByID("currentUC").requireRedraw();
                if (!ucTask.isDummyTask())
                {
                    SwingUtilities.invokeAndWait(new Runnable() 
                    {
                        @Override
                        public void run() 
                        {
                            state.getKahina().dispatchInstanceEvent(new KahinaUpdateEvent(state.getSelectedStepID()));
                        }
                    });
                    state.getKahina().dispatchInstanceEvent(new KahinaRedrawEvent());
                }
                startNextReduction();
            }
            else
            {
                System.err.println("ERROR: UCReducer has completed a task that is not an UCReductionTask???");
            }
        }
        catch (NullPointerException e)
        {
            System.err.println("WARNING: caught NullPointerException in UCReducer.taskFinished():");
            e.printStackTrace();
        }
        catch (InvocationTargetException e)
        {
            System.err.println("WARNING: InvocationTargetException in UCReducer.taskFinished():");
            e.printStackTrace();
        }
        catch (InterruptedException e)
        {
            System.err.println("WARNING: InterruptedException in UCReducer.taskFinished():");
            e.printStackTrace();
        }
        state.getKahina().getLogger().endMeasuring("for finishing task " + task);
        super.taskFinished(task);
    }
    
    private void startNextReduction()
    {
        state.getKahina().getLogger().startMeasuring();
        if (stopped)
        {
            if (getPanel() != null)
            {
                getPanel().displayStatus1("US reduction using " + heuristics.getName() + " stopped at size " + uc.getUc().size());
                getPanel().displayStatus2("after " + (numUNSATReductions + numSATReductions) 
                                   + " reductions (" + numSATReductions + " SAT, " 
                                   + numUNSATReductions + " UNSAT)" + " from size " + startSize + ". ");
            }
            return;
        }

        int candidate = heuristics.getNextCandidate();
        if (candidate == -1)
        {
            //we are done reducing, no further reductions are possible; this UCReducer has done its duty
            if (getPanel() != null)
            {
                getPanel().displayStatus1("MUS of size " + uc.getUc().size() + " found using " + heuristics.getName());
                getPanel().displayStatus2("after " + (numUNSATReductions + numSATReductions) 
                                   + " reductions (" + numSATReductions + " SAT, " 
                                   + numUNSATReductions + " UNSAT)" + " from size " + startSize + ". ");
                getPanel().displayCompletedState();
                stopped = true;
                getPanel().repaint();
            }
        }
        else
        {
            List<Integer> candidates = new LinkedList<Integer>();
            candidates.add(candidate);
            //System.err.println(this + ": Reducing " + candidate + " at step " + ucID);
            if (getPanel() != null)
            {
                getPanel().displayStatus1("US of size " + uc.getUc().size() + ", " + heuristics.getName() + " attempting to reduce clause " + candidate + ".");
                getPanel().displayStatus2((numUNSATReductions + numSATReductions) 
                        + " reductions (" + numUNSATReductions + " UNSAT, " 
                        + numSATReductions + " SAT)" + " so far. ");
                getPanel().repaint();
            }
            Integer removalLink = uc.getRemovalLink(candidate);
            //real reduction attempt involving a SAT call
            //if the heuristic needs a proof, the SAT solver call must be repeated!
            if (removalLink == null || removalLink == -2 || (removalLink != -1 && heuristics.usesProofs()))
            {
                this.addTask(new ReductionTask(null, this, state.getStatistics(), uc, ucID, candidates, files, state.getSatInstance()));
            }
            //simulated reduction attempt informing heuristics about the clause's criticality
            else if (removalLink == -1)
            {
                this.addTask(new ReductionTask(null, this, state.getStatistics(), uc, ucID, candidates, uc, state.getSatInstance()));
            }
            //simulated reduction attempt informing heuristics about the new clause
            else
            {
                MUCStep newUC = state.retrieve(MUCStep.class, removalLink);
                this.addTask(new ReductionTask(null, this, state.getStatistics(), uc, ucID, candidates, newUC, state.getSatInstance()));
            }
        }    
    }

    public MiniSATFiles getFiles()
    {
        return files;
    }
    
    public ReductionHeuristic getHeuristics()
    {
        return heuristics;
    }
    
    public void setPanel(UCReducerPanel panel)
    {
        if (panel != null)
        {
            if (this.panel != null)
            {
                panel.displayStatus1(this.panel.getStatus1());
                panel.displayStatus2(this.panel.getStatus2());
    
            }
            if (stopped) panel.displayCompletedState();
            this.panel = panel;
            panel.setReducer(this);
        }
        else
        {
            this.panel = null;
        }
    }
    
    public void setHeuristics(ReductionHeuristic heuristics)
    {
        this.heuristics = heuristics;
        heuristics.setNewUC(uc);
    }

    public void setSignalColor(Color signalColor)
    {
        this.signalColor = signalColor;
        this.getPath().setColor(signalColor);
        if (getPanel() != null)
        {
            getPanel().displayColor(signalColor);
        }
    }

    public Color getSignalColor()
    {
        return signalColor;
    }

    public ColoredPath getPath()
    {
        return path;
    }
    
    public void cancelTasks()
    {
        stopped = true;
    }

    public UCReducerPanel getPanel()
    {
        return panel;
    }
}