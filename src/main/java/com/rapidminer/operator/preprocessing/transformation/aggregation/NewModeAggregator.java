/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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
package com.rapidminer.operator.preprocessing.transformation.aggregation;

/**
 * This is an {@link Aggregator} for the {@link ModeAggregationFunction}. If the mode value is not unique, the first
 * value from the nominal mapping will be used. If the frequency of missing values is higher than any other mapping, the
 * missing value will be used.
 *
 * @author Jan Czogalla
 * @since 9.7
 */
public class NewModeAggregator extends LeastAggregator {

	private final boolean handlesMissings;

	public NewModeAggregator(AggregationFunction function) {
		super(function);
		handlesMissings = !function.isIgnoringMissings();
	}

	/**
	 * @return always 0
	 */
	@Override
	protected double getStartFrequency() {
		return 0;
	}

	/**
	 * @return always {@code false}
	 */
	@Override
	protected boolean handlesZeroMapEntries() {
		return false;
	}

	/**
	 * @return whether the function ignores missings or not
	 */
	@Override
	protected boolean handlesMissings() {
		return handlesMissings;
	}

	/**
	 * Tests the most criteria
	 */
	@Override
	protected boolean testCriteria(double frequency, double resultFrequency) {
		return frequency > resultFrequency;
	}

	/**
	 * @return always {@link Integer#MAX_VALUE}
	 */
	@Override
	protected int absoluteExtremum() {
		return Integer.MAX_VALUE;
	}
}
