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
package com.rapidminer.operator.performance;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.SimpleProcessSetupError;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ValueDouble;
import com.rapidminer.operator.learner.CapabilityProvider;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.CapabilityPrecondition;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.operator.ports.metadata.MetaDataUnderspecifiedError;
import com.rapidminer.operator.ports.metadata.PassThroughOrGenerateRule;
import com.rapidminer.operator.ports.metadata.PassThroughRule;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.operator.ports.quickfix.ParameterSettingQuickFix;
import com.rapidminer.operator.ports.quickfix.QuickFix;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.ParameterTypeStringCategory;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;


/**
 * <p>
 * This performance evaluator operator should be used for regression tasks, i.e. in cases where the
 * label attribute has a numerical value type. The operator expects a test {@link ExampleSet} as
 * input, whose elements have both true and predicted labels, and delivers as output a list of
 * performance values according to a list of performance criteria that it calculates. If an input
 * performance vector was already given, this is used for keeping the performance values.
 * </p>
 *
 * <p>
 * All of the performance criteria can be switched on using boolean parameters. Their values can be
 * queried by a {@link com.rapidminer.operator.visualization.ProcessLogOperator} using the same
 * names. The main criterion is used for comparisons and need to be specified only for processes
 * where performance vectors are compared, e.g. feature selection or other meta optimization process
 * setups. If no other main criterion was selected, the first criterion in the resulting performance
 * vector will be assumed to be the main criterion.
 * </p>
 *
 * <p>
 * The resulting performance vectors are usually compared with a standard performance comparator
 * which only compares the fitness values of the main criterion. Other implementations than this
 * simple comparator can be specified using the parameter <var>comparator_class</var>. This may for
 * instance be useful if you want to compare performance vectors according to the weighted sum of
 * the individual criteria. In order to implement your own comparator, simply subclass
 * {@link PerformanceComparator}. Please note that for true multi-objective optimization usually
 * another selection scheme is used instead of simply replacing the performance comparator.
 * </p>
 *
 * @author Ingo Mierswa
 */
public abstract class AbstractPerformanceEvaluator extends Operator implements CapabilityProvider {

	/** The parameter name for &quot;The criterion used for comparing performance vectors.&quot; */
	public static final String PARAMETER_MAIN_CRITERION = "main_criterion";

	/**
	 * The parameter name for &quot;If set to true, examples with undefined labels are
	 * skipped.&quot;
	 */
	public static final String PARAMETER_SKIP_UNDEFINED_LABELS = "skip_undefined_labels";

	/**
	 * The parameter name for &quot;Fully qualified classname of the PerformanceComparator
	 * implementation.&quot;
	 */
	public static final String PARAMETER_COMPARATOR_CLASS = "comparator_class";

	/** Indicates if example weights should be used for performance calculations. */
	private static final String PARAMETER_USE_EXAMPLE_WEIGHTS = "use_example_weights";

	public static final String INPUT_PORT_LABELLED_DATA = "labelled data";

	private InputPort exampleSetInput = getInputPorts().createPort(INPUT_PORT_LABELLED_DATA);
	private InputPort performanceInput = getInputPorts().createPort("performance");
	private OutputPort performanceOutput = getOutputPorts().createPort("performance");
	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

	/**
	 * The currently used performance vector. This is be used for logging / plotting purposes.
	 */
	private PerformanceVector currentPerformanceVector = null;

	public AbstractPerformanceEvaluator(OperatorDescription description) {
		super(description);

		// exampleSetInput.addPrecondition(new ExampleSetPrecondition(exampleSetInput,
		// Ontology.VALUE_TYPE, Attributes.LABEL_NAME,
		// Attributes.PREDICTION_NAME));
		exampleSetInput.addPrecondition(new CapabilityPrecondition(this, exampleSetInput));
		exampleSetInput.addPrecondition(new SimplePrecondition(exampleSetInput, new ExampleSetMetaData()) {

			@Override
			public void makeAdditionalChecks(MetaData metaData) {
				if (!(metaData instanceof ExampleSetMetaData)) {
					exampleSetInput.addError(new MetaDataUnderspecifiedError(exampleSetInput));
					return;
				}
				ExampleSetMetaData emd = (ExampleSetMetaData) metaData;
				if (emd.hasSpecial(Attributes.LABEL_NAME) == MetaDataInfo.YES
						&& emd.hasSpecial(Attributes.PREDICTION_NAME) == MetaDataInfo.YES) {
					int type1 = emd.getSpecial(Attributes.LABEL_NAME).getValueType();
					int type2 = emd.getSpecial(Attributes.PREDICTION_NAME).getValueType();
					if (type1 != type2) {
						exampleSetInput.addError(new SimpleMetaDataError(Severity.ERROR, exampleSetInput,
								"label_prediction_mismatch", new Object[] { Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(type1),
										Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(type2) }));
					} else if (!canEvaluate(type1)) {
						exampleSetInput.addError(new SimpleMetaDataError(Severity.ERROR, exampleSetInput,
								"cannot_evaluate_label_type", new Object[] { Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(type1) }));
					}
				}
			}
		});
		performanceInput.addPrecondition(new SimplePrecondition(performanceInput, new MetaData(PerformanceVector.class),
				false));
		PassThroughOrGenerateRule performanceRule = new PassThroughOrGenerateRule(performanceInput, performanceOutput,
				new MetaData(PerformanceVector.class));
		getTransformer().addRule(performanceRule);
		getTransformer().addRule(new PassThroughRule(exampleSetInput, exampleSetOutput, false));

		// add values for logging
		List<PerformanceCriterion> criteria = getCriteria();
		for (PerformanceCriterion criterion : criteria) {
			addPerformanceValue(criterion.getName(), criterion.getDescription());
		}

		addValue(new ValueDouble("performance", "The last performance (main criterion).") {

			@Override
			public double getDoubleValue() {
				if (currentPerformanceVector != null) {
					return currentPerformanceVector.getMainCriterion().getAverage();
				} else {
					return Double.NaN;
				}
			}
		});
	}

	/**
	 * Returns true iff this operator can evaluate labels of the given value type.
	 *
	 * @see Ontology.ATTRIBUTE_VALUE_TYPE
	 */
	protected abstract boolean canEvaluate(int valueType);

	/**
	 * Delivers the list of criteria which is able for this operator. Please note that all criteria
	 * in the list must be freshly instantiated since no copy is created in different runs of this
	 * operator. This is important in order to not mess up the results.
	 *
	 * This method must not return null but should return an empty list in this case.
	 *
	 * ATTENTION! This method is called during the creation of parameters. Do not try to get a
	 * parameter value inside this method, since this will create an infinite loop!
	 */
	public abstract List<PerformanceCriterion> getCriteria();

	/**
	 * Delivers class weights for performance criteria which implement the
	 * {@link ClassWeightedPerformance} interface. Might return null (for example for regression
	 * task performance evaluators).
	 *
	 * @throws UserError
	 */
	protected abstract double[] getClassWeights(Attribute label) throws UserError;

	/** Performs a check if this operator can be used for this type of exampel set at all. */
	protected abstract void checkCompatibility(ExampleSet exampleSet) throws OperatorException;

	@Override
	public boolean shouldAutoConnect(OutputPort port) {
		if (port == exampleSetOutput) {
			return getParameterAsBoolean("keep_example_set");
		} else {
			return super.shouldAutoConnect(port);
		}
	}

	/**
	 * This method will be invoked before the actual calculation is started. The default
	 * implementation does nothing. Subclasses might want to override this method.
	 */
	protected void init(ExampleSet exampleSet) {}

	/** Subclasses might override this method and return false. */
	protected boolean showSkipNaNLabelsParameter() {
		return true;
	}

	/** Subclasses might override this method and return false. */
	protected boolean showComparatorParameter() {
		return true;
	}

	/** Subclasses might override this method and return false. */
	protected boolean showCriteriaParameter() {
		return true;
	}

	/**
	 * Helper method if this operator is constructed anonymously. Assigns the exampleSet to the
	 * input and returns the PerformanceVector from the output.
	 */
	public PerformanceVector doWork(ExampleSet exampleSet) throws OperatorException {
		exampleSetInput.receive(exampleSet);
		doWork();
		return performanceOutput.getData(PerformanceVector.class);
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet testSet = exampleSetInput.getData(ExampleSet.class);
		checkCompatibility(testSet);
		init(testSet);
		PerformanceVector inputPerformance = performanceInput.getDataOrNull(PerformanceVector.class);
		performanceOutput.deliver(evaluate(testSet, inputPerformance));
		exampleSetOutput.deliver(testSet);
	}

	// --------------------------------------------------------------------------------

	/**
	 * Adds the performance criteria as plottable values, e.g. for the ProcessLog operator.
	 */
	private void addPerformanceValue(final String name, String description) {
		addValue(new ValueDouble(name, description) {

			@Override
			public double getDoubleValue() {
				if (currentPerformanceVector == null) {
					return Double.NaN;
				}
				PerformanceCriterion c = currentPerformanceVector.getCriterion(name);

				if (c != null) {
					return c.getAverage();
				} else {
					return Double.NaN;
				}
			}
		});
	}

	/**
	 * Creates a new performance vector if the given one is null. Adds all criteria demanded by the
	 * user. If the criterion was already part of the performance vector before it will be
	 * overwritten.
	 */
	private PerformanceVector initialisePerformanceVector(ExampleSet testSet, PerformanceVector performanceCriteria,
			List<PerformanceCriterion> givenCriteria) throws OperatorException {
		givenCriteria.clear();
		if (performanceCriteria == null) {
			performanceCriteria = new PerformanceVector();
		} else {
			for (int i = 0; i < performanceCriteria.getSize(); i++) {
				givenCriteria.add(performanceCriteria.getCriterion(i));
			}
		}

		List<PerformanceCriterion> criteria = getCriteria();
		for (PerformanceCriterion criterion : criteria) {
			if (checkCriterionName(criterion.getName())) {
				performanceCriteria.addCriterion(criterion);
			}
		}

		if (performanceCriteria.size() == 0) {
			throw new UserError(this, 910);
		}

		// set suitable main criterion
		if (performanceCriteria.size() == 0) {
			List<PerformanceCriterion> availableCriteria = getCriteria();
			if (availableCriteria.size() > 0) {
				PerformanceCriterion criterion = availableCriteria.get(0);
				performanceCriteria.addCriterion(criterion);
				performanceCriteria.setMainCriterionName(criterion.getName());
				logWarning(getName() + ": No performance criterion selected! Using the first available criterion ("
						+ criterion.getName() + ").");
			} else {
				logWarning(getName() + ": not possible to identify available performance criteria.");
				throw new UserError(this, 910);
			}
		} else {
			if (showCriteriaParameter()) {
				String mcName = getParameterAsString(PARAMETER_MAIN_CRITERION);
				if (mcName != null) {
					performanceCriteria.setMainCriterionName(mcName);
				}
			}
		}

		// comparator
		String comparatorClass = null;
		if (showComparatorParameter()) {
			comparatorClass = getParameterAsString(PARAMETER_COMPARATOR_CLASS);
		}

		if (comparatorClass == null) {
			performanceCriteria.setComparator(new PerformanceVector.DefaultComparator());
		} else {
			try {
				Class<?> pcClass = com.rapidminer.tools.Tools.classForName(comparatorClass);
				if (!PerformanceComparator.class.isAssignableFrom(pcClass)) {
					throw new UserError(this, 914, new Object[] { pcClass, PerformanceComparator.class });
				} else {
					performanceCriteria.setComparator((PerformanceComparator) pcClass.newInstance());
				}
			} catch (Throwable e) {
				throw new UserError(this, e, 904, new Object[] { comparatorClass, e });
			}
		}

		return performanceCriteria;
	}

	/**
	 * Returns true if the criterion with the given name should be added to the performance vector.
	 * This is either the case
	 * <ol>
	 * <li>if the boolean parameter was selected by the user</li>
	 * <li>if the given name is equal to the main criterion</li>
	 * </ol>
	 */
	private boolean checkCriterionName(String name) throws UndefinedParameterError {
		String mainCriterionName = getParameterAsString(PARAMETER_MAIN_CRITERION);
		if (name != null && name.trim().length() != 0 && !name.equals(PerformanceVector.MAIN_CRITERION_FIRST)
				&& name.equals(mainCriterionName)) {
			return true;
		} else {
			ParameterType type = getParameterType(name);
			if (type != null) {
				return getParameterAsBoolean(name);
			} else {
				return true;
			}
		}
	}

	/**
	 * Evaluates the given test set. All {@link PerformanceCriterion} instances in the given
	 * {@link PerformanceVector} must be subclasses of {@link MeasuredPerformance}.
	 */
	protected PerformanceVector evaluate(ExampleSet testSet, PerformanceVector inputPerformance) throws OperatorException {
		List<PerformanceCriterion> givenCriteria = new LinkedList<PerformanceCriterion>();
		this.currentPerformanceVector = initialisePerformanceVector(testSet, inputPerformance, givenCriteria);

		boolean skipUndefined = true;
		if (showComparatorParameter()) {
			skipUndefined = getParameterAsBoolean(PARAMETER_SKIP_UNDEFINED_LABELS);
		}
		boolean useExampleWeights = getParameterAsBoolean(PARAMETER_USE_EXAMPLE_WEIGHTS);
		evaluate(this, testSet, currentPerformanceVector, givenCriteria, skipUndefined, useExampleWeights);
		return currentPerformanceVector;
	}

	/**
	 * Static version of {@link #evaluate(ExampleSet,PerformanceVector)}. This method was introduced
	 * to enable testing of the method.
	 *
	 * @param evaluator
	 *            Ususally this. May be null for testing. Only needed for exception.
	 */
	public static void evaluate(AbstractPerformanceEvaluator evaluator, ExampleSet testSet,
			PerformanceVector performanceCriteria, List<PerformanceCriterion> givenCriteria, boolean skipUndefinedLabels,
			boolean useExampleWeights) throws OperatorException {
		if (testSet.getAttributes().getLabel() == null) {
			throw new UserError(evaluator, 105, new Object[0]);
		}
		if (testSet.getAttributes().getPredictedLabel() == null) {
			throw new UserError(evaluator, 107, new Object[0]);
		}

		// sanity check for weight attribute
		if (useExampleWeights) {
			Attribute weightAttribute = testSet.getAttributes().getWeight();
			if (weightAttribute != null) {
				if (!weightAttribute.isNumerical()) {
					throw new UserError(evaluator, 120, new Object[] { weightAttribute.getName(),
							Ontology.VALUE_TYPE_NAMES[weightAttribute.getValueType()],
							Ontology.VALUE_TYPE_NAMES[Ontology.NUMERICAL] });
				}

				testSet.recalculateAttributeStatistics(weightAttribute);
				double minimum = testSet.getStatistics(weightAttribute, Statistics.MINIMUM);
				if (Double.isNaN(minimum) || minimum < 0.0d) {
					throw new UserError(evaluator, 138, new Object[] { weightAttribute.getName(), "positive values",
							"negative for some examples" });
				}
			}
		}

		// initialize all criteria
		for (int pc = 0; pc < performanceCriteria.size(); pc++) {
			PerformanceCriterion c = performanceCriteria.getCriterion(pc);
			if (!givenCriteria.contains(c)) {
				if (!(c instanceof MeasuredPerformance)) {
					throw new UserError(evaluator, 903, new Object[0]);
				}
				// init all criteria
				((MeasuredPerformance) c).startCounting(testSet, useExampleWeights);

				// init weight handlers
				if (c instanceof ClassWeightedPerformance) {
					if (evaluator != null) {
						Attribute label = testSet.getAttributes().getLabel();
						if (label.isNominal()) {
							double[] weights = evaluator.getClassWeights(label);
							if (weights != null) {
								((ClassWeightedPerformance) c).setWeights(weights);
							}
						}
					}
				}
			}
		}

		Iterator<Example> exampleIterator = testSet.iterator();
		while (exampleIterator.hasNext()) {
			Example example = exampleIterator.next();

			if (skipUndefinedLabels && (Double.isNaN(example.getLabel()) || Double.isNaN(example.getPredictedLabel()))) {
				continue;
			}

			for (int pc = 0; pc < performanceCriteria.size(); pc++) {
				PerformanceCriterion criterion = performanceCriteria.getCriterion(pc);
				if (!givenCriteria.contains(criterion)) {
					if (criterion instanceof MeasuredPerformance) {
						((MeasuredPerformance) criterion).countExample(example);
					}
				}
			}
			if (evaluator != null) {
				evaluator.checkForStop();
			}
		}
	}

	private String[] getAllCriteriaNames() {
		List<PerformanceCriterion> criteria = getCriteria();
		String[] result = new String[criteria.size()];
		int counter = 0;
		for (PerformanceCriterion criterion : criteria) {
			result[counter++] = criterion.getName();
		}
		return result;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		if (showCriteriaParameter()) {
			String[] criteriaNames = getAllCriteriaNames();
			if (criteriaNames.length > 0) {
				String[] allCriteriaNames = new String[criteriaNames.length + 1];
				allCriteriaNames[0] = PerformanceVector.MAIN_CRITERION_FIRST;
				System.arraycopy(criteriaNames, 0, allCriteriaNames, 1, criteriaNames.length);
				ParameterType type = new ParameterTypeStringCategory(PARAMETER_MAIN_CRITERION,
						"The criterion used for comparing performance vectors.", allCriteriaNames, allCriteriaNames[0]);
				type.setExpert(false);
				types.add(type);
			}

			List<PerformanceCriterion> criteria = getCriteria();
			boolean isDefault = true;
			for (PerformanceCriterion criterion : criteria) {
				ParameterType type = new ParameterTypeBoolean(criterion.getName(), criterion.getDescription(), isDefault,
						false);
				types.add(type);
				isDefault = false;
			}
		}

		if (showSkipNaNLabelsParameter()) {
			types.add(new ParameterTypeBoolean(PARAMETER_SKIP_UNDEFINED_LABELS,
					"If set to true, examples with undefined labels are skipped.", true));
		}
		if (showComparatorParameter()) {
			types.add(new ParameterTypeString(PARAMETER_COMPARATOR_CLASS,
					"Fully qualified classname of the PerformanceComparator implementation.", true));
		}

		types.add(new ParameterTypeBoolean(PARAMETER_USE_EXAMPLE_WEIGHTS,
				"Indicated if example weights should be used for performance calculations if possible.", true));
		return types;
	}

	/**
	 * This will override the checkProperties method of operator in order to add some checks for
	 * possible quickfixes.
	 */
	@Override
	public int checkProperties() {
		boolean criterionChecked = false;
		List<PerformanceCriterion> criteria = null;
		try {
			criteria = getCriteria();
			if (criteria != null) {
				for (PerformanceCriterion criterion : criteria) {
					if (checkCriterionName(criterion.getName())) {
						criterionChecked = true;
						break;
					}
				}
			}
		} catch (UndefinedParameterError err) {
		}

		if (!criterionChecked && criteria != null) {
			// building quick fixes
			List<QuickFix> quickFixes = new LinkedList<QuickFix>();
			if (criteria.size() > 0) {
				quickFixes.add(new ParameterSettingQuickFix(AbstractPerformanceEvaluator.this, getCriteria().get(0)
						.getName(), "true"));
				addError(new SimpleProcessSetupError(Severity.ERROR, this.getPortOwner(), quickFixes,
						"performance_criterion_undefined", criteria.get(0).getName()));
			}
			if (criteria.size() > 1) {
				quickFixes.add(new ParameterSettingQuickFix(AbstractPerformanceEvaluator.this, getCriteria().get(1)
						.getName(), "true"));
			}
		}
		return super.checkDeprecations() + super.checkProperties();
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		return true;
	}
}
