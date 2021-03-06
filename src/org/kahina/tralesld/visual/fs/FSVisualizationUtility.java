package org.kahina.tralesld.visual.fs;

import gralej.Config;
import gralej.blocks.BlockPanel;
import gralej.controller.StreamInfo;
import gralej.om.EntityFactory;
import gralej.parsers.GraleParserFactory;
import gralej.parsers.IDataPackage;
import gralej.parsers.IGraleParser;
import gralej.parsers.ParseException;
import gralej.parsers.UnsupportedProtocolException;

import java.io.ByteArrayInputStream;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * 
 * @author ke
 */
public class FSVisualizationUtility
{
	private static final boolean verbose = false;

	private static FSVisualizationUtility def;

	private IGraleParser parser;

	public static FSVisualizationUtility getDefault()
	{
		if (def == null)
		{
			def = new FSVisualizationUtility();
		}
		return def;
	}

	public FSVisualizationUtility()
	{
		try
		{
			parser = GraleParserFactory.createParser(StreamInfo.GRISU);
		} catch (UnsupportedProtocolException e)
		{
			throw new RuntimeException("could not create Grisu format parser", e);
		}

		Config config = gralej.Config.currentConfig();
		config.set("behavior.selectOnClick", true);
		config.set("block.panel.different.background.color", "0xffffaa");
		config.set("behavior.nodeContentInitiallyVisible", true);
		config.set("behavior.autoexpandtags", true);
		config.set("behavior.alwaysfitsize", false);
		config.set("log.message.error", "stderr");
		config.set("log.message.critical", "stderr");
		applyAdditionalConfigSettings(config);
	}

	/**
	 * Override to customize. This default implementation does nothing.
	 * @param config
	 */
	protected void applyAdditionalConfigSettings(Config config)
	{
	}

	public IDataPackage parseGrisu(String grisuMessage) throws ParseException
	{
		IDataPackage dataPackage = null;
		try
		{
			if (verbose)
			{
				System.err.println(this + ".parseGrisu(" + grisuMessage + ")");
			}
			dataPackage = parser.parseAll(new ByteArrayInputStream(grisuMessage.getBytes()), StreamInfo.GRISU).get(0);
		} 
		catch (ParseException e)
		{
			JPanel result = new JPanel();
			result.add(new JLabel("Parse error: \n" + e.getMessage() + "\nGrisu message was: \n" + grisuMessage));
			// TODO: handle error message
			System.err.println("GRISU parse error!");
			throw e;
		}
		return dataPackage;
	}
	
	public static GraleJEditorBlockPanel buildEditor(IDataPackage data)
	{
		return new GraleJEditorBlockPanel(data.getModel(), data.getResidue(), data.getInequations());
	}

	/**
	 * 
	 * @param grisuMessage
	 *            A typed feature structure or tree in Grisu format.
	 * @return A GraleJ block panel, providing various methods to control
	 *         rendering, and a method called <code>getCanvas()</code> to obtain
	 *         the actual {@link JPanel}.
	 */
	public BlockPanel visualize(String grisuMessage)
	{
		BlockPanel blockPanel = null;
		try
		{
			if (verbose)
			{
				System.err.println(this + ".visualize(" + grisuMessage + ")");
			}
			blockPanel = parseGrisu(grisuMessage).createView();
		} 
		catch (ParseException e)
		{
			// TODO: restore display of error messages; display empty list as
			// temporary solution
			blockPanel = new BlockPanel(EntityFactory.getInstance().newList());
		}
		return blockPanel;
	}

	public JPanel makeJPanel(String grisuMessage)
	{
		try
		{
			return parseGrisu(grisuMessage).createView().getCanvas();
		} catch (ParseException e)
		{
			JPanel errorPanel = new JPanel();
			errorPanel.add(new JLabel("Parse error: \n" + e.getMessage() + "\nGrisu message was: \n" + grisuMessage));
			return errorPanel;
		}
	}

	public JPanel createFSFrame(String varName, String grisuMessage)
	{
		JPanel result = new JPanel();
		result.add(makeJPanel(grisuMessage));
		result.setBorder(BorderFactory.createTitledBorder(varName));
		return result;
	}
}