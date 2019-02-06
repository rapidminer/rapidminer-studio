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
package com.rapidminer.gui.renderer.models;

import com.rapidminer.gui.renderer.AbstractTableModelTableRenderer;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.learner.functions.LinearRegressionModel;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;


/**
 * Renderer for the linear regression model.
 * 
 * @author Simon Fischer, Ingo Mierswa
 */
public class LinearRegressionModelTableRenderer extends AbstractTableModelTableRenderer {

	private static class LinearRegressionModelTableModel extends AbstractTableModel {

		private static final long serialVersionUID = -2112928170124291591L;

		private final LinearRegressionModel model;

		public LinearRegressionModelTableModel(LinearRegressionModel model) {
			this.model = model;
		}

		@Override
		public int getColumnCount() {
			return 8;
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			switch (columnIndex) {
				case 0:
					return String.class;
				default:
					return Double.class;
			}
		}

		@Override
		public String getColumnName(int columnIndex) {
			switch (columnIndex) {
				case 0:
					return "Attribute";
				case 1:
					return "Coefficient";
				case 2:
					return "Std. Error";
				case 3:
					return "Std. Coefficient";
				case 4:
					return "Tolerance";
				case 5:
					return "t-Stat";
				case 6:
					return "p-Value";
				case 7:
					return "Code";
			}
			return null;
		}

		@Override
		public int getRowCount() {
			return model.getCoefficients().length - (model.usesIntercept() ? 0 : 1);
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			switch (columnIndex) {
				case 0:
					if (model.usesIntercept() && rowIndex == model.getCoefficients().length - 1) {
						return "(Intercept)";
					} else {
						return model.getSelectedAttributeNames()[rowIndex];
					}
				case 1:
					return model.getCoefficients()[rowIndex];
				case 2:
					return model.getStandardErrors()[rowIndex];
				case 3:
					return model.getStandardizedCoefficients()[rowIndex];
				case 4:
					return model.getTolerances()[rowIndex];
				case 5:
					return model.getTStats()[rowIndex];
				case 6:
					return model.getProbabilities()[rowIndex];
				case 7:
					double prob = model.getProbabilities()[rowIndex];
					if (prob < 0.001) {
						return "****";
					} else if (prob < 0.01) {
						return "***";
					} else if (prob < 0.05) {
						return "**";
					} else if (prob < 0.1) {
						return "*";
					} else {
						return "";
					}
			}
			return null;
		}
	}

	@Override
	public TableModel getTableModel(Object renderable, IOContainer ioContainer, boolean isReporting) {
		return new LinearRegressionModelTableModel((LinearRegressionModel) renderable);
	}
}
