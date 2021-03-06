package org.kahina.tulipa.data.grammar;

import java.util.HashMap;

import org.kahina.core.data.KahinaObject;

public class TulipaGrammar extends KahinaObject
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 8954717071257944662L;
	public HashMap<Integer, String> rcgClauses;
	
	public TulipaGrammar()
	{
		rcgClauses = new HashMap<Integer, String>();
	}
	
	public void addClause(int clauseID, String rcgClause)
	{
		rcgClauses.put(clauseID, rcgClause);
	}
	
	public int getSize()
	{
		return rcgClauses.size();
	}
	
	public String getClause(int clauseID)
	{
		return rcgClauses.get(clauseID);
	}
}
