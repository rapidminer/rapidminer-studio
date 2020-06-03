/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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
import com.rapidminer.belt.table.DatetimeTableWrapper;
import com.rapidminer.belt.table.DoubleTableWrapper;
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
	 * ExampleSet}. Advanced columns are replaced by a nominal column containing an error message.
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
				ExampleSet view =
						TableViewCreator.INSTANCE.createView(TableViewCreator.INSTANCE.replacedAdvancedWithError(ioTable.getTable()));
				copySourceAndAnnotations(ioTable, view);
				return view;
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
	 * 		if the table cannot be converted because it contains advanced columns
	 */
	public static Object replaceTableObject(Object result) {
		if (result instanceof IOTable) {
			return getView((IOTable) result);
		}
		return result;
	}

	/**
	 * Replaces the class name the same way as the other methods from this class replace the objects themselves.
	 * Necessary since {@link IOTable}s are temporarily viewed/written as {@link ExampleSet}s.
	 *
	 * @param data
	 * 		the ioobject to get the class name for
	 * @return the class name of what the data is stored as
	 * @since 9.6.0
	 */
	public static String replaceTableClassName(IOObject data) {
		return data instanceof IOTable ? ExampleSet.class.getName() : data.getClass().getName();
	}

	/**
	 * Wraps the {@link Table} of the given {@link IOTable} into an {@link ExampleSet} in order to visualize it.
	 *
	 * @param object
	 * 		the table object to wrap
	 * @return a view example set
	 * @throws BeltConverter.ConversionException
	 * 	 	if the table cannot be converted because it contains advanced columns
	 */
	public static ExampleSet getView(IOTable object) {
		Table table = object.getTable();
		ExampleSet view = TableViewCreator.INSTANCE.createView(table);
		copySourceAndAnnotations(object, view);
		return view;
	}

	/**
	 * Checks if the {@link ExampleSet} is a wrapper for a belt {@link Table} that can only be used for viewing purposes.
	 *
	 * @param exampleSet
	 * 		the example set to check
	 * @return {@code true} iff the example set cannot be in processes
	 * @since 9.6.0
	 */
	public static boolean isViewWrapper(ExampleSet exampleSet) {
		return exampleSet instanceof DoubleTableWrapper || exampleSet instanceof DatetimeTableWrapper;
	}

	/**
	 * Copies annotations and the source from the {@link IOTable} to the {@link ExampleSet}. When converting the {@link
	 * Table} to an {@link ExampleSet} this information cannot be handled.
	 */
	private static void copySourceAndAnnotations(IOTable from, ExampleSet to) {
		to.setSource(from.getSource());
		to.getAnnotations().putAll(from.getAnnotations());
	}
}