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
package com.rapidminer.gui.tools.dialogs.wizards.dataimport.excel;

import com.rapidminer.gui.properties.DefaultRMCellEditor;
import com.rapidminer.operator.Annotations;

import java.awt.Color;
import java.awt.Component;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JTable;


/**
 * This is the cell editor for annotation cells. They are by default colored in light gray to
 * separate them from the actual value column.
 * 
 * @author Simon Fischer, Sebastian Land
 * 
 */
public class AnnotationCellEditor extends DefaultRMCellEditor {

	public static final String NONE = "-";
	public static final String NAME = Annotations.ANNOTATION_NAME;

	private static final long serialVersionUID = 1L;
	private Color background;

	private static JComboBox<String> makeComboBox() {
		Vector<String> values = new Vector<String>();
		values.add(NONE);
		values.add(NAME);
		for (String a : Annotations.ALL_KEYS_ATTRIBUTE) {
			values.add(a);
		}
		return new JComboBox<>(values);
	}

	public AnnotationCellEditor() {
		super(makeComboBox());
	}

	public AnnotationCellEditor(Color background) {
		this();
		this.background = background;
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		Component tableCellEditorComponent = super.getTableCellEditorComponent(table, value, isSelected, row, column);
		if (background != null) {
			tableCellEditorComponent.setBackground(background);
		}
		return tableCellEditorComponent;
	}

}
