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
import com.rapidminer.operator.features.transformation.AbstractEigenvectorModel;
import com.rapidminer.operator.features.transformation.ComponentVector;
import com.rapidminer.tools.Tools;

import java.util.List;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;


/**
 * This is an renderer for {@link AbstractEigenvectorModel}s. It shows the EigenValues in a table
 * view.
 * 
 * @author Sebastian Land
 */
public class EigenvectorModelEigenvalueRenderer extends AbstractTableModelTableRenderer {

	public static class EigenvalueTableModel extends AbstractTableModel {

		private static final long serialVersionUID = -9026248524043239399L;

		private double varianceSum;

		private double[] cumulativeVariance;

		private List<? extends ComponentVector> eigenVectors;

		public EigenvalueTableModel(List<? extends ComponentVector> eigenVectors, double[] cumulativeVariance,
				double varianceSum) {
			this.eigenVectors = eigenVectors;
			this.cumulativeVariance = cumulativeVariance;
			this.varianceSum = varianceSum;
		}

		@Override
		public int getColumnCount() {
			return 4;
		}

		@Override
		public int getRowCount() {
			return eigenVectors.size();
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			switch (columnIndex) {
				case 0:
					return "PC " + (rowIndex + 1);
				case 1:
					return Tools.formatNumber(Math.sqrt(eigenVectors.get(rowIndex).getEigenvalue()));
				case 2:
					return Tools.formatNumber(eigenVectors.get(rowIndex).getEigenvalue() / this.varianceSum);
				case 3:
					return Tools.formatNumber(cumulativeVariance[rowIndex]);
				default:
					return "unknown";
			}
		}

		@Override
		public String getColumnName(int column) {
			switch (column) {
				case 0:
					return "Component";
				case 1:
					return "Standard Deviation";
				case 2:
					return "Proportion of Variance";
				case 3:
					return "Cumulative Variance";
				default:
					return "unknown";
			}
		}

	}

	@Override
	public String getName() {
		return "Eigenvalues";
	}

	@Override
	public TableModel getTableModel(Object renderable, IOContainer ioContainer, boolean isReporting) {
		AbstractEigenvectorModel model = (AbstractEigenvectorModel) renderable;
		return model.getEigenvalueTableModel();
	}
}
