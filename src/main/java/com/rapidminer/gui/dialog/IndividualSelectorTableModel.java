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
package com.rapidminer.gui.dialog;

import com.rapidminer.operator.features.Individual;
import com.rapidminer.operator.features.Population;
import com.rapidminer.operator.performance.PerformanceCriterion;
import com.rapidminer.operator.performance.PerformanceVector;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;


/**
 * The table model for the individual selector dialog.
 * 
 * @author Ingo Mierswa
 */
public class IndividualSelectorTableModel extends AbstractTableModel {

	private static final long serialVersionUID = -4666469076881936719L;

	private Population population;

	private List<String> columnNames = new ArrayList<String>();

	private int columnOffset = 3;

	private String[] attributeNames;

	public IndividualSelectorTableModel(String[] attributeNames, Population population) {
		this.attributeNames = attributeNames;
		this.population = population;
		if (population.getNumberOfIndividuals() > 0) {
			columnNames.add("Index");
			columnNames.add("Features");
			columnNames.add("Names");

			Individual individual = population.get(0);
			PerformanceVector performanceVector = individual.getPerformance();
			for (int i = 0; i < performanceVector.getSize(); i++) {
				PerformanceCriterion criterion = performanceVector.getCriterion(i);
				columnNames.add(criterion.getName());
			}
		}
	}

	@Override
	public Class<?> getColumnClass(int c) {
		switch (c) {
			case 0:
				return Integer.class;
			case 1:
				return Integer.class;
			case 2:
				return String.class;
			default:
				return Double.class;
		}
	}

	@Override
	public String getColumnName(int c) {
		return columnNames.get(c);
	}

	@Override
	public int getColumnCount() {
		return columnNames.size();
	}

	@Override
	public int getRowCount() {
		return population.getNumberOfIndividuals();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		switch (columnIndex) {
			case 0:
				return (rowIndex + 1);
			case 1:
				Individual individual = population.get(rowIndex);
				return individual.getNumberOfUsedAttributes();
			case 2:
				individual = population.get(rowIndex);
				double[] weights = individual.getWeights();
				StringBuffer names = new StringBuffer();
				boolean first = true;
				for (int w = 0; w < weights.length; w++) {
					if (weights[w] > 0.0) {
						if (!first) {
							names.append(", ");
						}
						names.append(attributeNames[w]);
						first = false;
					}
				}
				return names.toString();
			default:
				int perfIndex = columnIndex - columnOffset;
				individual = population.get(rowIndex);
				PerformanceCriterion criterion = individual.getPerformance().getCriterion(perfIndex);
				return criterion.getAverage();
		}
	}
}
