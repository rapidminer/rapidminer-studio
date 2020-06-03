/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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
package com.rapidminer.tools.belt;

import java.util.List;

import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.table.BeltConverter;
import com.rapidminer.belt.table.ColumnSelector;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.operator.UserError;


/**
 * Provides some convenience methods for using the belt framework.
 *
 * @author Kevin Majchrzak
 * @since 9.7.0
 */
public final class BeltTools {

	/**
	 * Disallow instances of this class.
	 */
	private BeltTools() {
		throw new AssertionError("No com.rapidminer.tools.belt.BeltTools instances for you!");
	}

	/**
	 * Returns a column selector containing the regular columns of the given table (columns without a {@link
	 * ColumnRole}).
	 *
	 * @param table
	 * 		the given table
	 * @return a {@link ColumnSelector}
	 */
	public static ColumnSelector selectRegularColumns(Table table) {
		return table.select().withoutMetaData(ColumnRole.class);
	}

	/**
	 * Returns a column selector containing the special columns of the given table (columns with a {@link ColumnRole}).
	 *
	 * @param table
	 * 		the given table
	 * @return a {@link ColumnSelector}
	 */
	public static ColumnSelector selectSpecialColumns(Table table) {
		return table.select().withMetaData(ColumnRole.class);
	}

	/**
	 * Return {@code true} iff the given label is the label of a special column (column with a {@link ColumnRole}) in
	 * the given table.
	 *
	 * @param table
	 * 		the table that contains the column
	 * @param label
	 * 		the column name
	 * @return {@code true} iff the column is a special column
	 */
	public static boolean isSpecial(Table table, String label) {
		return table.getFirstMetaData(label, ColumnRole.class) != null;
	}

	/**
	 * Throws an user error if any column in the given table is not nominal.
	 *
	 * @param table
	 * 		the table containing the columns to be checked
	 * @param origin
	 * 		this will be shown as the origin of the error
	 * @throws UserError
	 * 		if any column in the given table is not nominal
	 */
	public static void onlyNominal(Table table, String origin) throws UserError {
		List<String> labels = table.select().notOfTypeId(ColumnType.NOMINAL.id()).labels();
		if (!labels.isEmpty()) {
			throw new UserError(null, 103, origin, labels.iterator().next());
		}
	}

	/**
	 * Returns whether the type is an advanced column type, i.e. not part of the standard set of column types.
	 *
	 * @param type
	 * 		the column type
	 * @return {@code true} iff given type is advanced
	 */
	public static boolean isAdvanced(ColumnType<?> type) {
		return !BeltConverter.STANDARD_TYPES.contains(type.id());
	}

	/**
	 * Returns whether the table contains advanced columns.
	 *
	 * @param table
	 * 		the table to check
	 * @return {@code true} iff the table contains a column that is advanced
	 * @see #isAdvanced(ColumnType)
	 */
	public static boolean hasAdvanced(Table table) {
		for (int i = 0; i < table.width(); i++) {
			if (isAdvanced(table.column(i).type())) {
				return true;
			}
		}
		return false;
	}
}
