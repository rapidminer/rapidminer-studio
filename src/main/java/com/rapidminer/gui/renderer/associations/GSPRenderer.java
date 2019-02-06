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
package com.rapidminer.gui.renderer.associations;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import com.rapidminer.gui.renderer.AbstractTableModelTableRenderer;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.learner.associations.gsp.GSPSet;
import com.rapidminer.operator.learner.associations.gsp.Sequence;


/**
 * This class is the renderer for the GSP results. It shows the sequential pattern in a table view.
 *
 * @author Sebastian Land
 */
public class GSPRenderer extends AbstractTableModelTableRenderer {

	/**
	 * Position of the support column
	 */
	public static final int SUPPORT_COLUMN = 0;
	/**
	 * Position of the Transactions column
	 */
	public static final int TRANSACTIONS_COLUMN = 1;
	/**
	 * Position of the items column
	 */
	public static final int ITEMS_COLUMN = 2;
	/**
	 * The offset caused by the SUPPORT, TRANSACTIONS, and ITEMS fields
	 */
	private static final int FIELD_COUNT = new int[] { SUPPORT_COLUMN, TRANSACTIONS_COLUMN, ITEMS_COLUMN }.length;

	@Override
	public TableModel getTableModel(Object renderable, IOContainer ioContainer, boolean isReporting) {
		final GSPSet set = (GSPSet) renderable;
		final double[] supports = set.getSupportArray();
		final Sequence[] sequences = set.getSequenceArray();
		return new AbstractTableModel() {

			private static final long serialVersionUID = 7884607018618465026L;

			@Override
			public int getColumnCount() {
				return set.getMaxTransactions() + FIELD_COUNT;
			}

			@Override
			public int getRowCount() {
				return set.getNumberOfSequences();
			}

			@Override
			public Object getValueAt(int rowIndex, int columnIndex) {
				switch (columnIndex) {
					case SUPPORT_COLUMN:
						return supports[rowIndex];
					case TRANSACTIONS_COLUMN:
						return sequences[rowIndex].size();
					case ITEMS_COLUMN:
						return sequences[rowIndex].getNumberOfItems();
					default:
						Sequence sequence = sequences[rowIndex];
						if (sequence.size() > columnIndex - FIELD_COUNT) {
							return sequence.get(columnIndex - FIELD_COUNT).toString();
						} else {
							return "";
						}
				}
			}

			@Override
			public Class<?> getColumnClass(int columnIndex) {
				switch (columnIndex) {
					case SUPPORT_COLUMN:
						return Double.class;
					case TRANSACTIONS_COLUMN:
						return Integer.class;
					case ITEMS_COLUMN:
						return Integer.class;
					default:
						return String.class;
				}
			}

			@Override
			public String getColumnName(int columnIndex) {
				switch (columnIndex) {
					case SUPPORT_COLUMN:
						return "Support";
					case TRANSACTIONS_COLUMN:
						return "Transactions";
					case ITEMS_COLUMN:
						return "Items";
					default:
						return "Transaction " + (columnIndex - FIELD_COUNT);
				}
			}
		};
	}

	@Override
	public boolean isAutoresize() {
		return true;
	}

}
