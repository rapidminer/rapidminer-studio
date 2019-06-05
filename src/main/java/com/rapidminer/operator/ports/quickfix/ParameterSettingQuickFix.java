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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.gui.properties.AttributesPropertyDialog;
import com.rapidminer.gui.properties.ListPropertyDialog;
import com.rapidminer.gui.tools.dialogs.SetParameterDialog;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttributes;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeConfiguration;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.UndefinedParameterError;


/**
 * @author Sebastian Land
 */
public class ParameterSettingQuickFix extends AbstractQuickFix {

	private Operator operator;
	private String parameterName;
	private String value;

	public ParameterSettingQuickFix(Operator operator, String parameterName) {
		this(operator, parameterName, null, "set_parameter", parameterName.replace('_', ' '));

		ParameterType type = operator.getParameterType(parameterName);
		if (type instanceof ParameterTypeConfiguration) {
			seti18nKey("set_parameters_using_wizard");
		} else if (type instanceof ParameterTypeList) {
			seti18nKey("set_parameter_list", parameterName.replace('_', ' '));
		}
	}

	public ParameterSettingQuickFix(Operator operator, String parameterName, String value) {
		this(operator, parameterName, value, "correct_parameter_settings_by", parameterName, value);

		ParameterType type = operator.getParameterType(parameterName);
		if (type instanceof ParameterTypeConfiguration) {
			seti18nKey("correct_parameter_settings_with_wizard");
		} else if (type instanceof ParameterTypeList) {
			seti18nKey("correct_parameter_settings_list", parameterName.replace('_', ' '));
		} else if (value != null && type instanceof ParameterTypeBoolean) {
			String i18nKey;
			if (Boolean.parseBoolean(value)) {
				i18nKey = "correct_parameter_settings_boolean_enable";
			} else {
				i18nKey = "correct_parameter_settings_boolean_disable";
			}
			seti18nKey(i18nKey, parameterName.replace('_', ' '));
		}
	}

	/**
	 * This constructor will build a quickfix that let's the user select an appropriate value for
	 * the given parameter.
	 */
	public ParameterSettingQuickFix(Operator operator, String parameterName, String i18nKey, Object... i18nArgs) {
		this(operator, parameterName, null, i18nKey, i18nArgs);
	}

	/**
	 * This constructor will build a quickfix that will automatically set the parameter to the given
	 * value without further user interaction. Use this constructor if you can comprehend the
	 * correct value.
	 */
	public ParameterSettingQuickFix(Operator operator, String parameterName, String value, String i18nKey,
			Object... i18nArgs) {
		super(1, true, i18nKey, i18nArgs);
		this.operator = operator;
		this.parameterName = parameterName;
		this.value = value;
	}

	@Override
	public void apply() {
		ParameterType type = operator.getParameterType(parameterName);
		if (value != null) {
			operator.setParameter(parameterName, value);
		} else {
			if (type instanceof ParameterTypeConfiguration) {
				ParameterTypeConfiguration confType = (ParameterTypeConfiguration) type;
				confType.getWizardCreator().createConfigurationWizard(type, confType.getWizardListener());
			} else if (type instanceof ParameterTypeList) {
				List<String[]> list;
				try {
					list = operator.getParameterList(parameterName);
				} catch (UndefinedParameterError e) {
					list = new LinkedList<>();
				}
				ListPropertyDialog dialog = new ListPropertyDialog((ParameterTypeList) type, list, operator);
				dialog.setVisible(true);
				if (dialog.isOk()) {
					operator.setListParameter(parameterName, list);
				}
			} else if (type instanceof ParameterTypeAttributes) {
				AttributesPropertyDialog dialog = new AttributesPropertyDialog((ParameterTypeAttributes) type,
						Collections.emptyList());
				dialog.setVisible(true);
				if (dialog.isOk()) {
					boolean first = true;
					String attributeListString = "";
					Collection<String> attributeNames = dialog.getSelectedAttributeNames();
					for (String attributeName : attributeNames) {
						if (!first) {
							attributeListString = attributeListString.concat("|");
						}
						attributeListString = attributeListString.concat(attributeName);
						first = false;
					}
					operator.setParameter(parameterName, attributeListString);
				}
			} else {
				SetParameterDialog dialog = new SetParameterDialog(operator, type);
				dialog.setVisible(true);
			}
		}
	}
}
