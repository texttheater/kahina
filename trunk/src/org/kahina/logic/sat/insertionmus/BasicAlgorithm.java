package org.kahina.logic.sat.insertionmus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.TimeoutException;

import org.kahina.logic.sat.data.cnf.CnfSatInstance;
import org.kahina.logic.sat.io.cnf.DimacsCnfOutput;
import org.kahina.logic.sat.io.cnf.DimacsCnfParser;
import org.kahina.logic.sat.io.minisat.FreezeFile;
import org.kahina.logic.sat.io.minisat.MiniSAT;
import org.kahina.logic.sat.muc.data.MUCStatistics;
import org.kahina.logic.sat.muc.io.MUCExtension;
/**
 * Implementation of the insertion algorithm of J.K. de Siqueira N. and J.-F. Puget 
 * "Explanation-based generalisation of failures"
 * @author Seitz
 *
 */
public class BasicAlgorithm {

	protected CnfSatInstance instance; // The instance
	
	protected TreeSet<Integer> instanceIDs = new TreeSet<Integer>();
	protected TreeSet<Integer> M = new TreeSet<Integer>();// will become the MUS
	protected TreeSet<Integer> S = new TreeSet<Integer>(); // A subset of the instance, it is the subset currently looked at.

	protected int[] freeze; //variables that should be freezed are marked with 1;


	protected boolean finished = false;
	
	File instanceFile;

	public BasicAlgorithm(CnfSatInstance instance){
		this.instance = instance;
		for (int i = 0; i < instance.getHighestVar(); i++){
			instanceIDs.add(i);
		}
		
		MUCStatistics stat = new MUCStatistics();
		stat.instanceName = "../cnf/aim-100-1_6-no-1.cnf";

		MUCExtension.extendCNFBySelVars(new File("../cnf/aim-100-1_6-no-1.cnf"), new File("output.cnf"), stat); 
				
		this.instanceFile  = new File("output.cnf");

		freeze = new int[this.instance.getHighestVar()];
		Arrays.fill(freeze, FreezeFile.FREEZE);
	}
	/**
	 * 	is the MUS calculated?
	 * @return true if there is a MUS calculated
	 */
	public boolean isFinished(){
		return finished;
	}


	/**
	 * runs the next step of this algorithm
	 * precondition: this.instance.getSize() > 0
	 * @param clauseIndex the index of the clause that should be handled next.
	 * @return true if an MUS is found.
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws TimeoutException 
	 */
	public boolean selectNext(int clauseID) throws TimeoutException, InterruptedException, IOException{

		S.add(clauseID);
		instanceIDs.remove(clauseID);
		
		freeze[clauseID] = FreezeFile.UNFREEZE;

		
		File freezeFile = new File("freeze"+ Thread.currentThread().getId() + ".fr");
		
		FreezeFile.createFreezeFile(freeze, freezeFile, instance.getHighestVar()+1);
		File resultFile = new File("result");
		MiniSAT.solve(this.instanceFile, new File("proof") , resultFile, freezeFile);
		

		if (MiniSAT.wasUnsatisfiable(resultFile)){
			//if M united S is not SAT then the clause is part of the MUS
			System.out.println("UNSAT");
			M.add(clauseID);
			S.remove(clauseID);
			
			for (int f: S){
				freeze[f] = FreezeFile.FREEZE;
			}

			this.instanceIDs = S;
			this.S = new TreeSet<Integer>();
		}

		if (instanceIDs.size() == 0){
			//if the instance is empty then we are done.
			return true;
		}
		return false;
	}

	/**
	 * Simple testfunction for this algorithm.
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws TimeoutException 
	 */
	public static void main(String[] arg0) throws TimeoutException, InterruptedException, IOException{
		String path = "../cnf/aim-100-1_6-no-1.cnf";
		CnfSatInstance instance = DimacsCnfParser.parseDimacsCnfFile(path);

		BasicAlgorithm alg = new BasicAlgorithm(instance);

		while (!alg.selectNext(alg.instanceIDs.pollFirst())){
//			System.out.println(alg.instanceIDs.size());
		};
		System.out.println("Found a MUS");
//		DimacsCnfOutput.writeDimacsCnfFile("MUS.tmp.cnf", alg.getMUS());
		
		ArrayList<Integer> clauseIDs = new ArrayList<Integer>();
		for (int i = 0; i < alg.freeze.length; i++){
			if (alg.freeze[i] == FreezeFile.UNFREEZE){
				clauseIDs.add(i+1);
			}
		}
		DimacsCnfOutput.writeDimacsCnfFile("MUS.cnf", instance.selectClauses(clauseIDs));
	}
}
