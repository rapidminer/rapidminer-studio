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

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.tools.math.similarity.DistanceMeasure;

import javax.swing.table.AbstractTableModel;


/**
 * The table model for the similarity visualization.
 * 
 * @author Ingo Mierswa
 */
public class SimilarityTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 7451178433975831387L;

	private static final int COLUMN_FIRST = 0;
	private static final int COLUMN_SECOND = 1;
	private static final int COLUMN_SIMILARITY = 2;

	private DistanceMeasure similarity;

	private ExampleSet exampleSet;

	private Attribute idAttribute;

	public SimilarityTableModel(DistanceMeasure similarity, ExampleSet exampleSet) {
		this.similarity = similarity;
		this.exampleSet = exampleSet;
		this.idAttribute = exampleSet.getAttributes().getId();
	}

	@Override
	public String getColumnName(int column) {
		switch (column) {
			case 0:
				return "First";
			case 1:
				return "Second";
			case 2:
				if (similarity.isDistance()) {
					return "Distance";
				} else {
					return "Similarity";
				}
		}
		return "";
	}

	@Override
	public Class<?> getColumnClass(int column) {
		if (column == COLUMN_SIMILARITY) {
			return Double.class;
		} else {
			return String.class;
		}
	}

	@Override
	public int getColumnCount() {
		return 3;
	}

	@Override
	public int getRowCount() {
		int n = exampleSet.size();
		return ((n - 1) * n) / 2;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		int[] actualRows = getActualRows(rowIndex);
		Example first = exampleSet.getExample(actualRows[0]);
		Example second = exampleSet.getExample(actualRows[1]);
		switch (columnIndex) {
			case COLUMN_FIRST:
				return first.getValueAsString(idAttribute);
			case COLUMN_SECOND:
				return second.getValueAsString(idAttribute);
			case COLUMN_SIMILARITY:
				if (similarity.isDistance()) {
					return Double.valueOf(this.similarity.calculateDistance(first, second));
				} else {
					return Double.valueOf(this.similarity.calculateSimilarity(first, second));
				}
			default:
				// cannot happen
				return "?";
		}
	}

	private int[] getActualRows(int rowIndex) {
		int sum = 0;
		int currentLength = exampleSet.size() - 1;
		int result = 0;
		while ((sum + currentLength) <= rowIndex) {
			sum += currentLength;
			currentLength--;
			result++;
		}
		return new int[] { result, exampleSet.size() - (sum + currentLength - rowIndex) };
	}
}
