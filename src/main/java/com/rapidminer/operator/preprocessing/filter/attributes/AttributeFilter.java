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
package com.rapidminer.operator.preprocessing.filter.attributes;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.features.selection.AbstractFeatureSelection;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.tools.AttributeSubsetSelector;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.ProcessTools;


/**
 * <p>
 * This operator filters the attributes of an exampleSet. Therefore, different conditions may be
 * selected as parameter and only attributes fulfilling this condition are kept. The rest will be
 * removed from the exampleSet The conditions may be inverted. The conditions are tested over all
 * attributes and for every attribute over all examples. For example the numeric_value_filter with
 * the parameter string &quot;&gt; 6&quot; will keep all nominal attributes and all numeric
 * attributes having a value of greater 6 in every example. A combination of conditions is possible:
 * &quot;&gt; 6 ANDAND &lt; 11&quot; or &quot;&lt;= 5 || &lt; 0&quot;. But ANDAND and || must not be
 * mixed. Please note that ANDAND has to be replaced by two ampers ands.
 * </p>
 * 
 * <p>
 * The attribute_name_filter keeps all attributes which names match the given regular expression.
 * The nominal_value_filter keeps all numeric attribute and all nominal attributes containing at
 * least one of specified nominal values. &quot;rainy ANDAND cloudy&quot; would keep all attributes
 * containing at least one time &quot;rainy&quot; and one time &quot;cloudy&quot;. &quot;rainy ||
 * sunny&quot; would keep all attributes containing at least one time &quot;rainy&quot; or one time
 * &quot;sunny&quot;. ANDAND and || are not allowed to be mixed. And again, ANDAND has to be
 * replaced by two ampers ands.
 * </p>
 * 
 * @author Sebastian Land, Ingo Mierswa
 */
public class AttributeFilter extends AbstractFeatureSelection {

	private final AttributeSubsetSelector attributeSelector = new AttributeSubsetSelector(this, getExampleSetInputPort());

	public AttributeFilter(OperatorDescription description) {
		super(description);

	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) {
		ExampleSetMetaData subset = attributeSelector.getMetaDataSubset(metaData, true);
		Iterator<AttributeMetaData> amdIterator = metaData.getAllAttributes().iterator();
		while (amdIterator.hasNext()) {
			AttributeMetaData amd = amdIterator.next();
			AttributeMetaData subsetAMD = subset.getAttributeByName(amd.getName());
			if (subsetAMD == null) {
				amdIterator.remove();
			}
		}
		return metaData;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		Attributes attributes = exampleSet.getAttributes();
		Set<Attribute> attributeSubset = attributeSelector.getAttributeSubset(exampleSet, true);
		Iterator<Attribute> r = attributes.allAttributes();
		while (r.hasNext()) {
			Attribute attribute = r.next();
			if (!attributeSubset.contains(attribute)) {
				r.remove();
			}
		}
		return exampleSet;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.addAll(ProcessTools.setSubsetSelectorPrimaryParameter(attributeSelector.getParameterTypes(), true));
		return types;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), AttributeFilter.class,
				attributeSelector);
	}
}
