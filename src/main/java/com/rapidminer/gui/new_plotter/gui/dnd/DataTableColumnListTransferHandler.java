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
 * @deprecated since 9.2.0
 */
@Deprecated
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

	@SuppressWarnings("unchecked")
	@Override
	protected Transferable createTransferable(JComponent c) {
		List<DataTableColumn> columns = new LinkedList<>(((JList<DataTableColumn>) c).getSelectedValuesList());
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
