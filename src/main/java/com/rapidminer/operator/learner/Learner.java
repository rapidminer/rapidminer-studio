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
package com.rapidminer.operator.learner;

import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.performance.PerformanceVector;


/**
 * A <tt>Learner</tt> is an operator that encapsulates the learning step of a machine learning
 * method. Some Learners may be capable of estimating the performance of the generated model. In
 * that case, they additionally return a
 * {@link com.rapidminer.operator.performance.PerformanceVector}. Furthermore some learner can
 * calculate weights of the used attributes which can also be delivered.
 * 
 * @author Ingo Mierswa
 */
public interface Learner extends CapabilityProvider {

	/**
	 * Trains a model. This method should be called by apply() and is implemented by subclasses.
	 */
	public Model learn(ExampleSet exampleSet) throws OperatorException;

	/** Returns the name of the learner. */
	public String getName();

	/**
	 * Most learners will return false since they are not able to estimate the learning performance.
	 * However, if a learning scheme is able to calculate the performance (e.g. Xi-Alpha estimation
	 * of a SVM) it should return true.
	 */
	public boolean shouldEstimatePerformance();

	/**
	 * Most learners should throw an exception if they are not able to estimate the learning
	 * performance. However, if a learning scheme is able to calculate the performance (e.g.
	 * Xi-Alpha estimation of a SVM) it should return a performance vector containing the estimated
	 * performance.
	 */
	public PerformanceVector getEstimatedPerformance() throws OperatorException;

	/**
	 * Most learners will return false since they are not able to calculate attribute weights.
	 * However, if a learning scheme is able to calculate weights (e.g. the normal vector of a SVM)
	 * it should return true.
	 */
	public boolean shouldCalculateWeights();

	/**
	 * Most learners should throw an exception if they are not able to calculate attribute weights.
	 * However, if a learning scheme is able to calculate weights (e.g. the normal vector of a SVM)
	 * it should return an AttributeWeights object.
	 */
	public AttributeWeights getWeights(ExampleSet eSet) throws OperatorException;
}
