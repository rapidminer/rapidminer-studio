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

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import com.rapidminer.gui.renderer.AbstractTableModelTableRenderer;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.learner.bayes.DistributionModel;


/**
 * A Renderer for a DistributionModel.
 *
 * @author Tobias Malbrecht, Sebastian Land
 */
public class DistributionModelTableRenderer extends AbstractTableModelTableRenderer {

	private static class DistributionTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 2196512073454635516L;
		private DistributionModel model;
		private int totalNumberOfParameters = 0;
		private int[] rowDistributionIndices;
		private int[] rowParameterIndices;

		public DistributionTableModel(DistributionModel model) {
			this.model = model;
			for (int i = 0; i < model.getNumberOfAttributes(); i++) {
				totalNumberOfParameters += model.getDistribution(0, i).getNumberOfParameters();
			}
			rowDistributionIndices = new int[totalNumberOfParameters];
			rowParameterIndices = new int[totalNumberOfParameters];
			int row = 0;
			for (int i = 0; i < model.getNumberOfAttributes(); i++) {
				for (int j = 0; j < model.getDistribution(0, i).getNumberOfParameters(); j++) {
					rowDistributionIndices[row] = i;
					rowParameterIndices[row] = j;
					row++;
				}
			}

		}

		@Override
		public int getColumnCount() {
			return model.getNumberOfClasses() + 2;
		}

		@Override
		public int getRowCount() {
			return totalNumberOfParameters;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			switch (columnIndex) {
				case 0:
					return model.getAttributeNames()[rowDistributionIndices[rowIndex]];
				case 1:
					return model.getDistribution(0, rowDistributionIndices[rowIndex]).getParameterName(
							rowParameterIndices[rowIndex]);
				default:
					return model.getDistribution(columnIndex - 2, rowDistributionIndices[rowIndex]).getParameterValue(
							rowParameterIndices[rowIndex]);
			}
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			if (columnIndex == 0) {
				return String.class;
			}
			if (columnIndex == 1) {
				return String.class;
			}
			return Double.class;
		}

		@Override
		public String getColumnName(int columnIndex) {
			if (columnIndex == 0) {
				return "Attribute";
			}
			if (columnIndex == 1) {
				return "Parameter";
			}
			return model.getClassName(columnIndex - 2);
		}
	}

	@Override
	public TableModel getTableModel(Object renderable, IOContainer ioContainer, boolean isReporting) {
		DistributionModel distributionModel = (DistributionModel) renderable;
		if (distributionModel != null) {
			return new DistributionTableModel(distributionModel);
		} else {
			return null;
		}
	}

	@Override
	public String getName() {
		return "Distribution Table";
	}

	@Override
	public boolean isAutoresize() {
		return true;
	}

}
