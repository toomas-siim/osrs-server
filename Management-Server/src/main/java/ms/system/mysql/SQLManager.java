package ms.system.mysql;

import ms.ServerConstants;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages the sql connections.
 * @author Vexia
 * 
 */
public final class SQLManager {

	/**
	 * If the sql manager is locally hosted.
	 */
	public static final boolean LOCAL = false;

    /**
     * The database URL.
     */
    public static final String DATABASE_URL = (LOCAL ? "127.0.0.1" : "hivetech.ee") + ":3306/" + (SQLManager.LOCAL ? "2009scape" : ServerConstants.DATABASE_NAMES[0]);

    /**
     * The username of the user.
     */
    private static final String USERNAME = (LOCAL ? "root" : "2009scape_user");

    /**
     * The password of the user.
     */
    private static final String PASSWORD = (LOCAL ? "libetaria2009_password" : "libetaria2009_password");
	
	
	/**
	 * IF the sql manager is initialized.
	 */
	private static boolean initialized;
	
	/**
	 * Constructs a new {@code SQLManager} {@code Object}
	 */
	public SQLManager() {
		/**
		 * empty.
		 */
	}
	
	/**
	 * Initializes the sql manager.
	 */
	public static void init() {
		initialized = true;
		WorldListSQLHandler.clearWorldList();
	}
	
	/**
	 * Gets a connection from the pool.
	 * @return The connection.
	 */
	public static Connection getConnection() {
		try {
			return DriverManager.getConnection("jdbc:mysql://" + DATABASE_URL + "?useTimezone=true&serverTimezone=UTC",  USERNAME, PASSWORD);
		} catch (SQLException e) {
			System.out.println("Error: Mysql error message=" + e.getMessage() + ".");
		}
		return null;
	}

	/**
	 * Releases the connection so it's available for usage.
	 * @param connection The connection.
	 */
	public static void close(Connection connection) {
		try {
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Gets the initialized.
	 * @return the initialized
	 */
	public static boolean isInitialized() {
		return initialized;
	}

	/**
	 * Sets the bainitialized.
	 * @param initialized the initialized to set.
	 */
	public static void setInitialized(boolean initialized) {
		SQLManager.initialized = initialized;
	}

}
