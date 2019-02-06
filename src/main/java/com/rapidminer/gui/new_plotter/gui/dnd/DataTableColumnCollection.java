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
package com.rapidminer.gui.new_plotter.gui.dnd;

import com.rapidminer.gui.new_plotter.configuration.DataTableColumn;

import java.awt.datatransfer.DataFlavor;
import java.util.List;


/**
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class DataTableColumnCollection {

	private final static String MIME_TYPE = DataFlavor.javaJVMLocalObjectMimeType + ";class="
			+ DataTableColumnCollection.class.getName();
	public final static DataFlavor DATATABLE_COLUMN_COLLECTION_FLAVOR = new DataFlavor(MIME_TYPE,
			"DataTableColumnCollection");

	private final List<DataTableColumn> collection;

	/**
	 * @return the collection
	 */
	public List<DataTableColumn> getDataTableColumns() {
		return collection;
	}

	public DataTableColumnCollection(List<DataTableColumn> collection) {
		this.collection = collection;

	}

}
