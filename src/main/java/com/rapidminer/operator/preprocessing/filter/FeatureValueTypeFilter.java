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
package com.rapidminer.operator.preprocessing.filter;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.tools.Ontology;

import java.util.List;


/**
 * This operator switches off all features whose value type matches the one given in the parameter
 * <code>skip_features_of_type</code>. This can be useful e.g. for learning schemes that can handle
 * only nominal attributes.
 * 
 * @author Buelent Moeller, Ingo Mierswa
 */
public class FeatureValueTypeFilter extends FeatureFilter {

	/** The parameter name for &quot;All features of this type will be deselected.&quot; */
	public static final String PARAMETER_SKIP_FEATURES_OF_TYPE = "skip_features_of_type";

	/** The parameter name for &quot;All features of this type will not be deselected.&quot; */
	public static final String PARAMETER_EXCEPT_FEATURES_OF_TYPE = "except_features_of_type";

	public FeatureValueTypeFilter(OperatorDescription description) {
		super(description);
	}

	/**
	 * Implements the method required by the superclass. For features whose type is a subtype of the
	 * one given as a parameter for this operator, TRUE is returned (otherwise FALSE). If no
	 * parameter was provided, FALSE is always returned, so no feature is switched off.
	 * 
	 * @param role
	 *            Feature to check.
	 * @return TRUE if this feature should <b>not</b> be active in the output example set of this
	 *         operator. FALSE otherwise.
	 */
	@Override
	public boolean switchOffFeature(AttributeRole role) throws OperatorException {
		Attribute feature = role.getAttribute();

		// first type (most general) was omitted in parameters
		int type = getParameterAsInt(PARAMETER_SKIP_FEATURES_OF_TYPE) + 1;

		int exceptionType = getParameterAsInt(PARAMETER_EXCEPT_FEATURES_OF_TYPE);
		if (exceptionType == 0) {
			return Ontology.ATTRIBUTE_VALUE_TYPE.isA(feature.getValueType(), type);
		} else {
			return Ontology.ATTRIBUTE_VALUE_TYPE.isA(feature.getValueType(), type)
					&& !Ontology.ATTRIBUTE_VALUE_TYPE.isA(feature.getValueType(), exceptionType);
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		// omitt first (most general) type
		String[] valueTypes = new String[Ontology.VALUE_TYPE_NAMES.length - 1];
		for (int i = 0; i < valueTypes.length; i++) {
			valueTypes[i] = Ontology.VALUE_TYPE_NAMES[i + 1];
		}
		ParameterType type = new ParameterTypeCategory(PARAMETER_SKIP_FEATURES_OF_TYPE,
				"All features of this type will be deselected.", valueTypes, 0);
		type.setExpert(false);
		types.add(type);

		String[] exceptionValueTypes = new String[valueTypes.length + 1];
		exceptionValueTypes[0] = "none";
		System.arraycopy(valueTypes, 0, exceptionValueTypes, 1, valueTypes.length);
		types.add(new ParameterTypeCategory(PARAMETER_EXCEPT_FEATURES_OF_TYPE,
				"All features of this type will not be deselected.", exceptionValueTypes, 0));
		return types;
	}
}
