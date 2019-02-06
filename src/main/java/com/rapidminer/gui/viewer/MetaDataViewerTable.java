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
package com.rapidminer.gui.viewer;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.tools.CellColorProvider;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.gui.tools.ExtendedJTableSorterModel;
import com.rapidminer.gui.tools.SwingTools;

import java.awt.Color;
import java.awt.event.MouseEvent;

import javax.swing.table.JTableHeader;


/**
 * Can be used to display (parts of) the meta data by means of a JTable.
 * 
 * @author Ingo Mierswa
 */
public class MetaDataViewerTable extends ExtendedJTable {

	private static final int MAXIMAL_CONTENT_LENGTH = 200;

	private static final long serialVersionUID = -4879028136543294746L;

	private int numberOfSpecialAttributeRows = 0;

	private MetaDataViewerTableModel model = null;

	public MetaDataViewerTable() {
		setCellColorProvider(new CellColorProvider() {

			@Override
			public Color getCellColor(int row, int col) {
				int actualRowIndex = row;
				if (getModel() instanceof ExtendedJTableSorterModel) {
					actualRowIndex = ((ExtendedJTableSorterModel) getModel()).modelIndex(row);
				}
				if (actualRowIndex < numberOfSpecialAttributeRows) {
					return SwingTools.LIGHTEST_YELLOW;
				} else {
					if (row % 2 == 0) {
						return Color.WHITE;
					} else {
						return SwingTools.LIGHTEST_BLUE;
					}
				}
			}
		});

		setCutOnLineBreak(false);
		setMaximalTextLength(MAXIMAL_CONTENT_LENGTH);
		installToolTip();
	}

	public MetaDataViewerTable(ExampleSet exampleSet) {
		this();
		setExampleSet(exampleSet);
	}

	public void setExampleSet(ExampleSet exampleSet) {
		this.model = new MetaDataViewerTableModel(exampleSet);
		setModel(this.model);
		if (exampleSet != null) {
			this.numberOfSpecialAttributeRows = exampleSet.getAttributes().specialSize();
		} else {
			this.numberOfSpecialAttributeRows = 0;
		}
	}

	public MetaDataViewerTableModel getMetaDataModel() {
		return this.model;
	}

	/** This method ensures that the correct tool tip for the current column is delivered. */
	@Override
	protected JTableHeader createDefaultTableHeader() {
		return new JTableHeader(columnModel) {

			private static final long serialVersionUID = 1L;

			@Override
			public String getToolTipText(MouseEvent e) {
				java.awt.Point p = e.getPoint();
				int index = columnModel.getColumnIndexAtX(p.x);
				int realColumnIndex = convertColumnIndexToModel(index);
				return model.getColumnToolTip(realColumnIndex);
			}
		};
	}
}
