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

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.Partition;
import com.rapidminer.example.set.SplittedExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.PredictionModelMetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.RandomGenerator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * A metaclassifier for handling multi-class datasets with 2-class classifiers. This class supports
 * several strategies for multiclass classification including procedures which are capable of using
 * error-correcting output codes for increased accuracy.
 * 
 * @author Helge Homburg
 */
public class Binary2MultiClassLearner extends AbstractMetaLearner {

	/**
	 * The parameter name for &quot;What strategy should be used for multi class
	 * classifications?&quot;
	 */
	public static final String PARAMETER_CLASSIFICATION_STRATEGIES = "classification_strategies";

	/**
	 * The parameter name for &quot;A multiplier regulating the codeword length in random code
	 * modus.&quot;
	 */
	public static final String PARAMETER_RANDOM_CODE_MULTIPLICATOR = "random_code_multiplicator";

	private static final String[] STRATEGIES = { "1 against all", "1 against 1", "exhaustive code (ECOC)",
			"random code (ECOC)" };

	private static final int ONE_AGAINST_ALL = 0;

	private static final int ONE_AGAINST_ONE = 1;

	private static final int EXHAUSTIVE_CODE = 2;

	private static final int RANDOM_CODE = 3;

	/** This List stores a short description for the generated models. */
	private final LinkedList<String> modelNames = new LinkedList<String>();

	/**
	 * A class which stores all necessary information to train a series of models according to a
	 * certain classification strategy.
	 */
	private static class CodePattern {

		String[][] data;
		boolean[][] partitionEnabled;

		public CodePattern(int numberOfClasses, int numberOfFunctions) {
			data = new String[numberOfClasses][numberOfFunctions];
			partitionEnabled = new boolean[numberOfClasses][numberOfFunctions];
			for (int i = 0; i < numberOfClasses; i++) {
				for (int j = 0; j < numberOfFunctions; j++) {
					partitionEnabled[i][j] = true;
				}
			}
		}
	}

	public Binary2MultiClassLearner(OperatorDescription description) {
		super(description);
	}

	@Override
	protected MetaData modifyExampleSetMetaData(ExampleSetMetaData metaData) {
		AttributeMetaData labelAMD = metaData.getAttributeByRole(Attributes.LABEL_NAME);
		if (labelAMD != null) {
			if (labelAMD.isNominal()) {
				labelAMD.setType(Ontology.BINOMINAL);
				labelAMD.setValueSetRelation(SetRelation.SUBSET);
			}
		}
		return metaData;
	}

	@Override
	protected MetaData modifyGeneratedModelMetaData(PredictionModelMetaData unmodifiedMetaData) {
		for (AttributeMetaData amd : unmodifiedMetaData.getPredictionAttributeMetaData()) {
			if (amd.getRole().equals(Attributes.PREDICTION_NAME)) {
				MetaData mdInput = exampleSetInput.getMetaData();
				if (mdInput instanceof ExampleSetMetaData) {
					ExampleSetMetaData esmdInput = (ExampleSetMetaData) mdInput;
					AttributeMetaData labelAMD = esmdInput.getAttributeByRole(Attributes.LABEL_NAME);
					if (labelAMD != null && labelAMD.isNominal()) {
						amd.setType(labelAMD.getValueType());
						amd.setValueSetRelation(labelAMD.getValueSetRelation());
						unmodifiedMetaData.getPredictedLabelMetaData().setType(labelAMD.getValueType());
						unmodifiedMetaData.getPredictedLabelMetaData().setValueSetRelation(labelAMD.getValueSetRelation());
					}
				}
				break;
			}
		}
		return super.modifyGeneratedModelMetaData(unmodifiedMetaData);
	}

	private SplittedExampleSet constructClassPartitionSet(ExampleSet inputSet) {

		Attribute classLabel = inputSet.getAttributes().getLabel();
		int numberOfClasses = classLabel.getMapping().size();
		int[] examples = new int[inputSet.size()];
		Iterator<Example> exampleIterator = inputSet.iterator();
		int i = 0;
		while (exampleIterator.hasNext()) {
			Example e = exampleIterator.next();
			examples[i] = (int) e.getValue(classLabel);
			i++;
		}
		Partition separatedClasses = new Partition(examples, numberOfClasses);
		return new SplittedExampleSet(inputSet, separatedClasses);
	}

	/**
	 * Trains a series of models depending on the classification method specified by a certain code
	 * pattern.
	 */
	private Model[] applyCodePattern(SplittedExampleSet seSet, Attribute classLabel, CodePattern codePattern)
			throws OperatorException {
		int numberOfClasses = classLabel.getMapping().size();
		int numberOfFunctions = codePattern.data[0].length;
		Model[] models = new Model[numberOfFunctions];

		// Hash maps are used for addressing particular class values using indices without relying
		// upon a consistent index distribution of the corresponding substructure.
		HashMap<Integer, Integer> classIndexMap = new HashMap<Integer, Integer>(numberOfClasses);

		for (int currentFunction = 0; currentFunction < numberOfFunctions; currentFunction++) {
			// 1. Configure a split example set and add a temporary label.
			int counter = 0;
			seSet.clearSelection();

			for (String currentClass : classLabel.getMapping().getValues()) {
				classIndexMap.put(classLabel.getMapping().mapString(currentClass), counter);
				if (codePattern.partitionEnabled[counter][currentFunction]) {
					seSet.selectAdditionalSubset(classLabel.getMapping().mapString(currentClass));
				}
				counter++;
			}
			Attribute workingLabel = AttributeFactory.createAttribute("multiclass_working_label", Ontology.BINOMINAL);
			seSet.getExampleTable().addAttribute(workingLabel);
			seSet.getAttributes().addRegular(workingLabel);
			int currentIndex = 0;

			Iterator<Example> iterator = seSet.iterator();
			while (iterator.hasNext()) {
				Example e = iterator.next();
				currentIndex = classIndexMap.get((int) e.getValue(classLabel));

				if (codePattern.partitionEnabled[currentIndex][currentFunction]) {
					e.setValue(workingLabel,
							workingLabel.getMapping().mapString(codePattern.data[currentIndex][currentFunction]));
				}
			}
			seSet.getAttributes().remove(workingLabel);
			seSet.getAttributes().setLabel(workingLabel);

			// 2. Apply the example set to the inner learner.
			models[currentFunction] = applyInnerLearner(seSet);
			inApplyLoop();

			// 3. Clean up for the next run.
			seSet.getAttributes().setLabel(classLabel);
			seSet.getExampleTable().removeAttribute(workingLabel);
		}
		return models;
	}

	/**
	 * Builds a code pattern according to the "1 against all" classification scheme.
	 */
	private CodePattern buildCodePattern_ONE_VS_ALL(Attribute classLabel) {
		int numberOfClasses = classLabel.getMapping().size();
		CodePattern codePattern = new CodePattern(numberOfClasses, numberOfClasses); // ,
																						// ONE_AGAINST_ALL);
		Iterator<String> classIt = classLabel.getMapping().getValues().iterator();
		modelNames.clear();

		for (int i = 0; i < numberOfClasses; i++) {
			for (int j = 0; j < numberOfClasses; j++) {
				if (i == j) {
					String currentClass = classIt.next();
					modelNames.add(currentClass + " vs. all other");
					codePattern.data[i][j] = currentClass;
				} else {
					codePattern.data[i][j] = "all_other_classes";
				}
			}
		}
		return codePattern;
	}

	/**
	 * Builds a code pattern according to the "1 against 1" classification scheme.
	 */
	private CodePattern buildCodePattern_ONE_VS_ONE(Attribute classLabel) {
		int numberOfClasses = classLabel.getMapping().size();
		int numberOfCombinations = (numberOfClasses * (numberOfClasses - 1)) / 2;
		String[] classIndexMap = new String[numberOfClasses];
		CodePattern codePattern = new CodePattern(numberOfClasses, numberOfCombinations); // ,
																							// ONE_AGAINST_ONE);
		modelNames.clear();

		for (int i = 0; i < numberOfClasses; i++) {
			for (int j = 0; j < numberOfCombinations; j++) {
				codePattern.partitionEnabled[i][j] = false;
			}
		}
		int classIndex = 0;

		for (String className : classLabel.getMapping().getValues()) {
			classIndexMap[classIndex] = className;
			classIndex++;
		}
		int currentClassA = 0, currentClassB = 1;
		for (int counter = 0; counter < numberOfCombinations; counter++) {

			if (currentClassB > (numberOfClasses - 1)) {
				currentClassA++;
				currentClassB = currentClassA + 1;
			}
			if (currentClassA > (numberOfClasses - 2)) {
				break;
			}
			codePattern.partitionEnabled[currentClassA][counter] = true;
			codePattern.partitionEnabled[currentClassB][counter] = true;
			String currentClassNameA = classIndexMap[currentClassA];
			String currentClassNameB = classIndexMap[currentClassB];
			codePattern.data[currentClassA][counter] = currentClassNameA;
			codePattern.data[currentClassB][counter] = currentClassNameB;

			modelNames.add(currentClassNameA + " vs. " + currentClassNameB);

			currentClassB++;
		}
		return codePattern;
	}

	/**
	 * Builds a code pattern according to the "exhaustive code" classification scheme.
	 */
	private CodePattern buildCodePattern_EXHAUSTIVE_CODE(Attribute classLabel) {
		int numberOfClasses = classLabel.getMapping().size();
		int numberOfFunctions = (int) Math.pow(2, numberOfClasses - 1) - 1;
		CodePattern codePattern = new CodePattern(numberOfClasses, numberOfFunctions); // ,
																						// EXHAUSTIVE_CODE);

		for (int i = 0; i < numberOfFunctions; i++) {
			codePattern.data[0][i] = "true";
		}
		for (int i = 1; i < numberOfClasses; i++) {
			int currentStep = (int) Math.pow(2, numberOfClasses - (i + 1));
			for (int j = 0; j < numberOfFunctions; j++) {
				codePattern.data[i][j] = "" + (((j / currentStep) % 2) > 0);
			}
		}
		return codePattern;
	}

	/**
	 * Builds a code pattern according to the "random code" classification scheme.
	 */
	private CodePattern buildCodePattern_RANDOM_CODE(Attribute classLabel) throws OperatorException {
		double multiplicator = getParameterAsDouble(PARAMETER_RANDOM_CODE_MULTIPLICATOR);

		int numberOfClasses = classLabel.getMapping().size();
		CodePattern codePattern = new CodePattern(numberOfClasses, (int) (numberOfClasses * multiplicator)); // ,
																												// RANDOM_CODE);

		RandomGenerator randomGenerator = RandomGenerator.getRandomGenerator(this);

		for (int i = 0; i < codePattern.data.length; i++) {
			for (int j = 0; j < codePattern.data[0].length; j++) {
				codePattern.data[i][j] = "" + randomGenerator.nextBoolean();
			}
		}

		// TODO: Improve random codeword quality

		// Ensure that each column shows at least one occurrence of "1" (true) or "0" (false),
		// otherwise the following two-class classification procedure fails.
		for (int i = 0; i < codePattern.data[0].length; i++) {
			boolean containsNoOne = true, containsNoZero = true;
			for (int j = 0; j < codePattern.data.length; j++) {
				if ("true".equals(codePattern.data[j][i])) {
					containsNoOne = false;
				} else {
					containsNoZero = false;
				}
			}
			if (containsNoOne) {
				codePattern.data[(int) (randomGenerator.nextDouble() * (codePattern.data.length - 1))][i] = "true";
			}
			if (containsNoZero) {
				codePattern.data[(int) (randomGenerator.nextDouble() * (codePattern.data.length - 1))][i] = "false";
			}
		}
		return codePattern;
	}

	@Override
	public Model learn(ExampleSet inputSet) throws OperatorException {
		Attribute classLabel = inputSet.getAttributes().getLabel();

		if (classLabel.getMapping().size() == 2) {
			return applyInnerLearner(inputSet);
		}

		int classificationStrategy = getParameterAsInt(PARAMETER_CLASSIFICATION_STRATEGIES);
		CodePattern codePattern;
		Model[] models;

		SplittedExampleSet seSet = constructClassPartitionSet(inputSet);

		switch (classificationStrategy) {

			case ONE_AGAINST_ALL: {
				getLogger().fine("Binary2MultiCLassLearner set to <<1-vs-all>>");

				codePattern = buildCodePattern_ONE_VS_ALL(classLabel);
				models = applyCodePattern(seSet, classLabel, codePattern);

				return new Binary2MultiClassModel(inputSet, models, classificationStrategy, modelNames);
			}

			case ONE_AGAINST_ONE: {
				getLogger().fine("Binary2MultiCLassLearner set to <<1-vs-1>>");

				codePattern = buildCodePattern_ONE_VS_ONE(classLabel);
				models = applyCodePattern(seSet, classLabel, codePattern);

				return new Binary2MultiClassModel(inputSet, models, classificationStrategy, modelNames);
			}

			case EXHAUSTIVE_CODE: {
				getLogger().fine("Binary2MultiCLassLearner set to <<exhaustive code>>");

				codePattern = buildCodePattern_EXHAUSTIVE_CODE(classLabel);
				models = applyCodePattern(seSet, classLabel, codePattern);

				return new Binary2MultiClassModel(inputSet, models, classificationStrategy, codePattern.data);
			}

			case RANDOM_CODE: {
				getLogger().fine("Binary2MultiCLassLearner set to <<random code>>");

				codePattern = buildCodePattern_RANDOM_CODE(classLabel);
				models = applyCodePattern(seSet, classLabel, codePattern);

				return new Binary2MultiClassModel(inputSet, models, classificationStrategy, codePattern.data);
			}

			default: {
				throw new OperatorException("Binary2MultiCLassLearner: Unknown classification strategy selected");
			}

		}
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		switch (capability) {
			case NUMERICAL_LABEL:
			case NO_LABEL:
			case UPDATABLE:
			case FORMULA_PROVIDER:
				return false;
			default:
				return true;
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeCategory(PARAMETER_CLASSIFICATION_STRATEGIES,
				"What strategy should be used for multi class classifications?", STRATEGIES, ONE_AGAINST_ALL, false));

		ParameterTypeDouble type = new ParameterTypeDouble(PARAMETER_RANDOM_CODE_MULTIPLICATOR,
				"A multiplicator regulating the codeword length in random code modus.", 1.0d, Double.POSITIVE_INFINITY,
				2.0d, false);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_CLASSIFICATION_STRATEGIES, STRATEGIES, true,
				EXHAUSTIVE_CODE, RANDOM_CODE));
		types.add(type);

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));

		return types;
	}

}
