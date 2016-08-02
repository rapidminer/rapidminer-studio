/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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

import com.rapidminer.gui.renderer.AbstractTableModelTableRenderer;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.learner.associations.gsp.GSPSet;
import com.rapidminer.operator.learner.associations.gsp.Sequence;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;


/**
 * This class is the renderer for the GSP results. It shows the sequential pattern in a table view.
 * 
 * @author Sebastian Land
 */
public class GSPRenderer extends AbstractTableModelTableRenderer {

	@Override
	public TableModel getTableModel(Object renderable, IOContainer ioContainer, boolean isReporting) {
		final GSPSet set = (GSPSet) renderable;
		final double[] supports = set.getSupportArray();
		final Sequence[] sequences = set.getSequenceArray();
		return new AbstractTableModel() {

			private static final long serialVersionUID = 7884607018618465026L;

			@Override
			public int getColumnCount() {
				return set.getMaxTransactions() + 3;
			}

			@Override
			public int getRowCount() {
				return set.getNumberOfSequences();
			}

			@Override
			public Object getValueAt(int rowIndex, int columnIndex) {
				if (columnIndex == 0) {
					return supports[rowIndex];
				} else if (columnIndex == 1) {
					return sequences[rowIndex].size();
				} else if (columnIndex == 2) {
					return sequences[rowIndex].getNumberOfItems();
				} else {
					Sequence sequence = sequences[rowIndex];
					if (sequence.size() > columnIndex - 3) {
						return sequence.get(columnIndex - 3).toString();
					} else {
						return "";
					}
				}
			}

			@Override
			public Class<?> getColumnClass(int columnIndex) {
				if (columnIndex == 0) {
					return Double.class;
				} else if (columnIndex == 0) {
					return Integer.class;
				} else {
					return String.class;
				}
			}

			@Override
			public String getColumnName(int columnIndex) {
				if (columnIndex == 0) {
					return "Support";
				} else if (columnIndex == 1) {
					return "Transactions";
				} else if (columnIndex == 2) {
					return "Items";
				} else {
					return "Transaction " + (columnIndex - 3);
				}
			}
		};
	}

	@Override
	public boolean isAutoresize() {
		return true;
	}

}
