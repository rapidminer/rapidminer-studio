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

import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.features.selection.AbstractFeatureSelection;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;

import java.util.Iterator;
import java.util.List;


/**
 * This is an abstract superclass for feature filters. These operators take an
 * <code>ExampleSet</code> as input and switch off all features that fulfill a condition. Subclasses
 * must implement the condition in the method <code>switchOffFeature()</code>, which returns a
 * boolean.
 * 
 * @author Ingo Mierswa
 */
public abstract class FeatureFilter extends AbstractFeatureSelection {

	/** The parameter name for &quot;Filter also special attributes (label, id...)&quot; */
	public static final String PARAMETER_FILTER_SPECIAL_FEATURES = "filter_special_features";

	public FeatureFilter(OperatorDescription description) {
		super(description);
	}

	/**
	 * Must be implemented by subclasses. When operators extending this class are applied, they loop
	 * through the set of features and switch off all features for which this method returns TRUE,
	 * while keeping the status of the other features.
	 * 
	 * @param theFeature
	 *            Feature to check.
	 * @return TRUE if this feature should <b>not</b> be active in the output example set of this
	 *         operator. FALSE otherwise.
	 */
	public abstract boolean switchOffFeature(AttributeRole theFeature) throws OperatorException;

	/**
	 * Applies filtering of features by looping through all features and checking
	 * <code>switchOffFeature()</code>. If TRUE is returned, the feature is switched off, ie it
	 * won't be used by the following operators in the chain. If FALSE is returned by
	 * <code>switchOffFeature()</code>, the feature will keep its previous status.
	 * 
	 * @return An array of IOObjects, with the output example set as the only member.
	 */
	@Override
	public ExampleSet apply(ExampleSet eSet) throws OperatorException {
		log(eSet.getAttributes().size() + " features before filtering.");

		Iterator<AttributeRole> i = eSet.getAttributes().allAttributeRoles();
		boolean filterSpecial = getParameterAsBoolean(PARAMETER_FILTER_SPECIAL_FEATURES);
		while (i.hasNext()) {
			AttributeRole role = i.next();
			if ((role.isSpecial()) && (!filterSpecial)) {
				continue;
			}
			if (switchOffFeature(role)) {
				i.remove();
			}
			checkForStop();
		}

		log(eSet.getAttributes().size() + " features left after filtering.");
		return eSet;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeBoolean(PARAMETER_FILTER_SPECIAL_FEATURES,
				"Filter also special attributes (label, id...)", false));
		return types;
	}
}
