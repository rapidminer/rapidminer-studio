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

import com.rapidminer.gui.dnd.AbstractPatchedTransferHandler;
import com.rapidminer.gui.new_plotter.configuration.DataTableColumn;
import com.rapidminer.gui.new_plotter.configuration.ValueSource;
import com.rapidminer.gui.new_plotter.configuration.ValueSource.SeriesUsageType;
import com.rapidminer.gui.new_plotter.gui.AttributeDropTextField;
import com.rapidminer.gui.new_plotter.gui.treenodes.ValueSourceTreeNode;

import java.awt.datatransfer.Transferable;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;


/**
 * {@link TransferHandler} for TextFields that allow dropping {@link DataTableColumn}s on them.
 * 
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class DataTableColumnDropTextFieldTransferHandler extends AbstractPatchedTransferHandler implements
		TreeSelectionListener {

	private static final long serialVersionUID = 1L;
	private final SeriesUsageType seriesUsageType;

	private ValueSource currentValueSource;
	private final AttributeDropTextField aDTF;

	public DataTableColumnDropTextFieldTransferHandler(JTree plotConfigTree, SeriesUsageType seriesUsage,
			AttributeDropTextField aDTF) {
		seriesUsageType = seriesUsage;
		plotConfigTree.addTreeSelectionListener(this);
		this.aDTF = aDTF;
	}

	/*
	 * ***************** EXPORT **********************
	 */

	@Override
	public Icon getVisualRepresentation(Transferable t) {
		return null;
	}

	@Override
	protected Transferable createTransferable(JComponent c) {
		return null;
	}

	@Override
	protected void exportDone(JComponent source, Transferable data, int action) {
		return;
	}

	/*
	 * ****************** IMPORT ******************
	 */

	public boolean doesSupportFlavor(TransferSupport support) {

		// if attribute drop text field is not enabled, import is not possible
		if (!aDTF.isEnabled()) {
			return false;
		}

		if (!support.isDataFlavorSupported(DataTableColumnCollection.DATATABLE_COLUMN_COLLECTION_FLAVOR)) {
			return false;
		}

		return true;
	}

	@Override
	public boolean canImport(TransferSupport support) {

		if (!doesSupportFlavor(support)) {
			return false;
		}

		try {

			// fetch data table column
			Transferable transferable = support.getTransferable();
			DataTableColumnCollection dataTableColumnCollection = (DataTableColumnCollection) transferable
					.getTransferData(DataTableColumnCollection.DATATABLE_COLUMN_COLLECTION_FLAVOR);
			int size = dataTableColumnCollection.getDataTableColumns().size();
			if (size > 1 || size == 0) {
				return false;
			}

		} catch (Exception e) {
			return false;
		}

		// only support drops
		if (!support.isDrop()) {
			return false;
		}

		// check if the source actions (a bitwise-OR of supported actions)
		// contains the COPY action
		boolean copySupported = (COPY & support.getSourceDropActions()) == (COPY);
		if (copySupported) {
			support.setDropAction(COPY);
			return true;
		}

		// COPY or MOVE is not supported, so reject the transfer
		return false;
	}

	@Override
	public boolean importData(TransferSupport support) {
		if (!canImport(support)) {
			return false;
		}

		try {
			if (currentValueSource != null) {

				// fetch data table column
				Transferable transferable = support.getTransferable();
				DataTableColumnCollection dataTableColumnCollection = (DataTableColumnCollection) transferable
						.getTransferData(DataTableColumnCollection.DATATABLE_COLUMN_COLLECTION_FLAVOR);

				currentValueSource.setDataTableColumn(seriesUsageType, dataTableColumnCollection.getDataTableColumns()
						.get(0));

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	@Override
	public void valueChanged(TreeSelectionEvent e) {
		TreePath newLeadSelectionPath = e.getNewLeadSelectionPath();
		if (newLeadSelectionPath == null) {
			return;
		}
		Object lastPathComponent = newLeadSelectionPath.getLastPathComponent();
		if (lastPathComponent instanceof ValueSourceTreeNode) {

			ValueSourceTreeNode valueSourceNode = (ValueSourceTreeNode) lastPathComponent;
			// get the selected PVC
			ValueSource selectedValueSource = valueSourceNode.getUserObject();

			if (selectedValueSource == currentValueSource) {
				return;
			}

			// change current PlotValueConfig
			currentValueSource = selectedValueSource;

		} else {
			currentValueSource = null;
		}

	}
}
