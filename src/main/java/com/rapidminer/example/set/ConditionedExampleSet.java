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
package com.rapidminer.example.set;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.ExampleTable;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.OperatorProgress;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.operator.tools.ExpressionEvaluationException;
import com.rapidminer.tools.Tools;


/**
 * Hides {@link Example}s that do not fulfill a given {@link Condition}.
 *
 * @author Ingo Mierswa
 */
public class ConditionedExampleSet extends AbstractExampleSet {

	private static final long serialVersionUID = 877488093216198777L;

	/** Array of short names for the known conditions. */
	public static final String[] KNOWN_CONDITION_NAMES = {"all", "correct_predictions", "wrong_predictions",
			"no_missing_attributes", "missing_attributes", "no_missing_labels", "missing_labels", "attribute_value_filter",
			"expression", "custom_filters"};

	public static final int CONDITION_ALL = 0;
	public static final int CONDITION_CORRECT_PREDICTIONS = 1;
	public static final int CONDITION_WRONG_PREDICTIONS = 2;
	public static final int CONDITION_NO_MISSING_ATTRIBUTES = 3;
	public static final int CONDITION_MISSING_ATTRIBUTES = 4;
	public static final int CONDITION_NO_MISSING_LABELS = 5;
	public static final int CONDITION_MISSING_LABELS = 6;
	public static final int CONDITION_ATTRIBUTE_VALUE_FILTER = 7;
	public static final int CONDITION_EXPRESSION = 8;
	public static final int CONDITION_CUSTOM_FILTER = 9;

	/**
	 * Array of fully qualified classnames of implementations of {@link Condition} that are useful
	 * independently of special applications. All conditions given here must provide a construtor
	 * with arguments (ExampleSet data, String parameters).
	 */
	private static final String[] KNOWN_CONDITION_IMPLEMENTATIONS = {AcceptAllCondition.class.getName(),
			CorrectPredictionCondition.class.getName(), WrongPredictionCondition.class.getName(),
			NoMissingAttributesCondition.class.getName(), MissingAttributesCondition.class.getName(),
			NoMissingLabelsCondition.class.getName(), MissingLabelsCondition.class.getName(),
			AttributeValueFilter.class.getName(), ExpressionFilter.class.getName(), CustomFilter.class.getName()};

	private ExampleSet parent;

	private int[] mapping;

	/**
	 * Creates a new example which used only examples fulfilling the given condition.
	 *
	 * @throws ExpressionEvaluationException
	 */
	public ConditionedExampleSet(ExampleSet parent, Condition condition) throws ExpressionEvaluationException {
		this(parent, condition, false);
	}

	/**
	 * Creates a new example which used only examples fulfilling the given condition.
	 *
	 * @throws ExpressionEvaluationException
	 */
	public ConditionedExampleSet(ExampleSet parent, Condition condition, boolean inverted)
			throws ExpressionEvaluationException {
		this.parent = (ExampleSet) parent.clone();
		try {
			this.mapping = calculateMapping(condition, inverted, null);
		} catch (ProcessStoppedException e) {
			// Cannot happen because progress is null
		}
	}

	/**
	 * Creates a new example which used only examples fulfilling the given condition.
	 *
	 * @param progress
	 * 		the {@link OperatorProgress} to report the progress to
	 * @throws ExpressionEvaluationException
	 * @throws ProcessStoppedException
	 * 		if the process was stopped, can only happen if progress not {@code null}
	 */
	public ConditionedExampleSet(ExampleSet parent, Condition condition, boolean inverted, OperatorProgress progress)
			throws ExpressionEvaluationException, ProcessStoppedException {
		this.parent = (ExampleSet) parent.clone();
		this.mapping = calculateMapping(condition, inverted, progress);
	}

	/** Clone constructor. */
	public ConditionedExampleSet(ConditionedExampleSet exampleSet) {
		this.parent = (ExampleSet) exampleSet.parent.clone();
		this.mapping = new int[exampleSet.mapping.length];
		System.arraycopy(exampleSet.mapping, 0, this.mapping, 0, exampleSet.mapping.length);
	}

	@Override
	public boolean equals(Object o) {
		if (!super.equals(o)) {
			return false;
		}
		if (!(o instanceof ConditionedExampleSet)) {
			return false;
		}
		ConditionedExampleSet other = (ConditionedExampleSet) o;
		if (this.mapping.length != other.mapping.length) {
			return false;
		}
		for (int i = 0; i < this.mapping.length; i++) {
			if (this.mapping[i] != other.mapping[i]) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ Arrays.hashCode(this.mapping);
	}

	private int[] calculateMapping(Condition condition, boolean inverted, OperatorProgress progress)
			throws ExpressionEvaluationException, ProcessStoppedException {
		if (progress != null) {
			// +1 since a little is happening afterwards
			progress.setTotal(parent.size() + 1);
		}
		List<Integer> indices = new LinkedList<Integer>();

		// create mapping
		int exampleCounter = 0;
		for (Example example : parent) {
			if (!inverted) {
				if (condition.conditionOk(example)) {
					indices.add(exampleCounter);
				}
			} else {
				if (!condition.conditionOk(example)) {
					indices.add(exampleCounter);
				}
			}
			exampleCounter++;
			if (progress != null && exampleCounter % 1000 == 0) {
				progress.setCompleted(exampleCounter);
			}
		}

		int[] mapping = new int[indices.size()];
		int m = 0;
		for (int index : indices) {
			mapping[m++] = index;
		}
		return mapping;
	}

	/** Returns a {@link MappedExampleReader}. */
	@Override
	public Iterator<Example> iterator() {
		return new MappedExampleReader(parent.iterator(), this.mapping);
	}

	/** Returns the i-th example fulfilling the condition. */
	@Override
	public Example getExample(int index) {
		if ((index < 0) || (index >= this.mapping.length)) {
			throw new RuntimeException("Given index '" + index + "' does not fit the filtered ExampleSet!");
		} else {
			return parent.getExample(this.mapping[index]);
		}
	}

	/** Counts the number of examples which fulfills the condition. */
	@Override
	public int size() {
		return mapping.length;
	}

	@Override
	public Attributes getAttributes() {
		return parent.getAttributes();
	}

	@Override
	public ExampleTable getExampleTable() {
		return parent.getExampleTable();
	}

	/**
	 * Checks if the given name is the short name of a known condition and creates it. If the name
	 * is not known, this method creates a new instance of className which must be an implementation
	 * of {@link Condition} by calling its two argument constructor passing it the example set and
	 * the parameter string
	 */
	public static Condition createCondition(String name, ExampleSet exampleSet, String parameterString)
			throws ConditionCreationException {
		String className = name;
		for (int i = 0; i < KNOWN_CONDITION_NAMES.length; i++) {
			if (KNOWN_CONDITION_NAMES[i].equals(name)) {
				className = KNOWN_CONDITION_IMPLEMENTATIONS[i];
				break;
			}
		}
		try {
			Class<?> clazz = Tools.classForName(className);
			if (!Condition.class.isAssignableFrom(clazz)) {
				throw new ConditionCreationException("'" + className + "' does not implement Condition!");
			}
			Constructor<?> constructor = clazz.getConstructor(new Class[]{ExampleSet.class, String.class});
			return (Condition) constructor.newInstance(new Object[]{exampleSet, parameterString});
		} catch (ClassNotFoundException e) {
			throw new ConditionCreationException("Cannot find class '" + className + "'. Check your classpath.", e);
		} catch (NoSuchMethodException e) {
			throw new ConditionCreationException(
					"'" + className + "' must implement two argument constructor " + className + "(ExampleSet, String)!", e);
		} catch (IllegalAccessException e) {
			throw new ConditionCreationException(
					"'" + className + "' cannot access two argument constructor " + className + "(ExampleSet, String)!", e);
		} catch (InstantiationException e) {
			throw new ConditionCreationException(className + ": cannot create condition (" + e.getMessage() + ").", e);
		} catch (Throwable e) {
			if (e.getCause() instanceof ConditionCreationException) {
				throw (ConditionCreationException) e.getCause();
			}
			throw new ConditionCreationException(className + ": cannot invoke condition ("
					+ (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()) + ").", e);
		}
	}

	@Override
	public Annotations getAnnotations() {
		return parent.getAnnotations();
	}

	@Override
	public void cleanup() {
		parent.cleanup();
	}

	@Override
	public boolean isThreadSafeView() {
		return parent instanceof AbstractExampleSet && ((AbstractExampleSet) parent).isThreadSafeView();
	}
}
