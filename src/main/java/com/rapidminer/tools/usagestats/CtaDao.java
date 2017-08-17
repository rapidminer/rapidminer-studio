/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;
import java.util.logging.Level;

import com.rapidminer.tools.LogService;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector.Key;


/**
 * Data access object for the call to action database
 *
 * @author Jonas Wilms-Pfau
 * @since 7.5.0
 */
enum CtaDao {
	INSTANCE;

	/** Insert a new event with the current timestamp */
	private static final String INSERT_EVENT_QUERY = "INSERT INTO event(type,value,argument,count) VALUES (?,?,?,?)";

	/** Insert a new rule trigger event with the current timestamp */
	private static final String INSERT_RULE_QUERY = "INSERT INTO rule(id, action) VALUES (?,?)";

	/** This query returns an empty result set, if the offset is larger than the event count */
	private static final String SELECT_OVER_250K = "SELECT timestamp FROM event ORDER BY timestamp DESC LIMIT 1 OFFSET 250000";

	/** Delete events older than the given timestamp */
	private static final String DELETE_OLDER_THAN = "DELETE FROM event WHERE timestamp < ?";

	/** Delete events older than one month */
	private static final String DELETE_OLDER_ONE_MONTH = "DELETE FROM event WHERE timestamp < DATEADD('MONTH', -1, NOW())";

	/**
	 * Rule verification query cache, weak since old rules might be deleted
	 */
	private Map<Rule, List<PreparedStatement>> queryCache = new WeakHashMap<>();

	private PreparedStatement insertEvent = null;
	private PreparedStatement insertRule = null;
	private PreparedStatement selectOver250k = null;
	private PreparedStatement deleteOlderThan = null;
	private PreparedStatement deleteOlderOneMonth = null;

	private CtaDao() {
		try {
			Connection connection = CtaDataSource.INSTANCE.getConnection();
			insertEvent = connection.prepareStatement(INSERT_EVENT_QUERY);
			insertRule = connection.prepareStatement(INSERT_RULE_QUERY);
			selectOver250k = connection.prepareStatement(SELECT_OVER_250K);
			deleteOlderThan = connection.prepareStatement(DELETE_OLDER_THAN);
			deleteOlderOneMonth = connection.prepareStatement(DELETE_OLDER_ONE_MONTH);
		} catch (SQLException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.usagestats.CtaDao.init.failure", e);
		}
	}

	/**
	 * Store a rule triggered event
	 *
	 * @param rule
	 *            The Rule that has been triggered
	 * @param action
	 *            The users action
	 * @throws SQLException
	 */
	public void triggered(Rule rule, String action) throws SQLException {
		insertRule.setString(1, rule.getId());
		insertRule.setString(2, action);
		insertRule.execute();
	}

	/**
	 * Store events
	 *
	 * @param events
	 * @throws SQLException
	 */
	public void storeEvents(Map<Key, Long> events) throws SQLException {
		for (Entry<Key, Long> event : events.entrySet()) {
			Key key = event.getKey();
			insertEvent.setString(1, key.getType());
			insertEvent.setString(2, key.getValue());
			insertEvent.setString(3, key.getArg());
			insertEvent.setLong(4, event.getValue());
			insertEvent.addBatch();
			// We do not clear the parameter since we override them anyway
		}
		// This is also working for 0 batches
		insertEvent.executeBatch();
	}

	/**
	 * Verifies the rules
	 *
	 * @param rule
	 * @return true if the rule is fulfilled
	 * @throws SQLException
	 */
	public boolean verify(Rule rule) throws SQLException {
		// An empty rule should not be triggered
		boolean isValid = false;
		List<PreparedStatement> statements = getStatements(rule);
		int index = -1;
		for (PreparedStatement statement : statements) {
			index++;
			try (ResultSet result = statement.executeQuery()) {
				if (result.next()) {
					isValid = result.getBoolean(1);
				}
				// Abort if one condition is unsatisfied
				if (!isValid) {
					// Fail earlier next time
					if (index > 0) {
						Collections.swap(statements, 0, index);
					}
					return isValid;
				}
			}
		}
		return isValid;
	}

	/**
	 * Return a list of prepared statements for the rule
	 *
	 * @param rule
	 * @return
	 * @throws SQLException
	 */
	private List<PreparedStatement> getStatements(Rule rule) throws SQLException {
		List<PreparedStatement> statements = queryCache.get(rule);
		// Prepare the statements if necessary
		if (statements == null) {
			statements = new ArrayList<>();
			Connection connection = CtaDataSource.INSTANCE.getConnection();
			for (String query : rule.getQueries()) {
				statements.add(connection.prepareStatement(query));
			}
			queryCache.put(rule, statements);
		}
		return statements;
	}

	/**
	 * Cleans up the Database
	 * <ul>
	 * <li>Delete all events older than a month</li>
	 * <li>Delete all events over 1.000.000</li>
	 * </ul>
	 *
	 * @throws SQLException
	 */
	public void cleanUpDatabase() throws SQLException {
		deleteOlderOneMonth.executeUpdate();
		try (ResultSet oldResult = selectOver250k.executeQuery()) {
			if (oldResult.next()) {
				Timestamp old = oldResult.getTimestamp(1);
				deleteOlderThan.setTimestamp(1, old);
				deleteOlderThan.executeUpdate();
			}
		}
	}

}
