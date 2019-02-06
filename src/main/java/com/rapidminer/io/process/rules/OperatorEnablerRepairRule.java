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
package com.rapidminer.io.process.rules;

import com.rapidminer.gui.tools.VersionNumber;
import com.rapidminer.io.process.XMLImporter;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.meta.OperatorEnabler;
import com.rapidminer.operator.meta.OperatorSelector;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.OperatorService;

import java.util.Collections;
import java.util.List;


/**
 *
 * @author Sebastian Land
 */
public class OperatorEnablerRepairRule extends AbstractGenericParseRule {

	private static final VersionNumber APPLIES_UNTIL = new VersionNumber(5, 0, 0, null);

	@Override
	public String apply(Operator operator, VersionNumber processVersion, XMLImporter importer) {
		if (operator.getClass().equals(OperatorEnabler.class)
				&& (processVersion == null || processVersion.compareTo(APPLIES_UNTIL) < 0)) {
			OperatorEnabler enabler = (OperatorEnabler) operator;
			Operator selected = null;
			String selectedName;
			try {
				selectedName = enabler.getParameterAsString(OperatorEnabler.PARAMETER_OPERATOR_NAME);
			} catch (UndefinedParameterError e) {
				selectedName = null;
			}
			for (Operator op : enabler.getAllInnerOperators()) {
				if (op.getName().equals(selectedName)) {
					selected = op;
					break;
				}
			}

			OperatorSelector selector;
			try {
				selector = OperatorService.createOperator(OperatorSelector.class);
			} catch (OperatorCreationException e) {
				return "<em class=\"error\">The operator <var>" + enabler.getName()
						+ "</var> (<code>OperatorEnabler</code>) could not be imported: " + e.getMessage() + "</em>";
			}
			int selectedSubprocess = enabler.getParameterAsBoolean(OperatorEnabler.PARAMETER_ENABLE) ? 0 : 1;
			selector.setParameter(OperatorSelector.PARAMETER_SELECT_WHICH, "" + (selectedSubprocess + 1));
			while (!enabler.getSubprocess(0).getOperators().isEmpty()) {
				Operator child = enabler.getSubprocess(0).getOperators().get(0);
				child.remove();
				selector.getSubprocess(0).addOperator(child.cloneOperator(null, false));
				if (!child.getName().equals(selectedName)) {
					selector.getSubprocess(1).addOperator(child.cloneOperator(null, false));
				}
			}

			ExecutionUnit parent = enabler.getExecutionUnit();
			int oldIndex = parent.getOperators().indexOf(enabler);
			enabler.remove();
			selector.rename(enabler.getName());
			parent.addOperator(selector, oldIndex);
			if (selected == null) {
				return "<em class=\"error\">The operator <var>"
						+ enabler.getName()
						+ "</var> (<code>OperatorEnabler</code>) could not be correctly imported since the enabled/disabled child '"
						+ selectedName + "' was not found.</em>";
			} else {
				return "The operator <var>"
						+ enabler.getName()
						+ "</var> (<code>OperatorEnabler</code>) was replaced by an <code>OperatorSelector</code> where in subprocess "
						+ selectedSubprocess + " the operator <var>" + selected.getName() + "</var> was omitted.";
			}
		} else {
			return null;
		}
	}

	@Override
	public List<String> getApplicableOperatorKeys() {
		return Collections.singletonList("operatorenabler");
	}

}
