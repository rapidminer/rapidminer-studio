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
package com.rapidminer.operator.preprocessing;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.math.container.Range;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


/**
 * This operator adds an ID attribute to the given example set. Each example is tagged with an
 * incremental integer number. If the example set already contains an id attribute, the old
 * attribute will be removed before the new one is added.
 * 
 * @author Ingo Mierswa
 */
public class IdTagging extends AbstractDataProcessing {

	/**
	 * The parameter name for &quot;True if nominal ids (instead of integer ids) should be
	 * created&quot;
	 */
	public static final String PARAMETER_CREATE_NOMINAL_IDS = "create_nominal_ids";

	/**
	 * The parameter name for the offset added to the generated id.
	 */
	public static final String PARAMETER_OFFSET = "offset";

	public IdTagging(OperatorDescription description) {
		super(description);
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) throws UndefinedParameterError {
		// remove id attribute if available
		AttributeMetaData idAttribute = metaData.getAttributeByRole(Attributes.ID_NAME);
		if (idAttribute != null) {
			getExampleSetInputPort().addError(
					new SimpleMetaDataError(Severity.WARNING, getExampleSetInputPort(), "id_will_be_overwritten",
							idAttribute.getName()));
			metaData.removeAttribute(idAttribute);
		}

		// create new id attribute
		boolean nominalIds = getParameterAsBoolean(PARAMETER_CREATE_NOMINAL_IDS);
		idAttribute = new AttributeMetaData(AttributeFactory.createAttribute(Attributes.ID_NAME,
				nominalIds ? Ontology.NOMINAL : Ontology.INTEGER));
		idAttribute.setRole(Attributes.ID_NAME);

		// set ID range
		int offset = getParameterAsInt(PARAMETER_OFFSET);

		if (metaData.getNumberOfExamples().isKnown()) {
			if (nominalIds) {
				Set<String> values = new LinkedHashSet<>();
				int maxNumberOfNominals = AttributeMetaData.getMaximumNumberOfNominalValues();
				boolean incomplete = false;
				for (int i = 1 + offset; i <= (offset + metaData.getNumberOfExamples().getValue().intValue()); i++) {
					values.add("id_" + i);
					if (values.size() > maxNumberOfNominals) {
						incomplete = true;
						break;
					}
				}
				idAttribute.setValueSet(values, incomplete ? SetRelation.SUPERSET : SetRelation.EQUAL);
				if (incomplete) {
					metaData.setNominalDataWasShrinked(true);
				}
			} else {
				idAttribute.setValueRange(new Range(1 + offset, offset
						+ metaData.getNumberOfExamples().getValue().doubleValue()), SetRelation.EQUAL);
			}
		}

		// add ID attribute
		metaData.addAttribute(idAttribute);
		return metaData;
	}

	@Override
	public ExampleSet apply(ExampleSet eSet) throws OperatorException {
		// only warning, removing is done by createSpecialAttribute(...)
		Attribute idAttribute = eSet.getAttributes().getId();
		if (idAttribute != null) {
			getLogger().warning("Overwriting old id attribute!");
		}

		// create new id attribute
		boolean nominalIds = getParameterAsBoolean(PARAMETER_CREATE_NOMINAL_IDS);
		idAttribute = Tools.createSpecialAttribute(eSet, Attributes.ID_NAME, nominalIds ? Ontology.NOMINAL
				: Ontology.INTEGER);

		// set IDs
		int offset = getParameterAsInt(PARAMETER_OFFSET);
		int currentId = 1 + offset;
		Iterator<Example> r = eSet.iterator();
		while (r.hasNext()) {
			Example example = r.next();
			example.setValue(idAttribute, nominalIds ? idAttribute.getMapping().mapString("id_" + currentId) : currentId);
			currentId++;
			checkForStop();
		}

		return eSet;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeBoolean(PARAMETER_CREATE_NOMINAL_IDS,
				"True if nominal ids (instead of integer ids) should be created", false);
		type.setExpert(false);
		types.add(type);

		ParameterType offsetType = new ParameterTypeInt(PARAMETER_OFFSET, "The offset which will be added to each id",
				Integer.MIN_VALUE, Integer.MAX_VALUE, 0, true);
		types.add(offsetType);

		return types;
	}

	@Override
	public boolean writesIntoExistingData() {
		return false;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), IdTagging.class, null);
	}
}
