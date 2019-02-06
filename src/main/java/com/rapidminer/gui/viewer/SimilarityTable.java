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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.report.Tableable;
import com.rapidminer.tools.math.similarity.DistanceMeasure;


/**
 * The table for the similarity viewer.
 *
 * @author Ingo Mierswa
 */
public class SimilarityTable extends JPanel implements Tableable {

	private static final long serialVersionUID = 8251521865453407142L;

	private ExtendedJTable similarityTable;

	public SimilarityTable(DistanceMeasure measure, ExampleSet exampleSet) {
		GridBagLayout layout = new GridBagLayout();
		setLayout(layout);
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		c.gridwidth = GridBagConstraints.REMAINDER;

		similarityTable = new ExtendedJTable();
		SimilarityTableModel model = new SimilarityTableModel(measure, exampleSet);
		similarityTable.setModel(model);

		JScrollPane tablePane = new ExtendedJScrollPane(similarityTable);
		tablePane.setBorder(null);
		layout.setConstraints(tablePane, c);
		add(tablePane);
	}

	@Override
	public void prepareReporting() {
		similarityTable.prepareReporting();
	}

	@Override
	public void finishReporting() {
		similarityTable.finishReporting();
	}

	@Override
	public String getColumnName(int columnIndex) {
		return similarityTable.getColumnName(columnIndex);
	}

	@Override
	public String getCell(int row, int column) {
		return similarityTable.getCell(row, column);
	}

	@Override
	public int getColumnNumber() {
		return similarityTable.getColumnNumber();
	}

	@Override
	public int getRowNumber() {
		return similarityTable.getRowNumber();
	}

	@Override
	public boolean isFirstLineHeader() {
		return false;
	}

	@Override
	public boolean isFirstColumnHeader() {
		return false;
	}
}
