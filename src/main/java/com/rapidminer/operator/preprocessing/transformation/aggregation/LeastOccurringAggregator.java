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
 * This is an {@link Aggregator} for the {@link LeastOccurringAggregationFunction}. If the least
 * value that at least occurrs once is not unique, the first value from the nominal mapping will be
 * used.
 * 
 * @author Sebastian Land
 */
public class LeastOccurringAggregator extends LeastAggregator {

	public LeastOccurringAggregator(AggregationFunction function) {
		super(function);
	}


	/**
	 * @return always {@code false}
	 */
	@Override
	protected boolean handlesZeroMapEntries() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 * Additionally checks that the frequency is bigger than zero.
	 */
	@Override
	protected boolean testCriteria(double frequency, double resultFrequency) {
		return frequency > 0 && super.testCriteria(frequency, resultFrequency);
	}

	/**
	 * @return always {@code 1}
	 */
	@Override
	protected int absoluteExtremum() {
		return 1;
	}
}
