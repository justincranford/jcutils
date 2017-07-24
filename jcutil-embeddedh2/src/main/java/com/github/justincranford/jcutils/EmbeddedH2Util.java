package com.github.justincranford.jcutils;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings({"hiding","unused","unchecked"})
public class EmbeddedH2Util {
	private static final Logger      LOG         = Logger.getLogger(EmbeddedH2Util.class.getName());
	private static final ClassLoader ClASSLOADER = EmbeddedH2Util.class.getClassLoader();

	private static final String  H2_DATABASENAME             = "DATABASENAME";
	private static final String  H2_DATABASEDIRECTORY        = "DATABASEDIRECTORY";

	public static final String  H2_JDBC_DRIVER_CLASS          = "org.h2.Driver";
	public static final String  H2_JDBC_MEM_URL               = "jdbc:h2:mem:" + H2_DATABASENAME;
	public static final String  H2_JDBC_DISK_URL              = "jdbc:h2:" + H2_DATABASEDIRECTORY + H2_DATABASENAME;
	public static final String  DEFAULT_H2_JDBC_USERNAME      = "sa";
	public static final String  DEFAULT_H2_JDBC_PASSWORD      = null;
	public static final String  DEFAULT_H2_DATABASENAME       = "notsaved";
	public static final String  DEFAULT_H2_DATABASEDIRECTORY  = null;
	public static final String  H2_DEFAULT_JDBC_CONNECTION_PROPERTIES = "DB_CLOSE_DELAY=-1;AUTO_RECONNECT=TRUE;IFEXISTS=FALSE;DATABASE_TO_UPPER=false";

	private static Class<? extends Driver> JDBC_DRIVER_CLASS = null;

	private String jdbcDatabaseDirectory;
	private String jdbcDatabaseName;
	private String jdbcConnectionProperties;
	private String jdbcUsername;
	private String jdbcPassword;
	private String jdbcUrl;
	public EmbeddedH2Util(final String jdbcDatabaseDirectory, final String jdbcDatabaseName, final String jdbcConnectionProperties, final String jdbcUsername, final String jdbcPassword) {
		this.jdbcDatabaseDirectory    = jdbcDatabaseDirectory;
		this.jdbcDatabaseName         = jdbcDatabaseName;
		this.jdbcConnectionProperties = jdbcConnectionProperties;
		this.jdbcUsername             = jdbcUsername;
		this.jdbcPassword             = jdbcPassword;
		this.jdbcUrl                  = computeJdbcUrl(jdbcDatabaseDirectory, jdbcDatabaseName, jdbcConnectionProperties);
	}

	public EmbeddedH2Util(final String jdbcDatabaseDirectory, final String jdbcDatabaseName) {
		this(jdbcDatabaseDirectory, jdbcDatabaseName, H2_DEFAULT_JDBC_CONNECTION_PROPERTIES, DEFAULT_H2_JDBC_USERNAME, DEFAULT_H2_JDBC_PASSWORD);
	}

	public EmbeddedH2Util() {
		this(DEFAULT_H2_DATABASEDIRECTORY, DEFAULT_H2_DATABASENAME, H2_DEFAULT_JDBC_CONNECTION_PROPERTIES, DEFAULT_H2_JDBC_USERNAME, DEFAULT_H2_JDBC_PASSWORD);
	}

	/*package*/ static synchronized Class<? extends Driver> initializeJdbcDriver() throws ClassNotFoundException {
		if (null == EmbeddedH2Util.JDBC_DRIVER_CLASS) {
			EmbeddedH2Util.JDBC_DRIVER_CLASS = (Class<? extends Driver>) ClASSLOADER.loadClass(H2_JDBC_DRIVER_CLASS);
		}
		return EmbeddedH2Util.JDBC_DRIVER_CLASS;
	}

	/*package*/ static String computeJdbcUrl(final String jdbcDatabaseDirectory, final String jdbcDatabaseName, final String jdbcConnectionProperties) {
		final String nonNullDatabaseName = (null == jdbcDatabaseName) ? DEFAULT_H2_DATABASENAME : jdbcDatabaseName;
		final String jdbcUrl;
		if (null == jdbcDatabaseDirectory) {	// In-Memory or On-Disk
			jdbcUrl = H2_JDBC_MEM_URL.replace(H2_DATABASENAME, nonNullDatabaseName);
		} else if (jdbcDatabaseDirectory.endsWith("/")) {
			jdbcUrl = H2_JDBC_DISK_URL.replace(H2_DATABASENAME, nonNullDatabaseName).replace(H2_DATABASEDIRECTORY, jdbcDatabaseDirectory);
		} else {
			jdbcUrl = H2_JDBC_DISK_URL.replace(H2_DATABASENAME, nonNullDatabaseName).replace(H2_DATABASEDIRECTORY, jdbcDatabaseDirectory) + "/";
		}
		if (null == jdbcConnectionProperties) {	// Connection properties
			return jdbcUrl + ";" + H2_DEFAULT_JDBC_CONNECTION_PROPERTIES;
		}
		return jdbcUrl + ";" + jdbcConnectionProperties;
	}

	public void initializeDatabase() throws Exception {
		EmbeddedH2Util.initializeJdbcDriver();
		try (final Connection conn = this.getConnection()) {
			// do nothing, other than testing the connection
		}
	}

	public Connection getConnection() throws SQLException {
		return DriverManager.getConnection(this.jdbcUrl, this.jdbcUsername, this.jdbcPassword);
	}

	public String getJdbcDatabaseName() {
		return this.jdbcDatabaseName;
	}
	public String getJdbcDatabaseDirectory() {
		return this.jdbcDatabaseDirectory;
	}
	public String getJdbcConnectionProperties() {
		return this.jdbcConnectionProperties;
	}
	public String getJdbcUrl() {
		return this.jdbcUrl;
	}
	public String getJdbcUsername() {
		return this.jdbcUrl;
	}
	public String getJdbcPassword() {
		return this.jdbcUrl;
	}

	public static int selectCount(final Connection conn, final String statement) throws SQLException, Exception {
		try (PreparedStatement s = conn.prepareStatement(statement)) {
			try (ResultSet rs = s.executeQuery()) {
				if (rs.next()) {
					return rs.getInt(1);
				}
				throw new Exception("No result");
			}
		}
	}

	public static ArrayList<ArrayList<Object>> selectData(final Connection conn, final String statement) throws SQLException {
		try (PreparedStatement s = conn.prepareStatement(statement)) {
			try (ResultSet rs = s.executeQuery()) {
				final ArrayList<ArrayList<Object>> rows = new ArrayList<>();
			    final ArrayList<Object> columnNames = getColumnNames(rs);
				rows.add(columnNames);
				while (rs.next()) {
					rows.add(getRowValues(rs, columnNames.size()));
				}
				return rows;
			}
		}
	}

	public static ArrayList<Object> getColumnNames(final ResultSet rs) throws SQLException {
		final ResultSetMetaData metadata = rs.getMetaData();
	    final int columnCount = metadata.getColumnCount();
	    final ArrayList<Object> columnNames = new ArrayList<>(columnCount);
	    for (int columnNumber = 1; columnNumber <= columnCount; columnNumber++) {
	        columnNames.add(metadata.getColumnName(columnNumber));      
	    }
	    return columnNames;
	}

	public static ArrayList<Object> getRowValues(final ResultSet rs, final int columns) throws SQLException {
	    final ArrayList<Object> values = new ArrayList<>(columns);
	    for (int columnNumber = 1; columnNumber <= columns; columnNumber++) {
	        values.add(rs.getObject(columnNumber));      
	    }
	    return values;
	}

	public static void printCsvs(final Connection conn, final String query, final String message) throws Exception {
		final ArrayList<ArrayList<Object>> data = EmbeddedH2Util.selectData(conn, query);
		final int count = data.size()-1;	// first row is column names, not row data
		if (0 == count) {
			LOG.log(Level.WARNING, message + " Count=" + count + "\n");
			return;
		}
		final StringBuilder sb = new StringBuilder(256);
		for (final ArrayList<Object> columnNamesAndRow : data) {
			StringUtil.appendJoin(sb, StringUtil.toString(columnNamesAndRow), ",").append("\n");
		}
		LOG.log(Level.WARNING, message + " Count=" + count + "\n" + sb.toString());
	}

	public static void printCount(final Connection conn, final String query, final String message) throws Exception {
		final int count = EmbeddedH2Util.selectCount(conn, query);
		LOG.log(Level.WARNING, message + " Count=" + count + "\n");
	}
}