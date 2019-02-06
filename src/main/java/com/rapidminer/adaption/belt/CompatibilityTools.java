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
package com.rapidminer.adaption.belt;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.ColumnTypes;
import com.rapidminer.belt.execution.Context;
import com.rapidminer.belt.execution.Workload;
import com.rapidminer.belt.table.Builders;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.table.TableBuilder;


/**
 * Utility functions to provide compatibility with the legacy data core.
 *
 * Please note that this class is not part of any public API and might be modified or removed in future releases without
 * prior warning.
 *
 * @author Michael Knopf
 * @since 9.0.0
 */
public class CompatibilityTools {

	private CompatibilityTools() {
		// Suppress default constructor to prevent instantiation
		throw new AssertionError();
	}

	/**
	 * Converts all datetime columns of the given table to numeric columns containing the epoch milliseconds.
	 *
	 * @param table
	 * 		the table containing datetime columns
	 * @param context
	 * 		the context to work with
	 * @return table with all datetime columns replaced by numeric columns
	 */
	public static Table convertDatetimeToMilliseconds(Table table, Context context) {
		// Check for datetime columns
		List<String> labels = table.labels();
		List<String> datetimeLabels = new ArrayList<>();
		for (int i = 0; i < table.width(); i++) {
			Column column = table.column(i);
			if (ColumnTypes.DATETIME.equals(column.type())) {
				datetimeLabels.add(labels.get(i));
			}
		}

		// Nothing to convert...
		if (datetimeLabels.isEmpty()) {
			return table;
		}

		// Replace datetime columns by numeric columns containing the epoch milliseconds
		TableBuilder builder = Builders.newTableBuilder(table);
		for (String label : datetimeLabels) {
			Column replacement = table.transform(label).workload(Workload.SMALL)
					.applyObjectToReal(Instant.class,
							v -> v != null ? v.toEpochMilli() : Double.NaN,
							context).toColumn();
			builder.replace(label, replacement);
		}
		return builder.build(context);
	}

}
