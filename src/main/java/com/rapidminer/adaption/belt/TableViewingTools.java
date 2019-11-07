/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.adaption.belt;

import com.rapidminer.belt.table.BeltConverter;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.table.TableViewCreator;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.IOObject;


/**
 * Utility methods to view belt {@link IOTable}s as {@link ExampleSet}s for visualization purposes.
 *
 * Please note that this class is not part of any public API and might be modified or removed in future releases without
 * prior warning.
 *
 * @author Gisa Meier
 * @since 9.0.0
 */
public final class TableViewingTools {

	// Suppress default constructor for noninstantiability
	private TableViewingTools() {
		throw new AssertionError();
	}

	/**
	 * Check if the result is a data table.
	 *
	 * @param result
	 * 		the result to check
	 * @return whether the result should be displayed as a data table
	 */
	public static boolean isDataTable(IOObject result) {
		return result instanceof ExampleSet || result instanceof IOTable;
	}

	/**
	 * If the result is a {@link IOTable} it is replaced by a view allowing to read and display it as an {@link
	 * ExampleSet}. Custom columns are replaced by a nominal column containing an error message.
	 *
	 * @param result
	 * 		the result to check and maybe convert
	 * @return the input object or a view on a {@link Table}
	 */
	public static IOObject replaceTable(IOObject result) {
		if (result instanceof IOTable) {
			IOTable ioTable = (IOTable) result;
			try {
				return getView(ioTable);
			} catch (BeltConverter.ConversionException e) {
				return TableViewCreator.INSTANCE.createView(TableViewCreator.INSTANCE.replacedCustomsWithError(ioTable.getTable()));
			}
		}
		return result;
	}

	/**
	 * If the result is a {@link IOTable} it is replaced by a view allowing to read and display it as an {@link
	 * ExampleSet}.
	 *
	 * @param result
	 * 		the result to check and maybe convert
	 * @return the input object or a view on a {@link Table}
	 * @throws BeltConverter.ConversionException
	 * 		if the table cannot be converted because it contains custom columns
	 */
	public static Object replaceTableObject(Object result) {
		if (result instanceof IOTable) {
			return getView((IOTable) result);
		}
		return result;
	}

	/**
	 * Wraps the {@link Table} of the given {@link IOTable} into an {@link ExampleSet} in order to visualize it.
	 *
	 * @param object
	 * 		the table object to wrap
	 * @return a view example set
	 * @throws BeltConverter.ConversionException
	 * 	 	if the table cannot be converted because it contains custom columns
	 */
	public static ExampleSet getView(IOTable object) {
		Table table = object.getTable();
		return TableViewCreator.INSTANCE.createView(table);
	}

}