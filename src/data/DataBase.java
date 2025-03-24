package data;

import java.sql.*;

/**
 * Provides basic functionality for database communication
 * @author Stefan Grabuschnig
 *
 */
public class DataBase {
	private String dbDriver;
	private String dbURL;
	private String dbUser;
	private String dbPassword;

	private Connection connection = null;
	private Statement statement = null;

	private boolean connected;

	/**
	 * @param dbDriver classpath of the database driver
	 * @param dbURL url of the database
	 * @param dbUser username	
	 * @param dbPassword password
	 */
	public DataBase(String dbDriver, String dbURL,String dbUser,String dbPassword) {
		this.dbDriver = dbDriver;
		this.dbURL = dbURL;
		this.dbUser = dbUser;
		this.dbPassword = dbPassword;
		connected = false;

		try {
			Class.forName(this.dbDriver);
		// } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
		} catch (ClassNotFoundException e) {
			System.out.println(new Timestamp(System.currentTimeMillis()).toString()
					+ ": ClassNotFoundException in DataBase Constructor");
			System.out.println(e.getMessage());
		}
	}

	/**
	 * opens the database connection
	 */
	public void connect() {
		try {
			connection = DriverManager.getConnection(dbURL, dbUser, dbPassword);
			
			this.statement = connection.createStatement();
			this.connected = true;
		} catch (SQLException e) {
			System.out.println(new Timestamp(System.currentTimeMillis()).toString()
					+ ": SQLException in DataBase.connect()");
			System.out.println(e.getMessage());
		}

	}

	/**
	 * closes the database connection
	 */
	public void disconnect() {
		try {
			connection.close();
			this.statement = null;
			this.connected = false;

		} catch (SQLException e) {
			System.out.println(new Timestamp(System.currentTimeMillis()).toString()
					+ ": SQLException in DataBase.disconnect()");
			System.out.println(e.getMessage());
		}
	}

	/**
	 * performs an sql query
	 * @param query the sql query
	 * @return the ResultSet
	 */
	public ResultSet performQuery(String query) {
		ResultSet rs = null;
		if (this.connected) {
			try {
				rs = statement.executeQuery(query);
			} catch (SQLException e) {
				System.out.println(new Timestamp(System.currentTimeMillis()).toString()
						+ ": SQLException in DataBase.performQuery()");
				System.out.println(e.getMessage());
			}
		} else {
			System.out.println(new Timestamp(System.currentTimeMillis()).toString()
					+ ": Error in DataBase.performQuery(). No connection established");
		}
		return rs;
	}

	/**
	 * @return the connection state
	 */
	public boolean isConnected() {
		return this.connected;
	}
}
