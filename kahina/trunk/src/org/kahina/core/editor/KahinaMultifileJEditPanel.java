package org.kahina.core.editor;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

public class KahinaMultifileJEditPanel extends JPanel
{
	
	// TODO allow for closing files
	
	private static final long serialVersionUID = 7289857419713715346L;
	
	private JTabbedPane tabbedPane;
	
	private final Map<File, KahinaJEditPanel> panelByFile = new HashMap<File, KahinaJEditPanel>();
	
	public KahinaMultifileJEditPanel()
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(createTabbedPane());
	}
	
	private Component createTabbedPane()
	{
		tabbedPane = new JTabbedPane();
		return tabbedPane;
	}
	
	public void open(File file)
	{
		KahinaJEditPanel panel = panelByFile.get(file);
		if (panel == null)
		{
			panel = createPanel(file);
			panelByFile.put(file, panel);
			tabbedPane.addTab(file.getName(), panel);
		}
		tabbedPane.setSelectedComponent(panel);
	}

	private KahinaJEditPanel createPanel(File file)
	{
		KahinaJEditPanel result = new KahinaJEditPanel(file);
		result.addPropertyChangeListener(new PropertyChangeListener()
		{
			
			@Override
			public void propertyChange(PropertyChangeEvent event)
			{
				Object source = event.getSource();
				if (source instanceof KahinaJEditPanel)
				{
					KahinaJEditPanel panel = (KahinaJEditPanel) source;
					int index = tabbedPane.indexOfComponent(panel);
					if (index != -1)
					{
						if (KahinaJEditPanel.PROPERTY_DIRTY.equals(event.getPropertyName()))
						{
							updateDirty(index, (Boolean) event.getNewValue());
						}
					}
				}
			}
			
		});
		return result;
	}

	private void updateDirty(int index, boolean dirty)
	{
		if (dirty)
		{
			tabbedPane.setTitleAt(index, "*" + tabbedPane.getTitleAt(index));
		} else
		{
			tabbedPane.setTitleAt(index, tabbedPane.getTitleAt(index).substring(1));
		}
	}

}
