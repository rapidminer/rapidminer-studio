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
package com.rapidminer.operator.learner.meta;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.ports.metadata.SubprocessTransformRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.math.container.Range;

import java.util.Iterator;
import java.util.List;


/**
 * This class uses n+1 inner learners and generates n different models by using the last n learners.
 * The predictions of these n models are taken to create n new features for the example set, which
 * is finally used to serve as an input of the first inner learner.
 * 
 * @author Ingo Mierswa, Helge Homburg
 */
public class Stacking extends AbstractStacking {

	private static final int MD_MAX_CONFIDENCE_ATTRIBUTES = 100;

	private final OutputPort stackingExamplesInnerSource = getSubprocess(1).getInnerSources()
			.createPort("stacking examples");
	private final InputPort stackingModelInnerSink = getSubprocess(1).getInnerSinks().createPort("stacking model",
			PredictionModel.class);

	public static final String PARAMETER_KEEP_ALL_ATTRIBUTES = "keep_all_attributes";

	public static final String PARAMETER_KEEP_CONFIDENCES = "keep_confidences";

	public Stacking(OperatorDescription description) {
		super(description, "Base Learner", "Stacking Model Learner");
		getTransformer().addRule(() -> {
			MetaData metaData = exampleSetInput.getMetaData();
			if (metaData != null) {
				MetaData clonedMetaData = metaData.clone();
				if (clonedMetaData instanceof ExampleSetMetaData) {
					ExampleSetMetaData exampleSetMetaData = (ExampleSetMetaData) clonedMetaData;
					if (!keepOldAttributes()) {
						exampleSetMetaData.clearRegular();
					}
					// constructing new meta attributes
					List<MetaData> metaDatas = baseModelExtender.getMetaData(true);
					int numberOfModels = 0;
					for (MetaData md : metaDatas) {
						if (PredictionModel.class.isAssignableFrom(md.getObjectClass())) {
							numberOfModels++;
						}
					}
					// adding stacking attributes
					AttributeMetaData label = exampleSetMetaData.getLabelMetaData();
					if (label != null) {
						for (int i = 0; i < numberOfModels; i++) {
							AttributeMetaData newRegular = label.copy();
							newRegular.setRegular();
							newRegular.setName("base_prediction" + i);
							exampleSetMetaData.addAttribute(newRegular);
							if (label.isNominal() && keepConfidences()) {
								AttributeMetaData confidence = new AttributeMetaData("base_confidence" + i, Ontology.REAL);
								confidence.setValueRange(new Range(0, 1), SetRelation.SUBSET);
								Iterator<String> values = label.getValueSet().iterator();
								int valuesAdded  = 0;
								while (values.hasNext() && valuesAdded < MD_MAX_CONFIDENCE_ATTRIBUTES) {
									newRegular = confidence.copy();
									newRegular.setName("base_confidence_" + values.next() + i);
									exampleSetMetaData.addAttribute(newRegular);
									valuesAdded++;
								}
							}
						}
					}
				}
				stackingExamplesInnerSource.deliverMD(clonedMetaData);
			} else {
				stackingExamplesInnerSource.deliverMD(null);
			}
		});
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(1)));
	}

	@Override
	public String getModelName() {
		return "Stacking Model";
	}

	@Override
	public boolean keepOldAttributes() {
		return getParameterAsBoolean(PARAMETER_KEEP_ALL_ATTRIBUTES);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeBoolean(PARAMETER_KEEP_ALL_ATTRIBUTES,
				"Use all attributes of the input example set for training the stacked model.", true));
		types.add(new ParameterTypeBoolean(PARAMETER_KEEP_CONFIDENCES,
				"Use confidences generated by the base learners for training the stacked model.", false));
		return types;
	}

	@Override
	protected ExecutionUnit getBaseModelLearnerProcess() {
		return getSubprocess(0);
	}

	@Override
	protected Model getStackingModel(ExampleSet stackingLearningSet) throws OperatorException {
		stackingExamplesInnerSource.deliver(stackingLearningSet);
		getSubprocess(1).execute();
		return stackingModelInnerSink.getData(Model.class);
	}

	@Override
	protected boolean keepConfidences() {
		return getParameterAsBoolean(PARAMETER_KEEP_CONFIDENCES);
	}

}
