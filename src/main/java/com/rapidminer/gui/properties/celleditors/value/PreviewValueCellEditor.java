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
package com.rapidminer.gui.properties.celleditors.value;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JTable;

import com.rapidminer.gui.wizards.PreviewCreator;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterTypePreview;


/**
 * Cell editor consisting of a simple button which opens a preview for the corresponding operator.
 * 
 * @author Ingo Mierswa
 */
public class PreviewValueCellEditor extends AbstractCellEditor implements PropertyValueCellEditor {

	private static final long serialVersionUID = -7163760967040772736L;

	private transient ParameterTypePreview type;

	private JButton button;

	public PreviewValueCellEditor(ParameterTypePreview type) {
		this.type = type;
		button = new JButton("Show Preview...");
		button.setToolTipText(type.getDescription());
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				buttonPressed();
			}
		});
	}

	/** Does nothing. */
	@Override
	public void setOperator(Operator operator) {}

	private void buttonPressed() {
		PreviewCreator creator = type.getPreviewCreator();
		if (creator != null) {
			creator.createPreview(type.getPreviewListener());
		}
	}

	@Override
	public Object getCellEditorValue() {
		return null;
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {
		return button;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		return getTableCellEditorComponent(table, value, isSelected, row, column);
	}

	@Override
	public boolean useEditorAsRenderer() {
		return true;
	}

	@Override
	public boolean rendersLabel() {
		return false;
	}

	@Override
	public void activate() {
		button.doClick();
	}
}
