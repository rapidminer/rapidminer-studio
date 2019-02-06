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
package com.rapidminer.operator.preprocessing;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.SimpleAttributes;
import com.rapidminer.example.Statistics;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.ViewAttribute;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorProgress;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.RandomGenerator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * @author Sebastian Land
 */
public class NoiseModel extends PreprocessingModel {

	private static final long serialVersionUID = -1953073746280248791L;

	private static final int OPERATOR_PROGRESS_STEPS = 100;

	// settings
	private double attributeNoise;
	private double labelNoise;
	private String[] noiseAttributeNames;
	private double noiseOffset;
	private double noiseFactor;

	// data needed during viewing
	private Attribute viewLabelParent;
	private Attribute viewLabel;
	private Set<Attribute> noiseAttributes = new HashSet<Attribute>();
	private RandomGenerator random;
	private Map<String, Double> noiseMap;
	private double labelRange;

	public NoiseModel(ExampleSet exampleSet, RandomGenerator localRandom, List<String[]> noises, double attributeNoise,
			double labelNoise, double noiseOffsett, double noiseFactor, String[] attributeNames) {
		super(exampleSet);
		this.attributeNoise = attributeNoise;
		this.labelNoise = labelNoise;
		this.noiseOffset = noiseOffsett;
		this.noiseFactor = noiseFactor;
		this.noiseAttributeNames = attributeNames;
		this.random = localRandom;

		// read noise values from list
		noiseMap = new HashMap<String, Double>();

		Iterator<String[]> i = noises.iterator();
		while (i.hasNext()) {
			String[] pair = i.next();
			noiseMap.put(pair[0], Double.valueOf(pair[1]));
		}

		Attribute label = exampleSet.getAttributes().getLabel();
		if (label != null) {
			exampleSet.recalculateAttributeStatistics(label);
			double min = exampleSet.getStatistics(label, Statistics.MINIMUM);
			double max = exampleSet.getStatistics(label, Statistics.MAXIMUM);
			labelRange = Math.abs(max - min);
		}

	}

	@Override
	public ExampleSet applyOnData(ExampleSet exampleSet) throws OperatorException {

		// add noise to existing attributes
		Iterator<Example> reader = exampleSet.iterator();
		Attribute label = exampleSet.getAttributes().getLabel();
		OperatorProgress progress = null;
		if (getShowProgress() && getOperator() != null && getOperator().getProgress() != null) {
			progress = getOperator().getProgress();
			progress.setTotal(100);
		}
		int progressCounter = 0;
		Attribute[] regularAttributes = exampleSet.getAttributes().createRegularAttributeArray();
		while (reader.hasNext()) {
			Example example = reader.next();
			// attribute noise
			for (Attribute attribute : regularAttributes) {
				if (attribute.isNumerical()) {
					Double noiseObject = noiseMap.get(attribute.getName());
					double noise = noiseObject == null ? attributeNoise : noiseObject.doubleValue();
					double noiseValue = random.nextGaussian() * noise;
					example.setValue(attribute, example.getValue(attribute) + noiseValue);
				}
			}
			// label noise
			if (label != null) {
				if (label.isNumerical()) {
					double noiseValue = random.nextGaussian() * labelNoise * labelRange;
					example.setValue(label, example.getValue(label) + noiseValue);
				} else if (label.isNominal() && (label.getMapping().size() >= 2)) {
					if (random.nextDouble() < labelNoise) {
						int oldValue = (int) example.getValue(label);
						int newValue = random.nextInt(label.getMapping().size() - 1);
						if (newValue >= oldValue) {
							newValue++;
						}
						example.setValue(label, newValue);
					}
				}
			}

			if (progress != null && ++progressCounter % OPERATOR_PROGRESS_STEPS == 0) {
				progress.setCompleted((int) (40.0 * progressCounter / exampleSet.size()));
			}
		}

		// add new noise attributes
		List<Attribute> newAttributes = new LinkedList<Attribute>();
		progressCounter = 0;
		for (String name : noiseAttributeNames) {
			Attribute newAttribute = AttributeFactory.createAttribute(name, Ontology.REAL);
			newAttributes.add(newAttribute);
			exampleSet.getExampleTable().addAttribute(newAttribute);
			exampleSet.getAttributes().addRegular(newAttribute);
			if (progress != null) {
				progress.setCompleted((int) ((20.0 * ++progressCounter / noiseAttributeNames.length) + 40));
			}
		}

		progressCounter = 0;
		for (Example example : exampleSet) {
			for (Attribute attribute : newAttributes) {
				example.setValue(attribute, noiseOffset + noiseFactor * random.nextGaussian());
			}
			if (progress != null && ++progressCounter % OPERATOR_PROGRESS_STEPS == 0) {
				progress.setCompleted((int) ((40.0 * progressCounter / exampleSet.size()) + 60));
			}
		}
		return exampleSet;
	}

	@Override
	public Attributes getTargetAttributes(ExampleSet parentSet) {
		SimpleAttributes attributes = new SimpleAttributes();
		// add special attributes to new attributes
		Iterator<AttributeRole> specialRoles = parentSet.getAttributes().specialAttributes();
		while (specialRoles.hasNext()) {
			AttributeRole role = specialRoles.next();
			if (role.getSpecialName().equals(Attributes.LABEL_NAME) && labelNoise != 0d) {
				AttributeRole clonedRole = (AttributeRole) role.clone();
				viewLabelParent = role.getAttribute();
				viewLabel = new ViewAttribute(this, viewLabelParent, viewLabelParent.getName(),
						viewLabelParent.getValueType(), (viewLabelParent.isNominal()) ? viewLabelParent.getMapping() : null);
				clonedRole.setAttribute(viewLabel);
				attributes.add(clonedRole);
			} else {
				attributes.add(specialRoles.next());
			}
		}

		// add regular attributes
		Iterator<AttributeRole> i = parentSet.getAttributes().allAttributeRoles();
		while (i.hasNext()) {
			AttributeRole attributeRole = i.next();
			if (!attributeRole.isSpecial()) {
				Attribute attribute = attributeRole.getAttribute();
				if (attribute.isNumerical()) {
					attributes.addRegular(new ViewAttribute(this, attribute, attribute.getName(), Ontology.REAL, null));
				} else {
					attributes.add(attributeRole);
				}
			}
		}

		// add new noise attributes
		for (String name : noiseAttributeNames) {
			Attribute viewAttribute = new ViewAttribute(this, null, name, Ontology.REAL, null);
			attributes.addRegular(viewAttribute);
			noiseAttributes.add(viewAttribute);
		}
		return attributes;
	}

	@Override
	public double getValue(Attribute targetAttribute, double value) {
		if (targetAttribute == viewLabel) {
			// label noise
			if (viewLabel.isNumerical()) {
				double min = getTrainingHeader().getStatistics(viewLabelParent, Statistics.MINIMUM);
				double max = getTrainingHeader().getStatistics(viewLabelParent, Statistics.MAXIMUM);
				double labelRange = Math.abs(max - min);
				return value + random.nextGaussian() * labelNoise * labelRange;
			} else if (viewLabel.isNominal() && (viewLabel.getMapping().size() >= 2)) {
				if (random.nextDouble() < labelNoise) {
					int oldValue = (int) value;
					int newValue = oldValue;
					while (newValue == oldValue) {
						newValue = random.nextInt(viewLabel.getMapping().size());
					}
					return newValue;
				}
			}
		} else if (noiseAttributes.contains(targetAttribute)) {
			return noiseOffset + noiseFactor * random.nextGaussian();
		} else {
			// attributeNoise
			Double noiseObject = noiseMap.get(targetAttribute.getName());
			double noise = noiseObject == null ? attributeNoise : noiseObject.doubleValue();
			double noiseValue = random.nextGaussian() * noise;
			return value + noiseValue;
		}
		return 0;
	}

	@Override
	public boolean isSupportingAttributeRoles() {
		return true;
	}

	public double getAttributeNoise() {
		return attributeNoise;
	}

	public double getLabelNoise() {
		return labelNoise;
	}

	public double getNoiseOffset() {
		return noiseOffset;
	}

	public double getNoiseFactor() {
		return noiseFactor;
	}

	public double getLabelRange() {
		return labelRange;
	}

	public String[] getNoiseAttributeNames() {
		return noiseAttributeNames;
	}

	public Map<String, Double> getNoiseMap() {
		return noiseMap;
	}

	@Override
	protected boolean writesIntoExistingData() {
		return true;
	}

	@Override
	protected boolean needsRemapping() {
		return false;
	}

}
