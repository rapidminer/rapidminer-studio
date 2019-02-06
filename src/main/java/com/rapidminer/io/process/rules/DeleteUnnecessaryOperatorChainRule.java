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
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.SimpleOperatorChain;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * This parse rule replaces any unnecessary operator chain rule.
 *
 * @author Sebastian Land
 */
public class DeleteUnnecessaryOperatorChainRule extends AbstractGenericParseRule {

	private static final VersionNumber APPLIES_UNTIL = new VersionNumber(5, 0, 0, null);

	@Override
	public String apply(final Operator operator, VersionNumber processVersion, final XMLImporter importer) {
		if (operator.getClass() == SimpleOperatorChain.class
				&& (processVersion == null || processVersion.compareTo(APPLIES_UNTIL) <= 0)) {
			importer.doAfterTreeConstruction(new Runnable() {

				@Override
				public void run() {
					OperatorChain parent = operator.getParent();
					if (parent != null) {
						// then its not root: search for subprocess containing operator
						for (ExecutionUnit unit : parent.getSubprocesses()) {
							Collection<Operator> innerOperators = unit.getChildOperators();
							if (innerOperators.size() == 1) {
								// and if only contains this operator
								if (innerOperators.iterator().next() == operator) {
									// then place children directly
									importer.addMessage("<code>OperatorChain</code>s are unneccessary if they are the only operator inside a subprocess. Removed unnecessary OperatorChain <var>"
											+ operator.getName() + "</var> in <var>" + parent.getName() + "</var>.");
									OperatorChain self = (OperatorChain) operator;
									for (Operator child : new LinkedList<Operator>(self.getSubprocess(0).getOperators())) {
										child.remove();
										unit.addOperator(child);
									}
									self.remove();
								}
							}
						}
					}
				}
			});
		}
		return null;
	}

	@Override
	public List<String> getApplicableOperatorKeys() {
		return Collections.singletonList("subprocess");
	}

}
