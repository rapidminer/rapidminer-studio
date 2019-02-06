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

import com.rapidminer.operator.learner.associations.AssociationRule;
import com.rapidminer.operator.learner.associations.AssociationRules;
import com.rapidminer.operator.learner.associations.Item;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.table.AbstractTableModel;


/**
 * The table model for the association rules visualization.
 * 
 * @author Ingo Mierswa
 */
public class AssociationRuleTableModel extends AbstractTableModel {

	private static final long serialVersionUID = -4323147898914632476L;

	private static final String[] COLUMN_NAMES = { "No.", "Premises", "Conclusion", "Support", "Confidence", "LaPlace",
			"Gain", "p-s", "Lift", "Conviction" };

	private static final int COLUMN_RULE_ID = 0;
	private static final int COLUMN_PREMISES = 1;
	private static final int COLUMN_CONCLUSION = 2;
	private static final int COLUMN_SUPPORT = 3;
	private static final int COLUMN_CONFIDENCE = 4;
	private static final int COLUMN_LA_PLACE = 5;
	private static final int COLUMN_GAIN = 6;
	private static final int COLUMN_PS = 7;
	private static final int COLUMN_LIFT = 8;
	private static final int COLUMN_CONVICTION = 9;

	private AssociationRules rules;

	private int[] mapping = null;

	public AssociationRuleTableModel(AssociationRules rules) {
		this.rules = rules;
		createCompleteMapping();
	}

	@Override
	public Class<?> getColumnClass(int column) {
		if ((column != COLUMN_PREMISES) && (column != COLUMN_CONCLUSION)) {
			if ((column == COLUMN_RULE_ID)) {
				return Integer.class;
			} else {
				return Double.class;
			}
		} else {
			return String.class;
		}
	}

	@Override
	public String getColumnName(int column) {
		return COLUMN_NAMES[column];
	}

	@Override
	public int getColumnCount() {
		return COLUMN_NAMES.length;
	}

	@Override
	public int getRowCount() {
		return this.mapping.length;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		AssociationRule rule = rules.getRule(this.mapping[rowIndex]);
		switch (columnIndex) {
			case COLUMN_RULE_ID:
				return Integer.valueOf(this.mapping[rowIndex] + 1);
			case COLUMN_PREMISES:
				return getItemString(rule.getPremiseItems());
			case COLUMN_CONCLUSION:
				return getItemString(rule.getConclusionItems());
			case COLUMN_SUPPORT:
				return Double.valueOf(rule.getTotalSupport());
			case COLUMN_CONFIDENCE:
				return Double.valueOf(rule.getConfidence());
			case COLUMN_CONVICTION:
				return Double.valueOf(rule.getConviction());
			case COLUMN_GAIN:
				return Double.valueOf(rule.getGain());
			case COLUMN_PS:
				return Double.valueOf(rule.getPs());
			case COLUMN_LIFT:
				return Double.valueOf(rule.getLift());
			case COLUMN_LA_PLACE:
				return Double.valueOf(rule.getLaplace());
			default:
				// cannot happen
				return "?";
		}
	}

	private String getItemString(Iterator<Item> iterator) {
		StringBuffer result = new StringBuffer();
		boolean first = true;
		while (iterator.hasNext()) {
			if (!first) {
				result.append(", ");
			}
			Item item = iterator.next();
			result.append(item.toString());
			first = false;
		}
		return result.toString();
	}

	public void setFilter(boolean[] filter) {
		List<Integer> indices = new LinkedList<Integer>();
		for (int i = 0; i < filter.length; i++) {
			if (filter[i]) {
				indices.add(i);
			}
		}
		this.mapping = new int[indices.size()];
		Iterator<Integer> k = indices.iterator();
		int counter = 0;
		while (k.hasNext()) {
			this.mapping[counter++] = k.next();
		}

		fireTableStructureChanged();
	}

	private void createCompleteMapping() {
		this.mapping = new int[this.rules.getNumberOfRules()];
		for (int i = 0; i < this.mapping.length; i++) {
			this.mapping[i] = i;
		}
	}
}
