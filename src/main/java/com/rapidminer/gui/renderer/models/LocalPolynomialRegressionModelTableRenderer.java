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

import java.util.Iterator;

import com.rapidminer.datatable.AbstractDataTable;
import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.DataTableRow;
import com.rapidminer.gui.renderer.AbstractDataTableTableRenderer;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.learner.local.LocalPolynomialRegressionModel;
import com.rapidminer.operator.learner.local.LocalPolynomialRegressionModel.RegressionData;


/**
 * This class provides a viewer for the LocalPolynomialRegressionModel. It provides a table view on
 * all stored examples values.
 *
 * @author Sebastian Land
 */
public class LocalPolynomialRegressionModelTableRenderer extends AbstractDataTableTableRenderer {

	public static class LocalPolynomialRegressionModelDataTable extends AbstractDataTable {

		private LocalPolynomialRegressionModel model;

		public LocalPolynomialRegressionModelDataTable(String name, LocalPolynomialRegressionModel model) {
			super(name);
			this.model = model;
		}

		@Override
		public void add(DataTableRow row) {}

		@Override
		public int getColumnIndex(String name) {
			if (name.equals("Label")) {
				return 0;
			} else if (name.equals("Weight")) {
				return 1;
			} else {
				int i = 2;
				for (String attrName : model.getAttributeNames()) {
					if (attrName.equals(name)) {
						return i;
					}
					i++;
				}
			}
			return 0;
		}

		@Override
		public String getColumnName(int column) {
			if (column == 0) {
				return "Label";
			}
			if (column == 1) {
				return "Weight";
			}
			return model.getAttributeNames()[column - 2];
		}

		@Override
		public double getColumnWeight(int i) {
			return 1;
		}

		@Override
		public int getNumberOfColumns() {
			if (model.getSamples().size() > 0) {
				RegressionData data = model.getSamples().get(0);
				return data.getExampleValues().length + 2;
			} else {
				return 0;
			}
		}

		@Override
		public int getNumberOfRows() {
			return model.getSamples().size();
		}

		@Override
		public int getNumberOfSpecialColumns() {
			return 0;
		}

		@Override
		public int getNumberOfValues(int column) {
			return 0;
		}

		@Override
		public DataTableRow getRow(final int index) {
			return new DataTableRow() {

				@Override
				public String getId() {
					return index + "";
				}

				@Override
				public int getNumberOfValues() {
					RegressionData data = model.getSamples().get(index);
					return data.getExampleValues().length + 2;
				}

				@Override
				public double getValue(int columnIndex) {
					RegressionData data = model.getSamples().get(index);
					if (columnIndex == 0) {
						return data.getExampleLabel();
					}
					if (columnIndex == 1) {
						return data.getExampleWeight();
					}
					return data.getExampleValues()[columnIndex - 2];
				}

			};
		}

		@Override
		public boolean isDate(int index) {
			return false;
		}

		@Override
		public boolean isDateTime(int index) {
			return false;
		}

		@Override
		public boolean isNominal(int index) {
			return false;
		}

		@Override
		public boolean isNumerical(int index) {
			return true;
		}

		@Override
		public boolean isSpecial(int column) {
			return false;
		}

		@Override
		public boolean isSupportingColumnWeights() {
			return false;
		}

		@Override
		public boolean isTime(int index) {
			return false;
		}

		@Override
		public Iterator<DataTableRow> iterator() {
			return new Iterator<DataTableRow>() {

				private int rowIndex = 0;

				@Override
				public boolean hasNext() {
					return rowIndex < getNumberOfRows();
				}

				@Override
				public DataTableRow next() {
					DataTableRow row = getRow(this.rowIndex);
					this.rowIndex++;
					return row;
				}

				@Override
				public void remove() {}
			};
		}

		@Override
		public String mapIndex(int column, int index) {
			return "";
		}

		@Override
		public int mapString(int column, String value) {
			return 0;
		}

		@Override
		public DataTable sample(int newSize) {
			return this;
		}
	}

	@Override
	public DataTable getDataTable(Object renderable, IOContainer ioContainer, boolean isRendering) {
		final LocalPolynomialRegressionModel model = (LocalPolynomialRegressionModel) renderable;

		return new LocalPolynomialRegressionModelDataTable("Training Data", model);
	}

	@Override
	public boolean isAutoresize() {
		return true;
	}
}
