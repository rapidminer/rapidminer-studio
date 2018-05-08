/**
 * Copyright (C) 2001-2018 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.tools.usagestats;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

import org.h2.api.ErrorCode;
import org.h2.tools.DeleteDbFiles;

import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.LogService;


/**
 * Handles the H2 Database Connection
 *
 * @author Joao Pedro Pinheiro
 *
 * @since 7.5.0
 *
 */
enum CtaDataSource {
	INSTANCE;

	/** Database name */
	public static final String DATABASE = "cta";
	/**
	 * Params for h2
	 * <dl>
	 * <dt>AUTO_SERVER</dt>
	 * <dd>Allow connections from multiple RapidMiner instances</dd>
	 * <dt>FILE_LOCK</dt>
	 * <dd>.lock file approach, compatible to all file systems</dd>
	 * <dt>MV_STORE</dt>
	 * <dd>we use the old pageStore since its using less disk space</dd>
	 * </dl>
	 */
	public static final String PARAMS = ";AUTO_SERVER=TRUE;FILE_LOCK=FILE;MV_STORE=FALSE";
	/** SQL Statements */
	private static final String CREATE_EVENT_TABLE_STATEMENT = "CREATE CACHED TABLE IF NOT EXISTS event ( type VARCHAR, value VARCHAR, argument VARCHAR, count BIGINT, timestamp TIMESTAMP DEFAULT NOW())";
	private static final String CREATE_RULE_TABLE_STATEMENT = "CREATE CACHED TABLE IF NOT EXISTS rule ( id VARCHAR, triggered TIMESTAMP DEFAULT NOW(), action VARCHAR)";
	private static final String CREATE_CONSTANTS_TABLE_STATEMENT = "CREATE CACHED TABLE IF NOT EXISTS studio_constants ( con VARCHAR PRIMARY KEY, val VARCHAR)";
	private static final String CREATE_EVENT_INDEX = "CREATE INDEX IF NOT EXISTS event_index ON event(type, value, argument)";
	private static final String CREATE_RULE_INDEX = "CREATE INDEX IF NOT EXISTS id_index ON rule(id)";

	private final String RM_HOME = FileSystemService.getUserRapidMinerDir().getAbsolutePath();
	private final String DB_CONNECTION = "jdbc:h2:" + RM_HOME + File.separator + DATABASE + PARAMS;

	private Connection connection = null;

	/**
	 * Prepares the connection, removes corrupted database if necessary
	 */
	CtaDataSource() {
		try {
			// Might be needed in packaged version
			// Class.forName("org.h2.Driver");
			connection = getConnection();
		} catch (SQLException e) {
			if (e.getErrorCode() == ErrorCode.FILE_CORRUPTED_1) {
				// If the DB is corrupted, delete the files
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.usagestats.CtaDataSource.database.currupted",
						e);
				DeleteDbFiles.execute(RM_HOME, DATABASE, true);
				// The connection should be hopefully established on the next getConnection() call.
			} else {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.usagestats.CtaDataSource.open.failed", e);
			}

		}
	}

	/**
	 * This method returns the connection to the DB
	 *
	 * @return The connection
	 * @throws SQLException
	 */
	public Connection getConnection() throws SQLException {
		if (connection == null) {
			connection = DriverManager.getConnection(DB_CONNECTION);
			try (Statement stmt = connection.createStatement()) {
				stmt.executeUpdate(CREATE_EVENT_TABLE_STATEMENT);
				stmt.executeUpdate(CREATE_RULE_TABLE_STATEMENT);
				stmt.executeUpdate(CREATE_CONSTANTS_TABLE_STATEMENT);
				stmt.executeUpdate(CREATE_EVENT_INDEX);
				stmt.executeUpdate(CREATE_RULE_INDEX);
			} catch (SQLException e) {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.usagestats.CtaDataSource.open.failed", e);
			}
		}
		return connection;
	}

}
