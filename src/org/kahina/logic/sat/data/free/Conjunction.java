package org.kahina.logic.sat.data.free;

import java.util.List;

public class Conjunction
{
    final List<BooleanFormula> fms;
    
    public Conjunction(List<BooleanFormula> fms) 
    {
        this.fms = fms;
    }
    
    @Override
    public String toString() 
    {
      StringBuffer s = new StringBuffer();
      s.append("(");
      String separator = "";
      for(BooleanFormula fm : fms) 
      {
        s.append(separator);
        s.append(fm.toString());
        separator = "+";
      }
      s.append(")");
      return s.toString();
    }
}
