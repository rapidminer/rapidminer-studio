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
import java.util.Collection;
import java.util.LinkedList;

import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.gui.ExampleVisualizer;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.UserError;
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


/**
 * Abstract superclass of clusterers which defines the I/O behavior.
 *
 * @author Simon Fischer
 */
public abstract class AbstractClusterer extends Operator {

	private InputPort exampleSetInput = getInputPorts().createPort("example set");
	private OutputPort modelOutput = getOutputPorts().createPort("cluster model");
	private OutputPort exampleSetOutput = getOutputPorts().createPort("clustered set");

	protected static final OperatorVersion BEFORE_EMPTY_CHECKS = new OperatorVersion(7, 5, 3);

	public AbstractClusterer(OperatorDescription description) {
		super(description);
		exampleSetInput.addPrecondition(new SimplePrecondition(exampleSetInput, new ExampleSetMetaData()));
		getTransformer().addRule(new GenerateNewMDRule(modelOutput, new MetaData(getClusterModelClass())));
		getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, exampleSetOutput, SetRelation.EQUAL) {

			@Override
			public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) {
				if (addsClusterAttribute()) {
					String targetName = addsLabelAttribute() ? Attributes.LABEL_NAME : Attributes.CLUSTER_NAME;
					metaData.addAttribute(new AttributeMetaData(targetName, Ontology.NOMINAL, targetName));
					if (addsIdAttribute()) {
						MetaDataTools.checkAndCreateIds(metaData);
					}
					metaData.addAllAttributes(getAdditionalAttributes());
				}
				return metaData;
			}
		});
	}

	/**
	 * Generates a cluster model from an example set. Called by {@link #apply()}. Checks for
	 * additional preconditions to allow easy programmatic access.
	 *
	 * @see #additionalChecks(ExampleSet)
	 */
	public ClusterModel generateClusterModel(ExampleSet exampleSet) throws OperatorException {
		additionalChecks(exampleSet);
		return generateInternalClusterModel(exampleSet);
	}

	/**
	 * Performs additional checks on the given {@link ExampleSet} before
	 * {@link #generateInternalClusterModel(ExampleSet)}. By default will check for non-emptiness of
	 * the example set, i.e. there are at least one example and one regular attribute. Will log a
	 * warning or throw a {@link UserError}, depending on the compatibility level.
	 *
	 * @param exampleSet
	 *            the example set to check
	 * @throws UserError
	 *             if a check fails
	 * @since 7.6
	 * @see Tools#isNonEmpty(ExampleSet)
	 * @see #checksForExamples()
	 * @see Tools#hasRegularAttributes(ExampleSet)
	 * @see #checksForRegularAttributes()
	 */
	protected void additionalChecks(ExampleSet exampleSet) throws OperatorException {
		try {
			Tools.isNonEmpty(exampleSet);
		} catch (UserError ue) {
			if (checksForExamples()) {
				throw ue;
			}
			logWarning(ue.getMessage());
		}
		try {
			Tools.hasRegularAttributes(exampleSet);
		} catch (UserError ue) {
			if (checksForRegularAttributes()) {
				throw ue;
			}
			logWarning(ue.getMessage());
		}
	}

	/**
	 * Indicates whether this clusterer checks the example set for no examples. Returns {@code true}
	 * by default.
	 *
	 * @since 7.6
	 * @see #additionalChecks(ExampleSet)
	 */
	protected boolean checksForExamples() {
		return true;
	}

	/**
	 * Indicates whether this clusterer checks the example set for no regular attributes. Returns
	 * {@code true} by default.
	 *
	 * @since 7.6
	 * @see #additionalChecks(ExampleSet)
	 */
	protected boolean checksForRegularAttributes() {
		return true;
	}

	/**
	 * Indicates whether the implementation was affected by the error handling fix connected to
	 * {@link #BEFORE_EMPTY_CHECKS}.
	 *
	 * @since 7.6
	 */
	protected boolean affectedByEmptyCheck() {
		return false;
	}

	/**
	 * Generates a cluster model from an example set. Called by
	 * {@link #generateClusterModel(ExampleSet)}. Protected to prevent unchecked access (empty
	 * example set) and allow generic checks. Subclasses should override this instead of
	 * {@link #generateClusterModel(ExampleSet)}.
	 *
	 * @since 7.6
	 */
	protected abstract ClusterModel generateInternalClusterModel(ExampleSet exampleSet) throws OperatorException;

	/** Indicates whether {@link #doWork()} will add a cluster attribute to the example set. */
	protected abstract boolean addsClusterAttribute();

	/**
	 * Indicates whether the cluster attribute is set as cluster or label role. Returns
	 * {@code false} by default.
	 *
	 * @since 7.6
	 */
	protected boolean addsLabelAttribute() {
		return false;
	}

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
		ExampleSet output = input;
		if (addsClusterAttribute()) {
			output = (ExampleSet) input.clone();
		}
		ClusterModel clusterModel = generateClusterModel(output);

		// registering visualizer
		ObjectVisualizerService.addObjectVisualizer(clusterModel, new ExampleVisualizer((ExampleSet) output.clone()));

		modelOutput.deliver(clusterModel);
		exampleSetOutput.deliver(output); // generateClusterModel() may have added cluster attribute
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

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		OperatorVersion[] old = super.getIncompatibleVersionChanges();
		if (!affectedByEmptyCheck()) {
			return old;
		}
		OperatorVersion[] versions = Arrays.copyOf(old, old.length + 1);
		versions[old.length] = BEFORE_EMPTY_CHECKS;
		return versions;
	}
}
