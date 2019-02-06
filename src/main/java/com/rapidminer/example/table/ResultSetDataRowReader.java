/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
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
package com.rapidminer.example.table;

import com.rapidminer.example.Attribute;
import com.rapidminer.tools.LogService;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;


/**
 * Unlike a {@link FileDataRowReader} that reads examples from a file, objects of this class read
 * examples from a {@link ResultSet}, a data structure that is returned from a database query.
 * 
 * @see com.rapidminer.tools.jdbc.DatabaseHandler
 * @see com.rapidminer.operator.io.DatabaseDataReader
 * @see com.rapidminer.operator.io.KDBExampleSource
 * @author Simon Fischer, Ingo Mierswa ingomierswa Exp $
 */
public class ResultSetDataRowReader extends AbstractDataRowReader {

	private Attribute[] attributes;

	private ResultSet resultSet;

	private static final int DONT_KNOW_YET = 0;

	private static final int YES = 1;

	private static final int NO = 2;

	private int hasNext = DONT_KNOW_YET;

	/**
	 * Constructor.
	 * 
	 * @param attributeList
	 *            List of attributes
	 * @param resultSet
	 *            A ResultSet as returned from a database query
	 */
	public ResultSetDataRowReader(DataRowFactory dataRowFactory, List<Attribute> attributeList, ResultSet resultSet) {
		super(dataRowFactory);
		this.resultSet = resultSet;
		this.attributes = new Attribute[attributeList.size()];
		attributeList.toArray(this.attributes);
	}

	@Override
	public boolean hasNext() {
		switch (hasNext) {
			case YES:
				return true;
			case NO:
				return false;
			case DONT_KNOW_YET:
				try {
					if (resultSet.next()) {
						hasNext = YES;
						return true;
					} else {
						hasNext = NO;
						resultSet.close();
						return false;
					}
				} catch (SQLException e) {
					// LogService.getGlobal().logError("While reading examples from result set: " +
					// e.getMessage());
					LogService
							.getRoot()
							.log(Level.SEVERE,
									"com.rapidminer.example.table.ResultSetDataRowReader.error_while_reading_examples_from_result_set",
									e.getMessage());
					return false;
				}
			default:
				// impossible
				return false;
		}
	}

	@Override
	public DataRow next() {
		if (hasNext()) {
			hasNext = DONT_KNOW_YET;
			try {
				DataRow row = getFactory().create(attributes.length);
				for (int i = 0; i < attributes.length; i++) {
					double value = DatabaseDataRow.readColumn(resultSet, attributes[i]);
					row.set(attributes[i], value);
				}
				row.trim();
				return row;
			} catch (SQLException sqle) {
				throw new RuntimeException("Error accessing the result of a query:" + sqle.toString());
			}
		} else {
			return null;
		}
	}
}
