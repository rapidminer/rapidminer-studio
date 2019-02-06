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
package com.rapidminer.operator.validation.clustering;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ValueDouble;
import com.rapidminer.operator.clustering.ClusterModel;
import com.rapidminer.operator.performance.EstimatedPerformance;
import com.rapidminer.operator.performance.PerformanceCriterion;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.PassThroughOrGenerateRule;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;


/**
 * This operator does actually not compute a performance criterion but simply provides the number of
 * clusters as a value.
 * 
 * @author Cedric Copy, Timm Euler, Ingo Mierswa, Michael Wurst
 * 
 */
public class ClusterNumberEvaluator extends Operator {

	private int numberOfClusters;

	private InputPort clusterModelInput = getInputPorts().createPort("cluster model", ClusterModel.class);
	private OutputPort clusterModelOutput = getOutputPorts().createPort("cluster model");
	private InputPort performanceInput = getInputPorts().createPort("performance");
	private OutputPort performanceOutput = getOutputPorts().createPort("performance");

	/**
	 * Constructor for ClusterNumberEvaluator.
	 */
	public ClusterNumberEvaluator(OperatorDescription description) {
		super(description);

		performanceInput.addPrecondition(new SimplePrecondition(performanceInput, new MetaData(PerformanceVector.class),
				false));
		getTransformer().addRule(
				new PassThroughOrGenerateRule(performanceInput, performanceOutput, new MetaData(PerformanceVector.class)));
		getTransformer().addPassThroughRule(clusterModelInput, clusterModelOutput);

		addValue(new ValueDouble("clusternumber", "The number of clusters.", false) {

			@Override
			public double getDoubleValue() {
				return numberOfClusters;
			}
		});
	}

	@Override
	public boolean shouldAutoConnect(OutputPort port) {
		if (port == clusterModelOutput) {
			return getParameterAsBoolean("keep_cluster_model");
		} else {
			return super.shouldAutoConnect(port);
		}
	}

	@Override
	public void doWork() throws OperatorException {
		ClusterModel model = clusterModelInput.getData(ClusterModel.class);

		this.numberOfClusters = model.getNumberOfClusters();

		int numItems = 0;
		for (int i = 0; i < model.getNumberOfClusters(); i++) {
			numItems += model.getCluster(i).getNumberOfExamples();
		}

		PerformanceVector performance = performanceInput.getDataOrNull(PerformanceVector.class);

		if (performance == null) {
			performance = new PerformanceVector();
		}

		PerformanceCriterion pc = new EstimatedPerformance("Number of clusters", model.getNumberOfClusters(), 1, true);
		performance.addCriterion(pc);
		pc = new EstimatedPerformance("Cluster Number Index",
				1.0 - (((double) model.getNumberOfClusters()) / ((double) numItems)), numItems, false);
		performance.addCriterion(pc);

		clusterModelOutput.deliver(model);
		performanceOutput.deliver(performance);
	}
}
