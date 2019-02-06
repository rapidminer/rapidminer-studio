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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.example.set.SortedExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.container.Tupel;
import com.rapidminer.tools.math.container.BoundedPriorityQueue;
import com.rapidminer.tools.math.similarity.DistanceMeasure;
import com.rapidminer.tools.math.similarity.DistanceMeasures;


/**
 * This operator creates an exampleSet containing the distances between each example of the request
 * exampleSet and the k nearest of the reference exampleSet.
 *
 * This operator needs ID attributes in both example sets in order to work. If not present, new ones
 * are created.
 *
 * @author Sebastian Land
 */
public class CrossDistancesOperator extends Operator {

	public static final String PARAMETER_K = "k";
	public static final String PARAMETER_USE_K = "only_top_k";
	public static final String PARAMETER_SEARCH_MODE = "search_for";
	public static final String PARAMETER_COMPUTE_SIMILARITIES = "compute_similarities";

	private static final String[] SEARCH_MODE = new String[] { "nearest", "farthest" };
	private static final int MODE_NEAREST = 0;
	private static final int MODE_FARTHEST = 1;

	private InputPort requestSetInput = getInputPorts().createPort("request set", ExampleSet.class);
	private InputPort referenceSetInput = getInputPorts().createPort("reference set", ExampleSet.class);
	private OutputPort resultSetOutput = getOutputPorts().createPort("result set");
	private OutputPort requestSetOutput = getOutputPorts().createPort("request set");
	private OutputPort referenceSetOutput = getOutputPorts().createPort("reference set");

	public CrossDistancesOperator(OperatorDescription description) {
		super(description);

		getTransformer().addPassThroughRule(referenceSetInput, referenceSetOutput);
		getTransformer().addPassThroughRule(requestSetInput, requestSetOutput);
		getTransformer().addRule(new GenerateNewMDRule(resultSetOutput, ExampleSet.class) {

			@Override
			public MetaData modifyMetaData(MetaData unmodifiedMetaData) {
				try {
					// getting types of old id attributes
					ExampleSetMetaData refMD = (ExampleSetMetaData) referenceSetInput.getMetaData();
					ExampleSetMetaData requestMD = (ExampleSetMetaData) requestSetInput.getMetaData();
					AttributeMetaData refId = refMD == null ? null : refMD.getAttributeByRole(Attributes.ID_NAME);
					AttributeMetaData requestId = requestMD == null ? null
							: requestMD.getAttributeByRole(Attributes.ID_NAME);

					ExampleSetMetaData emd = new ExampleSetMetaData();
					emd.addAttribute(
							new AttributeMetaData("request", requestId == null ? Ontology.REAL : requestId.getValueType()));
					emd.addAttribute(
							new AttributeMetaData("document", refId == null ? Ontology.REAL : refId.getValueType()));
					emd.addAttribute(new AttributeMetaData("distance", Ontology.REAL));

					return emd;
				} catch (ClassCastException e) {
					return unmodifiedMetaData;
				}
			}
		});
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet requestSet = requestSetInput.getData(ExampleSet.class);
		ExampleSet documentSet = referenceSetInput.getData(ExampleSet.class);
		Tools.checkAndCreateIds(requestSet);
		Tools.checkAndCreateIds(documentSet);

		DistanceMeasure measure = DistanceMeasures.createMeasure(this);
		measure.init(requestSet.getAttributes(), documentSet.getAttributes());

		Attribute oldRequestId = requestSet.getAttributes().getId();
		Attribute oldDocumentId = documentSet.getAttributes().getId();

		// creating new exampleSet
		Attribute requestId = AttributeFactory.createAttribute("request", oldRequestId.getValueType());
		Attribute documentId = AttributeFactory.createAttribute("document", oldDocumentId.getValueType());
		Attribute distance = AttributeFactory.createAttribute("distance", Ontology.REAL);

		List<Attribute> newAttributes = new LinkedList<Attribute>();
		Collections.addAll(newAttributes, requestId, documentId, distance);
		ExampleSetBuilder builder = ExampleSets.from(newAttributes);

		double searchModeFactor = getParameterAsInt(PARAMETER_SEARCH_MODE) == MODE_FARTHEST ? -1d : 1d;
		boolean computeSimilarity = getParameterAsBoolean(PARAMETER_COMPUTE_SIMILARITIES);
		boolean useK = getParameterAsBoolean(PARAMETER_USE_K);
		int k = getParameterAsInt(PARAMETER_K);

		for (Example request : requestSet) {
			Collection<Tupel<Double, Double>> distances;
			if (useK) {
				distances = new BoundedPriorityQueue<Tupel<Double, Double>>(k);
			} else {
				distances = new ArrayList<Tupel<Double, Double>>();
			}

			// calculating distance
			for (Example document : documentSet) {
				if (computeSimilarity) {
					distances
							.add(new Tupel<Double, Double>(measure.calculateSimilarity(request, document) * searchModeFactor,
									document.getValue(oldDocumentId)));
				} else {
					distances.add(new Tupel<Double, Double>(measure.calculateDistance(request, document) * searchModeFactor,
							document.getValue(oldDocumentId)));
				}
				checkForStop();
			}

			// writing into table
			DataRowFactory factory = new DataRowFactory(DataRowFactory.TYPE_DOUBLE_ARRAY, '.');
			double requestIdValue = request.getValue(oldRequestId);
			if (oldRequestId.isNominal()) {
				requestIdValue = requestId.getMapping().mapString(request.getValueAsString(oldRequestId));
			}

			for (Tupel<Double, Double> tupel : distances) {
				double documentIdValue = tupel.getSecond();
				if (oldDocumentId.isNominal()) {
					documentIdValue = documentId.getMapping()
							.mapString(oldDocumentId.getMapping().mapIndex((int) documentIdValue));
				}
				DataRow row = factory.create(3);
				row.set(distance, tupel.getFirst() * searchModeFactor);
				row.set(requestId, requestIdValue);
				row.set(documentId, documentIdValue);
				builder.addDataRow(row);
				checkForStop();
			}
		}

		// sorting set
		ExampleSet result = new SortedExampleSet(builder.build(), distance,
				searchModeFactor == -1d ? SortedExampleSet.DECREASING : SortedExampleSet.INCREASING);

		requestSetOutput.deliver(requestSet);
		referenceSetOutput.deliver(documentSet);
		resultSetOutput.deliver(result);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.addAll(DistanceMeasures.getParameterTypes(this));

		ParameterType type = new ParameterTypeBoolean(PARAMETER_USE_K,
				"Only calculate the k nearest to each request example.", false);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeInt(PARAMETER_K, "Determines how many of the nearest examples are shown in the result.", 1,
				Integer.MAX_VALUE, 10);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_K, true, true));
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeCategory(PARAMETER_SEARCH_MODE,
				"Determines if the smallest (nearest) or the largest (farthest) distances or similarities should be selected. Keep in mind that the meaning inverses if you compute the similarity instead the distance between examples!",
				SEARCH_MODE, MODE_NEAREST, false);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_K, true, true));
		types.add(type);

		types.add(new ParameterTypeBoolean(PARAMETER_COMPUTE_SIMILARITIES,
				"If checked the similarities are computed instead of the distances. All measures will still be usable, but measures that are not originally distance or respectively similarity measures are transformed to match optimization direction. This will most likely transform the scale in a non linear way.",
				false, true));
		return types;
	}
}
