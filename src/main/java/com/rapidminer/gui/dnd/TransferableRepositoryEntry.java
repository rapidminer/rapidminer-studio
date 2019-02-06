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
package com.rapidminer.gui.dnd;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.Arrays;

import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.usagestats.DefaultUsageLoggable;
import com.rapidminer.tools.usagestats.UsageLoggable;


/**
 * Provides a transferable wrapper for {@link com.rapidminer.repository.Entry}s in order to drag-n-drop them.
 * 
 * @author Marco Boeck
 * @since 8.1
 */
public class TransferableRepositoryEntry extends DefaultUsageLoggable implements Transferable {


	public static final DataFlavor LOCAL_TRANSFERRED_REPOSITORY_LOCATION_FLAVOR = new DataFlavor(
			DataFlavor.javaJVMLocalObjectMimeType + ";class=" + RepositoryLocation.class.getName(), "repository location");

	public static final DataFlavor LOCAL_TRANSFERRED_REPOSITORY_LOCATION_LIST_FLAVOR = new DataFlavor(
			DataFlavor.javaJVMLocalObjectMimeType + ";class=\"" + RepositoryLocation[].class.getName() + "\"",
			"repository locations");

	private static final DataFlavor[] DATA_FLAVORS = { LOCAL_TRANSFERRED_REPOSITORY_LOCATION_FLAVOR, LOCAL_TRANSFERRED_REPOSITORY_LOCATION_LIST_FLAVOR,
			DataFlavor.stringFlavor };

	private final RepositoryLocation[] location;
	private final DataFlavor contentFlavor;

	/**
	 * The transferable location(s).
	 *
	 * @param location
	 * 		the location(s) of the entry/entries which should be drag & dropped
	 */
	public TransferableRepositoryEntry(RepositoryLocation... location) {
		if (location == null || location.length == 0) {
			throw new IllegalArgumentException("location must not be null or empty!");
		}

		this.location = location;
		this.contentFlavor = location.length == 1? LOCAL_TRANSFERRED_REPOSITORY_LOCATION_FLAVOR : LOCAL_TRANSFERRED_REPOSITORY_LOCATION_LIST_FLAVOR;
	}

	@Override
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
		if (flavor.equals(UsageLoggable.USAGE_FLAVOR)){
			// trigger usage stats if applicable
			logUsageStats();
			return null;
		}
		if (flavor.equals(LOCAL_TRANSFERRED_REPOSITORY_LOCATION_FLAVOR)) {
			return location[0];
		}
		if (flavor.equals(LOCAL_TRANSFERRED_REPOSITORY_LOCATION_LIST_FLAVOR)) {
			return location;
		}
		if (flavor.equals(DataFlavor.stringFlavor)) {
			return location.length == 1 ? location[0].toString() : Arrays.toString(location);
		}
		throw new UnsupportedFlavorException(flavor);
	}

	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return flavor.equals(contentFlavor) || flavor.equals(DataFlavor.stringFlavor);
	}

	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[]{contentFlavor, DataFlavor.stringFlavor};
	}

}
