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
package com.rapidminer.gui.new_plotter.gui.treenodes;

import com.rapidminer.gui.new_plotter.configuration.RangeAxisConfig;
import com.rapidminer.gui.new_plotter.configuration.ValueSource;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;


/**
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class RangeAxisConfigTreeNode extends DefaultMutableTreeNode implements Transferable {

	private static final long serialVersionUID = 1L;

	private final static String MIME_TYPE = DataFlavor.javaJVMLocalObjectMimeType + ";class="
			+ RangeAxisConfig.class.getName();
	public final static DataFlavor RANGE_AXIS_CONFIG_FLAVOR = new DataFlavor(MIME_TYPE, "RangeAxisConfigTreeNode");

	/**
	 * @param rangeAxis
	 */
	public RangeAxisConfigTreeNode(RangeAxisConfig rangeAxis) {
		super(rangeAxis, true);
	}

	public int getValueSourceIndex(ValueSource valueSource) {
		if (children == null) {
			return -1;
		}
		for (Object child : children) {
			if (valueSource == ((DefaultMutableTreeNode) child).getUserObject()) {
				return children.indexOf(child);
			}
		}
		return -1;
	}

	public TreeNode getChild(ValueSource source) {
		int valueSourceIndex = getValueSourceIndex(source);
		if (valueSourceIndex < 0) {
			return null;
		}
		return getChildAt(valueSourceIndex);
	}

	@Override
	public DataFlavor[] getTransferDataFlavors() {
		DataFlavor[] dataFlavour = { RANGE_AXIS_CONFIG_FLAVOR };
		return dataFlavour;
	}

	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		if (flavor.match(RANGE_AXIS_CONFIG_FLAVOR)) {
			return true;
		}
		return false;
	}

	@Override
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
		if (isDataFlavorSupported(flavor)) {
			return this;
		}
		throw new UnsupportedFlavorException(flavor);
	}

	@Override
	public boolean isLeaf() {
		return false;
	}

	@Override
	public RangeAxisConfig getUserObject() {
		return (RangeAxisConfig) super.getUserObject();
	}
}
