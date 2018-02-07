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
package com.rapidminer.gui.dnd;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.Arrays;

import com.rapidminer.repository.RepositoryLocation;


/**
 * Provides a transferable wrapper for {@link com.rapidminer.repository.Entry}s in order to drag-n-drop them.
 * 
 * @author Marco Boeck
 * @since 8.1
 */
public class TransferableRepositoryEntry implements Transferable {


	public static final DataFlavor LOCAL_TRANSFERRED_REPOSITORY_LOCATION_FLAVOR = new DataFlavor(
			DataFlavor.javaJVMLocalObjectMimeType + ";class=" + RepositoryLocation.class.getName(), "repository location");

	public static final DataFlavor LOCAL_TRANSFERRED_REPOSITORY_LOCATION_LIST_FLAVOR = new DataFlavor(
			DataFlavor.javaJVMLocalObjectMimeType + ";class=" + RepositoryLocationList.class.getName(),
			"repository locations");

	private static final DataFlavor[] DATA_FLAVORS = { LOCAL_TRANSFERRED_REPOSITORY_LOCATION_FLAVOR, LOCAL_TRANSFERRED_REPOSITORY_LOCATION_LIST_FLAVOR,
			DataFlavor.stringFlavor };

	private final RepositoryLocation[] location;

	/**
	 * The transferable location(s).
	 *
	 * @param location
	 * 		the location(s) of the entry/entries which should be drag & dropped
	 */
	public TransferableRepositoryEntry(RepositoryLocation... location) {
		if (location == null) {
			throw new IllegalArgumentException("location must not be null!");
		}

		this.location = location;
	}

	@Override
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
		if (flavor.equals(LOCAL_TRANSFERRED_REPOSITORY_LOCATION_FLAVOR)) {
			return location[0];
		} else if (flavor.equals(LOCAL_TRANSFERRED_REPOSITORY_LOCATION_LIST_FLAVOR)) {
			return location;
		} else if (flavor.equals(DataFlavor.stringFlavor)) {
			if (location.length == 1) {
				return location[0].toString();
			} else {
				return Arrays.toString(location);
			}
		} else {
			throw new UnsupportedFlavorException(flavor);
		}
	}

	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return Arrays.asList(DATA_FLAVORS).contains(flavor);
	}

	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return DATA_FLAVORS;
	}

}
