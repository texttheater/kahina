package org.kahina.tralesld.visual.fs;

import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

public class TraleSLDFeatureStructureEditorMenu extends JPopupMenu
{
	public TraleSLDFeatureStructureEditorMenu()
	{
		super();
	}
	
	public static TraleSLDFeatureStructureEditorMenu newTypeMenu(TraleSLDFeatureStructureEditor editor,
			List<String> subtypes, List<String> supertypes, List<String> siblingTypes,
			List<String> features, int editingMode, boolean identityMode)
	{
		TraleSLDFeatureStructureEditorMenu menu = new TraleSLDFeatureStructureEditorMenu();
		
		JMenu specializeMenu = new JMenu("Specialize to");
		if (subtypes.size() == 0)
		{
			specializeMenu.setEnabled(false);
		}
		else for (String type : subtypes)
		{
			JMenuItem typeItem = new JMenuItem(type);
			typeItem.setActionCommand("spe:" + type);
			typeItem.addActionListener(editor);
			specializeMenu.add(typeItem);
		}
		menu.add(specializeMenu);
		
		JMenu generalizeMenu = new JMenu("Generalize to");
		if (supertypes.size() == 0)
		{
			generalizeMenu.setEnabled(false);
		}
		else for (String type : supertypes)
		{
			JMenuItem typeItem = new JMenuItem(type);
			typeItem.setActionCommand("gen:" + type);
			typeItem.addActionListener(editor);
			generalizeMenu.add(typeItem);
		}
		menu.add(generalizeMenu);
		
		JMenu switchMenu = new JMenu("Switch to");
		if (siblingTypes.size() == 0)
		{
			switchMenu.setEnabled(false);
		}
		else for (String type : siblingTypes)
		{
			JMenuItem typeItem = new JMenuItem(type);
			typeItem.setActionCommand("swi:" + type);
			typeItem.addActionListener(editor);
			switchMenu.add(typeItem);
		}
		menu.add(switchMenu);
		
		menu.addSeparator();
		
		if (editingMode != TraleSLDFeatureStructureEditor.TTF_MODE)
		{
			JMenu featMenu = new JMenu("Add feature");
			if (features.size() == 0)
			{
				featMenu.setEnabled(false);
			}
			else for (String feat : features)
			{
				JMenuItem featItem = new JMenuItem(feat);
				featItem.setActionCommand("fea:" + feat);
				featItem.addActionListener(editor);
				featMenu.add(featItem);
			}
			menu.add(featMenu);
			menu.addSeparator();
		}
		
		JMenuItem beginIdentityItem = new JMenuItem("Begin identity");
		beginIdentityItem.setActionCommand("Begin");
		beginIdentityItem.addActionListener(editor);
		menu.add(beginIdentityItem);
		
		JMenuItem createIdentityItem = new JMenuItem("Finish identity");
		createIdentityItem.setActionCommand("Identity");
		createIdentityItem.addActionListener(editor);
		if (!identityMode) createIdentityItem.setEnabled(false);
		menu.add(createIdentityItem);
			
		menu.addSeparator();
		
		JMenuItem copyItem = new JMenuItem("Copy");
		copyItem.addActionListener(editor);
		menu.add(copyItem);
		
		JMenuItem replacePasteItem = new JMenuItem("Replacement Paste");
		replacePasteItem.setActionCommand("replPaste");
		replacePasteItem.addActionListener(editor);
		if (editor.getBufferedStructure() == null)
		{
			replacePasteItem.setEnabled(false);
		}
		menu.add(replacePasteItem);
		
		JMenuItem unifyPasteItem = new JMenuItem("Unifying Paste");
		unifyPasteItem.setActionCommand("unifPaste");
		unifyPasteItem.addActionListener(editor);
		if (editor.getBufferedStructure() == null)
		{
			unifyPasteItem.setEnabled(false);
		}
		menu.add(unifyPasteItem);
		
		return menu;
	}
	
	public static TraleSLDFeatureStructureEditorMenu newFeatureMenu(TraleSLDFeatureStructureEditor editor, int editingMode)
	{
		TraleSLDFeatureStructureEditorMenu menu = new TraleSLDFeatureStructureEditorMenu();
		if (editingMode != TraleSLDFeatureStructureEditor.TTF_MODE)
		{
			JMenuItem removeItem = new JMenuItem("Remove");
			removeItem.addActionListener(editor);
			menu.add(removeItem);			
		}
		
		if (editingMode == TraleSLDFeatureStructureEditor.TF_MODE)
		{
			menu.addSeparator();
		}
		
		if (editingMode != TraleSLDFeatureStructureEditor.FREE_MODE)
		{
			JMenuItem resetItem = new JMenuItem("Reset to type MGS");
			resetItem.setActionCommand("Reset");
			resetItem.addActionListener(editor);
			menu.add(resetItem);
		}
		return menu;
	}
	
	public static TraleSLDFeatureStructureEditorMenu newTagMenu(TraleSLDFeatureStructureEditor editor)
	{
		TraleSLDFeatureStructureEditorMenu menu = new TraleSLDFeatureStructureEditorMenu();	
		JMenuItem dissolveIdentityItem = new JMenuItem("Dissolve Identity");
		dissolveIdentityItem.setActionCommand("Dissolve");
		dissolveIdentityItem.addActionListener(editor);
		menu.add(dissolveIdentityItem);
		return menu;
	}
	
	public static TraleSLDFeatureStructureEditorMenu newAtomMenu(TraleSLDFeatureStructureEditor editor)
	{
		TraleSLDFeatureStructureEditorMenu menu = new TraleSLDFeatureStructureEditorMenu();	
		JMenuItem changeAtomItem = new JMenuItem("Change Atom");
		changeAtomItem.setActionCommand("ChangeAtom");
		changeAtomItem.addActionListener(editor);
		menu.add(changeAtomItem);
		JMenuItem generalizeAtomItem = new JMenuItem("Generalize to bot");
		generalizeAtomItem.setActionCommand("GezAtom");
		generalizeAtomItem.addActionListener(editor);
		menu.add(generalizeAtomItem);
		return menu;
	}
	
	public static TraleSLDFeatureStructureEditorMenu newListMenu(TraleSLDFeatureStructureEditor editor, int listIndex, int listLength)
	{
		TraleSLDFeatureStructureEditorMenu menu = new TraleSLDFeatureStructureEditorMenu();	
		
		JMenuItem addElementItem = new JMenuItem("Add entry");
		addElementItem.setActionCommand("ListAdd");
		addElementItem.addActionListener(editor);
		menu.add(addElementItem);
		
		JMenuItem listPasteItem = new JMenuItem("Paste as entry");
		listPasteItem.setActionCommand("ListPaste");
		listPasteItem.addActionListener(editor);
		if (editor.getBufferedStructure() == null)
		{
			listPasteItem.setEnabled(false);
		}
		menu.add(listPasteItem);
		
		if (listIndex < listLength)
		{
			menu.addSeparator();
		}
		
		if (listLength > 0 && listIndex < listLength)
		{
			JMenuItem removeElementItem = new JMenuItem("Remove next entry");
			removeElementItem.setActionCommand("ListRemove");
			removeElementItem.addActionListener(editor);
			menu.add(removeElementItem);
		}
		
		if (listIndex == 0 && listLength > 0)
		{
			JMenuItem clearListItem = new JMenuItem("Clear list");
			clearListItem.setActionCommand("ListClear");
			clearListItem.addActionListener(editor);
			menu.add(clearListItem);
		}

		return menu;
	}
}
