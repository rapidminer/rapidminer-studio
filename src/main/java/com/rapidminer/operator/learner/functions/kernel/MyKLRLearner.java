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
package com.rapidminer.operator.learner.functions.kernel;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.examples.SVMExamples;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.kernel.Kernel;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.svm.SVMInterface;
import com.rapidminer.operator.learner.functions.kernel.logistic.KLR;
import com.rapidminer.parameter.ParameterType;

import java.util.Iterator;
import java.util.List;


/**
 * This is the Java implementation of <em>myKLR</em> by Stefan R&uuml;ping. myKLR is a tool for
 * large scale kernel logistic regression based on the algorithm of Keerthi/etal/2003 and the code
 * of mySVM.
 * 
 * @rapidminer.index KLR
 * @author Ingo Mierswa
 */
public class MyKLRLearner extends AbstractMySVMLearner {

	public MyKLRLearner(OperatorDescription description) {
		super(description);
	}

	@Override
	public boolean supportsCapability(OperatorCapability lc) {
		if (lc == OperatorCapability.NUMERICAL_ATTRIBUTES) {
			return true;
		}
		if (lc == OperatorCapability.BINOMINAL_LABEL) {
			return true;
		}
		return false;
	}

	@Override
	public AbstractMySVMModel createSVMModel(ExampleSet exampleSet, SVMExamples sVMExamples, Kernel kernel, int kernelType) {
		return new MyKLRModel(exampleSet, sVMExamples, kernel, kernelType);
	}

	@Override
	public SVMInterface createSVM(Attribute label, Kernel kernel, SVMExamples sVMExamples,
			com.rapidminer.example.ExampleSet rapidMinerExamples) throws OperatorException {
		Tools.hasNominalLabels(rapidMinerExamples, "MyKLR");
		return new KLR(this);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		// important: myKLR does not support determinition of the value C!
		Iterator<ParameterType> p = types.iterator();
		while (p.hasNext()) {
			ParameterType type = p.next();
			if (type.getKey().equals(PARAMETER_C)) {
				type.setDefaultValue(Double.valueOf(1.0d));
			}
		}

		return types;
	}
}
