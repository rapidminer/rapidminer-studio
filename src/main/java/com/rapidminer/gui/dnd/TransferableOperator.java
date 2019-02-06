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
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.rapidminer.operator.Operator;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.usagestats.DefaultUsageLoggable;
import com.rapidminer.tools.usagestats.UsageLoggable;


/**
 * Provides a transferable wrapper for Operators in order to drag-n-drop them in the Process-Tree.
 * 
 * @see com.rapidminer.gui.operatortree.OperatorTree
 * @author Helge Homburg, Michael Knopf, Adrian Wilke
 */
public class TransferableOperator extends DefaultUsageLoggable implements Transferable {

	public static final DataFlavor LOCAL_TRANSFERRED_OPERATORS_FLAVOR = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType
			+ ";class=" + Operator.class.getName(), "RapidMiner operator");

	public static final DataFlavor LOCAL_TRANSFERRED_REPOSITORY_LOCATION_FLAVOR = TransferableRepositoryEntry.LOCAL_TRANSFERRED_REPOSITORY_LOCATION_FLAVOR;

	public static final DataFlavor LOCAL_TRANSFERRED_REPOSITORY_LOCATION_LIST_FLAVOR = TransferableRepositoryEntry.LOCAL_TRANSFERRED_REPOSITORY_LOCATION_LIST_FLAVOR;

	private static final DataFlavor[] DATA_FLAVORS = { TransferableOperator.LOCAL_TRANSFERRED_OPERATORS_FLAVOR,
			DataFlavor.stringFlavor };

	private final Operator[] originalOperators;

	private final Operator[] clonedOperators;

	public TransferableOperator(Operator[] operators) {
		// references to original operators (required to delete operators in case of a cut and paste
		// event)
		this.originalOperators = operators;
		// cloning the operators ensures that further editing (e.g., in the process view) does not
		// affect the copied elements
		List<Operator> clones = Tools.cloneOperators(Arrays.asList(operators));
		this.clonedOperators = clones.toArray(new Operator[clones.size()]);
	}

	@Override
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
		if (flavor.equals(UsageLoggable.USAGE_FLAVOR)){
			// trigger usage stats if applicable
			logUsageStats();
			return null;
		}
		if (flavor.equals(LOCAL_TRANSFERRED_OPERATORS_FLAVOR)) {
			return this.clonedOperators;
		}
		if (flavor.equals(DataFlavor.stringFlavor)) {
			StringBuilder b = new StringBuilder();
			for (Operator op : clonedOperators) {
				b.append(op.getXML(false));
			}
			return b.toString();
		}
		throw new UnsupportedFlavorException(flavor);
	}

	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return Arrays.asList(DATA_FLAVORS).contains(flavor);
	}

	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return DATA_FLAVORS;
	}

	/**
	 * @return an array that contains references to the original cloned operators.
	 */
	protected Operator[] getOperators() {
		return this.originalOperators;
	}

}
