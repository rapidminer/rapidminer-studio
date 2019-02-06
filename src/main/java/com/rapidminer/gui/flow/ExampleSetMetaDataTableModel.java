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
package com.rapidminer.gui.flow;

import java.awt.Component;
import java.awt.Dimension;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JScrollPane;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.tools.Ontology;


/**
 * This is a table model for the example set meta data.
 *
 * @author Simon Fischer
 */
public class ExampleSetMetaDataTableModel implements TableModel {

	private final List<AttributeMetaData> attributes;

	private static final String[] COLUMN_NAMES = { "Role", "Name", "Type", "Range", "Missings", "Comment" };
	private static final int ROLE_COLUMN = 0;
	private static final int NAME_COLUMN = 1;
	private static final int TYPE_COLUMN = 2;
	private static final int RANGE_COLUMN = 3;
	private static final int MISSINGS_COLUMN = 4;
	private static final int COMMENT_COLUMN = 5;

	public ExampleSetMetaDataTableModel(ExampleSetMetaData emd) {
		super();
		this.attributes = new LinkedList<AttributeMetaData>(emd.getAllAttributes());
	}

	/** Table is immutable. We ignore all listeners. */
	@Override
	public void addTableModelListener(TableModelListener l) {}

	@Override
	public void removeTableModelListener(TableModelListener l) {}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		switch (columnIndex) {
			case RANGE_COLUMN:
				return String.class;
			default:
				return String.class;
		}
	}

	@Override
	public int getColumnCount() {
		return COLUMN_NAMES.length;
	}

	@Override
	public String getColumnName(int columnIndex) {
		return COLUMN_NAMES[columnIndex];
	}

	@Override
	public int getRowCount() {
		return attributes.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		AttributeMetaData amd = attributes.get(rowIndex);
		switch (columnIndex) {
			case ROLE_COLUMN:
				return amd.getRole();
			case NAME_COLUMN:
				String unit = amd.getAnnotations().getAnnotation(Annotations.KEY_UNIT);
				String name = amd.getName();
				if (unit != null) {
					name += " [" + unit + "]";
				}
				return name;
			case TYPE_COLUMN:
				return Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(amd.getValueType());
			case RANGE_COLUMN:
				return amd.getRangeString();
			case MISSINGS_COLUMN:
				return amd.getNumberOfMissingValues();
			case COMMENT_COLUMN:
				return amd.getAnnotations().getAnnotation(Annotations.KEY_COMMENT);
			default:
				return null;
		}

	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		throw new UnsupportedOperationException("Table is read only.");
	}

	public static Component makeTableForToolTip(ExampleSetMetaData emd) {
		ExtendedJTable table = new ExtendedJTable(new ExampleSetMetaDataTableModel(emd), true, true, true, false, false);
		JScrollPane scrollPane = new JScrollPane(table);
		scrollPane.setBorder(null);
		scrollPane.setPreferredSize(new Dimension(300, 200));
		scrollPane.setBackground(Colors.WHITE);
		scrollPane.getViewport().setBackground(Colors.WHITE);
		return scrollPane;
	}
}
