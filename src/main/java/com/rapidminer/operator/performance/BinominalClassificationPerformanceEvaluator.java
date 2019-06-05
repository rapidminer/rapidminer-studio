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

import static com.rapidminer.tools.FunctionWithThrowable.suppress;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeSuggestion;
import com.rapidminer.parameter.SuggestionProvider;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.Ontology;


/**
 * <p>
 * This performance evaluator operator should be used for classification tasks, i.e. in cases where
 * the label attribute has a binominal value type. Other polynominal classification tasks, i.e.
 * tasks with more than two classes can be handled by the
 * {@link PolynominalClassificationPerformanceEvaluator} operator. This operator expects a test
 * {@link ExampleSet} as input, whose elements have both true and predicted labels, and delivers as
 * output a list of performance values according to a list of performance criteria that it
 * calculates. If an input performance vector was already given, this is used for keeping the
 * performance values.
 * </p>
 *
 * <p>
 * All of the performance criteria can be switched on using boolean parameters. Their values can be
 * queried by a ProcessLogOperator using the same names. The main criterion is used for comparisons
 * and need to be specified only for processes where performance vectors are compared, e.g. feature
 * selection or other meta optimization process setups. If no other main criterion was selected, the
 * first criterion in the resulting performance vector will be assumed to be the main criterion.
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
public class BinominalClassificationPerformanceEvaluator extends AbstractPerformanceEvaluator {

	/**
	 * Iff this checkbox is {@code true}, the positive class parameter is shown to the user.
	 */
	public static final String PARAMETER_POSITIVE_CLASS_CHECKBOX = "manually_set_positive_class";

	/**
	 * The user optionally can use this parameter to explicitly specify the positive class.
	 */
	public static final String PARAMETER_POSITIVE_CLASS = "positive_class";

	/**
	 * The positive class parameter checkbox and the positive class parameter are added to the existing parameter types
	 * at this index.
	 */
	private static final int POSITIVE_CLASS_PARAMETER_INDEX = 0;

	private String positiveClassName;

	public BinominalClassificationPerformanceEvaluator(OperatorDescription description) {
		super(description);
		positiveClassName = null;
	}

	@Override
	public List<PerformanceCriterion> getCriteria() {
		List<PerformanceCriterion> performanceCriteria = new LinkedList<>();

		// standard classification measures
		for (int i = 0; i < MultiClassificationPerformance.NAMES.length; i++) {
			performanceCriteria.add(new MultiClassificationPerformance(i));
		}

		// AUC
		AreaUnderCurve aucOpt = new AreaUnderCurve.Optimistic();
		AreaUnderCurve auc = new AreaUnderCurve.Neutral();
		AreaUnderCurve aucPes = new AreaUnderCurve.Pessimistic();

		aucOpt.setUserDefinedPositiveClassName(positiveClassName);
		auc.setUserDefinedPositiveClassName(positiveClassName);
		aucPes.setUserDefinedPositiveClassName(positiveClassName);

		performanceCriteria.add(aucOpt);
		performanceCriteria.add(auc);
		performanceCriteria.add(aucPes);

		// binary classification criteria
		for (int i = 0; i < BinaryClassificationPerformance.NAMES.length; i++) {
			BinaryClassificationPerformance b = new BinaryClassificationPerformance(i);
			b.setUserDefinedPositiveClassName(positiveClassName);
			performanceCriteria.add(b);
		}
		return performanceCriteria;
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		switch (capability) {
			case BINOMINAL_LABEL:
				return true;
			case POLYNOMINAL_LABEL:
			case NUMERICAL_LABEL:
			case ONE_CLASS_LABEL:
				return false;
			case POLYNOMINAL_ATTRIBUTES:
			case BINOMINAL_ATTRIBUTES:
			case NUMERICAL_ATTRIBUTES:
			case WEIGHTED_EXAMPLES:
			case MISSING_VALUES:
				return true;
			case NO_LABEL:
			case UPDATABLE:
			case FORMULA_PROVIDER:
			default:
				return false;
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new ArrayList<>(super.getParameterTypes());
		ParameterType posClassType = createPositiveClassParameter();
		posClassType.registerDependencyCondition(new BooleanParameterCondition(this,
				PARAMETER_POSITIVE_CLASS_CHECKBOX, true, true));
		types.add(POSITIVE_CLASS_PARAMETER_INDEX, posClassType);
		types.add(POSITIVE_CLASS_PARAMETER_INDEX, new ParameterTypeBoolean(PARAMETER_POSITIVE_CLASS_CHECKBOX,
				"Check this to manually specify the positive class.", false, false));
		return types;
	}

	@Override
	protected void checkCompatibility(ExampleSet exampleSet) throws OperatorException {
		Tools.isNonEmpty(exampleSet);
		Tools.hasNominalLabels(exampleSet, "the calculation of performance criteria for binominal classification tasks");

		Attribute label = exampleSet.getAttributes().getLabel();
		NominalMapping mapping = label.getMapping();
		if (mapping.size() != 2) {
			throw new UserError(this, 114, "the calculation of performance criteria for binominal classification tasks",
					label.getName());
		}

		// check if there is a user specified positive class and if it is valid
		if (getParameterAsBoolean(PARAMETER_POSITIVE_CLASS_CHECKBOX)) {
			String posClass = Optional.of(getParameterAsString(PARAMETER_POSITIVE_CLASS)).filter(s -> !s.isEmpty()).orElse(null);
			if (posClass == null || mapping.getIndex(posClass) == -1) {
				throw new UserError(this, "invalid_positive_class", posClass);
			}
		}
	}

	@Override
	protected void init(ExampleSet exampleSet) {
		super.init(exampleSet);
		if (getParameterAsBoolean(PARAMETER_POSITIVE_CLASS_CHECKBOX)) {
			positiveClassName = Optional.of(PARAMETER_POSITIVE_CLASS).map(suppress(this::getParameterAsString))
					.filter(s -> !s.isEmpty()).orElse(null);
		} else {
			positiveClassName = null;
		}
	}

	/**
	 * Returns null.
	 */
	@Override
	protected double[] getClassWeights(Attribute label) throws UndefinedParameterError {
		return null;
	}

	@Override
	protected boolean canEvaluate(int valueType) {
		return Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.BINOMINAL);
	}

	/**
	 * Creates the positive class parameter as {@link ParameterTypeSuggestion}.
	 */
	private ParameterType createPositiveClassParameter() {
		InputPort in = getInputPorts().getPortByName(INPUT_PORT_LABELLED_DATA);
		SuggestionProvider<String> suggestionProvider = (op, pl) -> {
			if (op != BinominalClassificationPerformanceEvaluator.this) {
				return new ArrayList<>();
			}
			return Optional.ofNullable(in).map(suppress(ip -> ip.getMetaData(ExampleSetMetaData.class)))
					.map(ExampleSetMetaData::getLabelMetaData).filter(AttributeMetaData::isNominal)
					.map(AttributeMetaData::getValueSet).filter(vs -> vs.size() == 2)
					.map(ArrayList::new).orElse(new ArrayList<>());
		};
		return new ParameterTypeSuggestion(PARAMETER_POSITIVE_CLASS, "Please select the positive class.",
				suggestionProvider, true);
	}
}
