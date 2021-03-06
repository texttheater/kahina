package org.kahina.logic.sat.muc.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.kahina.logic.sat.data.cnf.CnfSatInstance;
import org.kahina.logic.sat.data.cnf.GroupCnfSatInstance;
import org.kahina.logic.sat.io.cnf.DimacsCnfParser;
import org.kahina.logic.sat.io.minisat.MiniSAT;
import org.kahina.logic.sat.io.minisat.MiniSATFiles;
import org.kahina.logic.sat.muc.MUCInstance;
import org.kahina.logic.sat.muc.MetaLearningMode;
import org.kahina.logic.sat.muc.bridge.MUCBridge;
import org.kahina.logic.sat.muc.bridge.MUCInstruction;
import org.kahina.logic.sat.muc.data.MUCStatistics;
import org.kahina.logic.sat.muc.io.MUCExtension;

public class MinUnsatCore
{
    public static boolean kahina = false;
    public static final boolean VERBOSE = false;
    
    public static List<Integer> computeRandomMUCs(int numMUCs, String sourceFileName, String targetFileName, boolean group, long timeout)
    {
        List<Integer> sizes = new LinkedList<Integer>();
        HashSet<String> agenda = new HashSet<String>();
        long startTime = System.currentTimeMillis();
        MiniSATFiles files = new MiniSATFiles();
        files.sourceFile = new File(sourceFileName);
        files.createExtendedFile(targetFileName);
        files.createTargetFile(targetFileName);
        files.createTempFiles(targetFileName);
        MUCStatistics stat = new MUCStatistics();
        stat.instanceName = files.sourceFile.getName();
        if (!group)
        {
            MUCExtension.extendCNFBySelVars(files.sourceFile, files.tmpFile, stat);
        }
        else
        {
            MUCExtension.extendGroupCNFBySelVars(files.sourceFile, files.tmpFile, stat);
        }
        Set<Integer> stillToTryClauses = new HashSet<Integer>();
        for (int i = 0; i < stat.numClausesOrGroups; i++)
        {
            stillToTryClauses.add(i);
        }
        List<Integer> unknownStateClauses = new LinkedList<Integer>();
        for (int i = 0; i < stat.numClausesOrGroups; i++)
        {
            unknownStateClauses.add(i);
        }
        HashSet<Integer> knownToBeCritical = new HashSet<Integer>();
        for (int i = 0; i < numMUCs; i++)
        {  
            int reductionIndex = 0;
            if (unknownStateClauses.size() == 0)
            {
                if (stillToTryClauses.size() == 0)
                {
                    return sizes;
                }
                reductionIndex = (int) (Math.random() * stat.numClausesOrGroups);
            }
            else
            {
                reductionIndex = unknownStateClauses.get((int) (Math.random() * unknownStateClauses.size()));
            }
            stillToTryClauses.remove(new Integer(reductionIndex));
            //System.err.println("unkownState: " + unknownStateClauses.size() + " stillToTry: " + stillToTryClauses.size());
            if (VERBOSE) System.err.print("Reducing index " + reductionIndex + " ... ");
            TreeSet<Integer> mus = computeMUC(files, stat, startTime, timeout, group, reductionIndex);
            if (mus != null)
            {
                if (mus.size() == 0)
                {
                    if (VERBOSE) System.err.println(" satisfiable! Repeat ...");
                    knownToBeCritical.add(reductionIndex);
                    unknownStateClauses.remove(new Integer(reductionIndex));
                    i--;
                }
                else
                {
                    if (VERBOSE) System.err.println(" MUS size " + mus.size());
                    if (!agenda.contains(mus.toString()))
                    {
                        sizes.add(mus.size());
                        agenda.add(mus.toString());
                    }
                    for (int j = 0; j < unknownStateClauses.size(); j++)
                    {
                        if (!mus.contains(unknownStateClauses.get(j)))
                        {     
                            unknownStateClauses.remove(j);
                            j--;
                        }
                    }
                }
            }
        }
        stat.runtime = System.currentTimeMillis() - startTime;
        //stat.printNicely(files.targetFile);
        files.deleteTempFiles();
        //System.err.println("SAT Solver Calls: " + stat.numSATCalls);
        //System.err.println("Initial number of relevant assumptions: " + stat.initNumRelAsm);
        //System.err.println("Decrease in number of relevant assumptions: " + (stat.initNumRelAsm - stat.mucSize - stat.mucCandSize));
        return sizes;
    }
    
    public static void computeMUC(String sourceFileName, String targetFileName, boolean group, long timeout)
    {
        long startTime = System.currentTimeMillis();
        MiniSATFiles files = new MiniSATFiles();
        files.sourceFile = new File(sourceFileName);
        files.createExtendedFile(targetFileName);
        files.createTargetFile(targetFileName);
        files.createTempFiles(targetFileName);
        MUCStatistics stat = new MUCStatistics();
        stat.instanceName = files.sourceFile.getName();
        if (!group)
        {
            MUCExtension.extendCNFBySelVars(files.sourceFile, files.tmpFile, stat);
        }
        else
        {
            MUCExtension.extendGroupCNFBySelVars(files.sourceFile, files.tmpFile, stat);
        }
        List<Integer> unknownStateClauses = new LinkedList<Integer>();
        for (int i = 0; i < stat.numClausesOrGroups; i++)
        {
            unknownStateClauses.add(i);
        }
        computeMUC(files, stat, startTime, timeout, group, unknownStateClauses.get((int) (Math.random() * unknownStateClauses.size())));
        stat.runtime = System.currentTimeMillis() - startTime;
        //stat.printNicely(files.targetFile);
        files.deleteTempFiles();
        //System.err.println("SAT Solver Calls: " + stat.numSATCalls);
        //System.err.println("Initial number of relevant assumptions: " + stat.initNumRelAsm);
        //System.err.println("Decrease in number of relevant assumptions: " + (stat.initNumRelAsm - stat.mucSize - stat.mucCandSize));
    }

    private static TreeSet<Integer> computeMUC(MiniSATFiles files, MUCStatistics stat, long startTime, long timeout, boolean group, int firstReduction)
    {
        MUCBridge bridge = null;
        if (kahina)
        {
            if (group)
            {
                GroupCnfSatInstance satInstance = GroupCnfSatInstance.parseDimacsGroupCnfFile(files.sourceFile.getAbsolutePath());
                System.err.println("Starting Kahina for MinUnsatCore on group SAT instance at " + files.sourceFile.getAbsolutePath());
                System.err.println("  Instance Size: (" + satInstance.getSize() + "," + satInstance.getHighestVar() + "," + satInstance.getNumGroups() + ")");
                MUCInstance kahinaInstance = new MUCInstance(MetaLearningMode.BLOCK_PARTITION, satInstance, stat, files);
                bridge = kahinaInstance.startNewSession();
            }
            else
            {
                CnfSatInstance satInstance = DimacsCnfParser.parseDimacsCnfFile(files.sourceFile.getAbsolutePath());
                System.err.println("Starting Kahina for MinUnsatCore on SAT instance at " + files.sourceFile.getAbsolutePath());
                System.err.println("  Instance Size: (" + satInstance.getSize() + "," + satInstance.getHighestVar() + ")");
                MUCInstance kahinaInstance = new MUCInstance(MetaLearningMode.BLOCK_PARTITION, satInstance, stat, files);
                bridge = kahinaInstance.startNewSession();
            }
        }
        Integer howmuchsmaller = 0, alreadyused = -10, rel_asm_last = 0;
        List<Integer> muc_cands = new ArrayList<Integer>();
        TreeSet<Integer> muc = new TreeSet<Integer>();
        int[] freezeVariables = new int[stat.numVarsExtended - stat.highestID];
        Arrays.fill(freezeVariables, 1);
        freezeVariables[firstReduction] = -1;
        try
        {
            MiniSAT.createFreezeFile(freezeVariables, files.tmpFreezeFile, stat.highestID + 1);
            MiniSAT.solve(files.tmpFile, files.tmpProofFile, files.tmpResultFile, files.tmpFreezeFile);
        }
        catch (Exception e)
        {
            System.out.println("Timeout");
            files.deleteTempFiles();
            files.targetFile.delete();
            System.err.println("Timeout: " + files.sourceFile.getAbsolutePath());
            if (!kahina) System.exit(0);
        }
        //System.out.println("Solver finished");
        // if unsatisfiable
        if (MiniSAT.wasUnsatisfiable())
        {
            List<Integer> relevantAssumptions = MiniSAT.getRelevantAssumptions(freezeVariables, stat.highestID + 1);
            stat.initNumRelAsm = relevantAssumptions.size();
            rel_asm_last = relevantAssumptions.size();
            if (VERBOSE) System.err.println("unsat: relAssumptions: " + relevantAssumptions);
            for (Integer a : relevantAssumptions)
            {
                muc_cands.add(a);
            }
            relevantAssumptions.clear();
            if (kahina)
            {
                bridge.registerMUC(muc_cands.toArray(new Integer[0]), muc.toArray(new Integer[0]));
            }
            //loop as long as Kahina is open; internal code sets kahina to false if it is closed
            while (kahina || muc_cands.size() > 0)
            {
                if ((System.currentTimeMillis() - startTime) > timeout)
                {
                    //do not use any timeout when using kahina!
                    if (!kahina) return null;
                }
                //System.out.println("");

                Integer k = 0;
                if (kahina)
                {
                    MUCInstruction instr = getNextMUCInstruction(bridge);
                    muc_cands.clear();
                    muc.clear();
                    for (int i : instr.step.getUc())
                    {
                        if (instr.step.getIcStatus(i) == 2)
                        {
                            muc.add(i);
                        }
                        else
                        {
                            muc_cands.add(i);
                        }
                    }
                    k = instr.selCandidate;
                }
                else
                {
                    //int idx = muc_cands.get(muc_cands.size() - 1);
                    
                    int idx = (int) (Math.random() * muc_cands.size());
                    k = muc_cands.get(idx); 
                    if (VERBOSE) System.err.println("Tested Clause: " + k + " at " + (idx + 1) + "/" + muc_cands.size() + " " + muc_cands);
                }
                if (alreadyused == k)
                {
                    System.err.println("alreadused == k, which should not happen");
                    System.err.println(howmuchsmaller);
                    System.exit(0);
                }
                alreadyused = k;
                muc_cands.remove(new Integer(k));
                changeFreezeVariables(freezeVariables, muc_cands, muc);
                long time2 = System.currentTimeMillis();
                try
                {
                    MiniSAT.createFreezeFile(freezeVariables, files.tmpFreezeFile, stat.highestID + 1);
                    MiniSAT.solve(files.tmpFile, files.tmpProofFile, files.tmpResultFile, files.tmpFreezeFile);
                }
                catch (Exception e)
                {
                    System.err.println("Timeout");
                    files.deleteTempFiles();
                    files.targetFile.delete();
                    System.err.println("Timeout: " + files.sourceFile.getAbsolutePath());
                    System.exit(0);
                }
                //System.err.println("DauerSolver: " + (System.currentTimeMillis() - time2));
                //System.err.flush();
                stat.numSATCalls++;
                // if satisfiable
                if (!MiniSAT.wasUnsatisfiable())
                {
                    muc.add(k);
                    if (VERBOSE) System.err.println("sat!");
                    if (kahina)
                    {
                        bridge.registerSatisfiable();
                    }
                    stat.numSAT++;
                }
                else
                {
                    howmuchsmaller++;
                    stat.numUNSAT++;
                    relevantAssumptions = MiniSAT.getRelevantAssumptions(freezeVariables, stat.highestID + 1);
                    stat.registerNumRemovedClauses(rel_asm_last - relevantAssumptions.size());
                    rel_asm_last = relevantAssumptions.size();
                    if (VERBOSE) System.err.println("unsat: relAssumptions: " + relevantAssumptions);
                    muc_cands = new ArrayList<Integer>();
                    for (Integer a : relevantAssumptions)
                    {
                        if (!muc.contains(a))
                        {
                            muc_cands.add(a);
                        }
                    }
                    if (kahina)
                    {
                        bridge.registerMUC(muc_cands.toArray(new Integer[0]), muc.toArray(new Integer[0]));
                    }
                }
                relevantAssumptions.clear();
            }
        }
        else
        {
            //System.err.println("Problem is satisfiable. No minimal unsatisfiable core available!");
        }
        stat.mucSize = muc.size();
        stat.mucCandSize = muc_cands.size();
        //System.err.println("Minimization complete! mucSize = " + muc.size());
        //System.err.println("Minimization complete! muc = " + muc.toString());
        while (kahina)
        {
            getNextMUCInstruction(bridge);
        }
        return muc;
    }

    private static void changeFreezeVariables(int[] freezeVariables, List<Integer> muc_cands, Set<Integer> muc)
    {
        Arrays.fill(freezeVariables, -1);
        for (Integer a : muc_cands)
        {
            freezeVariables[a] = 1;
        }
        for (Integer a : muc)
        {
            freezeVariables[a] = 1;
        }
    }
    
    private static MUCInstruction getNextMUCInstruction(MUCBridge bridge)
    {
        System.err.println("MinUnsatCore.getNextMUCInstruction()");
        MUCInstruction instr = null;
        while (instr == null)
        {
            try
            {
                Thread.sleep(100);
            }
            catch (InterruptedException e)
            {
                
            }
            instr = bridge.getNextInstruction();
        }
        return instr;
    }

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        if (args.length < 3)
        {
            System.out.println("Benoetigte Argumente:  Quelldatei, Zieldatei, Gruppen?(0=Nein, 1=Ja)");
        }
        else
        {
            if (args[2].equals("0"))
            {
                MinUnsatCore.computeRandomMUCs(10, args[0], args[1], false, 14400000);
            }
            else
            {
                MinUnsatCore.computeMUC(args[0], args[1], true, 14400000);
            }
        }
        System.exit(0);
    }
}
