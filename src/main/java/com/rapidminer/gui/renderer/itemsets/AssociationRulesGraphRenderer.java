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
package com.rapidminer.gui.renderer.itemsets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.rapidminer.gui.graphs.AssociationRulesGraphCreator;
import com.rapidminer.gui.graphs.GraphCreator;
import com.rapidminer.gui.renderer.AbstractGraphRenderer;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.learner.associations.AssociationRule;
import com.rapidminer.operator.learner.associations.AssociationRules;


/**
 * A renderer for the graph view of association rules.
 *
 * @author Ingo Mierswa, Nils Woehler
 */
public class AssociationRulesGraphRenderer extends AbstractGraphRenderer {

	private final int MAX_RULE_NUMBER = 500;

	@Override
	public GraphCreator<String, String> getGraphCreator(Object renderable, IOContainer ioContainer) {
		AssociationRules rules = (AssociationRules) renderable;
		int numberOfRules = rules.getNumberOfRules();
		if (numberOfRules > 0) {
			if (numberOfRules > MAX_RULE_NUMBER) {
				List<AssociationRule> ruleList = new ArrayList<AssociationRule>(numberOfRules);
				for (int i = 0; i < numberOfRules; ++i) {
					ruleList.add(rules.getRule(i));
				}
				Collections.sort(ruleList, new Comparator<AssociationRule>() {

					@Override
					public int compare(AssociationRule rule1, AssociationRule rule2) {
						if (rule1.getConfidence() == rule2.getConfidence()) {
							return 0;
						} else if (rule1.getConfidence() < rule2.getConfidence()) {
							return -1;
						} else {
							return 1;
						}
					}
				});
				AssociationRules filteredRules = new AssociationRules();
				for (int i = 0; i < MAX_RULE_NUMBER; ++i) {
					filteredRules.addItemRule(ruleList.get(i));
				}
				rules = filteredRules;
			}
			rules.sort();
			return new AssociationRulesGraphCreator(rules);

		} else {
			return null;
		}
	}
}
