/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.TransferHandler;


/**
 * {@link TransferHandler} for exporting {@link DataTableColumn}s from Lists.
 * 
 * @author Nils Woehler
 * 
 */
public class DataTableColumnListTransferHandler extends AbstractPatchedTransferHandler {

	private static final long serialVersionUID = 1L;

	/*
	 * ***************** EXPORT **********************
	 */

	@Override
	public void exportToClipboard(JComponent comp, Clipboard clip, int action) throws IllegalStateException {
		return; // do not export data table columns
	}

	@Override
	public int getSourceActions(JComponent c) {
		return COPY;
	}

	@Override
	public Icon getVisualRepresentation(Transferable t) {
		// String i18nKey = "plotter.configuration_dialog.table_column";
		//
		// // set label icon
		// String icon = I18N.getMessageOrNull(I18N.getGUIBundle(), "gui.label." + i18nKey +
		// ".icon");
		// if (icon != null) {
		// ImageIcon iicon = SwingTools.createIcon("16/" + icon);
		// return iicon;
		// }
		return null;
	}

	@Override
	protected Transferable createTransferable(JComponent c) {
		JList source = (JList) c;
		Object[] selectedValues = source.getSelectedValues();
		List<DataTableColumn> columns = new LinkedList<DataTableColumn>();
		if (selectedValues != null) {
			int length = selectedValues.length;
			for (int i = 0; i < length; ++i) {
				columns.add((DataTableColumn) selectedValues[i]);
			}
		}
		return new DataTableColumnCollectionTransferable(new DataTableColumnCollection(columns));
	}

	@Override
	protected void exportDone(JComponent source, Transferable data, int action) {
		return;
	}

	/*
	 * ****************** IMPORT ******************
	 */

	@Override
	public boolean canImport(TransferSupport support) {
		return false;
	}

	@Override
	public boolean importData(TransferSupport support) {
		return false;
	}

}
