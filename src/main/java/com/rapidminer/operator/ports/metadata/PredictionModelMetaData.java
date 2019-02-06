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
package com.rapidminer.operator.ports.metadata;

import com.rapidminer.example.Attributes;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.math.container.Range;

import java.util.LinkedList;
import java.util.List;


/**
 * 
 * @author Simon Fischer
 * 
 */
public class PredictionModelMetaData extends ModelMetaData {

	private static final long serialVersionUID = 1L;

	private AttributeMetaData predictedLabelMetaData;

	private List<AttributeMetaData> generatedPredictionAttributes = new LinkedList<AttributeMetaData>();

	/** Clone constructor */
	protected PredictionModelMetaData() {}

	public PredictionModelMetaData(Class<? extends PredictionModel> modelClass) {
		this(modelClass, null);
	}

	public PredictionModelMetaData(Class<? extends PredictionModel> modelClass, ExampleSetMetaData trainingSetMetaData) {
		super(modelClass, trainingSetMetaData);
		if (trainingSetMetaData != null) {
			AttributeMetaData labelAttributeMetaData = trainingSetMetaData.getLabelMetaData();
			if (labelAttributeMetaData != null) {
				this.predictedLabelMetaData = labelAttributeMetaData.copy();
				this.predictedLabelMetaData.setRole(Attributes.PREDICTION_NAME);
				this.predictedLabelMetaData.setName("prediction(" + predictedLabelMetaData.getName() + ")");
				if (predictedLabelMetaData.isNumerical()) {
					this.predictedLabelMetaData.setValueSetRelation(SetRelation.SUPERSET);
				}
				this.predictedLabelMetaData.setMean(new MDReal());

				// creating confidence attributes
				generatedPredictionAttributes.add(predictedLabelMetaData);
				if (predictedLabelMetaData.isNominal()) {
					if (predictedLabelMetaData.getValueSet().isEmpty()) {
						AttributeMetaData confidence = new AttributeMetaData(Attributes.CONFIDENCE_NAME + "(?)",
								Ontology.REAL, Attributes.CONFIDENCE_NAME + "_" + "?");
						confidence.setValueRange(new Range(0, 1), SetRelation.SUBSET);
						generatedPredictionAttributes.add(confidence);
						predictedLabelMetaData.setValueSetRelation(SetRelation.SUPERSET);
					} else {
						for (String value : predictedLabelMetaData.getValueSet()) {
							AttributeMetaData confidence = new AttributeMetaData(Attributes.CONFIDENCE_NAME + "(" + value
									+ ")", Ontology.REAL, Attributes.CONFIDENCE_NAME + "_" + value);
							confidence.setValueRange(new Range(0, 1), SetRelation.SUBSET);
							generatedPredictionAttributes.add(confidence);
						}
					}
				}
			}
		}
	}

	@Override
	public ExampleSetMetaData applyEffects(ExampleSetMetaData emd, InputPort inputPort) {
		if (predictedLabelMetaData == null) {
			return emd;
		}
		List<AttributeMetaData> predictionAttributes = getPredictionAttributeMetaData();
		if (predictionAttributes != null) {
			emd.addAllAttributes(predictionAttributes);
			emd.mergeSetRelation(getPredictionAttributeSetRelation());
		}
		return emd;
	}

	public List<AttributeMetaData> getPredictionAttributeMetaData() {
		return generatedPredictionAttributes;
	}

	public AttributeMetaData getPredictedLabelMetaData() {
		return predictedLabelMetaData;
	}

	public SetRelation getPredictionAttributeSetRelation() {
		if (predictedLabelMetaData != null) {
			return predictedLabelMetaData.getValueSetRelation();
		} else {
			return SetRelation.UNKNOWN;
		}
	}

	@Override
	public String getDescription() {
		return super.getDescription() + "; generates: " + predictedLabelMetaData;
	}

	@Override
	public PredictionModelMetaData clone() {
		PredictionModelMetaData clone = (PredictionModelMetaData) super.clone();
		if (this.predictedLabelMetaData != null) {
			clone.predictedLabelMetaData = this.predictedLabelMetaData.clone();
		}
		if (this.generatedPredictionAttributes != null) {
			clone.generatedPredictionAttributes = new LinkedList<>();
			for (AttributeMetaData amd : this.generatedPredictionAttributes) {
				clone.generatedPredictionAttributes.add(amd.clone());
			}
		}
		return clone;
	}

}
