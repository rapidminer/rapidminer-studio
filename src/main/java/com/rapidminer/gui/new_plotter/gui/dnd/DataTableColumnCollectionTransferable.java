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
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;


/**
 * A container to transfer multiple {@link DataTableColumn}s via DragAndDrop.
 * 
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class DataTableColumnCollectionTransferable implements Transferable {

	private final DataTableColumnCollection collection;

	public DataTableColumnCollectionTransferable(DataTableColumnCollection collection) {
		this.collection = collection;
	}

	@Override
	public DataFlavor[] getTransferDataFlavors() {
		DataFlavor[] dataFlavour = { DataTableColumnCollection.DATATABLE_COLUMN_COLLECTION_FLAVOR };
		return dataFlavour;
	}

	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		if (flavor.match(DataTableColumnCollection.DATATABLE_COLUMN_COLLECTION_FLAVOR)) {
			return true;
		}
		return false;
	}

	@Override
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
		if (isDataFlavorSupported(flavor)) {
			return collection;
		}
		throw new UnsupportedFlavorException(flavor);
	}

}
