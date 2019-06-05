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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.preprocessing.RemoveUnusedNominalValuesModel.MappingTranslation;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;


/**
 * This operator will remove each unused (=not occurring) nominal value from the mapping.
 *
 * @author Sebastian Land
 */
public class RemoveUnusedNominalValuesOperator extends PreprocessingOperator {

	private static final String PARAMETER_SORT_MAPPING_ALPHABETICALLY = "sort_alphabetically";

	/**
	 * Incompatible version, old version writes into the exampleset, if original output port is not
	 * connected.
	 */
	private static final OperatorVersion VERSION_MAY_WRITE_INTO_DATA = new OperatorVersion(7, 1, 1);

	/**
	 * Versions <= this version did not sort the internal mapping if an attribute did not have any unused values
	 * regardless of the sort alphabetically parameter.
	 */
	private static final OperatorVersion VERSION_DOESNT_SORT_IF_NO_UNUSED_VARIABLES
			= new OperatorVersion(9, 2, 1);

	public RemoveUnusedNominalValuesOperator(OperatorDescription description) {
		super(description);
	}

	@Override
	protected Collection<AttributeMetaData> modifyAttributeMetaData(ExampleSetMetaData emd, AttributeMetaData amd)
			throws UndefinedParameterError {
		amd.setValueSetRelation(SetRelation.SUBSET);
		return Collections.singleton(amd);
	}

	@Override
	public PreprocessingModel createPreprocessingModel(ExampleSet exampleSet) throws OperatorException {
		boolean sortMappings = getParameterAsBoolean(PARAMETER_SORT_MAPPING_ALPHABETICALLY);

		Map<String, MappingTranslation> translations = new HashMap<>();

		exampleSet.recalculateAllAttributeStatistics();
		for (Attribute attribute : exampleSet.getAttributes()) {

			if (attribute.isNominal()) {
				MappingTranslation translation = new MappingTranslation((NominalMapping) attribute.getMapping().clone());
				for (String value : attribute.getMapping().getValues()) {
					double count = exampleSet.getStatistics(attribute, Statistics.COUNT, value);
					if (count > 0) {
						translation.newMapping.mapString(value);
					}
				}
				if (translation.newMapping.size() < attribute.getMapping().size() ||
						(sortMappings && getCompatibilityLevel().isAbove(VERSION_DOESNT_SORT_IF_NO_UNUSED_VARIABLES))) {
					if (sortMappings) {
						translation.newMapping.sortMappings();
					}
					translations.put(attribute.getName(), translation);
				}
				// else: don't need a translation as nothing is changed
			}
		}
		return new RemoveUnusedNominalValuesModel(exampleSet, translations);
	}

	@Override
	protected int[] getFilterValueTypes() {
		return new int[] { Ontology.NOMINAL };
	}

	@Override
	public Class<? extends PreprocessingModel> getPreprocessingModelClass() {
		return RemoveUnusedNominalValuesModel.class;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeBoolean(PARAMETER_SORT_MAPPING_ALPHABETICALLY,
				"If checked, the resulting mapping will be sorted alphabetically.", true, false));

		return types;
	}

	@Override
	public boolean writesIntoExistingData() {
		if (getCompatibilityLevel().isAbove(VERSION_MAY_WRITE_INTO_DATA)) {
			return false;
		} else {
			// old version: true only if original output port is connected
			return isOriginalOutputConnected() && super.writesIntoExistingData();
		}
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return (OperatorVersion[]) ArrayUtils.addAll(super.getIncompatibleVersionChanges(),
				new OperatorVersion[]{VERSION_MAY_WRITE_INTO_DATA, VERSION_DOESNT_SORT_IF_NO_UNUSED_VARIABLES });
	}
}
