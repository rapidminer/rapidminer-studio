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

import com.rapidminer.gui.properties.tablepanel.TablePanel;
import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellTypeCheckBox;
import com.rapidminer.gui.properties.tablepanel.model.TablePanelModel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;


/**
 * GUI component for the {@link TablePanel} for {@link CellTypeCheckBox}.
 * 
 * @author Marco Boeck
 * 
 */
public class CellTypeCheckBoxImpl extends JCheckBox implements CellTypeCheckBox {

	private static final long serialVersionUID = -2006834470031594342L;

	/**
	 * Creates a {@link CellTypeCheckBoxImpl} for the specified cell. Does not validate the model,
	 * so make sure this call works!
	 * 
	 * @param model
	 * @param rowIndex
	 * @param columnIndex
	 */
	public CellTypeCheckBoxImpl(final TablePanelModel model, final int rowIndex, final int columnIndex) {
		super();

		// misc settings
		setText(model.getSyntaxHelpAt(rowIndex, columnIndex));
		setToolTipText(model.getHelptextAt(rowIndex, columnIndex));
		setSelected(Boolean.parseBoolean(String.valueOf(model.getValueAt(rowIndex, columnIndex))));
		addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				model.setValueAt(isSelected(), rowIndex, columnIndex);
			}
		});
	}

}
