package org.kahina.io.database;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.kahina.core.KahinaException;

public class DatabaseHandler
{
	// database connection
	private Connection connection;

	// TODO move this to a database table for persistence
	private Set<String> clientIDs = new HashSet<String>();

	public DatabaseHandler()
	{
		try
		{
			startDatabase();
		} catch (ClassNotFoundException e)
		{
			System.err.println("Problem loading Apache Derby: "
					+ e.getMessage());
		} catch (SQLException e)
		{
			System.err.println("A database error occured: " + e.getMessage());
		} catch (IOException e)
		{
			System.err.println("Database file problem: " + e.getMessage());
		}
	}

	public DatabaseHandler(File file)
	{
		this();
		// TODO Use the provided file.
	}

	public void execute(String sqlString)
	{
		try
		{
			Statement statement = connection.createStatement();
			statement.execute(sqlString);
		} catch (SQLException e)
		{
			throw new KahinaException("SQL error.", e);
		}
	}

	public int queryInteger(PreparedStatement statement)
	{
		try
		{
			ResultSet resultSet = statement.executeQuery();
			if (!resultSet.next())
			{
				throw new KahinaException("No results.");
			}
			return resultSet.getInt(1);
		} catch (SQLException e)
		{
			throw new KahinaException("SQL error.", e);
		}
	}

	public List<Integer> queryIntList(PreparedStatement statement)
	{
		List<Integer> result = new ArrayList<Integer>();
		try
		{
			ResultSet resultSet = statement.executeQuery();
			while (resultSet.next())
			{
				result.add(resultSet.getInt(1));
			}
			return result;
		} catch (SQLException e)
		{
			throw new KahinaException("SQL error.", e);
		}
	}

	public Set<Integer> queryIntSet(PreparedStatement statement)
	{
		Set<Integer> result = new HashSet<Integer>();
		try
		{
			ResultSet resultSet = statement.executeQuery();
			while (resultSet.next())
			{
				result.add(resultSet.getInt(1));
			}
			return result;
		} catch (SQLException e)
		{
			throw new KahinaException("SQL error.", e);
		}
	}

	public String queryString(PreparedStatement statement)
	{
		try
		{
			ResultSet resultSet = statement.executeQuery();
			if (!resultSet.next())
			{
				throw new KahinaException("No results.");
			}
			return resultSet.getString(1);
		} catch (SQLException e)
		{
			throw new KahinaException("SQL error.", e);
		}
	}

	private void startDatabase() throws ClassNotFoundException, SQLException,
			IOException
	{
		Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
		File file = File.createTempFile("kahinadb", null);
		deleteRecursively(file);
		connection = DriverManager.getConnection("jdbc:derby:" + file.getPath()
				+ ";create=true");
		// db.deleteOnExit(); // should work but doesn't
		Statement statement = connection.createStatement();
		try
		{
			statement.executeUpdate("DROP TABLE data");
		} catch (SQLException e)
		{
			// ignore - gotta hate Derby for not supporting DROP TABLE IF EXISTS
		}

		statement
				.executeUpdate("CREATE TABLE data (id BIGINT NOT NULL , value VARCHAR(32) NOT NULL, PRIMARY KEY (id))");
		statement.close();
	}

	/**
	 * A client using this {@link DatabaseHandler} should register here with a
	 * unique ID (e.g. a fully qualified class name of a class all of whose
	 * instances will use the same set of database tables) after the required
	 * tables have been created.
	 * 
	 * @param clientID
	 */
	public void register(String clientID)
	{
		clientIDs.add(clientID);
	}

	/**
	 * Clients using this {@link DatabaseHandler} can use this method to quickly
	 * determine if they already created the tables they need.
	 * 
	 * @param clientID
	 * @return
	 */
	public boolean isRegistered(String clientID)
	{
		return clientIDs.contains(clientID);
	}

	public PreparedStatement prepareStatement(String sql)
	{
		try
		{
			return connection.prepareStatement(sql);
		} catch (SQLException e)
		{
			throw new KahinaException("Failed to prepare statement.", e);
		}
	}

	private static void deleteRecursively(File directory)
	{
		if (directory.isDirectory())
		{
			for (File file : directory.listFiles())
			{
				deleteRecursively(file);
			}
		}
		directory.delete();
	}
}
