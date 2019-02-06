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
package com.rapidminer.gui.viewer.metadata.dialogs;

import java.awt.GridLayout;
import java.awt.Window;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableModel;

import com.rapidminer.example.Attribute;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.gui.viewer.metadata.AttributeStatisticsPanel;
import com.rapidminer.gui.viewer.metadata.model.NominalValueTableModel;
import com.rapidminer.tools.container.ValueAndCount;


/**
 * This dialog displays the nominal values of nominal {@link Attribute}s displayed by an
 * {@link AttributeStatisticsPanel} in a table with 3 columns: name, absolute count and relative
 * count.
 *
 * @author Marco Boeck
 *
 */
public class NominalValueDialog extends ButtonDialog {

	private static final long serialVersionUID = 7061405741134293387L;

	/**
	 * Creates a new {@link NominalValueDialog} instance.
	 *
	 * @param listOfValues
	 */
	public NominalValueDialog(Window owner, List<ValueAndCount> listOfValues) {
		super(owner, "attribute_statistics.nominal_values_dialog", ModalityType.APPLICATION_MODAL, new Object[] {});

		JPanel tablePanel = new JPanel();
		tablePanel.setLayout(new GridLayout(1, 1));
		TableModel model = new NominalValueTableModel(listOfValues);
		ExtendedJTable dataTable = new ExtendedJTable(model, true);
		dataTable.setRowHighlighting(true);

		// change JTable settings
		dataTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
		dataTable.getColumnModel().getColumn(NominalValueTableModel.INDEX_INDEX).setPreferredWidth(10);

		// add to GUI
		JScrollPane scrollpane = new ExtendedJScrollPane(dataTable);
		scrollpane.setBorder(null);
		tablePanel.add(scrollpane);

		setDefaultSize(ButtonDialog.MESSAGE_EXTENDED);
		layoutDefault(tablePanel, makeCloseButton());
	}
}
