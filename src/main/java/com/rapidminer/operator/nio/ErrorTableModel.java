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
package com.rapidminer.operator.nio;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.rapidminer.operator.nio.model.ParsingError;
import com.rapidminer.studio.io.gui.internal.steps.configuration.AbstractErrorWarningTableModel;


/**
 * A table model to display {@link ParsingError}s.
 *
 * @author Simon Fischer
 *
 */
public class ErrorTableModel extends AbstractErrorWarningTableModel {

	private static final long serialVersionUID = 1L;

	private List<ParsingError> errors = new ArrayList<ParsingError>();

	@Override
	public String getColumnName(int column) {
		switch (column) {
			case 0:
				return "Row, Column";
			case 1:
				return "Error";
			case 2:
				return "Original value";
			case 3:
				return "Message";
			default:
				return super.getColumnName(column);
		}
	}

	@Override
	public int getRowCount() {
		return errors.size();
	}

	@Override
	public int getColumnCount() {
		return 4;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		ParsingError error = errors.get(rowIndex);
		switch (columnIndex) {
			case 0:
				if (error.getRow() < 0 && error.getColumn() < 0) {
					if (error.getColumns() == null) {
						return "";
					} else {
						return "columns " + listToString(error.getColumns());
					}
				} else {
					return error.getRow() + 1 + ", " + (error.getColumn() + 1);
				}
			case 1:
				return error.getErrorCode().getMessage();
			case 2:
				return error.getOriginalValue();
			case 3:
				return error.getCause() != null ? error.getCause().getMessage() : null;
			default:
				return null;
		}
	}

	public void setErrors(Collection<ParsingError> errors) {
		if (!this.errors.equals(errors)) {
			this.errors.clear();
			this.errors.addAll(errors);
			Collections.sort(this.errors, new Comparator<ParsingError>() {

				@Override
				public int compare(ParsingError o1, ParsingError o2) {
					int rowDiff = o1.getRow() - o2.getRow();
					if (rowDiff != 0) {
						return rowDiff;
					} else {
						return o1.getColumn() - o2.getColumn();
					}
				}
			});
			fireTableStructureChanged();
		}
	}

	public ParsingError getErrorInRow(int index) {
		if (index < errors.size()) {
			return errors.get(index);
		} else {
			return null;
		}
	}

	private String listToString(List<Integer> list) {
		Collections.sort(list);
		String output = "";
		for (int i = 0; i < list.size(); i++) {
			Integer column = list.get(i) + 1;
			if (i < list.size() - 2) {
				output += column + ", ";
			} else if (i == list.size() - 2) {
				output += column + " and ";
			} else {
				output += column;
			}
		}
		return output;
	}

	@Override
	public int getErrorCount() {
		return 0;
	}

	@Override
	public int getWarningCount() {
		return getRowCount();
	}

}
