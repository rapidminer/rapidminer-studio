/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.template.gui;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;


/**
 * {@link TableModel} for an {@link ExampleSet} that can optionally be limited to
 * {@link #MAX_NUMBER_OF_ROWS} rows.
 * 
 * @author Simon Fischer
 * */
public class ExampleSetTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 1L;

	private static final int MAX_NUMBER_OF_ROWS = 6;

	private ExampleSet exampleSet;
	private ArrayList<AttributeRole> attributesByIndex;

	private int limit;

	private boolean limited;

	public ExampleSetTableModel(ExampleSet exampleSet, boolean limited) {
		this.limited = limited;
		init(exampleSet);
	}

	private void init(ExampleSet exampleSet) {
		this.exampleSet = exampleSet;
		attributesByIndex = new ArrayList<>(exampleSet.getAttributes().allSize());
		Iterator<AttributeRole> i = exampleSet.getAttributes().specialAttributes();
		while (i.hasNext()) {
			AttributeRole role = i.next();
			attributesByIndex.add(role);
		}
		i = exampleSet.getAttributes().regularAttributes();
		while (i.hasNext()) {
			AttributeRole role = i.next();
			attributesByIndex.add(role);
		}
		if (this.limited) {
			limit = Math.min(exampleSet.size(), MAX_NUMBER_OF_ROWS);
		} else {
			limit = exampleSet.size();
		}
	}

	public void setExampleSet(ExampleSet newSet) {
		if (isSameAttributeSet(newSet, this.exampleSet)) {
			// Maps attribute names to their old indices
			Map<String, Integer> oldIndices = new TreeMap<>();
			int j = 0;
			for (AttributeRole role : attributesByIndex) {
				oldIndices.put(role.getAttribute().getName(), j++);
			}

			Iterator<AttributeRole> i = newSet.getAttributes().allAttributeRoles();
			while (i.hasNext()) {
				AttributeRole role = i.next();
				attributesByIndex.set(oldIndices.get(role.getAttribute().getName()), role);
			}
			this.exampleSet = newSet;

			fireTableStructureChanged();
		} else {
			init(newSet);
			fireTableStructureChanged();
		}
	}

	private boolean isSameAttributeSet(ExampleSet set1, ExampleSet set2) {
		Set<String> names1 = new HashSet<>();
		Set<String> names2 = new HashSet<>();
		Iterator<AttributeRole> i = set1.getAttributes().allAttributeRoles();
		while (i.hasNext()) {
			names1.add(i.next().getAttribute().getName());
		}
		i = set2.getAttributes().allAttributeRoles();
		while (i.hasNext()) {
			names2.add(i.next().getAttribute().getName());
		}
		return names1.equals(names2);
	}

	@Override
	public int getRowCount() {
		return limit;
	}

	@Override
	public int getColumnCount() {
		return exampleSet.getAttributes().allSize();
	}

	@Override
	public String getColumnName(int columnIndex) {
		return this.attributesByIndex.get(columnIndex).getAttribute().getName();
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return this.attributesByIndex.get(columnIndex).getAttribute().isNumerical() ? Double.class : String.class;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (rowIndex >= exampleSet.size()) {
			return null;
		} else {
			Example example = exampleSet.getExample(rowIndex);
			Attribute att = this.attributesByIndex.get(columnIndex).getAttribute();
			return att.isNumerical() ? example.getValue(att) : att.isDateTime() ? example.getDateValue(att) : example
					.getNominalValue(att);
		}
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		throw new UnsupportedOperationException("Cells not editable");
	}

	public List<AttributeRole> getAttributesByIndex() {
		return attributesByIndex;
	}
}
