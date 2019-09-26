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
package com.rapidminer.tools.math;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeWeight;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.performance.AbsoluteError;
import com.rapidminer.operator.performance.AreaUnderCurve;
import com.rapidminer.operator.performance.AreaUnderCurve.Neutral;
import com.rapidminer.operator.performance.AreaUnderCurve.Optimistic;
import com.rapidminer.operator.performance.AreaUnderCurve.Pessimistic;
import com.rapidminer.operator.performance.BinaryClassificationPerformance;
import com.rapidminer.operator.performance.CorrelationCriterion;
import com.rapidminer.operator.performance.CrossEntropy;
import com.rapidminer.operator.performance.EstimatedPerformance;
import com.rapidminer.operator.performance.LenientRelativeError;
import com.rapidminer.operator.performance.LogisticLoss;
import com.rapidminer.operator.performance.MDLCriterion;
import com.rapidminer.operator.performance.Margin;
import com.rapidminer.operator.performance.MeasuredPerformance;
import com.rapidminer.operator.performance.MinMaxCriterion;
import com.rapidminer.operator.performance.MultiClassificationPerformance;
import com.rapidminer.operator.performance.NormalizedAbsoluteError;
import com.rapidminer.operator.performance.ParameterizedMeasuredPerformanceCriterion;
import com.rapidminer.operator.performance.PredictionAverage;
import com.rapidminer.operator.performance.RankCorrelation;
import com.rapidminer.operator.performance.RelativeError;
import com.rapidminer.operator.performance.RootMeanSquaredError;
import com.rapidminer.operator.performance.RootRelativeSquaredError;
import com.rapidminer.operator.performance.SimpleClassificationError;
import com.rapidminer.operator.performance.SoftMarginLoss;
import com.rapidminer.operator.performance.SquaredCorrelationCriterion;
import com.rapidminer.operator.performance.SquaredError;
import com.rapidminer.operator.performance.StrictRelativeError;
import com.rapidminer.operator.performance.WeightedMultiClassPerformance;
import com.rapidminer.operator.performance.cost.ClassificationCostCriterion;
import com.rapidminer.operator.performance.cost.RankingCriterion;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.tools.Ontology;

/**
 * Tests for {@link Averagable} and subclasses. So far only makes sure that known subclasses are {@link Cloneable}.
 *
 * @author Jan Czogalla
 * @since 9.4
 */
public class AveragableTest {

	private static final Class<? extends Averagable>[] AVERAGABLE_SUBCLASSES = new Class[]{AttributeWeight.class, RankCorrelation.class, Margin.class,
			AreaUnderCurve.class, Optimistic.class, Neutral.class, Pessimistic.class, NormalizedAbsoluteError.class,
			WeightedMultiClassPerformance.class, MDLCriterion.class, PredictionAverage.class, RankingCriterion.class,
			SoftMarginLoss.class, MinMaxCriterion.class, LogisticLoss.class, RelativeError.class, LenientRelativeError.class,
			SimpleClassificationError.class, SquaredError.class, AbsoluteError.class, RootMeanSquaredError.class,
			StrictRelativeError.class, BinaryClassificationPerformance.class, RootRelativeSquaredError.class,
			CorrelationCriterion.class, SquaredCorrelationCriterion.class, CrossEntropy.class, MultiClassificationPerformance.class,
			ClassificationCostCriterion.class, ParameterizedMeasuredPerformanceCriterion.class, EstimatedPerformance.class};
	private static final BiPredicate<Object, Object> NULL_CHECK_EQUALS = (a, b) -> (a == null) == (b == null);
	private static final String[] LABEL_PRED_VALUES = {"a", "b"};

	private static Map<Class, Object> defaultValueMap = createDefaultValueMap();
	private static Map<Class, BiPredicate<Object, Object>> specialEquals;

	@BeforeClass
	public static void setup() {
		defaultValueMap = createDefaultValueMap();
		specialEquals = new HashMap<Class, BiPredicate<Object, Object>>() {

			@Override
			public BiPredicate<Object, Object> getOrDefault(Object key, BiPredicate<Object, Object> defaultValue) {
				if (key instanceof Class && ((Class) key).isArray() && !((Class) key).getComponentType().isPrimitive()) {
					// handle all non primitive arrays the same
					key = Object[].class;
				}
				return super.getOrDefault(key, defaultValue);
			}
		};
		// no equals method for ROCDataGenerator; only check for null state
		specialEquals.put(ROCDataGenerator.class, NULL_CHECK_EQUALS);
		// non primitive arrays (includes mutli-dim primitive arrays)
		specialEquals.put(Object[].class, (a, b) -> Arrays.deepEquals((Object[]) a, (Object[]) b));
		// register all other (primitive) array equals methods
		for (Method method : Arrays.class.getMethods()) {
			if (!method.getName().equals("equals") || method.getParameterCount() != 2 || method.getReturnType() != boolean.class) {
				continue;
			}
			Class<?>[] parameterTypes = method.getParameterTypes();
			if (parameterTypes[0] != parameterTypes[1] || !parameterTypes[0].isArray() || !parameterTypes[0].getComponentType().isPrimitive()) {
				continue;
			}
			specialEquals.put(parameterTypes[0], (a, b) -> {
				try {
					return (boolean) method.invoke(null, a, b);
				} catch (Exception e) {
					return false;
				}
			});
		}
	}

	/** For each (known) subclass of {@link Averagable}, tests cloneability */
	@Test
	public void testCloning() {
		for (Class<? extends Averagable> aClass : AVERAGABLE_SUBCLASSES) {
			try {
				aClass.getConstructor(aClass);
			} catch (NoSuchMethodException e) {
				fail("No copy constructor for " + aClass);
			}
			if (Modifier.isAbstract(aClass.getModifiers())) {
				continue;
			}
			testCloningOnObjects(aClass);
		}
	}

	/** For each constructor of the given class, tests if the cloning can be done properly. */
	private void testCloningOnObjects(Class<? extends Averagable> aClass) {
		for (Constructor<?> constructor : aClass.getConstructors()) {
			int pCount = constructor.getParameterCount();
			Class<?>[] parameterTypes = constructor.getParameterTypes();
			if (pCount == 0 || pCount == 1 && parameterTypes[0] == aClass) {
				continue;
			}
			Object[] params = new Object[pCount];
			for (int i = 0; i < parameterTypes.length; i++) {
				params[i] = defaultValueMap.get(parameterTypes[i]);
				assertNotNull("No default for class " + parameterTypes[i], params[i]);
			}
			Averagable original;
			try {
				original = (Averagable) constructor.newInstance(params);
			} catch (Exception e) {
				e.printStackTrace();
				fail("Error creating " + aClass + " with " + constructor);
				continue;
			}
			Averagable clone;
			try {
				clone = (Averagable) original.clone();
			} catch (CloneNotSupportedException e) {
				fail("Cloning not possible: " + aClass);
				continue;
			}
			testEqualityOfClone(aClass, original, clone);
			// test cloning after statistics are counted if possible
			if (original instanceof MeasuredPerformance) {
				try {
					MeasuredPerformance performance = (MeasuredPerformance) original;
					ExampleSet set = (ExampleSet) defaultValueMap.get(ExampleSet.class);
					performance.startCounting(set, false);
					for (Example example : set) {
						performance.countExample(example);
					}
					try {
						clone = (Averagable) original.clone();
					} catch (CloneNotSupportedException e) {
						fail("Cloning not possible after counting: " + aClass);
						continue;
					}
					testEqualityOfClone(aClass, original, clone);
				} catch (OperatorException e) {
					// ignore
					return;
				}
			}
		}
	}

	/**
	 * Check if all fields are the same/similar after cloning; goes through all super class fields, too
	 * relies {@link #specialEquals} map or Objects{@link #equals(Object)}
	 */
	private void testEqualityOfClone(Class<? extends Averagable> aClass, Averagable original, Averagable clone) {
		Class c = aClass;
		while (Averagable.class.isAssignableFrom(c)) {
			for (Field field : c.getDeclaredFields()) {
				field.setAccessible(true);
				try {
					Object originalField = field.get(original);
					Object clonedField = field.get(clone);
					if (!NULL_CHECK_EQUALS.test(originalField, clonedField)) {
						fail("Cloning field " + field + " failed: unexpected (non) null value "
								+ originalField + "/" + clonedField);
					}
					if (originalField == null) {
						continue;
					}
					Class<?> ofc = originalField.getClass();
					BiPredicate<Object, Object> equals = specialEquals.getOrDefault(ofc, Objects::equals);
					assertTrue("Cloning field " + field + " failed", equals.test(originalField, clonedField));
				} catch (IllegalAccessException e) {
					// ignore
				}
			}
			c = c.getSuperclass();
		}
	}

	@AfterClass
	public static void tearDown() {
		defaultValueMap = null;
		specialEquals = null;
	}

	/** Create default values for different classes */
	private static Map<Class, Object> createDefaultValueMap() {
		Map<Class, Object> parameterMap = new HashMap<>();
		parameterMap.put(boolean.class, true);
		parameterMap.put(int.class, 1);
		parameterMap.put(int[].class, new int[] {1, 2, 3});
		parameterMap.put(float.class, 1f);
		parameterMap.put(double.class, 1d);
		// arbitrary double[] and double[][]
		double[] ds = {1, 2, 3};
		parameterMap.put(double[].class, ds);
		parameterMap.put(double[][].class, new double[][] {ds, ds, ds});
		parameterMap.put(String.class, "x");
		parameterMap.put(AttributeWeights.class, new AttributeWeights());
		parameterMap.put(ROCBias.class, ROCBias.NEUTRAL);
		// one normal attribute, a label and a prediction
		Attribute att = AttributeFactory.createAttribute("att", Ontology.REAL);
		Attribute label = AttributeFactory.createAttribute(Attributes.LABEL_NAME, Ontology.NOMINAL);
		for (String value : LABEL_PRED_VALUES) {
			label.getMapping().mapString(value);
		}
		Attribute prediction = AttributeFactory.createAttribute(Attributes.PREDICTION_NAME, Ontology.NOMINAL);
		for (String value : LABEL_PRED_VALUES) {
			prediction.getMapping().mapString(value);
		}
		parameterMap.put(Attribute.class, label);
		// empty small example set
		ExampleSet set = ExampleSets.from(att, label, prediction).withRole(label, label.getName())
				.withRole(prediction, prediction.getName()).withBlankSize(10).build();
		parameterMap.put(ExampleSet.class, set);
		parameterMap.put(ParameterHandler.class, set);
		// map (String, int) [ClassificationCostCriterion]
		HashMap map = new HashMap();
		int i = 1;
		for (String value : LABEL_PRED_VALUES) {
			map.put(value, i++);
		}
		parameterMap.put(Map.class, map);
		// arbitrary delegate [MinMaxCriterion]
		parameterMap.put(MeasuredPerformance.class, new Margin());
		return parameterMap;
	}

}