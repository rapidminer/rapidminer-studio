/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.properties.celleditors.value;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;

import com.rapidminer.gui.properties.DefaultRMCellEditor;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.components.LinkButton;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterTypeLinkButton;


/**
 *
 * Cell editor consisting of a {@link LinkButton} that executes a {@link ResourceAction} when
 * clicked upon.
 *
 * @author Gisa Schaefer
 * @since 6.4.0
 */
public class LinkButtonValueCellEditor extends DefaultRMCellEditor implements PropertyValueCellEditor {

	private static final long serialVersionUID = 1L;

	private final JPanel container;

	/**
	 * Creates a {@link LinkButton} that executes the action stored in type.
	 *
	 * @param type
	 *            the type
	 */
	public LinkButtonValueCellEditor(final ParameterTypeLinkButton type) {
		super(new JTextField());
		this.container = new JPanel(new GridBagLayout());
		this.container.setToolTipText(type.getDescription());

		GridBagConstraints gbc = new GridBagConstraints();

		JComponent linkButton = new LinkButton(type.getAction(), false);
		gbc.gridx += 1;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		container.add(linkButton, gbc);

	}

	@Override
	public boolean rendersLabel() {
		return true;
	}

	@Override
	public boolean useEditorAsRenderer() {
		return true;
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		return container;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		return container;
	}

	@Override
	public Object getCellEditorValue() {
		return null;
	}

	@Override
	public void setOperator(Operator operator) {
		// do nothing
	}
}
