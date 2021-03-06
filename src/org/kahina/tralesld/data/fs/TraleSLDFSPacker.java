package org.kahina.tralesld.data.fs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * A {@link TraleSLDFSPacker} can be used to reduce memory usage when
 * storing many strings with many shared substrings. This is achieved by
 * breaking strings down into smaller units and storing the full strings as
 * objects that recursively share substructures (a packed forest). The heuristic
 * used for splitting strings up is geared towards strings where frequently
 * occuring substrings start with {@code "("} and end with {@code ")"}.
 * 
 * Note that strings stored using a bracket packer will not be garbage-collected
 * until the bracket packer itself is.
 */
public class TraleSLDFSPacker
{
	private static final boolean VERBOSE = false;
	
	private Map<String, TraleSLDFS> terminalByString = new HashMap<String, TraleSLDFS>();

	private Map<List<TraleSLDFS>, TraleSLDFS> nonTerminalByChildren = new HashMap<List<TraleSLDFS>, TraleSLDFS>();

	private String string;

	private int length;

	private int index;

	/**
	 * Returns a structure-shared representation of the given string.
	 */
	public synchronized TraleSLDFS pack(String string)
	{
		this.string = string;
		length = string.length();
		index = 0;
		List<TraleSLDFS> rootChildren = new LinkedList<TraleSLDFS>();
		while (index < length)
		{
			rootChildren.add(parseNode());
		}
		return produceNode(rootChildren);
	}

	/**
	 * Returns a node representing the "constituent" starting at
	 * {@code index}. This will typically be a non-terminal if there is an
	 * opening bracket right ahead, a terminal otherwise.
	 */
	private TraleSLDFS parseNode()
	{
		if (string.charAt(index) == '(')
		{
			return parseNonTerminal();
		}
		return parseTerminal();
	}

	/**
	 * Returns a node representing a the "constituent" starting at
	 * {@code index}. This typically consists of a first terminal child such as
	 * {@code "(S*0"} (i.e. it includes the opening bracket), followed by a
	 * number of recursively parsed bracketed constituents, intervening
	 * terminals and lastly, if the brackets in the string are properly
	 * balanced, the closing bracket.
	 */
	private TraleSLDFS parseNonTerminal()
	{
		List<TraleSLDFS> children = new LinkedList<TraleSLDFS>();
		children.add(parseTerminal());
		while (index < length && string.charAt(index) != ')')
		{
			children.add(parseNode());
		}
		if (index < length)
		{
			children.add(produceTerminal(")"));
			index++;
		}
		return produceNode(children);
	}

	/**
	 * Returns a terminal representing the substring starting at {@code index},
	 * and ending before the first opening or closing bracket after that, if
	 * any.
	 */
	private TraleSLDFS parseTerminal()
	{
		char first = string.charAt(index);
		String terminalString;
		if (first == '"')
		{
			terminalString = parseStringLiteral();
		} else if (Character.isDigit(first))
		{
			terminalString = parseNumberLiteral();
		} else
		{
			terminalString = parseOther();
		}
		
		/*String terminalString;
		if (index + 1 == length)
		{
			terminalString = string.substring(index);
		} else
		{
			int lbIndex = string.indexOf('(', index + 1);
			int rbIndex = string.indexOf(')', index + 1);
			if (lbIndex > 0 && rbIndex > 0)
			{
				terminalString = string.substring(index, Math.min(lbIndex, rbIndex));
			} else if (lbIndex > 0)
			{
				terminalString = string.substring(index, lbIndex);
			} else if (rbIndex > 0)
			{
				terminalString = string.substring(index, rbIndex);
			} else
			{
				terminalString = string.substring(index);
			}
		}
		index += terminalString.length();*/
		return produceTerminal(terminalString);
	}
	
	private String parseStringLiteral()
	{
		if (index == length - 1)
		{
			index++;
			return string.substring(index);
		}
		int endIndex = string.indexOf("\"", index + 1);
		if (endIndex == -1)
		{
			String terminalString = string.substring(index);
			index = length;
			return terminalString;
		}
		endIndex++; // catch closing quote
		String terminalString = string.substring(index, endIndex);
		index = endIndex;
		return terminalString;
	}
	
	private String parseNumberLiteral()
	{
		int startIndex = index;
		do
		{
			index++;
		} while (index < length && Character.isDigit(string.charAt(index)));
		return string.substring(startIndex, index);
	}
	
	private String parseOther()
	{
		int startIndex = index;
		char character;
		do
		{
			index++;
			if (index >= length)
			{
				break;
			}
			character = string.charAt(index);
		} while (index < length && !Character.isDigit(character) && character != '"' && character != ')' && character != '(');
		return string.substring(startIndex, index);
	}
	
	/**
	 * Retrieves a parent node with the given children if one is already stored,
	 * otherwise creates and stores it. There are two exceptions: If the list of
	 * children is empty, returns the terminal representing the empty string. If
	 * there ist just one child, returns that child.
	 */
	private TraleSLDFS produceNode(List<TraleSLDFS> children)
	{
		int size = children.size();
		if (size == 1)
		{
			return children.get(0);
		}
		if (size == 0)
		{
			return produceTerminal("");
		}
		TraleSLDFS result = nonTerminalByChildren.get(children);
		if (result == null)
		{
			children = new ArrayList<TraleSLDFS>(children);
			result = new TraleSLDPackedFSNonTerminal(children);
			nonTerminalByChildren.put(children, result);
		} else if (VERBOSE)
		{
			System.err.println("Recognized non-terminal: " + result);
		}
		return result;
	}

	/**
	 * Retrieves a terminal representing the given string if one is already
	 * stored, otherwise creates and stores it.
	 */
	private TraleSLDFS produceTerminal(String string)
	{
		TraleSLDFS result = terminalByString.get(string);
		if (result == null)
		{
			result = new TraleSLDPackedFSTerminal(string);
			terminalByString.put(string, result);
		} else if (VERBOSE)
		{
			System.err.println("Recognized terminal: " + string);
		}
		return result;
	}

}
