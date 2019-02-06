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
package com.rapidminer.operator.learner.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import com.rapidminer.core.concurrency.ConcurrencyContext;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.tree.criterions.ColumnCriterion;
import com.rapidminer.operator.learner.tree.criterions.Criterion;
import com.rapidminer.studio.internal.Resources;


/**
 * Used to calculate the benefit for splitting at a certain attribute.
 *
 * @author Gisa Schaefer
 *
 */
public class BenefitCalculator {

	private Operator operator;

	private ColumnExampleTable columnTable;

	private ColumnCriterion criterion;

	private ColumnNumericalSplitter splitter;

	public BenefitCalculator(ColumnExampleTable columnTable, ColumnCriterion criterion, Operator operator) {
		this(columnTable, criterion, operator, new ColumnNumericalSplitter(columnTable, criterion));
	}

	protected BenefitCalculator(ColumnExampleTable columnTable, ColumnCriterion criterion, Operator operator,
			ColumnNumericalSplitter splitter) {
		this.columnTable = columnTable;
		this.criterion = criterion;
		this.operator = operator;
		this.splitter = splitter;
	}

	/**
	 * This method calculates the benefit of the given attribute. This implementation utilizes the
	 * defined {@link Criterion}.
	 */
	private ParallelBenefit calculateBenefit(Map<Integer, int[]> allSelectedExamples, int attributeNumber) {
		if (columnTable.representsNominalAttribute(attributeNumber)) {
			return new ParallelBenefit(criterion.getNominalBenefit(columnTable,
					SelectionCreator.getArbitraryValue(allSelectedExamples), attributeNumber), attributeNumber);
		} else {
			// numerical attribute
			int[] selectedExamples = allSelectedExamples.get(attributeNumber);
			return splitter.getBestSplitBenefit(selectedExamples, attributeNumber);
		}
	}

	/**
	 * Calculates the benefits for all selected attributes on the given selected examples in
	 * parallel.
	 *
	 * @param allSelectedExamples
	 * @param selectedAttributes
	 * @return
	 * @throws OperatorException
	 */
	public List<ParallelBenefit> calculateAllBenefitsParallel(final Map<Integer, int[]> allSelectedExamples,
			final int[] selectedAttributes) throws OperatorException {
		ConcurrencyContext context = Resources.getConcurrencyContext(operator);

		final Vector<ParallelBenefit> benefits = new Vector<>();
		final int numberOfParallel = Math.min(context.getParallelism(), selectedAttributes.length);
		List<Callable<Void>> tasks = new ArrayList<>(numberOfParallel);

		for (int i = 0; i < numberOfParallel; i++) {
			final int counter = i;
			Callable<Void> task = new Callable<Void>() {

				@Override
				public Void call() {
					for (int j = counter; j < selectedAttributes.length; j += numberOfParallel) {

						int attribute = selectedAttributes[j];
						ParallelBenefit currentBenefit = calculateBenefit(allSelectedExamples, attribute);
						if (currentBenefit != null) {
							benefits.add(currentBenefit);
						}

					}
					return null;
				}
			};
			tasks.add(task);
		}

		try {
			context.call(tasks);
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException) {
				throw (RuntimeException) cause;
			} else if (cause instanceof Error) {
				throw (Error) cause;
			} else {
				throw new OperatorException(cause.getMessage(), cause);
			}

		}
		return benefits;
	}

	/**
	 * Calculates the benefits for all selected attributes on the given selected examples.
	 *
	 * @param allSelectedExamples
	 * @param selectedAttributes
	 * @return
	 */
	public List<ParallelBenefit> calculateAllBenefits(Map<Integer, int[]> allSelectedExamples, int[] selectedAttributes) {
		List<ParallelBenefit> benefits = new ArrayList<>();

		for (int attribute : selectedAttributes) {
			ParallelBenefit currentBenefit = calculateBenefit(allSelectedExamples, attribute);
			if (currentBenefit != null) {
				benefits.add(currentBenefit);
			}
		}

		return benefits;
	}

}
