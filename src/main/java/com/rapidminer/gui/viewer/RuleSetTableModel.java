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

import com.rapidminer.operator.learner.subgroups.RuleSet;
import com.rapidminer.operator.learner.subgroups.hypothesis.Rule;
import com.rapidminer.operator.learner.subgroups.utility.Accuracy;
import com.rapidminer.operator.learner.subgroups.utility.Bias;
import com.rapidminer.operator.learner.subgroups.utility.Binomial;
import com.rapidminer.operator.learner.subgroups.utility.Coverage;
import com.rapidminer.operator.learner.subgroups.utility.Lift;
import com.rapidminer.operator.learner.subgroups.utility.Odds;
import com.rapidminer.operator.learner.subgroups.utility.OddsRatio;
import com.rapidminer.operator.learner.subgroups.utility.Precision;
import com.rapidminer.operator.learner.subgroups.utility.Squared;
import com.rapidminer.operator.learner.subgroups.utility.WRAcc;

import javax.swing.table.AbstractTableModel;


/**
 * The table model for the rule set visualization.
 * 
 * @author Ingo Mierswa, Tobias Malbrecht
 */
public class RuleSetTableModel extends AbstractTableModel {

	private static final long serialVersionUID = -4323147898914632476L;

	private static final String[] COLUMN_NAMES = { "Premise", "Conclusion", "Pos", "Neg", "Size", "Coverage", "Precision",
			"Accuracy", "Bias", "Lift", "Binomial", "WRAcc", "Squared", "Odds", "Odds Ratio", "Length" };

	private static final int COLUMN_PREMISES = 0;
	private static final int COLUMN_CONCLUSION = 1;
	private static final int COLUMN_POSITIVE = 2;
	private static final int COLUMN_NEGATIVE = 3;
	private static final int COLUMN_SIZE = 4;
	private static final int COLUMN_COVERAGE = 5;
	private static final int COLUMN_PRECISION = 6;
	private static final int COLUMN_ACCURACY = 7;
	private static final int COLUMN_BIAS = 8;
	private static final int COLUMN_LIFT = 9;
	private static final int COLUMN_BINOMIAL = 10;
	private static final int COLUMN_WRACC = 11;
	private static final int COLUMN_SQUARED = 12;
	private static final int COLUMN_ODDS = 13;
	private static final int COLUMN_OR = 14;
	private static final int COLUMN_LENGTH = 15;

	private RuleSet rules;

	public RuleSetTableModel(RuleSet rules) {
		this.rules = rules;
	}

	@Override
	public Class<?> getColumnClass(int column) {
		if ((column != COLUMN_PREMISES) && (column != COLUMN_CONCLUSION)) {
			return Double.class;
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
		return rules.getNumberOfRules();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		Rule rule = rules.getRule(rowIndex);
		switch (columnIndex) {
			case COLUMN_PREMISES:
				return rule.getPremise().toString();
			case COLUMN_CONCLUSION:
				return rule.getConclusion().getValueAsString();
			case COLUMN_POSITIVE:
				return rule.getPositiveWeight();
			case COLUMN_NEGATIVE:
				return rule.getNegativeWeight();
			case COLUMN_SIZE:
				return rule.getCoveredWeight();
			case COLUMN_COVERAGE:
				return rule.getUtility(Coverage.class);
			case COLUMN_PRECISION:
				return rule.getUtility(Precision.class);
			case COLUMN_ACCURACY:
				return rule.getUtility(Accuracy.class);
			case COLUMN_BIAS:
				return rule.getUtility(Bias.class);
			case COLUMN_LIFT:
				return rule.getUtility(Lift.class);
			case COLUMN_BINOMIAL:
				return rule.getUtility(Binomial.class);
			case COLUMN_WRACC:
				return rule.getUtility(WRAcc.class);
			case COLUMN_SQUARED:
				return rule.getUtility(Squared.class);
			case COLUMN_ODDS:
				return rule.getUtility(Odds.class);
			case COLUMN_OR:
				return rule.getUtility(OddsRatio.class);
			case COLUMN_LENGTH:
				return rule.getHypothesis().getNumberOfLiterals();
			default:
				// cannot happen
				return "?";
		}
	}

}
