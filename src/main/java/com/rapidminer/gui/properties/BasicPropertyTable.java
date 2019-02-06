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
package com.rapidminer.gui.properties;

import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.Parameters;
import com.rapidminer.parameter.UndefinedParameterError;

import java.util.LinkedList;
import java.util.List;

import javax.swing.table.AbstractTableModel;


/**
 * This is a basic property table which can be simply build from a {@link Parameters} object. This
 * property table does not rely on an operator.
 * 
 * @author Ingo Mierswa
 */
public class BasicPropertyTable extends PropertyTable {

	private static final long serialVersionUID = -2054750632363559123L;

	private ParameterType[] shownParameterTypes = new ParameterType[0];

	private Parameters parameters = new Parameters();

	private class BasicPropertyModel extends AbstractTableModel {

		private static final long serialVersionUID = -6246875779676802335L;

		@Override
		public int getColumnCount() {
			return 2;
		}

		@Override
		public int getRowCount() {
			return shownParameterTypes.length;
		}

		@Override
		public String getColumnName(int index) {
			if (index == 0) {
				return "Key";
			} else {
				return "Value";
			}
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			String key = getParameterType(rowIndex).getKey();
			if (columnIndex == 0) {
				return key;
			} else {
				try {
					return parameters.getParameter(key);
				} catch (UndefinedParameterError e) {
					return null;
				}
			}
		}

		@Override
		public void setValueAt(Object value, int row, int column) {
			if (column == 1) {
				String key = getParameterType(row).getKey();
				parameters.setParameter(key, (String) value);
				// updateParameterTypes();
			}
		}
	}

	public BasicPropertyTable() {
		setModel(new BasicPropertyModel());
	}

	public void setParameters(Parameters parameters) {
		this.parameters = parameters;
		updateParameterTypes();
	}

	private void updateParameterTypes() {
		List<ParameterType> viewableList = new LinkedList<ParameterType>();
		for (String key : this.parameters) {
			ParameterType type = this.parameters.getParameterType(key);
			if (!type.isHidden()) {
				viewableList.add(type);
			}
		}

		this.shownParameterTypes = new ParameterType[viewableList.size()];
		viewableList.toArray(this.shownParameterTypes);

		setModel(new BasicPropertyModel());

		updateEditorsAndRenderers();
	}

	public void setValue(String key, Object value) {
		this.parameters.setParameter(key, (String) value);
		// updateParameterTypes();
	}

	@Override
	public boolean isCellEditable(int row, int column) {
		if (column == 0) {
			return false;
		} else {
			return true;
		}
	}

	/** Returns null. */
	@Override
	public Operator getOperator(int row) {
		return null;
	}

	@Override
	public ParameterType getParameterType(int row) {
		return this.shownParameterTypes[row];
	}

	public void clearParameterTypes() {
		this.parameters = new Parameters();
		updateParameterTypes();
	}

	public void setValue(int row, Object value) {
		getModel().setValueAt(value, row, 1);
	}
}
