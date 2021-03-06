package org.kahina.logic.sat.io.cnf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.kahina.logic.sat.data.cnf.CnfSatInstance;

public class DimacsCnfParser
{
    public static CnfSatInstance parseDimacsCnfFile(String fileName)
    {
        CnfSatInstance sat = new CnfSatInstance();
        try
        {
            Scanner in = new Scanner(new FileInputStream(fileName));

            //ignore comment header
            String problemLine = in.nextLine();
            while (problemLine.startsWith("c"))
            {
                problemLine = in.nextLine();
            }
            //process the problem line   
            String[] params = problemLine.split(" ");
            if (!params[0].equals("p"))
            {
                System.err.println("ERROR: Dimacs CNF file appears to miss the problem line!");
                System.err.println("       Returning empty SAT instance!");
                return sat;
            }
            if (!params[1].equals("cnf"))
            {
                System.err.println("ERROR: Parsing a non-CNF Dimacs file with the Dimacs CNF parser!");
                System.err.println("       Returning empty SAT instance!");
            }
            
            String currentLine;
            String[] tokens;
            List<Integer> currentClause = new LinkedList<Integer>();
            //read in clauses and comment lines which encode symbol definitions
            int lineID = 0;
            while (in.hasNext())
            {
            	//Dimacs splites information with a 0, however nearly everybody implements it with a new line            	
                currentLine = in.nextLine();
                lineID++;
                //System.err.println("line #" + lineID);
                tokens = currentLine.split("\\s");
                if (tokens[0].equals("c"))
                {
                    //check whether the comment is a symbol definition
                    if (tokens.length == 3)
                    {
                        sat.setSymbolMapping(Integer.parseInt(tokens[1]), tokens[2]);
                    }
                    else
                    {
                        //ignore other comments
                    }
                }
                else
                {
                    for (int i = 0; i < tokens.length; i++)
                    {
                        //System.err.println("  token #" + i + ": " + tokens[i]);
                        Integer literal = Integer.parseInt(tokens[i]);
                        if (literal == 0)
                        {
                            sat.addClause(currentClause);
                            currentClause = new LinkedList<Integer>();
                            break;
                        }
                        else
                        {
                            currentClause.add(literal);
                        }
                    }      
                }
            }
        }
        catch (FileNotFoundException e)
        {
            System.err.println("ERROR: Dimacs CNF file not found: " + fileName);
            System.err.println("       Returning empty SAT instance!");
        }
        return sat;
    }
}
