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
package com.rapidminer.operator.similarity;

import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.example.set.SimilarityExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.DistanceMeasurePrecondition;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.math.similarity.DistanceMeasure;
import com.rapidminer.tools.math.similarity.DistanceMeasureHelper;
import com.rapidminer.tools.math.similarity.DistanceMeasures;
import com.rapidminer.tools.metadata.MetaDataTools;

import java.util.List;


/**
 * This operator creates a new data set from the given one based on the specified similarity. The
 * created data set is merely a view so that no memory problems should occur.
 * 
 * @author Ingo Mierswa, Sebastian Land
 */
public class ExampleSet2SimilarityExampleSet extends Operator {

	private InputPort exampleSetInput = getInputPorts().createPort("example set", ExampleSet.class);
	private OutputPort exampleSetOutput = getOutputPorts().createPort("similarity example set");

	private DistanceMeasureHelper measureHelper = new DistanceMeasureHelper(this);

	public ExampleSet2SimilarityExampleSet(OperatorDescription description) {
		super(description);

		exampleSetInput.addPrecondition(new DistanceMeasurePrecondition(exampleSetInput, this));

		getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, exampleSetOutput, SetRelation.EQUAL) {

			@Override
			public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) {
				AttributeMetaData idAttribute = metaData.getSpecial(Attributes.ID_NAME);
				if (idAttribute == null) {
					MetaDataTools.checkAndCreateIds(metaData);
				}
				idAttribute = metaData.getSpecial(Attributes.ID_NAME);

				ExampleSetMetaData newSet = new ExampleSetMetaData();
				AttributeMetaData firstId = idAttribute.copy();
				AttributeMetaData secondId = idAttribute.copy();
				firstId.setName("FIRST_ID");
				firstId.setRole(Attributes.ATTRIBUTE_NAME);
				secondId.setName("SECOND_ID");
				secondId.setRole(Attributes.ATTRIBUTE_NAME);

				// determining if its distance or similarity
				DistanceMeasure measure;
				String name = "SIMILARITY";
				try {
					measure = DistanceMeasures.createMeasure(ExampleSet2SimilarityExampleSet.this);
					if (measure.isDistance()) {
						name = "DISTANCE";
					}
				} catch (UndefinedParameterError e) {
				} catch (OperatorException e) {
				}

				AttributeMetaData distanceAttribute = new AttributeMetaData(name, Ontology.REAL, Attributes.ATTRIBUTE_NAME);
				newSet.addAttribute(firstId);
				newSet.addAttribute(secondId);
				newSet.addAttribute(distanceAttribute);

				// calculating size
				if (metaData.getNumberOfExamples().isKnown()) {
					newSet.setNumberOfExamples(metaData.getNumberOfExamples().getValue().intValue()
							* metaData.getNumberOfExamples().getValue().intValue());
				}
				return newSet;
			}
		});
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
		Tools.checkAndCreateIds(exampleSet);

		DistanceMeasure measure = measureHelper.getInitializedMeasure(exampleSet);
		exampleSetOutput.deliver(new SimilarityExampleSet(exampleSet, measure));
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.addAll(DistanceMeasures.getParameterTypes(this));
		return types;
	}
}
