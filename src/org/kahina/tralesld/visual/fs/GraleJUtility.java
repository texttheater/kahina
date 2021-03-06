package org.kahina.tralesld.visual.fs;

import gralej.om.EntityFactory;
import gralej.om.IAny;
import gralej.om.IEntity;
import gralej.om.IFeatureValuePair;
import gralej.om.IList;
import gralej.om.IRelation;
import gralej.om.ITag;
import gralej.om.IType;
import gralej.om.ITypedFeatureStructure;
import gralej.parsers.IDataPackage;
import gralej.parsers.ParseException;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.kahina.tralesld.data.signature.TraleSLDSignature;

public class GraleJUtility 
{
    static boolean verbose = false;
	static EntityFactory ef = EntityFactory.getInstance();
	static TraleSLDFeatureStructureEditor editor = null;
	
	public static void setFeatureStructureEditor(TraleSLDFeatureStructureEditor e)
	{
		editor = e;
	}
	
	public static IEntity spz(IEntity e, List<String> path, String ty, TraleSLDSignature sig)
	{
		IEntity et = delta(e,path);
		if (et == null)
		{
			failMsg("Specialize failed: Unable to evaluate address!");
			return e;
		}
		if (et instanceof ITag)
		{
			et = ((ITag) et).target();
		}
		String eType = getType(et);
		if (eType != null && sig.dominates(eType,ty))
		{
			if (eType.equals("list"))
			{
				IEntity parent = goUpToLast(e,path);
				if (parent instanceof ITag)
				{
					parent = ((ITag) parent).target();
				}
				IList list = ef.newList();
				if (ty.equals("ne_list"))
				{
					list.append(ef.newTFS("bot"));
				}
				successMsg("List specialization successful!");
				if (parent == null) return list;
				replace(parent,et,list);
			}
			else
			{
				setType(et,ty);
				successMsg("Type specialization successful!");
			}
		}
		else
		{
			failMsg("Specialize failed: Dominance condition violated!");
		}
		return e;
	}
	
	public static IEntity gez(IEntity e, List<String> path, String ty, TraleSLDSignature sig)
	{
		IEntity ent = delta(e,path);
		if (ent == null)
		{
			failMsg("Generalize failed: Unable to evaluate address!");
			return e;
		}
		if (ent instanceof ITag)
		{
			ent = ((ITag) ent).target();
		}
		String eType = getType(ent);
		if (eType != null && sig.dominates(ty,eType))
		{
			setType(ent,ty);
			successMsg("Type generalization successful!");
		}
		else
		{
			failMsg("Specialize failed: Dominance condition violated!");
		}
		return e;
	}
	
	public static IEntity swi(IEntity e, List<String> path, String ty, TraleSLDSignature sig)
	{
		IEntity ent = delta(e,path);
		if (ent == null)
		{
			failMsg("Switching failed: Unable to evaluate address!");
			return e;
		}
		if (ent instanceof ITag)
		{
			ent = ((ITag) ent).target();
		}
		String oldType = getType(ent);
		String commonSupertype = null;
		//find common ancestor
		for (String supertype : sig.getSupertypes(getType(ent)))
		{
			if (sig.getSubtypes(supertype).contains(ty))
			{
				commonSupertype = supertype;
				break;
			}
		}
		if (commonSupertype == null)
		{
			failMsg("Switching failed: No common supertype found for types " + oldType + " and " + ty + "!");
		}
		else
		{
			//use sequence of generalization and specialization (inefficient, but clean)
			gez(e,path,commonSupertype,sig);
			spz(e,path,ty,sig);
			successMsg("Type switching successful!");
		}
		return e;
	}
	
	public static IEntity changeAtom(IEntity e, List<String> path, String newName, TraleSLDSignature sig)
	{
		IEntity parent = goUpToLast(e,path);
		IEntity et = goLast(parent,path);
		if (parent == null) et = e;	
		if (et == null)
		{
			failMsg("Atom change failed: Unable to evaluate address!");
			return e;
		}
		if (et instanceof ITag)
		{
			parent = et;
			et = ((ITag) et).target();
		}
		if (et instanceof ITypedFeatureStructure)
		{
			IAny replacement = ef.newAny(newName);
			successMsg("Atom change successful!");
			if (parent == null) return replacement;
			replace(parent,et,replacement);
			return e;
		}
		else if (et instanceof IAny)
		{
			IAny atom = (IAny) et;
			atom.setValue(newName);
			successMsg("Atom change successful!");
			return e;
		}
		failMsg("Atom change failed: not possible in context " + et + ".");
		return e;
	}
	
	public static IEntity generalizeAtom(IEntity e, List<String> path, TraleSLDSignature sig)
	{
		IEntity parent = goUpToLast(e,path);
		IEntity et = goLast(parent,path);
		if (parent == null) et = e;	
		if (et == null)
		{
			failMsg("Atom generalization failed: Unable to evaluate address!");
			return e;
		}
		if (et instanceof ITag)
		{
			parent = et;
			et = ((ITag) et).target();
		}
		if (et instanceof IAny)
		{
			ITypedFeatureStructure replacement = ef.newTFS("bot");
			successMsg("Atom generalization successful!");
			if (parent == null) return replacement;
			replace(parent,et,replacement);
			return e;
		}
		else if (et instanceof ITypedFeatureStructure)
		{
			ITypedFeatureStructure fs = (ITypedFeatureStructure) et;
			fs.featureValuePairs().clear();
			fs.setType(ef.newType("bot"));
			successMsg("Atom generalization successful!");
			return e;
		}
		failMsg("Atom generalization failed: not possible in context " + et + ".");
		return e;
	}
    
    public static void tf(IEntity fs, TraleSLDSignature sig)
    {
        purge(fs,sig);
        typInf(fs,sig);
    }
    
    public static void ttf(IEntity fs, TraleSLDSignature sig)
    {
        purge(fs,sig);
        fill(fs,sig);
        typInf(fs,sig);
    }
    
    public static void purge(IEntity e, TraleSLDSignature sig)
    {
        if (e instanceof ITypedFeatureStructure)
        {
            ITypedFeatureStructure fs = (ITypedFeatureStructure) e;
            Map<String,String> appropFeats = sig.getTypeRestrictions(getType(fs));
            Map<String,IFeatureValuePair> featureMap = new HashMap<String,IFeatureValuePair>();
            for (IFeatureValuePair pair : fs.featureValuePairs())
            {
                featureMap.put(pair.feature(), pair);
            }
            for (IFeatureValuePair fv : featureMap.values())
            {
                if (appropFeats.get(fv.feature()) == null)
                {
                    fs.featureValuePairs().remove(fv);
                }
                else
                {
                    purge(fv.value(),sig);
                }
            }
        }
        else if (e instanceof ITag)
        {
            purge(((ITag) e).target(),sig);
        }
        else if (e instanceof IList)
        {
            for (IEntity ent : ((IList) e).elements())
            {
                purge(ent,sig);
            }
        }
    }
    
    public static void typInf(IEntity e, TraleSLDSignature sig)
    {
        if (e instanceof ITypedFeatureStructure)
        {
            ITypedFeatureStructure fs = (ITypedFeatureStructure) e;
            Map<String,String> appropFeats = sig.getTypeRestrictions(getType(fs));
            List<String> appropFeatsList = new LinkedList<String>();
            appropFeatsList.addAll(appropFeats.keySet());
            Collections.sort(appropFeatsList);
            Map<String,IFeatureValuePair> featureMap = new HashMap<String,IFeatureValuePair>();
            for (IFeatureValuePair pair : fs.featureValuePairs())
            {
                featureMap.put(pair.feature(), pair);
            }
            for (String feat : appropFeatsList)
            {
                IFeatureValuePair fv = featureMap.get(feat);
                if (fv != null)
                {
                    if (!sig.dominates(appropFeats.get(feat),getType(fv.value())))
                    {
                        List<String> path = new LinkedList<String>();
                        path.add(feat);
                        spz(e,path,appropFeats.get(feat),sig);
                    }
                    typInf(fv.value(),sig);
                }
            }
        }
        else if (e instanceof ITag)
        {
            typInf(((ITag) e).target(),sig);
        }
        else if (e instanceof IList)
        {
            for (IEntity ent : ((IList) e).elements())
            {
                typInf(ent,sig);
            }
        }
    }
    
    public static void fill(IEntity e, TraleSLDSignature sig)
    {
        if (e instanceof ITypedFeatureStructure)
        {
            ITypedFeatureStructure fs = (ITypedFeatureStructure) e;
            Map<String,String> appropFeats = sig.getTypeRestrictions(getType(fs));
            List<String> appropFeatsList = new LinkedList<String>();
            appropFeatsList.addAll(appropFeats.keySet());
            Collections.sort(appropFeatsList);
            Map<String,IFeatureValuePair> featureMap = new HashMap<String,IFeatureValuePair>();
            for (IFeatureValuePair pair : fs.featureValuePairs())
            {
                featureMap.put(pair.feature(), pair);
            }
            for (String feat : appropFeatsList)
            {
                IFeatureValuePair fv = featureMap.get(feat);
                IEntity value;
                if (fv == null)
                {
                    String type = appropFeats.get(feat);

                    if (type.equals("e_list"))
                    {
                        value = ef.newList();
                    }
                    else if (type.equals("ne_list"))
                    {
                        IList list = ef.newList();
                        list.append(ef.newTFS("bot"));
                        value = list;
                    }
                    else
                    {
                        value =  ef.newTFS(type);
                    }
                    fs.addFeatureValue(ef.newFeatVal(feat,value));
                }
                else
                {
                    value = fv.value();
                }
                fill(value,sig);
            }
        }
        else if (e instanceof ITag)
        {
            fill(((ITag) e).target(),sig);
        }
        else if (e instanceof IList)
        {
            for (IEntity ent : ((IList) e).elements())
            {
                fill(ent,sig);
            }
        }
    }
	
	public static IEntity fin(IEntity e, List<String> path, String feat, IEntity val, TraleSLDSignature sig)
	{
		IEntity parent = goUpToLast(e,path);
		IEntity et = goLast(parent,path);
		if (parent == null) et = e;	
		if (et == null)
		{
			failMsg("Feature introduction failed: Unable to evaluate address!");
			return e;
		}
		if (et instanceof ITag)
		{
			parent = et;
			et = ((ITag) et).target();
		}
		IFeatureValuePair fv = ef.newFeatVal(feat, val); 
		if (et instanceof IType)
		{
			List<IFeatureValuePair> fvList = new LinkedList<IFeatureValuePair>();
			fvList.add(fv);
			IType type = (IType) et;
			ITypedFeatureStructure replacement = ef.newTFS(type, fvList);
			successMsg("Feature introduction successful!");
			if (parent == null) return replacement;
			replace(parent,et,replacement);
			return e;
		}
		else if (et instanceof ITypedFeatureStructure)
		{
			ITypedFeatureStructure fs = (ITypedFeatureStructure) et;
			fs.addFeatureValue(fv);
			successMsg("Feature introduction successful!");
			return e;
		}
		failMsg("Feature introduction failed: not possible in context " + et + ".");
		return e;
	}
	
	public static IEntity fre(IEntity e, List<String> path, String feat)
	{
		IEntity et = goUpToLast(e,path);
		if (et == null)
		{
			failMsg("Feature removal failed: Unable to evaluate address!");
			return e;
		}
		if (et  instanceof ITypedFeatureStructure)
		{
			ITypedFeatureStructure fs = (ITypedFeatureStructure) et;
			for (int i = 0; i < fs.featureValuePairs().size(); i++)
			{
				if (fs.featureValuePairs().get(i).feature().equals(feat))
				{
					fs.featureValuePairs().remove(i);
					successMsg("Feature removal successful!");
					return e;
				}
			}
		}
		else
		{
			failMsg("Feature removal failed: not a typed feature structure!");
		}
		return e;
	}
	
	public static IEntity resetFeat(IEntity e, List<String> path, String feat, TraleSLDSignature sig)
	{
		IEntity parent = goUpToLast(e,path);
		IEntity et = goLast(parent,path);
		if (et instanceof ITag)
		{
			parent = et;
			et = ((ITag) et).target();
		}
		if (et == null || parent == null)
		{
			failMsg("Feature reset failed: Unable to evaluate addresses!");
			return e;
		}
		IEntity replacement = ef.newTFS(getType(et));
		replace(parent,et,replacement);
		successMsg("Feature reset successful!");
		return e;
	}
	
	public static IEntity ids(IEntity e, List<String> path)
	{
		Map<Integer,List<List<String>>> identities = getIdentities(e);
		IEntity parent = goUpToLast(e,path);
		IEntity et = goLast(parent,path);
		if (et == null)
		{
			failMsg("Identity dissolval failed: Unable to evaluate address!");
			return e;
		}
		if (et instanceof ITag)
		{
			ITag tag = (ITag) et;
			identities.get(tag.number()).remove(path);
			e = removeTag(e, path);
			if (identities.get(tag.number()).size() == 1)
			{
				List<String> otherPath = identities.get(tag.number()).get(0);
				e = removeTag(e,otherPath);
				identities.remove(tag.number());
			}
			successMsg("Identity dissolval successful!");
		}
		else
		{
			failMsg("Identity dissolval failed: No reentrancy at this address!");
		}
		return e;
	}
	
	private static IEntity removeTag(IEntity e, List<String> path)
	{
		IEntity parent = goUpToLast(e,path);
		IEntity et = goLast(parent,path);
		if (et == null)
		{
			failMsg("Tag removal failed: Unable to evaluate address!");
			return e;
		}
		if (et instanceof ITag)
		{
			ITag tag = (ITag) et;
			IEntity replacement = copy(tag.target());
			replace(parent,et,replacement);
		}
		else
		{
			failMsg("Tag removal failed: No tag at address " + path);
		}
		return e;
	}
	
	public static IEntity addListElement(IEntity e, List<String> path, int listIndex, TraleSLDSignature sig)
	{
		IEntity ent = delta(e,path);
		if (ent == null)
		{
			failMsg("List manipulation failed: Unable to evaluate address!");
			return e;
		}
		if (ent instanceof ITag)
		{
			ent = ((ITag) ent).target();
		}
		if (!(ent instanceof IList))
		{
			failMsg("List manipulation failed: No list at this address!");
			return e;
		}
		IEntity newElement = ef.newTFS("bot");
		IList list = (IList) ent;
		List<IEntity> listEl = new LinkedList<IEntity>();
		for (IEntity el : list.elements())
		{
			listEl.add(el);
		}
		listEl.add(listIndex, newElement);
		list.clear();
		for (IEntity el : listEl)
		{
			list.append(el);
		}
		successMsg("New list element successfully added.");
		return e;
	}
	
	public static IEntity removeListElement(IEntity e, List<String> path, int listIndex, TraleSLDSignature sig)
	{
		IEntity ent = delta(e,path);
		if (ent == null)
		{
			failMsg("List manipulation failed: Unable to evaluate address!");
			return e;
		}
		if (ent instanceof ITag)
		{
			ent = ((ITag) ent).target();
		}
		if (!(ent instanceof IList))
		{
			failMsg("List manipulation failed: No list at this address!");
			return e;
		}
		IList list = (IList) ent;
		List<IEntity> listEl = new LinkedList<IEntity>();
		for (IEntity el : list.elements())
		{
			listEl.add(el);
		}
		listEl.remove(listIndex);
		list.clear();
		for (IEntity el : listEl)
		{
			list.append(el);
		}
		successMsg("List element successfully removed.");
		return e;
	}
	
	public static IEntity clearList(IEntity e, List<String> path, TraleSLDSignature sig)
	{
		IEntity ent = delta(e,path);
		if (ent == null)
		{
			failMsg("List manipulation failed: Unable to evaluate address!");
			return e;
		}
		if (ent instanceof ITag)
		{
			ent = ((ITag) ent).target();
		}
		if (!(ent instanceof IList))
		{
			failMsg("List manipulation failed: No list at this address!");
			return e;
		}
		IList list = (IList) ent;
		list.clear();
		successMsg("List successfully cleared.");
		return e;
	}
	
	public static IEntity copy(IEntity e)
	{
		return e;
	}
	
	public static IEntity newAtom(String name)
	{
		return ef.newAny(name);
	}
	
	public static IEntity itd(IEntity e, List<String> path1, List<String> path2, TraleSLDSignature sig)
	{
		IEntity ent1 = delta(e,path1);
		IEntity ent2 = delta(e,path2);
		if (ent1 == null || ent2 == null)
		{
			failMsg("Identity introduction failed: Unable to evaluate address!");
			return null;
		}
		IEntity parent1 = goUpToLast(e,path1);
		IEntity parent2 = goUpToLast(e,path2);
		if (ent1 instanceof ITag)
		{
			parent1 = ent1;
			ent1 = ((ITag) ent1).target();
		}
		if (ent2 instanceof ITag)
		{
			parent2 = ent2;
			ent2 = ((ITag) ent2).target();
		}
		IEntity mgu = sigMGU(e,e,path1,path2,sig);
		if (mgu == null) return null;
        Map<Integer,List<List<String>>> identities = getIdentities(e);
		int number = getFreeTagID(identities);
		replace(parent1,ent1,ef.newTag(number,mgu));
		replace(parent2,ent2,ef.newTag(number,mgu));
		if (contractTags(e, parent1) && contractTags(e, parent2))
		{
			successMsg("Identity introduction successful!");
		}
		return e;
	}
	
	private static int getFreeTagID(Map<Integer,List<List<String>>> identities)
	{
		for (int i = 0; ; i++)
		{
			if (identities.get(i) == null) return i;
		}
	}
	
	private static boolean contractTags(IEntity e, IEntity ent)
	{
		Map<Integer,List<List<String>>> identities = getIdentities(e);
		if (ent instanceof ITag)
		{
			ITag tag1 = (ITag) ent;
			if (tag1.target() instanceof ITag)
			{
				ITag tag2 = (ITag) tag1.target();
				int lowerNumber = tag1.number();
				int higherNumber = tag2.number();
				if (lowerNumber > higherNumber)
				{
					lowerNumber = tag2.number();
					higherNumber = tag1.number();
				}
				replace(tag1,tag2,tag2.target());
				for (List<String> path : identities.get(higherNumber))
				{
					IEntity otherE = delta(e,path);
					if (otherE instanceof ITag)
					{
						((ITag) otherE).setNumber(lowerNumber);
					}
					else
					{
						failMsg("Failed to rename tag at path: " + path);
						return false;
					}
				}
			}
			else
			{
				failMsg("Expected double tag was not encountered!");
				return false;
			}
		}
		return true;
	}

    public static IEntity sigMGU(IEntity e1, IEntity e2, List<String> path1, List<String> path2, TraleSLDSignature sig)
    {
        Map<Integer,Map<String,String>> paths1 = convertToPaths(delta(e1,path1));
        Map<Integer,Map<String,String>> paths2 = convertToPaths(delta(e2,path2));
        if (verbose)
        {
        	System.err.println("Path representation of structure 1:");
        	printPaths(paths1);
        	System.err.println("Path representation of structure 2:");
        	printPaths(paths2);
        }
        Map<Integer,Map<String,String>> resP = new HashMap<Integer,Map<String,String>>();
        boolean success = unify(paths1, paths2, resP, sig,-1,-1,"","");
        if (success)
        {
        	if (verbose)
        	{
        		System.err.println("Path representation of result structure:");
        		printPaths(resP);
        	}
            return convertToGraleJ(resP);
        }
        else
        {
        	return null;
        }
    }
    
    private static Map<Integer,Map<String,String>> convertToPaths(IEntity e1)
    {
        Map<Integer,Map<String,String>> paths = new HashMap<Integer,Map<String,String>>();
        paths.put(-1, new HashMap<String,String>());
        convertToPaths(e1,"",paths, -1);
        return paths;
    }
    
    private static void convertToPaths(IEntity e, String path, Map<Integer,Map<String,String>> paths, int i)
    {
        if (e instanceof ITag)
        {
            ITag tag = (ITag) e;
            paths.get(i).put(path, "#" + tag.number());
            if (paths.get(tag.number()) == null)
            {
                paths.put(tag.number(), new HashMap<String,String>());
                convertToPaths(tag.target(),"", paths, tag.number());
            }
        }
        else if (e instanceof IAny)
        {
            paths.get(i).put(path, "a_" + ((IAny) e).text());
        }
        else if (e instanceof IList)
        {
            IList list = (IList) e;
            IEntity lastElement = null;
            String extendedPath = path;
            for (IEntity ent : list.elements())
            {
                if (lastElement != null)
                {
                    paths.get(i).put(extendedPath + ":hd", "ne_list");
                    convertToPaths(lastElement,extendedPath + ":hd",paths,i);
                    extendedPath += ":tl";
                }
                lastElement = ent;
            }
            //IList of length 0 is represented by type e_list
            if (lastElement == null)
            {
                paths.get(i).put(path, "e_list");
            }
            else
            {
                paths.get(i).put(path, "ne_list");
                convertToPaths(lastElement,extendedPath + ":hd",paths,i);
                //empty tail
                paths.get(i).put(extendedPath + ":tl", "e_list");
            }
        }
        else if (e instanceof ITypedFeatureStructure)
        {
            ITypedFeatureStructure fs = (ITypedFeatureStructure) e;
            paths.get(i).put(path, fs.typeName());
            for (IFeatureValuePair fv : fs.featureValuePairs())
            {
                convertToPaths(fv.value(), path + ":" + fv.feature(), paths, i);
            }
        }
    }
    
    private static void printPaths(Map<Integer,Map<String,String>> paths)
    {
        List<Integer> tagList = new LinkedList<Integer>();
        tagList.addAll(paths.keySet());
        Collections.sort(tagList);
        for (int i : tagList)
        {
            List<String> pathList = new LinkedList<String>();
            pathList.addAll(paths.get(i).keySet());
            Collections.sort(pathList);
            String tag = "";
            if (i != -1) tag = "#" + i;
            for (String path : pathList)
            {
                System.err.println(tag + path + " -> " + paths.get(i).get(path));
            }
        }
    }
    
    private static IEntity convertToGraleJ(Map<Integer,Map<String,String>> paths)
    {
        Map<Integer,List<ITag>> tagsPerIndex = new HashMap<Integer,List<ITag>>();
        Map<Integer,IEntity> structPerIndex = new HashMap<Integer,IEntity>();
        String prefixUpToTag = null;
        for (int tagID : paths.keySet())
        {
            IEntity struct = ef.newTFS("bot");
            structPerIndex.put(tagID, struct);
            Map<String,String> ps = paths.get(tagID);
            List<String> pList = new LinkedList<String>();
            pList.addAll(ps.keySet());
            Collections.sort(pList);
            for (String path : pList)
            {
                String[] feats;
                if (prefixUpToTag != null && path.startsWith(prefixUpToTag))
                {
                	continue;
                }
                else if (path.startsWith(":"))
                {
                    feats = path.substring(1).split(":");
                }
                else
                {
                    feats = path.split(":");
                }
                if (verbose) 
                {
                    System.err.print("Processing path: ");
                    for (String feat : feats)
                    {
                        System.err.print("->" + feat);
                    }
                    System.err.println();
                }
                IEntity currentStruct = struct;
                int listCounter = 0;
                for (int i = 0; i < feats.length; i++)
                {
                	//traverse paths
                    if (currentStruct instanceof ITypedFeatureStructure)
                    {
                        ITypedFeatureStructure tfs = (ITypedFeatureStructure) currentStruct;
                        currentStruct = null;
                        for (IFeatureValuePair fv : tfs.featureValuePairs())
                        {
                            if (fv.feature().equals(feats[i]))
                            {
                                currentStruct = fv.value();
                                break;
                            }
                        }
                        if (currentStruct == null)
                        {
                        	//end of path reached
                            if (i == feats.length - 1)
                            {
                                if (verbose) System.err.println("  Index " + i + ": " + ps.get(path));
                                String type = ps.get(path);
                                if (type.equals("ne_list"))
                                {
                                    if (path.equals(""))
                                    {
                                        struct = ef.newList();
                                        structPerIndex.put(tagID, struct);
                                    }
                                    else
                                    {
                                        tfs.addFeatureValue(ef.newFeatVal(feats[i],ef.newList()));
                                    }
                                }
                                else if (type.equals("e_list"))
                                {
                                    if (path.equals(""))
                                    {
                                        struct = ef.newList();
                                        structPerIndex.put(tagID, struct);
                                    }
                                    else
                                    {
                                        tfs.addFeatureValue(ef.newFeatVal(feats[i],ef.newList()));
                                    }
                                }
                                else if (type.startsWith("a_"))
                                {
                                    if (path.equals(""))
                                    {
                                        struct = ef.newAny(type.substring(2));
                                        structPerIndex.put(tagID, struct);
                                    }
                                    else
                                    {
                                        tfs.addFeatureValue(ef.newFeatVal(feats[i],ef.newAny(type.substring(2))));
                                    }
                                }
                                else if (type.startsWith("#"))
                                {
                                	prefixUpToTag = path;
                                    int refID = Integer.parseInt(type.substring(1));
                                    ITag newTag = ef.newTag(refID);
                                    if (path.equals(""))
                                    {
                                        structPerIndex.put(tagID, newTag);
                                    }
                                    else
                                    {
                                        tfs.addFeatureValue(ef.newFeatVal(feats[i],newTag));
                                    }
                                    List<ITag> tagList = tagsPerIndex.get(refID);
                                    if (tagList == null)
                                    {
                                        tagList = new LinkedList<ITag>();
                                        tagsPerIndex.put(refID, tagList);
                                    }
                                    tagList.add(newTag);
                                }
                                else
                                {
                                    if (path.equals(""))
                                    {
                                        tfs.setType(ef.newType(type));
                                    }
                                    else
                                    {
                                        tfs.addFeatureValue(ef.newFeatVal(feats[i],ef.newTFS(type)));
                                    }
                                }
                            }
                            else
                            {
                                System.err.println("ERROR: attempted non-incremental construction!");
                            }
                        }
                    }
                    else if (currentStruct instanceof IList)
                    {
                        if (feats[i].equals("tl"))
                        {
                            listCounter++;
                        }
                        //addressed list element reached
                        else if (feats[i].equals("hd"))
                        {
                            IList list = (IList) currentStruct;
                            IEntity newEnt = null;
                            String type = ps.get(path);
                            if (type.equals("ne_list"))
                            {
                                newEnt = ef.newList();
                            }
                            else if (type.equals("e_list"))
                            {
                                newEnt = ef.newList();
                            }
                            else if (type.startsWith("a_"))
                            {
                                newEnt = ef.newAny(type.substring(2));
                            }
                            else if (type.startsWith("#"))
                            {
                            	prefixUpToTag = path;
                                int refID = Integer.parseInt(type.substring(1));
                                ITag newTag = ef.newTag(refID);
                                newEnt = newTag;
                                List<ITag> tagList = tagsPerIndex.get(refID);
                                if (tagList == null)
                                {
                                    tagList = new LinkedList<ITag>();
                                    tagsPerIndex.put(refID, tagList);
                                }
                                tagList.add(newTag);
                            }
                            else
                            {
                                newEnt = ef.newTFS(type);
                            }
                            //do not use list counter, list elements occur in correct order because of path sorting
                            list.append(newEnt);
                            listCounter = 0;
                        }
                    }
                    else if (currentStruct instanceof ITag)
                    {
                        //ERROR: this should not happen
                    }
                    else
                    {
                        //ERROR: this should not happen either
                    }
                }
            }
        }
        for (Integer i : tagsPerIndex.keySet())
        {
            for (ITag tag : tagsPerIndex.get(i))
            {
                tag.setTarget(structPerIndex.get(i));
            }
        }
        return structPerIndex.get(-1);
    }
    
    private static boolean unify(Map<Integer,Map<String,String>> paths1, Map<Integer,Map<String,String>> paths2, Map<Integer,Map<String,String>> resP, TraleSLDSignature sig, int tag1, int tag2, String prefix1, String prefix2)
    {
        //determine the common tag ID for the result structure
        int resTag = -1;
        if (tag1 != -1)
        {
            resTag = tag1;
        }
        else if (tag2 != -1)
        {
            resTag = tag2;
        }
        //check whether the common tag ID already points to a structure
        Map<String,String> res = resP.get(resTag);
        if (res == null)
        {
            res = new HashMap<String,String>();
            resP.put(resTag, res);
        }
        else
        {
            //TODO: unify existing structure with new structures
        }
        Map<String,String> p1 = paths1.get(tag1);
        Map<String,String> p2 = paths2.get(tag2);
        List<String> pathList = new LinkedList<String>();
        for (String path : p1.keySet())
        {
            if (path.startsWith(prefix1))
            {
                pathList.add(path.substring(prefix1.length()));
            }
        }
        for (String path : p2.keySet())
        {
            if (path.startsWith(prefix2))
            {
                pathList.add(path.substring(prefix2.length()));
            }
        }
        Collections.sort(pathList);
        for (String path : pathList)
        {
            String ty1 = p1.get(prefix1 + path);
            String ty2 = p2.get(prefix2 + path);
            if (ty1 == null)
            {
                res.put(path, ty2);
            }
            else if (ty2 == null)
            {
                res.put(path, ty1);
            }
            else if (ty1.startsWith("#"))
            {
                int newTag1 = Integer.parseInt(ty1.substring(1));
                if (ty2.startsWith("#"))
                {
                    //two tags need to be unified
                    int newTag2 = Integer.parseInt(ty2.substring(1));
                    res.put(path, "#" + newTag1);
                    unify(paths1,paths2,resP,sig,newTag1,newTag2,"","");
                }
                else
                {
                    //unify tag in p1 with structure in p2
                    res.put(path, "#" + newTag1);
                    unify(paths1,paths2,resP,sig,newTag1,tag2,"",path);
                }
            }
            else if (ty2.startsWith("#"))
            {
                //unify tag in p2 with structure in p1
                int newTag2 = Integer.parseInt(ty2.substring(1));
                res.put(path, "#" + newTag2);
                unify(paths1,paths2,resP,sig,tag1,newTag2,path,"");
            }
            else if (ty1.startsWith("a_"))
            {
                //atom in p1: can only be unified with identical atom or bot
                if (ty1.equals(ty2) || ty2.equals("bot"))
                {
                    res.put(path, ty1);
                }
                else
                {
                    failMsg("Unification failed: " + ty1 + " and " + ty2 + " at path " + path + " are incompatible.");
                    return false;
                }
            }
            else if (ty2.startsWith("a_"))
            {
                //atom in p2, but not in p1: can only be unified with bot
                if (ty1.equals("bot"))
                {
                    res.put(path, ty2);
                }
                else
                {
                    failMsg("Unification failed: " + ty1 + " and " + ty2 + " at path " + path + " are incompatible.");
                    return false;
                }
            }
            else
            {
                //normal types: subsumption check, select more specific one
                String unifTy = null;
                if (sig.dominates(ty1,ty2))
                {
                    unifTy = ty2;
                }
                else if (sig.dominates(ty2,ty1))
                {
                    unifTy = ty1;
                }
                else
                {
                    failMsg("Unification failed: types " + ty1 + " and " + ty2 + " at path " + path + " are incompatible.");
                    return false;
                }
                res.put(path, unifTy);
            }
        }
        return true;
    }
	
	/**
	 * Retrieves the substructure of an IEntity at a given a path.
	 * @param e the entity to address into
	 * @param path - a list of strings encoding a path of features
	 * @return the IEntity at the path in e, null if no such structure was found
	 */
	public static IEntity delta(IEntity e, List<String> path)
	{
		return goSubpath(e,path,0,path.size());
	}
	
	private static IEntity goSubpath(IEntity e, List<String> path, int start, int end)
	{
		//failMsg("goSubpath(" + e + "," + path + "," + start + "," + end + ")");
		if (start < 0 || end < start) return null;
		if (start == end)
		{
			//if (e instanceof ITag) return ((ITag) e).target();
			return e;
		}
		else
		{
			if (e  instanceof ITypedFeatureStructure)
			{
				String feat = path.get(start);
				ITypedFeatureStructure fs = (ITypedFeatureStructure) e;
				for (int i = 0; i < fs.featureValuePairs().size(); i++)
				{
					if (fs.featureValuePairs().get(i).feature().equals(feat))
					{
						return goSubpath(fs.featureValuePairs().get(i).value(), path, start+1, end);
					}
				}
			}
			else if (e instanceof IList)
			{
				int i = 0;
				Iterator<IEntity> entIt = ((IList) e).elements().iterator();
				IEntity selectedItem = entIt.next();
				while (path.get(start + i).equals("tl"))
				{
					selectedItem = entIt.next();
					i++;
				}
				if (!path.get(start + i).equals("hd")) return null;
				return goSubpath(selectedItem,path,start+i+1,end);
			}
			else if (e instanceof ITag)
			{
				return goSubpath(((ITag) e).target(),path,start,end);
			}
			else if (e instanceof IRelation)
			{
			    String argument = path.get(start);
			    if (argument.startsWith("arg"))
			    {
			        Integer arg = Integer.parseInt(argument.substring(3));
			        if (arg >= ((IRelation) e).arity()) return null;
			        IEntity selectedItem = ((IRelation) e).arg(arg);
		            return goSubpath(selectedItem,path,start+1,end);
			    }
			}
		}
		return null;
	}
	
	private static IEntity goUpToLast(IEntity e, List<String> path)
	{
		if (path.size() == 0) return null;
		//special treatment for list case
		int lastBeforeList = path.size() - 1;
		if (path.get(lastBeforeList).equals("hd"))
		{
			lastBeforeList--;
			while (path.get(lastBeforeList).equals("tl"))
			{
				lastBeforeList--;
			}
			//System.err.println("upToLast: " + path.subList(0, lastBeforeList + 1));
			return goSubpath(e,path,0, lastBeforeList + 1);
		}
		else
		{
			return goSubpath(e,path,0, path.size() - 1);
		}
	}
	
	private static IEntity goLast(IEntity e, List<String> path)
	{
		if (path.size() == 0) return null;
		//special treatment for list case
		int lastBeforeList = path.size() - 1;
		if (path.get(lastBeforeList).equals("hd"))
		{
			lastBeforeList--;
			while (path.get(lastBeforeList).equals("tl"))
			{
				lastBeforeList--;
			}
			//System.err.println("last: " + path.subList(lastBeforeList + 1, path.size()));
			return goSubpath(e,path,lastBeforeList + 1, path.size());
		}
		else
		{
			return goSubpath(e,path,path.size() - 1, path.size());
		}
	}
	
	private static boolean setType(IEntity e, String ty)
	{
		if (e instanceof IType)
		{
			IType type = (IType) e;
			type.setTypeName(ty);
			return true;
		}
		else if (e instanceof ITypedFeatureStructure)
		{
			ITypedFeatureStructure fs = (ITypedFeatureStructure) e;
			fs.type().setTypeName(ty);
			return true;
		}
		return false;
	}
	
	public static IEntity replacePaste(IEntity e, List<String> path, IEntity paste, TraleSLDSignature sig)
	{
		//System.err.println("replacePaste(" + e + "," + path + "," + paste + "," + sig + ");");
		IEntity parent = goUpToLast(e,path);
		IEntity et = goLast(parent,path);
		if (parent == null) et = e;	
		if (et == null)
		{
			failMsg("Paste failed: Unable to evaluate address!");
			return e;
		}
		if (et instanceof ITag)
		{
			parent = et;
			et = ((ITag) et).target();
		}
        IEntity typeParent = goUpToLast(e,path);
        if (typeParent == null) typeParent = e;
        String approValTy = "bot";
        if (typeParent != null)
        {
            approValTy = sig.getAppropriateValueType(getType(typeParent), path.get(path.size() - 1));
        }
		if (sig.dominates(approValTy, getType(paste)))
        {
		    successMsg(approValTy + " dominates " + getType(paste) + "! Replacement paste successful.");
		    if (parent == null) return paste;
		    replace(parent,et,paste);
        }
        else
        {
            failMsg("Replacement paste failed: inappropriate value.");
        }
		return e;
	}
    
    public static IEntity unifyPaste(IEntity e, List<String> path, IEntity paste, TraleSLDSignature sig)
    {
        //System.err.println("replacePaste(" + e + "," + path + "," + paste + "," + sig + ");");
        IEntity parent = goUpToLast(e,path);
        IEntity et = goLast(parent,path);
        if (parent == null) et = e; 
        if (et == null)
        {
            failMsg("Paste failed: Unable to evaluate address!");
            return e;
        }
        if (et instanceof ITag)
        {
            parent = et;
            et = ((ITag) et).target();
        }
        IEntity replacement = sigMGU(e,paste,path,new LinkedList<String>(),sig);
        if (replacement != null)
        {
            successMsg("Unifying paste successful.");
            if (parent == null) return replacement;
            replace(parent,et,replacement);
        }
        return e;
    }
	
	private static void replace(IEntity parent, IEntity e, IEntity replacement)
	{
		//System.err.println("replace(" + parent + "," + e + "," + replacement + ");");
		if (parent instanceof ITypedFeatureStructure)
		{
			
			ITypedFeatureStructure fs = (ITypedFeatureStructure) parent;
			for (IFeatureValuePair fv : fs.featureValuePairs())
			{
				if (fv.value() == e)
				{
					fv.setValue(replacement);
					return;
				}
			}
		}
		else if (parent instanceof IList)
		{
			IList list = (IList) parent;
			List<IEntity> elList = new LinkedList<IEntity>();
			for (IEntity el : list.elements())
			{
				if (el == e)
				{
					elList.add(replacement);
				}
				else
				{
					elList.add(el);
				}
			}
			list.clear();
			for (IEntity el : elList)
			{
				list.append(el);
			}
		}
		else if (parent instanceof ITag)
		{
			((ITag) parent).setTarget(replacement);
		}
		else
		{
			System.err.println("Replace failed at " + parent);
		}
	}
	
	public static int listLength(IList list)
	{
		int i = 0;
		//the selected block is ">"; set index to end of list
		for (@SuppressWarnings("unused") IEntity e : list.elements())
		{
			i++;
		}
		return i;
	}
	
	public static String getType(IEntity e)
	{
		String type = "?";
		if (e instanceof IType)
		{
			IType ty = (IType) e;
			type = ty.typeName();
		}
		else if (e instanceof ITypedFeatureStructure)
		{
			ITypedFeatureStructure fs = (ITypedFeatureStructure) e;
			type = fs.typeName();
		}
		else if (e instanceof IList)
		{
			type = "list";
		}
		else if (e instanceof IRelation)
		{
		    type = ((IRelation) e).name();
		}
		//the way to deal with mgsat(Type) for the moment
		if (type.startsWith("mgsat("))
		{
			type = type.substring(6, type.length() - 1);
		}
		return type;
	}
    
    /*
     * Performs alpha conversion on two structures, and returns the combined identity information.
     * @param e1 an IEntity object
     * @param e2 an IEntity object
     * @return The path identities in the alpha-converted structures, indexed by tag IDs.
     */
    private static Map<Integer,List<List<String>>> getAlphaConvertedIdentities(IEntity e1, IEntity e2)
    {
        if (e1 == e2)
        {
            //degenerate case: just one structure, no alpha-conversion necessary
            return getIdentities(e1);
        }
        else
        {
            Map<Integer,List<List<String>>> identities1 = getIdentities(e1);
            Map<Integer,List<List<String>>> identities2 = getIdentities(e2);
            Map<Integer,List<List<String>>> commonIdentities = new HashMap<Integer,List<List<String>>>();
            //determine maximum tag ID
            int maximumTagID = 0;
            for (Integer i : identities1.keySet())
            {
                if (i > maximumTagID) maximumTagID = i;
            }
            for (Integer i : identities2.keySet())
            {
                if (i > maximumTagID) maximumTagID = i;
            }
            //go through IDs and resolve clashes
            int nextFreeID = 0;
            for (int i = 0; i < maximumTagID; i++)
            {
                List<List<String>> identI1 = identities1.get(i);
                List<List<String>> identI2 = identities2.get(i);
                if (identI1 != null && identI2 != null)
                {
                    //clash: determine next free ID (TODO: be more efficient here)
                    while (identities1.get(nextFreeID) != null || identities2.get(nextFreeID) != null)
                    {
                        nextFreeID++;
                    }
                    //rename reentrancies in e2 to fresh tag ID
                    alphaConversionStep(e2,identI2,nextFreeID);
                    //store identities under their respective IDs
                    commonIdentities.put(nextFreeID, identI1);
                    commonIdentities.put(nextFreeID, identI2);
                    //the new tag ID is now used in common identities, do not use it again
                    nextFreeID++;
                }
                else if (identI1 != null)
                {
                    //no clash: take over path identities from e1
                    commonIdentities.put(i, identI1);
                }
                else if (identI2 != null)
                {
                    //no clash: take over path identities from e2
                    commonIdentities.put(i, identI2);
                }
                else
                {
                    //no identity encoded by tag ID i; do nothing
                }
            }
            return commonIdentities;
        }
    }
    
    private static void alphaConversionStep(IEntity e, List<List<String>> tagPaths, int newTagID)
    {
        for (List<String> path : tagPaths)
        {
            IEntity tagE = delta(e,path);
            if (tagE instanceof ITag)
            {
                ((ITag) tagE).setNumber(newTagID);
            }
            else
            {
                System.err.println("WARNING: no tag at path " + path + " -> alpha conversion failed!");
            }
        }
    }
	
	private static Map<Integer,List<List<String>>> getIdentities(IEntity e)
	{
		Map<Integer,List<List<String>>> identities = new HashMap<Integer,List<List<String>>>();
		List<String> currentPath = new LinkedList<String>();
		fillIdentities(e,currentPath,identities);
		/*for (Integer i : identities.keySet())
		{
			System.err.println(i + " -> " + identities.get(i));
		}*/
		return identities;
	}
	
	private static void fillIdentities(IEntity ent, List<String> path, Map<Integer,List<List<String>>> identities)
	{
		List<String> currentPath = new LinkedList<String>();
		currentPath.addAll(path);
		if (ent instanceof ITag)
		{
			ITag tag = (ITag) ent;
			int i = tag.number();
			List<List<String>> paths = identities.get(i);
			if (paths == null)
			{
				paths = new LinkedList<List<String>>();
				identities.put(i, paths);
			}
			paths.add(currentPath);
			fillIdentities(tag.target(),currentPath,identities);
		}
		else if (ent instanceof IAny)
		{

		}
		else if (ent instanceof IList)
		{
			IList list = (IList) ent;
			currentPath.add("hd");
			for (IEntity lEnt : list.elements())
			{
				fillIdentities(lEnt, currentPath, identities);
				currentPath.add(currentPath.size() - 1, "tl");
			}
		}
		else if (ent instanceof ITypedFeatureStructure)
		{
			ITypedFeatureStructure tfs = (ITypedFeatureStructure) ent;
			for (IFeatureValuePair fv : tfs.featureValuePairs())
			{
				fillIdentities(fv, currentPath, identities);
			}
		}
		else if (ent instanceof IType)
		{

		}
		else if (ent instanceof IFeatureValuePair)
		{
			IFeatureValuePair fv = (IFeatureValuePair) ent;
			currentPath.add(fv.feature());
			fillIdentities(fv.value(), currentPath, identities);
		}
	}
	
	public static List<String> listFeatures(IEntity e)
	{
		List<String> features = new LinkedList<String>();
		if (e instanceof ITypedFeatureStructure)
		{
			ITypedFeatureStructure fs = (ITypedFeatureStructure) e;
			for (IFeatureValuePair fv : fs.featureValuePairs())
			{
				features.add(fv.feature());
			}
		}
		return features;
	}
	
	public static IEntity sigMGS(String type, TraleSLDSignature sig)
	{
		if (!sig.getTypes().contains(type))
		{
			return ef.newType("mgsat(" + type + ")");
		}
		IEntity struct;
		if (type.equals("e_list"))
		{
			struct = ef.newList();
		}
		else if (type.equals("ne_list"))
		{
			IList list = ef.newList();
			list.append(ef.newTFS("bot"));
			struct = list;
		}
		else
		{
			struct = ef.newTFS(type);
            ttf(struct,sig);
		}
		return struct;
	}
	
	public static IEntity grisuToGralej(String grisu)
	{
		IDataPackage data = null;
		try
		{
			data = FSVisualizationUtility.getDefault().parseGrisu(grisu);
		}
		catch (ParseException e)
		{
			failMsg("Parsing of GRISU string failed!" + e.getMessage());
			return null;
		}
		return (IEntity) data.getModel();
	}
	
	public static String gralejToGrisu(IEntity ent)
	{
		int[] counter = {0};
		StringBuilder s = new StringBuilder("!newdata\"grisu\"");
		HashMap<Integer,IEntity> ref = new HashMap<Integer,IEntity>();
		HashSet<Integer> procRef = new HashSet<Integer>();
		gralejToGrisu(ent, s, counter, ref, procRef);
		while (ref.size() > 0)
		{
			resolveReferenced(s,counter,ref, procRef);
		}
		s.append("\n");
		return s.toString();
	}
	
	private static void resolveReferenced(StringBuilder s, int[] counter, Map<Integer,IEntity> ref, Set<Integer> procRef)
	{
		int number = ref.keySet().iterator().next();
		IEntity target = ref.remove(number);
		procRef.add(number);
		s.append("(R");
		s.append(counter[0]++);
		s.append(" ");
		s.append(number);
		gralejToGrisu(target,s,counter,ref,procRef);
		s.append(")");
	}
	
	private static void gralejToGrisu(IEntity ent, StringBuilder s, int[] counter, Map<Integer,IEntity> ref, Set<Integer> procRef)
	{
		if (ent instanceof IList)
		{
			IList list = (IList) ent;
			s.append("(L");
			s.append(counter[0]++);
			for (IEntity lEnt : list.elements())
			{
				gralejToGrisu(lEnt, s, counter, ref, procRef);
			}
			s.append(")");
		}
		else if (ent instanceof IAny)
		{
			IAny atom = (IAny) ent;
			s.append("(S");
			s.append(counter[0]++);
			s.append("(");
			s.append(counter[0]++);
			s.append("\"");
			s.append(atom.value());
			s.append("\"");
			s.append("))");
		}
		else if (ent instanceof ITag)
		{
			ITag tag = (ITag) ent;
			s.append("(#");
			s.append(counter[0]++);
			s.append(" ");
			s.append(tag.number());
			if (!procRef.contains(tag.number()))
			{
				ref.put(tag.number(), tag.target());
			}
			s.append(")");
		}
		else if (ent instanceof ITypedFeatureStructure)
		{
			ITypedFeatureStructure tfs = (ITypedFeatureStructure) ent;
			s.append("(S" + (counter[0] + 1));
			gralejToGrisu(tfs.type(), s, counter, ref, procRef);
			counter[0]++;
			for (IFeatureValuePair fv : tfs.featureValuePairs())
			{
				gralejToGrisu(fv, s, counter, ref, procRef);
			}
			s.append(")");
		}
		else if (ent instanceof IType)
		{
			IType type = (IType) ent;
			s.append("(");
			s.append(counter[0]++);
			s.append("\"");
			s.append(type.text());
			s.append("\"");
			s.append(")");
		}
		else if (ent instanceof IFeatureValuePair)
		{
			IFeatureValuePair fv = (IFeatureValuePair) ent;
			s.append("(V");
			s.append(counter[0]++);
			s.append("\"");
			s.append(fv.feature());
			s.append("\"");
			gralejToGrisu(fv.value(), s, counter, ref, procRef);
			s.append(")");
		}
	}
	
	private static void failMsg(String string)
	{
		if (editor != null)
		{
			editor.failureMessage(string);
		}
		else
		{
			System.err.println(string);
		}
	}
	
	private static void successMsg(String string)
	{
		if (editor != null)
		{
			editor.success(string);
		}
		else
		{
			failMsg(string);
		}
	}
}
