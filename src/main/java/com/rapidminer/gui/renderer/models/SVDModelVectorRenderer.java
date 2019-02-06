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

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.gui.renderer.AbstractTableModelTableRenderer;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.features.transformation.SVDModel;
import com.rapidminer.tools.Tools;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;


/**
 * This is a renderer for showing a table of the Singular Value Vectors.
 * 
 * @author Sebastian Land
 */
public class SVDModelVectorRenderer extends AbstractTableModelTableRenderer {

	/**
	 * Wrapper class for wraping a {@link SVDModel} into a {@link TableModel}.
	 * 
	 * @author Sebastian Land
	 */
	private class SVDVectorTableModel extends AbstractTableModel {

		private static final long serialVersionUID = -9026248524043239399L;

		private String[] attributeNames;

		private SVDModel model;

		private int numberOfComponents;

		public SVDVectorTableModel(SVDModel model) {
			Attributes attributes = model.getTrainingHeader().getAttributes();
			attributeNames = new String[attributes.size()];
			int i = 0;
			for (Attribute attribute : attributes) {
				attributeNames[i] = attribute.getName();
				i++;
			}
			this.numberOfComponents = model.getNumberOfComponents();

			this.model = model;
		}

		@Override
		public int getColumnCount() {
			return numberOfComponents;
		}

		@Override
		public int getRowCount() {
			return attributeNames.length;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			if (columnIndex == 0) {
				return attributeNames[rowIndex];
			} else {
				return Tools.formatNumber(model.getSingularVectorValue(columnIndex - 1, rowIndex));
			}
		}

		@Override
		public String getColumnName(int column) {
			if (column == 0) {
				return "Attribute";
			} else {
				return "SVD Vector " + column;
			}
		}
	}

	@Override
	public String getName() {
		return "SVD Vectors";
	}

	@Override
	public TableModel getTableModel(Object renderable, IOContainer ioContainer, boolean isReporting) {
		SVDModel model = (SVDModel) renderable;
		return new SVDVectorTableModel(model);
	}
}
