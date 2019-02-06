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
package com.rapidminer.gui.properties;

import com.rapidminer.Process;
import com.rapidminer.gui.properties.celleditors.key.PropertyKeyCellEditor;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeStringCategory;


/**
 * The parameter selection change listener used by {@link PropertyTable}s.
 * 
 * @author Ingo Mierswa
 */
public class PropertyTableParameterChangeListener implements ParameterChangeListener {

	private PropertyTable propertyTable;

	public PropertyTableParameterChangeListener(PropertyTable propertyTable) {
		this.propertyTable = propertyTable;
	}

	@Override
	public void parameterSelectionChanged(Operator parentOperator, String operatorName, String parameterName) {
		if (parentOperator != null) {
			Process process = parentOperator.getProcess();
			if (process != null) {
				Operator paramOp = process.getOperator(operatorName);
				if (paramOp != null) {
					ParameterType parameterType = paramOp.getParameterType(parameterName);
					if (parameterType != null) {
						String range = null;
						if (parameterType instanceof ParameterTypeBoolean) {
							range = "true, false";
						} else if (parameterType instanceof ParameterTypeCategory) {
							ParameterTypeCategory categoryType = (ParameterTypeCategory) parameterType;
							StringBuffer rangeBuffer = new StringBuffer();
							for (int i = 0; i < categoryType.getNumberOfCategories(); i++) {
								if (i != 0) {
									rangeBuffer.append(", ");
								}
								rangeBuffer.append(categoryType.getCategory(i));
							}
							range = rangeBuffer.toString();
						} else if (parameterType instanceof ParameterTypeStringCategory) {
							ParameterTypeStringCategory categoryType = (ParameterTypeStringCategory) parameterType;
							boolean first = true;
							StringBuffer rangeBuffer = new StringBuffer();
							for (String category : categoryType.getValues()) {
								if (!first) {
									rangeBuffer.append(", ");
								}
								rangeBuffer.append(category);
								first = false;
							}
							range = rangeBuffer.toString();
						}
						if (range == null) {
							Object defaultValue = parameterType.getDefaultValue();
							if (defaultValue != null) {
								range = defaultValue.toString();
							}
						}

						for (int r = propertyTable.getNumberOfKeyEditors() - 1; r >= 0; r--) {
							PropertyKeyCellEditor keyEditor = propertyTable.getKeyEditor(r);
							if (keyEditor != null) {
								String keyValue = (String) keyEditor.getCellEditorValue();
								if ((keyValue != null) && (keyValue.startsWith(operatorName + "." + parameterName))) {
									propertyTable.getModel().setValueAt(range, r, 1);
									break;
								}
							}
						}
					}
				}
			}
		}
	}
}
