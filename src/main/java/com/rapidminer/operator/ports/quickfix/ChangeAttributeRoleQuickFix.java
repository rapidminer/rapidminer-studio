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
package com.rapidminer.operator.ports.quickfix;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.preprocessing.filter.ChangeAttributeRole;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorService;


/**
 * @author Sebastian Land
 * 
 */
public class ChangeAttributeRoleQuickFix extends OperatorInsertionQuickFix {

	private final InputPort inputPort;
	private final String role;

	/**
	 * @param rating
	 * @param isInteractive
	 * @param i18nKey
	 * @param i18nArgs
	 */
	public ChangeAttributeRoleQuickFix(InputPort inputPort, String role, String i18nKey, Object... i18nArgs) {
		super(i18nKey, i18nArgs, 10, inputPort);
		// super(3, true, i18nKey, i18nArgs);
		this.inputPort = inputPort;
		this.role = role;
	}

	@Override
	public Operator createOperator() throws OperatorCreationException {
		MetaData metaData = inputPort.getMetaData();
		if ((metaData == null) || !(metaData instanceof ExampleSetMetaData)) {
			return null;
		}

		ChangeAttributeRole car = OperatorService.createOperator(ChangeAttributeRole.class);

		Object[] options = (((ExampleSetMetaData) metaData).getAttributeNamesByType(Ontology.VALUE_TYPE)).toArray();
		if (options.length > 0) {
			Object option = SwingTools.showInputDialog("quickfix.replace_by_dictionary", options, options[0], car
					.getParameters().getParameterType(ChangeAttributeRole.PARAMETER_NAME).getDescription());
			if (option != null) {
				car.setParameter(ChangeAttributeRole.PARAMETER_NAME, option.toString());
				car.setParameter(ChangeAttributeRole.PARAMETER_TARGET_ROLE, role);
				return car;
			} else {
				return null;
			}
		} else {
			return car;
		}
	}

	// Object option = SwingTools.showInputDialog("quickfix.replace_by_dictionary", options,
	// options[nearestOption], description);

	/*
	 * @Override public void apply() { try { if (inputPort.getMetaData() instanceof
	 * ExampleSetMetaData) { ExampleSetMetaData emd = (ExampleSetMetaData) inputPort.getMetaData();
	 * Operator operator = OperatorService.createOperator(ChangeAttributeRole.class); ExecutionUnit
	 * process = inputPort.getPorts().getOwner().getOperator().getExecutionUnit();
	 * process.addOperator(operator); OutputPort source = inputPort.getSource();
	 * source.disconnect(); source.connectTo(operator.getInputPorts().getPortByIndex(0));
	 * operator.getOutputPorts().getPortByIndex(0).connectTo(inputPort);
	 * 
	 * // selecting attribute new AttributeSelectionQuickFix(emd,
	 * ChangeAttributeRole.PARAMETER_NAME, operator, "").apply();
	 * 
	 * // switching role parameter operator.setParameter(ChangeAttributeRole.PARAMETER_TARGET_ROLE,
	 * role); } } catch (Exception e) { SwingTools.showSimpleErrorMessage("Cannot insert operator: "
	 * + e, e); } }
	 */
}
