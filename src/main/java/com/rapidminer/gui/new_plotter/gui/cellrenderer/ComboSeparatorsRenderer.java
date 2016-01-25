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
package com.rapidminer.gui.new_plotter.gui.cellrenderer;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.ListCellRenderer;


/**
 * @author Nils Woehler
 * 
 */
public abstract class ComboSeparatorsRenderer implements ListCellRenderer {

	private ListCellRenderer delegate;
	private JPanel separatorPanel = new JPanel(new BorderLayout());
	private JSeparator separator = new JSeparator();

	public ComboSeparatorsRenderer(ListCellRenderer delegate) {
		this.delegate = delegate;
	}

	@Override
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
			boolean cellHasFocus) {
		Component comp = delegate.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		if (index != -1 && addSeparatorAfter(list, value, index)) {
			separatorPanel.removeAll();
			separatorPanel.add(comp, BorderLayout.CENTER);
			separatorPanel.add(separator, BorderLayout.NORTH);
			return separatorPanel;
		} else {
			return comp;
		}
	}

	protected abstract boolean addSeparatorAfter(JList list, Object value, int index);
}
