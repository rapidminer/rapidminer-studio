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
package com.rapidminer.gui.tools;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;


/**
 * Provides an extended list cell renderer which allows the disabling of list elements according to
 * their status in an extended list model.
 *
 * @author Tobias Malbrecht
 */
public class ExtendedListCellRenderer extends DefaultListCellRenderer {

	public static final long serialVersionUID = 90320323254252311L;

	ExtendedListModel<?> model;

	public ExtendedListCellRenderer(ExtendedListModel<?> model) {
		this.model = model;
	}

	@Override
	public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
			boolean cellHasFocus) {
		Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		component.setEnabled(model.isEnabled(value));
		return component;
	}
}
