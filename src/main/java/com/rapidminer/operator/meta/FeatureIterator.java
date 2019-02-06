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
package com.rapidminer.operator.meta;

import java.util.List;
import java.util.Set;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ValueDouble;
import com.rapidminer.operator.ValueString;
import com.rapidminer.operator.ports.CollectingPortPairExtender;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.PassThroughRule;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.operator.ports.metadata.SubprocessTransformRule;
import com.rapidminer.operator.tools.AttributeSubsetSelector;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeString;


/**
 * <p>
 * This operator takes an input data set and applies its inner operators as often as the number of
 * features of the input data is. Inner operators can access the current feature name by a macro,
 * whose name can be specified via the parameter <code>iteration_macro</code>.
 * </p>
 *
 * <p>
 * The user can specify with a parameter if this loop should iterate over all features or only over
 * features with a specific value type, i.e. only over numerical or over nominal features. A regular
 * expression can also be specified which is used as a filter, i.e. the inner operators are only
 * applied for feature names matching the filter expression.
 * </p>
 *
 * @author Ingo Mierswa, Tobias Malbrecht
 *
 * @deprecated since 7.4 replaced by the LoopAttributesOperator in the Concurrency extension
 */

@Deprecated
public class FeatureIterator extends OperatorChain {

	private final InputPort exampleSetInput = getInputPorts().createPort("example set", ExampleSet.class);
	private final OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
	private final OutputPort exampleSetInnerSource = getSubprocess(0).getInnerSources().createPort("example set");
	private final InputPort exampleSetInnerSink = getSubprocess(0).getInnerSinks().createPort("example set");

	public static final String PARAMETER_ITERATION_MACRO = "iteration_macro";

	public static final String DEFAULT_ITERATION_MACRO_NAME = "loop_attribute";

	private int iteration;

	private String currentName = null;

	private final AttributeSubsetSelector attributeSelector = new AttributeSubsetSelector(this, exampleSetInput);

	private final CollectingPortPairExtender innerSinkExtender;

	public FeatureIterator(OperatorDescription description) {
		super(description, "Subprocess");

		exampleSetInnerSink.addPrecondition(new SimplePrecondition(exampleSetInnerSink, new ExampleSetMetaData(), false));
		innerSinkExtender = new CollectingPortPairExtender("result", getSubprocess(0).getInnerSinks(), getOutputPorts());
		innerSinkExtender.start();

		getTransformer().addRule(new PassThroughRule(exampleSetInput, exampleSetInnerSource, false));
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
		getTransformer().addRule(innerSinkExtender.makePassThroughRule());
		getTransformer().addRule(new PassThroughRule(exampleSetInput, exampleSetOutput, false) {

			@Override
			public MetaData modifyMetaData(MetaData unmodifiedMetaData) {
				if (exampleSetInnerSink.isConnected()) {
					return exampleSetInnerSink.getMetaData();
				} else {
					// due to side effects, we cannot make any guarantee about the output.
					return new ExampleSetMetaData();
				}
			}
		});

		addValue(new ValueDouble("iteration", "The number of the current iteration / loop.") {

			@Override
			public double getDoubleValue() {
				return iteration;
			}
		});

		addValue(new ValueString("feature_name", "The number of the current feature.") {

			@Override
			public String getStringValue() {
				return currentName;
			}
		});
	}

	@Override
	public void doWork() throws OperatorException {
		innerSinkExtender.reset();
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
		String iterationMacroName = getParameterAsString(PARAMETER_ITERATION_MACRO);
		Set<Attribute> selectedAttributes = attributeSelector.getAttributeSubset(exampleSet, false);

		// init ProgressListener
		getProgress().setTotal(selectedAttributes.size());

		// filter and loop
		iteration = 0;
		for (Attribute attribute : selectedAttributes) {
			String name = attribute.getName();
			getProcess().getMacroHandler().addMacro(iterationMacroName, name);
			currentName = name;
			applyInnerOperators(exampleSet);
			innerSinkExtender.collect();
			getProgress().setCompleted(++iteration);
		}
		getProcess().getMacroHandler().removeMacro(iterationMacroName);

		if (exampleSetInnerSink.isConnected()) {
			exampleSetOutput.deliver(exampleSetInnerSink.getData(IOObject.class));
		} else {
			exampleSetOutput.deliver(exampleSet);
		}
	}

	private void applyInnerOperators(ExampleSet inputExampleSet) throws OperatorException {
		ExampleSet iterationSet;
		// if inner sink is connected, use its data as an input to iteration 2...n
		// otherwise, always feed a clone into the subprocess.
		if (exampleSetInnerSink.isConnected()) {
			if (iteration == 0) {
				iterationSet = inputExampleSet;
			} else {
				iterationSet = exampleSetInnerSink.getData(ExampleSet.class);
			}
		} else {
			iterationSet = (ExampleSet) inputExampleSet.clone();
		}
		exampleSetInnerSource.deliver(iterationSet);
		getSubprocess(0).execute();
	}

	@Override
	public boolean shouldAutoConnect(InputPort inputPort) {
		if (inputPort == exampleSetInnerSink) {
			return true;
		} else {
			return super.shouldAutoConnect(inputPort);
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.addAll(attributeSelector.getParameterTypes());
		types.add(new ParameterTypeString(PARAMETER_ITERATION_MACRO,
				"The name of the macro which holds the name of the current feature in each iteration.",
				DEFAULT_ITERATION_MACRO_NAME, false));
		return types;
	}
}
