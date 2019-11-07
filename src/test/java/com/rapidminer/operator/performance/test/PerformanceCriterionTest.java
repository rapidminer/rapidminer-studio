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
package com.rapidminer.operator.performance.test;

import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.WrapperOperatorRuntimeException;
import com.rapidminer.operator.performance.MeasuredPerformance;
import com.rapidminer.operator.performance.PerformanceCriterion;


/**
 * Tests for {@link PerformanceCriterion}, mainly comparisons.
 *
 * @author Jan Czogalla
 * @since 8.2
 */
public class PerformanceCriterionTest {


	/**
	 * Test different classes of {@link PerformanceCriterion}.
	 */
	@Test
	public void compareToDifferentClassesTest() {
		PerformanceCriterion pcA = Mockito.mock(PerformanceCriterion.class);
		PerformanceCriterion pcB = Mockito.mock(MeasuredPerformance.class);
		PerformanceCriterion[] pcs = new PerformanceCriterion[]{pcA, pcB};
		for (PerformanceCriterion pc : pcs) {
			Mockito.when(pc.compareTo(Mockito.any())).thenCallRealMethod();
		}
		for (int i = 0; i < pcs.length; i++) {
			testCompareForErrors(pcs[i], pcs[1 - i], "performance_criterion_class_mismatch", o -> o.getClass().getName());
		}
	}

	/** Test two mismatched {@link PerformanceCriterion} instances. */
	private void testCompareForErrors(PerformanceCriterion pcA, PerformanceCriterion pcB, String i18nKey, Function<PerformanceCriterion, String> nameProvider) {
		try {
			pcA.compareTo(pcB);
		} catch (WrapperOperatorRuntimeException e) {
			String expectedError = OperatorException.getErrorMessage(i18nKey,
					new Object[]{nameProvider.apply(pcA), nameProvider.apply(pcB)});
			Assert.assertEquals(expectedError, e.getMessage());
		}
	}

	/**
	 * Test different subtypes of {@link PerformanceCriterion}.
	 */
	@Test
	public void compareToDifferentTypesTest() {
		PerformanceCriterion pcA = Mockito.mock(PerformanceCriterion.class);
		Mockito.when(pcA.getName()).thenReturn("Type A");
		PerformanceCriterion pcB = Mockito.mock(PerformanceCriterion.class);
		Mockito.when(pcB.getName()).thenReturn("Type B");
		PerformanceCriterion[] pcs = new PerformanceCriterion[]{pcA, pcB};
		for (PerformanceCriterion pc : pcs) {
			Mockito.when(pc.compareTo(Mockito.any())).thenCallRealMethod();
		}
		for (int i = 0; i < pcs.length; i++) {
			testCompareForErrors(pcs[i], pcs[1 - i], "performance_criterion_type_mismatch", PerformanceCriterion::getName);
		}
	}

	/**
	 * Test comparable subtypes of {@link PerformanceCriterion}.
	 */
	@Test
	public void compareToSameTypes() {
		PerformanceCriterion pcA = Mockito.mock(PerformanceCriterion.class);
		Mockito.when(pcA.getName()).thenReturn("Type A");
		Mockito.when(pcA.getFitness()).thenReturn(0d);
		Mockito.when(pcA.compareTo(Mockito.any())).thenCallRealMethod();
		PerformanceCriterion pcB = Mockito.mock(PerformanceCriterion.class);
		Mockito.when(pcB.getName()).thenReturn("Type A");
		Mockito.when(pcB.getFitness()).thenReturn(1d);
		Mockito.when(pcB.compareTo(Mockito.any())).thenCallRealMethod();
		PerformanceCriterion pcC = Mockito.mock(PerformanceCriterion.class);
		Mockito.when(pcC.getName()).thenReturn("Type A");
		Mockito.when(pcC.getFitness()).thenReturn(Double.NaN);
		Mockito.when(pcC.compareTo(Mockito.any())).thenCallRealMethod();
		Assert.assertEquals("Not comparable to itself", 0, pcA.compareTo(pcA));
		Assert.assertEquals("Not comparable to itself", 0, pcB.compareTo(pcB));
		Assert.assertEquals("Not comparable to itself", 0, pcC.compareTo(pcC));
		Assert.assertEquals("Comparison of fitness is incorrect", -1, pcA.compareTo(pcB));
		Assert.assertEquals("Comparison of fitness is incorrect", 1, pcB.compareTo(pcA));
		Assert.assertEquals("Comparison of fitness is incorrect", 1, pcA.compareTo(pcC));
		Assert.assertEquals("Comparison of fitness is incorrect", 1, pcB.compareTo(pcC));
		Assert.assertEquals("Comparison of fitness is incorrect", -1, pcC.compareTo(pcA));
		Assert.assertEquals("Comparison of fitness is incorrect", -1, pcC.compareTo(pcB));

	}

}