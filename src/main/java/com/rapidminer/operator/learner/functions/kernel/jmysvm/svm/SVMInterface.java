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
package com.rapidminer.operator.learner.functions.kernel.jmysvm.svm;

import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.examples.SVMExample;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.examples.SVMExamples;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.kernel.Kernel;


/**
 * The interface of all SVMs.
 *
 * @author Stefan Rueping, Ingo Mierswa
 */
public interface SVMInterface {

	/** Initializes this SVM. */
	public void init(Kernel kernel, SVMExamples examples);

	/** Train this SVM. */
	public void train() throws OperatorException;

	/** Perform a prediction of label for all examples. */
	public void predict(SVMExamples examples);

	/** Perform a prediction of label for all examples. */
	public double predict(SVMExample sVMExample);

	/** Returns the weights of all features. */
	public double[] getWeights();

	/** Returns the value of b. */
	public double getB();
}
