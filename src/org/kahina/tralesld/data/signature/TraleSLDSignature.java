package org.kahina.tralesld.data.signature;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.kahina.core.data.KahinaObject;

public class TraleSLDSignature extends KahinaObject
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 7411049956894696227L;

	//types and features can simply be stored as strings because they have unique names
	//these lists define the order in which they are displayed
	List<String> types;
	List<String> features;
	
	//the immediate subtypes and supertypes for each type are stored here
	//this also implicitly encodes the sibling relation used by the signature-enhanced editor
	Map<String,Set<String>> subtypes;
	Map<String,Set<String>> supertypes;
	
	//store the paths for each type
	Map<String,Set<List<String>>> paths;
	
	//store the type where each feature for each type was introduced
	Map<String,Map<String,String>> featIntroType;
	//inherited features are listed again when a type restriction is tightened
	Map<String,Map<String,String>> typeRestr;
	//store the features whose value can be of a given type
	Map<String,Set<String>> usage;
	
	public TraleSLDSignature()
	{

		types = new LinkedList<String>();
		features = new LinkedList<String>();
		subtypes = new HashMap<String,Set<String>>();
		supertypes = new HashMap<String,Set<String>>();
		paths = new HashMap<String,Set<List<String>>>();
		featIntroType = new HashMap<String,Map<String,String>>();
		typeRestr = new HashMap<String,Map<String,String>>();
		usage = new HashMap<String,Set<String>>();
		
		//the empty signature contains the root type "bot" without features,
		//and a special pseudo-type (atom) to represent unconstrained strings
		addSubtypeRelation("bot","(atom)");
	}
	
	public void registerFeature(String feature)
	{
		features.add(feature);
	}
	
	public void registerType(String type)
	{
		types.add(type);
		subtypes.put(type, new HashSet<String>());
		supertypes.put(type, new HashSet<String>());
		paths.put(type, new HashSet<List<String>>());
		featIntroType.put(type, new HashMap<String,String>());
		typeRestr.put(type, new HashMap<String,String>());
		usage.put(type, new HashSet<String>());
	}

	public void addSubtypeRelation(String type, String subtype) 
	{
		if (!types.contains(type))
		{
			registerType(type);
		}
		if (!types.contains(subtype))
		{
			registerType(subtype);
		}
		subtypes.get(type).add(subtype);
		supertypes.get(subtype).add(type);
	}
	
	public void addAppropriateFeature(String type, String feature, String valueRestr) 
	{
		if (!types.contains(type))
		{
			registerType(type);
		}
		if (!types.contains(valueRestr))
		{
			registerType(valueRestr);
		}
		if (!features.contains(feature))
		{
			registerFeature(feature);
		}
		typeRestr.get(type).put(feature,valueRestr);
		usage.get(valueRestr).add(type + ":" + feature);
	}
	
	public List<String> getTypes()
	{
		return types;
	}
	
	public List<String> getFeatures()
	{
		return features;
	}
	
	public Set<String> getSubtypes(String type)
	{
		return subtypes.get(type);
	}
	
	public Set<String> getSupertypes(String type)
	{
		return supertypes.get(type);
	}
	
	public Set<String> getSiblingTypes(String type)
	{
		HashSet<String> siblingTypes = new HashSet<String>();
		Set<String> supertypes = getSupertypes(type);
		if (supertypes != null)
		{
			for (String supertype : getSupertypes(type))
			{
				siblingTypes.addAll(getSubtypes(supertype));
			}
			siblingTypes.remove(type);
		}
		return siblingTypes;
	}
	
	public Map<String,String> getTypeRestrictions(String type)
	{
		return typeRestr.get(type);
	}
	
	public Set<String> getUses(String type)
	{
		return usage.get(type);
	}
	
	public Map<String,String> getAppropriateness(String type)
	{
		Map<String,String> appro = typeRestr.get(type);
		if (appro == null) appro = new HashMap<String,String>();
		return appro;
	}
	
	public String getAppropriateValueType(String type, String feat)
	{
		Map<String,String> appro = getAppropriateness(type);
		String val = appro.get(feat);
		if (val == null) return "bot";
		return val;
	}
	
	public Set<List<String>> getPaths(String type)
	{
		return paths.get(type);
	}
	
	public boolean dominates(String domer, String domee)
	{
		if (domer.equals(domee)) return true;
		if (getSupertypes(domee) == null) return false;
		for (String supertype : getSupertypes(domee))
		{
			if (dominates(domer, supertype)) return true;
		}
		return false;
	}
	
	/**
	 * Compute the most general satisfier of a type as a GRISU string.
	 * @param type a type defined in this signature.
	 * @return a GRISU string representing the MGS.
	 */
	public String computeGrisuMGS(String type)
	{
		if (!types.contains(type))
		{
			return "ERROR: no type information in signature!";
		}
		String output = "(S1(0\"" + type + "\")";
		Map<String,String> appropFeats = typeRestr.get(type);
		//TODO: inefficient; precompute lists or entire MGSs
		List<String> appropFeatsList = new LinkedList<String>();
		appropFeatsList.addAll(appropFeats.keySet());
		Collections.sort(appropFeatsList);
		for (String feat : appropFeatsList)
		{
			output += "(V2\"" + feat + "\"" + computeGrisuMGS(appropFeats.get(feat)) + ")";
		}
		output += ")";
		return output;
	}
	
	/**
	 * Resolves mgsat/1 instances in a GRISU string
	 * @param grisuString
	 * @return the grisuString with mgsat/1 clauses resolved
	 */
	public String resolveMGSs(String grisu)
	{
		while (grisu.contains("mgsat("))
		{
			int mgsExpStart = grisu.indexOf("mgsat(");
			//extract the type name
			int mgsExpEnd = grisu.indexOf(")", mgsExpStart);
			String type = grisu.substring(mgsExpStart + 6, mgsExpEnd);
			String mgsGrisu = computeGrisuMGS(type);
			//find the left and right boundaries of the replacement string
			int repExpStart = grisu.lastIndexOf("(",grisu.lastIndexOf("(", mgsExpStart) - 1);
			int repExpEnd = grisu.indexOf(")",grisu.indexOf(")", mgsExpEnd + 1) + 1) + 1;
			//insert MGS GRISU string for (S1(0"mgsat(type)"))
			grisu = grisu.substring(0,repExpStart) + mgsGrisu + grisu.substring(repExpEnd);
		}
		return grisu;
	}
	
	public String getIntroducer(String type, String feat)
	{
		Map<String,String> feats = featIntroType.get(type);
		if (feats == null) return null;
		return feats.get(feat);
	}
	
	/**
	 * Fills the introFeats map. 
	 * This needs to be called once for the visualization to work.
	 */
	//TODO: precompute and cache more type information:
	//		- an indexing structure that directly encodes where features come from
	//		- another indexing structure that makes all type restrictions for a feature explicit
	public void inferCachedInformation()
	{
		//generate path information via a depth-first traversal of the type hierarchy
		LinkedList<String> botPath = new LinkedList<String>();
		botPath.add("bot");
		paths.get("bot").add(botPath);
		for (String type : subtypes.get("bot"))
		{
			buildPaths(paths.get("bot"), type);
		}
		
		//TODO: take tightening of appropriateness conditions into account
		
		//fill featIntroType in a rather brute-force manner
		//for each appropriateness condition ...
		for (String type : types)
		{
			for (String feat : typeRestr.get(type).keySet())
			{
				//... find out the type where it was introduced
				featIntroType.get(type).put(feat, findIntroducer(type,feat));
			}
		}
		
		//enrich usage information to also include subtypes of value restrictions
		for (String type : types)
		{
			for (String feat : typeRestr.get(type).keySet())
			{
				if (featIntroType.get(type).get(feat).equals(type))
				{
					List<String> subtypeList = new LinkedList<String>();
					subtypeList.add(typeRestr.get(type).get(feat));
					while (subtypeList.size() > 0)
					{
						String subtype = subtypeList.remove(0);
						usage.get(subtype).add(type + ":" + feat);
						subtypeList.addAll(subtypes.get(subtype));
					}
				}
			}
		}
	}
	
	private String findIntroducer(String type, String feat)
	{
		for (String supertype : supertypes.get(type))
		{
			if (typeRestr.get(supertype).get(feat) != null)
			{
				return findIntroducer(supertype, feat);
			}
		}
		return type;
	}
	
	private void buildPaths(Set<List<String>> superpaths, String type)
	{
		Set<List<String>> newPaths = new HashSet<List<String>>();
		for (List<String> superpath : superpaths)
		{
			List<String> path = new LinkedList<String>();
			path.addAll(superpath);
			path.add(type);
			newPaths.add(path);
		}
		paths.get(type).addAll(newPaths);
		for (String subtype : subtypes.get(type))
		{
			buildPaths(newPaths, subtype);
		}
	}
	
	/**
	 * Prints out the signature in formal notation according to Carpenter 1992.
	 * @return the formal representation as a String;
	 */
	public String formalRepresentation()
	{
		StringBuilder builder = new StringBuilder("⟨");
		//print set of types in alphabetical order
		List<String> typeList = new LinkedList<String>();
		typeList.addAll(types);
		Collections.sort(typeList);
		builder.append("{");
		for (String type : typeList)
		{
			builder.append(type + ",");		
		}
		builder.deleteCharAt(builder.length() - 1);
		builder.append("}");
		//print out pairs of subsumption relation in alphabetical order (brute force!)
		builder.append("{");
		for (String type1 : typeList)
		{
			for (String type2 : typeList)
			{
				if (dominates(type1, type2))
				{
					builder.append("(" + type1 + "," + type2 + "),");
				}
			}
		}
		builder.deleteCharAt(builder.length() - 1);
		builder.append("}");
		//collect all features and print out feature set
		Set<String> feats = new HashSet<String>();
		for (String type : types)
		{
			feats.addAll(typeRestr.get(type).keySet());
		}
		List<String> featList = new LinkedList<String>();
		featList.addAll(feats);
		Collections.sort(featList);
		builder.append("{");
		for (String feat : featList)
		{
			builder.append(feat + ",");		
		}
		builder.deleteCharAt(builder.length() - 1);
		builder.append("}");
		//print out appropriateness function (again brute force!)
		builder.append("{");
		for (String feat : featList)
		{
			for (String type : typeList)
			{
				String valType = typeRestr.get(type).get(feat);
				if (valType != null)
				{
					builder.append("(" + feat + "," + type + ") ↦ " + valType + ",");
				}
			}
		}
		builder.deleteCharAt(builder.length() - 1);
		builder.append("}");
		builder.append("⟩");
		return builder.toString();
	}
	
	/**
	 * Prints out a graph representation in GraphViz DOT format.
	 * @return a representation of the signature in DOT format.
	 */
	public String graphViz()
	{
		StringBuilder builder = new StringBuilder("digraph signature{\n    node [shape=plaintext];\n");
		for (String type : types)
		{
			builder.append(type + " [label=<");
			builder.append(type + "<BR/>");
			List<String> featList = new LinkedList<String>();
			featList.addAll(typeRestr.get(type).keySet());
			Collections.sort(featList);
			for (String feat : featList)
			{
				builder.append(feat.toUpperCase() + ":" + typeRestr.get(type).get(feat) + "<BR/>");
			}
			builder.append(">];\n");
		}
		for (String type : types)
		{
			for (String subtype : subtypes.get(type))
			{
				builder.append("    " + type + " -> " + subtype + ";\n");
			}
		}
		builder.append("}\n");
		return builder.toString();
	}
}
