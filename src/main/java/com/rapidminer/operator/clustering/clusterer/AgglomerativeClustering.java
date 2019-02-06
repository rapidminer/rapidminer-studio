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
package com.rapidminer.operator.clustering.clusterer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.gui.ExampleVisualizer;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.clustering.DendogramHierarchicalClusterModel;
import com.rapidminer.operator.clustering.HierarchicalClusterLeafNode;
import com.rapidminer.operator.clustering.HierarchicalClusterModel;
import com.rapidminer.operator.clustering.HierarchicalClusterNode;
import com.rapidminer.operator.learner.CapabilityProvider;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.CapabilityPrecondition;
import com.rapidminer.operator.ports.metadata.DistanceMeasurePrecondition;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeStringCategory;
import com.rapidminer.tools.ObjectVisualizerService;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.math.similarity.DistanceMeasure;
import com.rapidminer.tools.math.similarity.DistanceMeasureHelper;
import com.rapidminer.tools.math.similarity.DistanceMeasures;


/**
 * This operator implements agglomerative clustering, providing the three different strategies
 * SingleLink, CompleteLink and AverageLink. The last is also called UPGMA. The result will be a
 * hierarchical cluster model, providing distance information to plot as a dendogram.
 *
 * @author Sebastian Land
 */
public class AgglomerativeClustering extends Operator implements CapabilityProvider {

	public static final String PARAMETER_MODE = "mode";

	public static final String[] modes = new String[] { "SingleLink", "CompleteLink", "AverageLink" };

	private static final double INTERMEDIATE_PROGRESS = 60;

	private static final int OPERATOR_PROGRESS_STEPS = 10;

	private InputPort exampleSetInput = getInputPorts().createPort("example set", new ExampleSetMetaData());
	private OutputPort modelOutput = getOutputPorts().createPort("cluster model");
	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
	private DistanceMeasureHelper measureHelper = new DistanceMeasureHelper(this);

	public AgglomerativeClustering(OperatorDescription description) {
		super(description);

		exampleSetInput.addPrecondition(new DistanceMeasurePrecondition(exampleSetInput, this));
		exampleSetInput.addPrecondition(new CapabilityPrecondition(this, exampleSetInput));

		getTransformer().addRule(new GenerateNewMDRule(modelOutput, new MetaData(HierarchicalClusterModel.class)));
		getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, exampleSetOutput, SetRelation.EQUAL) {

			@Override
			public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) {
				metaData.addAttribute(new AttributeMetaData(Attributes.ID_NAME, Ontology.INTEGER, Attributes.ID_NAME));
				return metaData;
			}
		});
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
		DistanceMeasure measure = measureHelper.getInitializedMeasure(exampleSet);

		// additional checks
		Tools.isNonEmpty(exampleSet);
		try {
			Tools.hasRegularAttributes(exampleSet);
		} catch (UserError ue) {
			if (exampleSet.size() > 1 || getCompatibilityLevel().isAbove(AbstractClusterer.BEFORE_EMPTY_CHECKS)) {
				throw ue;
			}
			logWarning(ue.getMessage());
		}
		Tools.onlyFiniteValues(exampleSet, getOperatorClassName(), this, new String[0]);
		Tools.checkAndCreateIds(exampleSet);

		// initialize operator progress
		getProgress().setTotal(100);

		Attribute idAttribute = exampleSet.getAttributes().getId();
		boolean idAttributeIsNominal = idAttribute.isNominal();
		DistanceMatrix matrix = new DistanceMatrix(exampleSet.size());
		Map<Integer, HierarchicalClusterNode> clusterMap = new HashMap<Integer, HierarchicalClusterNode>(exampleSet.size());
		int[] clusterIds = new int[exampleSet.size()];
		// filling the distance matrix
		int nextClusterId = 0;
		for (Example example1 : exampleSet) {
			checkForStop();
			clusterIds[nextClusterId] = nextClusterId;
			int y = 0;
			for (Example example2 : exampleSet) {
				if (y > nextClusterId) {
					matrix.set(nextClusterId, y, measure.calculateDistance(example1, example2));
				}
				y++;
			}
			if (idAttributeIsNominal) {
				clusterMap.put(nextClusterId,
						new HierarchicalClusterLeafNode(nextClusterId, example1.getValueAsString(idAttribute)));
			} else {
				clusterMap.put(nextClusterId,
						new HierarchicalClusterLeafNode(nextClusterId, example1.getValue(idAttribute)));
			}
			nextClusterId++;
			if (nextClusterId % OPERATOR_PROGRESS_STEPS == 0) {
				getProgress().setCompleted((int) (INTERMEDIATE_PROGRESS * nextClusterId / exampleSet.size()));
			}
		}

		// creating linkage method
		AbstractLinkageMethod linkage = new SingleLinkageMethod(matrix, clusterIds);
		if (getParameterAsString(PARAMETER_MODE).equals(modes[1])) {
			linkage = new CompleteLinkageMethod(matrix, clusterIds);
		} else if (getParameterAsString(PARAMETER_MODE).equals(modes[2])) {
			linkage = new AverageLinkageMethod(matrix, clusterIds);
		}

		// now building agglomerative tree bottom up
		int clusterMapStartSize = clusterMap.size();
		while (clusterMap.size() > 1) {
			Agglomeration agglomeration = linkage.getNextAgglomeration(nextClusterId, clusterMap);
			HierarchicalClusterNode newNode = new HierarchicalClusterNode(nextClusterId, agglomeration.getDistance());
			newNode.addSubNode(clusterMap.get(agglomeration.getClusterId1()));
			newNode.addSubNode(clusterMap.get(agglomeration.getClusterId2()));
			clusterMap.remove(agglomeration.getClusterId1());
			clusterMap.remove(agglomeration.getClusterId2());
			clusterMap.put(nextClusterId, newNode);
			nextClusterId++;
			if (nextClusterId % OPERATOR_PROGRESS_STEPS == 0) {
				getProgress().setCompleted((int) (INTERMEDIATE_PROGRESS + (100.0 - INTERMEDIATE_PROGRESS)
						* (clusterMapStartSize - clusterMap.size()) / clusterMapStartSize));
			}
		}

		// creating model
		HierarchicalClusterModel model = new DendogramHierarchicalClusterModel(
				clusterMap.entrySet().iterator().next().getValue());

		// registering visualizer
		ObjectVisualizerService.addObjectVisualizer(model, new ExampleVisualizer((ExampleSet) exampleSet.clone()));

		modelOutput.deliver(model);
		exampleSetOutput.deliver(exampleSet);
	}

	@Override
	public boolean shouldAutoConnect(OutputPort port) {
		if (port == exampleSetOutput) {
			return getParameterAsBoolean("keep_example_set");
		} else {
			return super.shouldAutoConnect(port);
		}
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		switch (capability) {
			case BINOMINAL_ATTRIBUTES:
			case POLYNOMINAL_ATTRIBUTES:
			case NUMERICAL_ATTRIBUTES:
			case NO_LABEL:
			case NUMERICAL_LABEL:
			case BINOMINAL_LABEL:
			case ONE_CLASS_LABEL:
			case POLYNOMINAL_LABEL:
				return true;
			case WEIGHTED_EXAMPLES:
			case MISSING_VALUES:
			case FORMULA_PROVIDER:
			case UPDATABLE:
			default:
				return false;

		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterTypeStringCategory type = new ParameterTypeStringCategory(PARAMETER_MODE, "Specifies the cluster mode.",
				modes, modes[0], false);
		type.setExpert(false);
		types.add(type);

		types.addAll(DistanceMeasures.getParameterTypes(this));
		return types;
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		OperatorVersion[] old = super.getIncompatibleVersionChanges();
		OperatorVersion[] versions = Arrays.copyOf(old, old.length + 1);
		versions[old.length] = AbstractClusterer.BEFORE_EMPTY_CHECKS;
		return versions;
	}
}
