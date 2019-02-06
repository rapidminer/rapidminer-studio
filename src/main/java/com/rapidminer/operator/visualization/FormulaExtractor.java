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
package com.rapidminer.operator.visualization;

import com.rapidminer.operator.Model;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ResultObjectAdapter;
import com.rapidminer.operator.learner.FormulaProvider;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;


/**
 * This operator extracts a prediction calculation formula from the given model and stores the
 * formula in a formula result object which can then be written to a file, e.g. with the
 * ResultWriter operator.
 * 
 * Please note that not all RapidMiner models provide a formula.
 * 
 * @author Ingo Mierswa
 */
public class FormulaExtractor extends Operator {

	private static class FormulaResult extends ResultObjectAdapter {

		private static final long serialVersionUID = 4106026192882970425L;

		private String formula;

		public FormulaResult(String formula) {
			this.formula = formula;
		}

		@Override
		public String getName() {
			return "Formula";
		}

		@Override
		public String toString() {
			return formula;
		}
	}

	private InputPort modelInput = getInputPorts().createPort("model", Model.class);
	private OutputPort formulaOutput = getOutputPorts().createPort("formula");
	private OutputPort modelOutput = getOutputPorts().createPort("model");

	public FormulaExtractor(OperatorDescription description) {
		super(description);
		getTransformer().addPassThroughRule(modelInput, modelOutput);
		getTransformer().addRule(new GenerateNewMDRule(formulaOutput, FormulaResult.class));
	}

	@Override
	public void doWork() throws OperatorException {
		Model model = modelInput.getData(Model.class);

		if (model instanceof FormulaProvider) {
			formulaOutput.deliver(new FormulaResult(((FormulaProvider) model).getFormula()));
		} else {
			logWarning("The model is not capable of producing formulas, formula will be empty...");
			formulaOutput.deliver(new FormulaResult("Model is not capable of producing formulas."));
		}
		modelOutput.deliver(model);
	}
}
