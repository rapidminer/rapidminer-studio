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
package com.rapidminer.gui.properties.tablepanel.cells.implementations;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import com.rapidminer.gui.properties.tablepanel.TablePanel;
import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellTypeComboBox;
import com.rapidminer.gui.properties.tablepanel.model.TablePanelModel;
import com.rapidminer.gui.tools.autocomplete.AutoCompleteComboBoxAddition;


/**
 * GUI component for the {@link TablePanel} for {@link CellTypeComboBox}.
 *
 * @author Marco Boeck
 *
 */
public class CellTypeComboBoxImpl extends JComboBox<String> implements CellTypeComboBox {

	private static final long serialVersionUID = 5923158263372081013L;

	/**
	 * Creates a {@link CellTypeComboBoxImpl} for the specified cell. Does not validate the model,
	 * so make sure this call works!
	 *
	 * @param model
	 * @param rowIndex
	 * @param columnIndex
	 */
	public CellTypeComboBoxImpl(final TablePanelModel model, final int rowIndex, final int columnIndex) {
		super(new Vector<String>(model.getPossibleValuesForCellOrNull(rowIndex, columnIndex) != null
				? model.getPossibleValuesForCellOrNull(rowIndex, columnIndex) : Collections.<String>emptyList()));

		// distinguish between editable comboboxes and non-editable ones
		if (model.isCellEditable(rowIndex, columnIndex)) {
			setEditable(true);
			new AutoCompleteComboBoxAddition(this);
		} else {
			int indexOf = ((DefaultComboBoxModel<?>) getModel()).getIndexOf(model.getValueAt(rowIndex, columnIndex));
			// if the combobox is NOT editable and the model has a value which is not part of the
			// combobox, change the model value
			if (indexOf == -1) {
				model.setValueAt(getSelectedItem(), rowIndex, columnIndex);
			}
		}

		// misc settings
		setSelectedItem(model.getValueAt(rowIndex, columnIndex));
		setToolTipText(model.getHelptextAt(rowIndex, columnIndex));
		addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				model.setValueAt(getSelectedItem(), rowIndex, columnIndex);
				setToolTipText(model.getHelptextAt(rowIndex, columnIndex));
			}
		});

		// set size so comboboxes don't grow larger when they get the chance
		setPreferredSize(new Dimension(150, 20));
		setMinimumSize(new Dimension(100, 15));
		setMaximumSize(new Dimension(300, 30));
	}

}
