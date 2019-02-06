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
package com.rapidminer.studio.io.gui.internal.steps.configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.rapidminer.core.io.data.ColumnMetaData;
import com.rapidminer.tools.AbstractObservable;
import com.rapidminer.tools.I18N;


/**
 * Validates the settings of the {@link ConfigureDataStep}. Notifies observers if the parsing errors
 * or the column errors have changed. Notifies observes with a set of indices if the columns with
 * this indices now have a duplicated name/role or don't anymore.
 *
 * @author Dominik Halfkann, Gisa Schaefer
 */
class ConfigureDataValidator extends AbstractObservable<Set<Integer>> {

	private static final String AND = " " + I18N.getGUILabel("io.dataimport.step.data_column_configuration.error_table.and")
			+ " ";

	/** map from column role to the indices of columns with this role */
	private final Map<String, List<Integer>> columnRoles = new HashMap<String, List<Integer>>();

	/** map from column name to the indices of columns with this name */
	private final Map<String, List<Integer>> columnNames = new HashMap<String, List<Integer>>();

	/** set of column indices that appear in the {@link #parsingErrorList} */
	private final Set<Integer> parsingErrorAffectedColumns = new HashSet<>();

	/** set of indices of columns that have the same name as some other column(s) */
	private final Set<Integer> duplicateNameColumn = new HashSet<Integer>();

	/**
	 * set of indices of columns that had the same name as some other column(s) in the last
	 * validation
	 */
	private final Set<Integer> oldDuplicateNameColumn = new HashSet<Integer>();

	/** set of indices of columns that have the same role as some other column(s) */
	private final Set<Integer> duplicateRoleColumn = new HashSet<Integer>();

	/**
	 * set of indices of columns that had the same role as some other column(s) in the last
	 * validation
	 */
	private final Set<Integer> oldDuplicateRoleColumn = new HashSet<Integer>();

	/**
	 * The list of column errors (duplicate names or roles), updated on every call of
	 * {@link #validate(int)}
	 */
	private final List<ColumnError> columnErrorList = new LinkedList<>();

	/**
	 * The list of column errors (duplicate names or roles) from the last call
	 */
	private final List<ColumnError> oldColumnErrorList = new LinkedList<>();

	/** Keeps track of which columns are not removed */
	private final Set<Integer> selectedColumns = new HashSet<>();

	/** The list of parsing errors, only changed by {@link #setParsingErrors(List)} */
	private List<ParsingError> parsingErrorList = new ArrayList<ParsingError>();

	/** the meta data for the columns */
	private List<ColumnMetaData> columnMetaData;

	/**
	 * Initializes the validator with the given columnMetaData.
	 *
	 * @param columnMetaData
	 */
	void init(List<ColumnMetaData> columnMetaData) {
		this.columnMetaData = columnMetaData;
		columnRoles.clear();
		columnNames.clear();
		int columnIndex = 0;
		for (ColumnMetaData column : columnMetaData) {
			addColumnToColumnsMaps(columnIndex, column);
			if (!column.isRemoved()) {
				selectedColumns.add(columnIndex);
			}
			columnIndex++;
		}
		checkForDuplicates();
	}

	/**
	 * Adds the column to {@link #columnNames} and {@link #columnRoles}.
	 *
	 * @param columnIndex
	 *            the index of the column
	 * @param column
	 *            the meta data of the column
	 */
	private void addColumnToColumnsMaps(int columnIndex, ColumnMetaData column) {
		if (!column.isRemoved()) {
			// add to name map
			List<Integer> listForName = columnNames.get(column.getName());
			if (listForName == null) {
				List<Integer> indexList = Collections.synchronizedList(new ArrayList<>());
				indexList.add(columnIndex);
				columnNames.put(column.getName(), indexList);
			} else {
				listForName.add(columnIndex);
			}
			// add to role map
			String role = column.getRole();
			if (role != null) {
				List<Integer> listForRole = columnRoles.get(role);
				if (listForRole == null) {
					List<Integer> indexList = Collections.synchronizedList(new ArrayList<>());
					indexList.add(columnIndex);
					columnRoles.put(role, indexList);
				} else {
					listForRole.add(columnIndex);
				}
			}
		}
	}

	/**
	 * Sets the parsing errors.
	 *
	 * @param parsingErrors
	 *            the parsing errors to set
	 */
	void setParsingErrors(List<ParsingError> parsingErrors) {
		this.parsingErrorList = parsingErrors;
		for (ParsingError error : parsingErrors) {
			parsingErrorAffectedColumns.add(error.getColumn());
		}
		fireUpdate();
	}

	/**
	 * @return the {@link ParsingError}s of columns that are not removed
	 */
	List<ParsingError> getParsingErrors() {
		List<ParsingError> errorList = new LinkedList<>();
		for (ParsingError error : parsingErrorList) {
			if (!columnMetaData.get(error.getColumn()).isRemoved()) {
				errorList.add(error);
			}
		}
		return errorList;
	}

	/**
	 * @return the list of {@link ColumnError}s
	 */
	List<ColumnError> getColumnErrors() {
		return columnErrorList;
	}

	/**
	 * Deletes the columnIndex from the maps and adds it again.
	 *
	 * @param columnIndex
	 *            the index of the column to update
	 * @return
	 */
	private void updateColumnMaps(int columnIndex) {
		deleteColumnIndexFromMaps(columnIndex);
		addColumnToColumnsMaps(columnIndex, columnMetaData.get(columnIndex));
	}

	/**
	 * Validates the settings for the column with the given columnIndex.
	 *
	 * @param columnIndex
	 *            the index of the column to check
	 */
	void validate(int columnIndex) {
		updateColumnMaps(columnIndex);
		checkForDuplicates();
		checkEmptySelection(columnIndex);
		checkIfUpdate(columnIndex);
	}

	/**
	 * Checks if all columns are removed.
	 */
	private void checkEmptySelection(int columnIndex) {
		if (columnMetaData.get(columnIndex).isRemoved()) {
			selectedColumns.remove(columnIndex);
		} else {
			selectedColumns.add(columnIndex);
		}
		if (selectedColumns.isEmpty()) {
			columnErrorList.add(new ColumnError(Collections.<Integer> emptyList(), null,
					I18N.getGUILabel("io.dataimport.step.data_column_configuration.error_table.no_column_error")));
		}
	}

	/**
	 * Stores the indices of the columns with duplicate name or role and stores the associated
	 * errors to the list of {@link ColumnError}s.
	 */
	private void checkForDuplicates() {
		oldDuplicateNameColumn.clear();
		oldDuplicateNameColumn.addAll(duplicateNameColumn);
		duplicateNameColumn.clear();

		oldDuplicateRoleColumn.clear();
		oldDuplicateRoleColumn.addAll(duplicateRoleColumn);
		duplicateRoleColumn.clear();

		oldColumnErrorList.clear();
		oldColumnErrorList.addAll(columnErrorList);
		columnErrorList.clear();

		for (Entry<String, List<Integer>> roleEntry : columnRoles.entrySet()) {
			if (roleEntry.getValue().size() > 1) {
				duplicateRoleColumn.addAll(roleEntry.getValue());
				columnErrorList.add(makeDuplicateRoleError(roleEntry));
			}
		}

		for (Entry<String, List<Integer>> nameEntry : columnNames.entrySet()) {
			if (nameEntry.getValue().size() > 1) {
				duplicateNameColumn.addAll(nameEntry.getValue());
				columnErrorList.add(makeDuplicateNameError(nameEntry));
			}
		}

	}

	/**
	 * Creates an error for the given roleEntry.
	 */
	private ColumnError makeDuplicateRoleError(Entry<String, List<Integer>> roleEntry) {
		final String duplicateRoleMessage = I18N.getGUILabel(
				"io.dataimport.step.data_column_configuration.error_table.column_error.duplicate_role_message",
				roleEntry.getKey(), listToString(roleEntry.getValue()));
		return new ColumnError(roleEntry.getValue(), roleEntry.getKey(), duplicateRoleMessage);
	}

	/**
	 * Create an error for the given nameEntry.
	 */
	private ColumnError makeDuplicateNameError(Entry<String, List<Integer>> nameEntry) {
		String duplicatenNameMessage = I18N.getGUILabel(
				"io.dataimport.step.data_column_configuration.error_table.column_error.duplicate_name_message",
				nameEntry.getKey(), listToString(nameEntry.getValue()));
		return new ColumnError(nameEntry.getValue(), nameEntry.getKey(), duplicatenNameMessage);
	}

	/**
	 * Converts the integer list to a string where the entries are separated by "," and "and".
	 */
	private static String listToString(List<Integer> list) {
		Collections.sort(list);
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < list.size(); i++) {
			Integer column = list.get(i) + 1;
			if (i < list.size() - 2) {
				builder.append(column);
				builder.append(", ");
			} else if (i == list.size() - 2) {
				builder.append(column);
				builder.append(AND);
			} else {
				builder.append(column);
			}
		}
		return builder.toString();
	}

	/**
	 * Checks if an update should be fired. First checks if the sets of duplicate name or role
	 * columns have changed. If this is not the case checks if the column errors have changed or if
	 * a column changed that has a parsing error.
	 *
	 */
	private void checkIfUpdate(int columnIndex) {
		if (!oldDuplicateNameColumn.equals(duplicateNameColumn) || !oldDuplicateRoleColumn.equals(duplicateRoleColumn)) {
			// if one of the duplicates lists aren't equal, fire update
			Set<Integer> columnsUpdate = new HashSet<Integer>();
			columnsUpdate.addAll(oldDuplicateNameColumn);
			columnsUpdate.addAll(duplicateNameColumn);
			columnsUpdate.addAll(oldDuplicateRoleColumn);
			columnsUpdate.addAll(duplicateRoleColumn);

			fireUpdate(columnsUpdate);
		} else if (!oldColumnErrorList.equals(columnErrorList) || parsingErrorAffectedColumns.contains(columnIndex)) {
			// fire update with no indices if either the column errors changed or a parsing error
			// affected column changed
			fireUpdate();
		}
	}

	/**
	 * Deletes the columnNumber from {@link #columnNames} and {@link #columnRoles}.
	 */
	private void deleteColumnIndexFromMaps(int columnNumber) {
		for (Entry<String, List<Integer>> nameEntry : columnNames.entrySet()) {
			if (nameEntry.getValue() != null) {
				nameEntry.getValue().remove((Integer) columnNumber);
			}
		}
		for (Entry<String, List<Integer>> roleEntry : columnRoles.entrySet()) {
			if (roleEntry.getValue() != null) {
				roleEntry.getValue().remove((Integer) columnNumber);
			}
		}
	}

	/**
	 * Whether the column has a name that is also used in another column.
	 *
	 * @param column
	 *            the index of the column to check
	 * @return {@code true} if the name of this column is also used in another column
	 */
	boolean isDuplicateNameColumn(int column) {
		return duplicateNameColumn.contains(column);
	}

	/**
	 * Whether the column has a role that is also used in another column.
	 *
	 * @param column
	 *            the index of the column to check
	 * @return {@code true} if the role of this column is also used in another column
	 */
	boolean isDuplicateRoleColumn(int column) {
		return duplicateRoleColumn.contains(column);
	}

	/**
	 * Checks if the given column name is already in use.
	 *
	 * @param name
	 *            the column name which should be checked
	 * @return {@code true} if a duplicate entry exists, otherwise {@code false}
	 */
	boolean isNameUsed(String name) {
		List<Integer> columnsWithName = columnNames.get(name);
		return columnsWithName != null && !columnsWithName.isEmpty();
	}

	/**
	 * Checks if the given role is already in use.
	 *
	 * @param role
	 *            the role which should be checked
	 * @return {@code true} if a duplicate entry exists, otherwise {@code false}
	 */
	public boolean isRoleUsed(String role) {
		List<Integer> columnsWithRole = columnRoles.get(role);
		return columnsWithRole != null && !columnsWithRole.isEmpty();
	}
}
