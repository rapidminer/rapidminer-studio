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
package com.rapidminer.operator.ports.metadata;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.quickfix.QuickFix;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;


/**
 * This precondition can be used, if a single attribute must be contained in the example set. Three
 * properties of the attribute might be given: Name, Type and Role. Type and Role are optional. The
 * attribute name is not given explicitly, instead a parameter name of an operator is given, from
 * which the attribute name is retrieved during runtime.
 * 
 * @author Sebastian Land
 * 
 */
public class AttributeParameterPrecondition extends AbstractPrecondition {

	private final Operator operator;
	private final String parameterName;
	private final int attributeType;
	private final String attributeRole;

	/**
	 * This precondition will only check the name. No Role and type checks will be performed.
	 */
	public AttributeParameterPrecondition(InputPort inport, Operator operator, String parameterName) {
		this(inport, operator, parameterName, null, Ontology.VALUE_TYPE);
	}

	/**
	 * This precondition will not perform any role check.
	 */
	public AttributeParameterPrecondition(InputPort inport, Operator operator, String parameterName, int attributeType) {
		this(inport, operator, parameterName, null, attributeType);
	}

	public AttributeParameterPrecondition(InputPort inport, Operator operator, String parameterName, String attributeRole,
			int attributeType) {
		super(inport);
		this.operator = operator;
		this.parameterName = parameterName;
		this.attributeType = attributeType;
		this.attributeRole = attributeRole;

	}

	@Override
	public void check(MetaData metaData) {
		if (metaData != null) {
			if (metaData instanceof ExampleSetMetaData) {
				ExampleSetMetaData emd = (ExampleSetMetaData) metaData;
				String attributeName = getName();
				if (attributeName != null) {
					// checking if attribute with name and type exists
					MetaDataInfo containsRelation = emd.containsAttributeName(attributeName);
					if (containsRelation == MetaDataInfo.YES) {
						AttributeMetaData amd = emd.getAttributeByName(attributeName);
						if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(amd.getValueType(), attributeType)) {
							if (attributeRole != null && !attributeRole.equals(amd.getRole())) {
								createError(Severity.ERROR, "attribute_must_have_role", attributeName, attributeRole);
							}
						} else {
							createError(Severity.ERROR, "attribute_has_wrong_type", attributeName,
									Ontology.ATTRIBUTE_VALUE_TYPE.getNames()[attributeType]);
						}
					} else {
						if (containsRelation == MetaDataInfo.UNKNOWN) {
							createError(Severity.WARNING, "missing_attribute", attributeName);
						} else {
							createError(Severity.ERROR, "missing_attribute", attributeName);
						}
					}
				}
				makeAdditionalChecks(emd);
			}
		}
	}

	/**
	 * This method returns the name of the attribute that must be contained in the meta data. It
	 * might return null, if no check should be performed.
	 */
	protected String getName() {
		try {
			return operator.getParameterAsString(parameterName);
		} catch (UndefinedParameterError e) {
			return null;
		}
	}

	@Override
	public void assumeSatisfied() {
		getInputPort().receiveMD(new ExampleSetMetaData());
	}

	/** Can be implemented by subclasses in order to specify quickfixes. */
	public QuickFix getQuickFix(ExampleSetMetaData emd) throws UndefinedParameterError {
		return null;
	}

	/** Can be implemented by subclasses. */
	public void makeAdditionalChecks(ExampleSetMetaData emd) {}

	@Override
	public String getDescription() {
		return "<em>expects:</em> ExampleSet";
	}

	@Override
	public boolean isCompatible(MetaData input, CompatibilityLevel level) {
		return ExampleSet.class.isAssignableFrom(input.getObjectClass());
	}

	@Override
	public MetaData getExpectedMetaData() {
		return new ExampleSetMetaData();
	}

}
