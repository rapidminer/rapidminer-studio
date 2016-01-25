/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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

import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.ExampleVisualizer;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.clustering.ClusterModel;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.tools.ObjectVisualizerService;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.metadata.MetaDataTools;

import java.util.Collection;
import java.util.LinkedList;


/**
 * Abstract superclass of clusterers which defines the I/O behavior.
 * 
 * @author Simon Fischer
 */
public abstract class AbstractClusterer extends Operator {

	private InputPort exampleSetInput = getInputPorts().createPort("example set");
	private OutputPort modelOutput = getOutputPorts().createPort("cluster model");
	private OutputPort exampleSetOutput = getOutputPorts().createPort("clustered set");

	public AbstractClusterer(OperatorDescription description) {
		super(description);
		exampleSetInput.addPrecondition(new SimplePrecondition(exampleSetInput, new ExampleSetMetaData()));
		getTransformer().addRule(new GenerateNewMDRule(modelOutput, new MetaData(getClusterModelClass())));
		getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, exampleSetOutput, SetRelation.EQUAL) {

			@Override
			public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) {
				if (addsClusterAttribute()) {
					if (addsClusterAttribute()) {
						metaData.addAttribute(new AttributeMetaData(Attributes.CLUSTER_NAME, Ontology.NOMINAL,
								Attributes.CLUSTER_NAME));
					}
					if (addsIdAttribute()) {
						MetaDataTools.checkAndCreateIds(metaData);
					}
					metaData.addAllAttributes(getAdditionalAttributes());
				}
				return metaData;
			}
		});
	}

	/** Generates a cluster model from an example set. Called by {@link #apply()}. */
	public abstract ClusterModel generateClusterModel(ExampleSet exampleSet) throws OperatorException;

	/** Indicates whether {@link #doWork()} will add a cluster attribute to the example set. */
	protected abstract boolean addsClusterAttribute();

	/** Indicates whether {@link #doWork()} will add an id attribute to the example set. */
	protected abstract boolean addsIdAttribute();

	/**
	 * Subclasses might override this method in order to add additional attributes to the
	 * metaDataSet
	 */
	protected Collection<AttributeMetaData> getAdditionalAttributes() {
		return new LinkedList<AttributeMetaData>();
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet input = exampleSetInput.getData(ExampleSet.class);
		ClusterModel clusterModel = generateClusterModel(input);

		// registering visualizer
		ObjectVisualizerService.addObjectVisualizer(clusterModel, new ExampleVisualizer((ExampleSet) input.clone()));

		modelOutput.deliver(clusterModel);
		exampleSetOutput.deliver(input); // generateClusterModel() may have added cluster attribute
	}

	/**
	 * Subclasses might overwrite this method in order to return the appropriate class of their
	 * model if postprocessing is needed.
	 */
	public Class<? extends ClusterModel> getClusterModelClass() {
		return ClusterModel.class;
	}

	@Override
	public boolean shouldAutoConnect(OutputPort outputPort) {
		if (outputPort == exampleSetOutput) {
			// TODO: Remove in later versions
			return addsClusterAttribute();
		} else {
			return super.shouldAutoConnect(outputPort);
		}
	}

	public InputPort getExampleSetInputPort() {
		return exampleSetInput;
	}
}
