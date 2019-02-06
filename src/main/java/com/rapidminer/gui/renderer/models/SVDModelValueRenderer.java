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
import com.rapidminer.operator.features.transformation.SVDModel;
import com.rapidminer.tools.Tools;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;


/**
 * This is an renderer for {@link SVDModel}s. It shows the Singular Values in a table view.
 * 
 * @author Sebastian Land
 */
public class SVDModelValueRenderer extends AbstractTableModelTableRenderer {

	private class SVDModelValueTableModel extends AbstractTableModel {

		private static final long serialVersionUID = -9026248524043239399L;

		private SVDModel model;

		public SVDModelValueTableModel(SVDModel model) {
			this.model = model;
		}

		@Override
		public int getColumnCount() {
			return 5;
		}

		@Override
		public int getRowCount() {
			return model.getNumberOfComponents();
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			switch (columnIndex) {
				case 0:
					return "SVD " + (rowIndex + 1);
				case 1:
					return Tools.formatNumber(model.getSingularValue(rowIndex));
				case 2:
					return Tools.formatNumber(model.getSingularValueProportion(rowIndex));
				case 3:
					return Tools.formatNumber(model.getCumulativeSingularValue(rowIndex));
				case 4:
					return Tools.formatNumber(model.getCumulativeSingularValueProportion(rowIndex));
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
					return "Singular Value";
				case 2:
					return "Proportion of Singular Values";
				case 3:
					return "Cumulative Singular Values";
				case 4:
					return "Cumulative Proportion of Singular Values";
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
		SVDModel model = (SVDModel) renderable;
		return new SVDModelValueTableModel(model);
	}
}
