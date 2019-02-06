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
package com.rapidminer.operator.clustering;

import java.util.HashMap;
import java.util.Vector;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.ExampleSetPrecondition;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.operator.ports.quickfix.AttributeToNominalQuickFixProvider;
import com.rapidminer.tools.Ontology;


/**
 * This operator estimates a mapping between a given clustering and a prediction. It adjusts the
 * given clusters with the given labels and so estimates the best fitting pairs.
 * 
 * @author Regina Fritsch
 */
public class ClusterToPrediction extends Operator {

	private final InputPort exampleSetInput = getInputPorts().createPort("example set");
	private final InputPort clusterModelInput = getInputPorts().createPort("cluster model", ClusterModel.class);
	private final OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
	private final OutputPort clusterModelOutput = getOutputPorts().createPort("cluster model");

	public ClusterToPrediction(OperatorDescription description) {
		super(description);

		exampleSetInput.addPrecondition(new ExampleSetPrecondition(exampleSetInput, -1, Attributes.LABEL_NAME,
				Attributes.CLUSTER_NAME));
		getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, exampleSetOutput, null) {

			@Override
			public MetaData modifyMetaData(MetaData metaData) {
				if (metaData instanceof ExampleSetMetaData) {
					ExampleSetMetaData emd = (ExampleSetMetaData) metaData;
					switch (emd.hasSpecial(Attributes.LABEL_NAME)) {
						case NO:
							exampleSetInput.addError(new SimpleMetaDataError(Severity.ERROR, exampleSetInput,
									"special_missing", "label"));
							return emd;
						case UNKNOWN:
							exampleSetInput.addError(new SimpleMetaDataError(Severity.WARNING, exampleSetInput,
									"special_unknown", "label"));
							return emd;
						case YES:
							AttributeMetaData label = emd.getLabelMetaData();
							AttributeMetaData predictionMD = AttributeMetaData.createPredictionMetaData(label);
							emd.addAttribute(predictionMD);
							AttributeMetaData.createConfidenceAttributeMetaData(emd);
							if (!label.isNominal()) {
								exampleSetInput.addError(
										new SimpleMetaDataError(Severity.ERROR, exampleSetInput, AttributeToNominalQuickFixProvider.labelToNominal(exampleSetInput, label), "special_attribute_has_wrong_type", label.getName(), Attributes.LABEL_NAME, Ontology.VALUE_TYPE_NAMES[Ontology.NOMINAL]));
							}
							return emd;
						default:
							return emd;
					}
				}
				return metaData;
			}
		});
		getTransformer().addPassThroughRule(clusterModelInput, clusterModelOutput);

	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
		ClusterModel model = clusterModelInput.getData(ClusterModel.class);
		Tools.hasNominalLabels(exampleSet);

		// generate the predicted attribute
		Attribute labelAttribute = exampleSet.getAttributes().getLabel();
		PredictionModel.createPredictedLabel(exampleSet, labelAttribute);
		Attribute predictedLabel = exampleSet.getAttributes().getPredictedLabel();

		HashMap<Integer, String> intToClusterMapping = new HashMap<Integer, String>();
		int[][] mappingTable = new int[model.getNumberOfClusters()][model.getNumberOfClusters()];

		// count the occurrence of each label with every cluster
		int a = 0;
		for (int i = 0; i < model.getNumberOfClusters(); i++) {
			HashMap<String, Integer> labelOccurrence = new HashMap<String, Integer>();
			for (Example example : exampleSet) {
				String label = example.getValueAsString(labelAttribute);
				if (!labelOccurrence.containsKey(label)) {
					labelOccurrence.put(label, 0);
					if (i == 0) {
						intToClusterMapping.put(a, label);
						a++;
					}
				}
				if (example.getValue(example.getAttributes().getCluster()) == i) {
					labelOccurrence.put(label, labelOccurrence.get(label) + 1);
				}
			}

			if (i == 0 && model.getNumberOfClusters() != labelOccurrence.size()) {
				throw new UserError(this, 943, labelOccurrence.size(), model.getNumberOfClusters());
			}

			for (int j = 0; j < mappingTable[i].length; j++) {
				String clusterName = intToClusterMapping.get(j);
				int occ = labelOccurrence.get(clusterName);
				mappingTable[i][j] = occ;
			}
		}
		/*
		 * Munkres-algorithm or the hungarian method
		 */
		// find the maximum
		int maxValue = -1;
		for (int i = 0; i < mappingTable.length; i++) {
			for (int j = 0; j < mappingTable[i].length; j++) {
				if (mappingTable[i][j] > maxValue) {
					maxValue = mappingTable[i][j];
				}
			}
		}

		// compute the new (inverted) table (and column-minima)
		for (int i = 0; i < mappingTable.length; i++) {
			int minimum = Integer.MAX_VALUE;
			for (int j = 0; j < mappingTable[i].length; j++) {
				mappingTable[i][j] = maxValue - mappingTable[i][j];
				if (mappingTable[i][j] < minimum) {
					minimum = mappingTable[i][j];
				}
			}
			// subtract the column-minima
			if (minimum > 0) {
				for (int j = 0; j < mappingTable[i].length; j++) {
					mappingTable[i][j] = mappingTable[i][j] - minimum;
				}
			}
		}
		// compute and subtract the row-minima
		for (int i = 0; i < mappingTable[0].length; i++) {
			int minimum = Integer.MAX_VALUE;
			for (int j = 0; j < mappingTable.length; j++) {
				if (mappingTable[j][i] < minimum) {
					minimum = mappingTable[j][i];
				}
			}
			// subtract the row-minima
			if (minimum > 0) {
				for (int j = 0; j < mappingTable.length; j++) {
					mappingTable[j][i] = mappingTable[j][i] - minimum;
				}
			}
		}
		while (!assignmentAvailable(mappingTable)) {
			Vector<Integer> markedRows = new Vector<Integer>();
			Vector<Integer> markedColumns = new Vector<Integer>();

			// mark all rows which have no marked zero (start labeling)
			for (int i = 0; i < mappingTable[0].length; i++) {
				boolean markedZero = false;
				for (int j = 0; j < mappingTable.length; j++) {
					if (mappingTable[j][i] == Integer.MIN_VALUE) {
						markedZero = true;
						break;
					}
				}
				if (!markedZero) {
					markedRows.add(i);
				}
			}

			boolean newMarked = true;
			while (newMarked) {
				newMarked = false;
				// mark all columns with a slashed zero in a marked row
				for (int i = 0; i < mappingTable.length; i++) {
					for (int j = 0; j < mappingTable[i].length; j++) {
						if (mappingTable[i][j] == Integer.MAX_VALUE) {
							if (markedRows.contains(j) && !markedColumns.contains(i)) {
								newMarked = true;
								markedColumns.add(i);
							}
						}
					}
				}
				// mark all rows with a marked zero in a marked column
				for (int i = 0; i < mappingTable[0].length; i++) {
					for (int j = 0; j < mappingTable.length; j++) {
						if (mappingTable[j][i] == Integer.MIN_VALUE) {
							if (markedColumns.contains(j) && !markedRows.contains(i)) {
								newMarked = true;
								markedRows.add(i);
							}
						}
					}
				}
			} // end while (newMarked)

			// inverting of the marked columns
			for (int i = 0; i < mappingTable.length; i++) {
				if (!markedColumns.contains(i)) {
					markedColumns.add(i);
				} else {
					markedColumns.removeElement(i);
				}
			}

			// find the minimum in the marked range
			int minimum = Integer.MAX_VALUE;
			for (int i = 0; i < markedRows.size(); i++) {
				for (int j = 0; j < markedColumns.size(); j++) {
					if (mappingTable[markedColumns.get(j)][markedRows.get(i)] < minimum) {
						minimum = mappingTable[markedColumns.get(j)][markedRows.get(i)];
					}
				}
			}
			// substract the minimum from all elements in the marked range
			for (int i = 0; i < markedRows.size(); i++) {
				for (int j = 0; j < markedColumns.size(); j++) {
					mappingTable[markedColumns.get(j)][markedRows.get(i)] = mappingTable[markedColumns.get(j)][markedRows
							.get(i)] - minimum;
				}
			}

			// add the minimum to all elements which are neither marked in a row nor in a column
			for (int i = 0; i < mappingTable.length; i++) {
				if (!markedColumns.contains(i)) {
					for (int j = 0; j < mappingTable[i].length; j++) {
						if (!markedRows.contains(j)) {
							mappingTable[i][j] = mappingTable[i][j] + minimum;
						}
					}
				}
			}
			// reset the Integer.MIN_VALUE and Integer.MAX_VALUE to zero
			for (int i = 0; i < mappingTable.length; i++) {
				for (int j = 0; j < mappingTable[i].length; j++) {
					if (mappingTable[i][j] == Integer.MAX_VALUE) {
						mappingTable[i][j] = 0;
					}
					if (mappingTable[i][j] == Integer.MIN_VALUE) {
						mappingTable[i][j] = 0;
					}
				}
			}
		} // end while(!assignmentAvailable)

		// compute the mapping (there must be a possible assignment)
		HashMap<Integer, String> clusterToPrediction = new HashMap<Integer, String>();
		for (int i = 0; i < mappingTable.length; i++) {
			int result = -1;
			for (int j = 0; j < mappingTable[i].length; j++) {
				if (mappingTable[i][j] == Integer.MIN_VALUE) {
					result = j;
					break;
				}
			}
			String resultCluster = intToClusterMapping.get(result);
			clusterToPrediction.put(i, resultCluster);
		}

		// insert the result in the predicted attribute
		HashMap<String, Integer> predictionToCluster = new HashMap<String, Integer>();
		// set the preditedLabel in the example table and compute to each prediction the cluster
		int i = 0;
		Attribute clusterAttribute = exampleSet.getAttributes().getCluster();
		for (Example example : exampleSet) {
			String resultLabel = clusterToPrediction.get((int) example.getValue(example.getAttributes().getCluster()));
			example.setValue(predictedLabel, resultLabel);
			if (predictionToCluster.size() < model.getNumberOfClusters()) {
				if (!predictionToCluster.containsKey(example.getValueAsString(example.getAttributes().getPredictedLabel()))) {
					String clusterNumber = example.getValueAsString(clusterAttribute).replaceAll("[^\\d]+", "");
					try {
						int number = Integer.parseInt(clusterNumber);
						if (number < 0 || number >= model.getNumberOfClusters()) {
							throw new UserError(this, 145, clusterAttribute.getName());
						}
						predictionToCluster.put(example.getValueAsString(example.getAttributes().getPredictedLabel()),
								number);
					} catch (NumberFormatException e) {
						throw new UserError(this, 145, clusterAttribute.getName());
					}
				}
			}
			i++;
		}

		// set the confidence in the example table
		i = 0;
		for (Example example : exampleSet) {
			if (model.getClass() == FlatFuzzyClusterModel.class) {
				FlatFuzzyClusterModel fuzzyModel = (FlatFuzzyClusterModel) model;
				for (int j = 0; j < clusterToPrediction.size(); j++) {
					String label = clusterToPrediction.get(j);
					example.setConfidence(label,
							fuzzyModel.getExampleInClusterProbability(i, predictionToCluster.get(label)));
				}
			} else {
				example.setConfidence(clusterToPrediction.get((int) example.getValue(example.getAttributes().getCluster())),
						1);
			}
			i++;
		}

		exampleSetOutput.deliver(exampleSet);
		clusterModelOutput.deliver(model);
	}

	/* Returns true, if there is a solution availble. */
	private boolean assignmentAvailable(int[][] mappingTable) {
		int markedZeros = 0;
		boolean modificationDone = true;

		while (modificationDone) {
			while (modificationDone) {
				modificationDone = false;
				// column by column
				for (int i = 0; i < mappingTable.length; i++) {
					int position = -1;
					for (int j = 0; j < mappingTable[i].length; j++) {
						if (mappingTable[i][j] == 0) {
							if (position == -1) {
								position = j;
							} else {
								position = -1;
								break;
							}
						}
					}
					if (position != -1) {
						modificationDone = true;
						mappingTable[i][position] = Integer.MIN_VALUE; // marked zero
						for (int k = 0; k < mappingTable.length; k++) {
							if (mappingTable[k][position] == 0) {
								mappingTable[k][position] = Integer.MAX_VALUE; // slashed zeros
							}
						}
						markedZeros++;
					}
				}
				if (markedZeros == mappingTable.length) {
					return true;
				}

				// line by line
				for (int i = 0; i < mappingTable[0].length; i++) {
					int position = -1;
					for (int j = 0; j < mappingTable.length; j++) {
						if (mappingTable[j][i] == 0) {
							if (position == -1) {
								position = j;
							} else {
								position = -1;
								break;
							}
						}
					}
					if (position != -1) {
						modificationDone = true;
						mappingTable[position][i] = Integer.MIN_VALUE;// marked zero
						for (int k = 0; k < mappingTable[0].length; k++) {
							if (mappingTable[position][k] == 0) {
								mappingTable[position][k] = Integer.MAX_VALUE; // slashed zeros
							}
						}
						markedZeros++;
					}
				}
				if (markedZeros == mappingTable.length) {
					return true;
				}
			}
			// modificationDone is here always false
			// ambiguous zeros
			int aktMarkedZeros = markedZeros;
			for (int i = 0; i < mappingTable.length; i++) {
				for (int j = 0; j < mappingTable[i].length; j++) {
					if (mappingTable[i][j] == 0) {
						mappingTable[i][j] = Integer.MIN_VALUE;// marked zero
						for (int k = j + 1; k < mappingTable[i].length; k++) {
							if (mappingTable[i][k] == 0) {
								mappingTable[i][k] = Integer.MAX_VALUE; // slashed zeros in the same
																		// column
							}
						}
						for (int k = 0; k < mappingTable.length; k++) {
							if (mappingTable[k][j] == 0) {
								mappingTable[k][j] = Integer.MAX_VALUE; // slashed zeros
							}
						}
						modificationDone = true;
						markedZeros++;
						break;
					}
				}
				if (aktMarkedZeros != markedZeros) {
					break;
				}
			}
			if (markedZeros == mappingTable.length) {
				return true;
			}
		}

		return false;
	}
}
