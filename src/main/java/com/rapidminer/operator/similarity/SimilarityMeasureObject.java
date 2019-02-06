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
package com.rapidminer.operator.similarity;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.ResultObjectAdapter;
import com.rapidminer.tools.math.similarity.DistanceMeasure;


/**
 * This is a wrapper around a {@link DistanceMeasure}.
 * 
 * @author Sebastian Land
 */
public class SimilarityMeasureObject extends ResultObjectAdapter {

	private static final long serialVersionUID = 5678007721570146770L;

	private DistanceMeasure measure;
	private ExampleSet exampleSet;

	public SimilarityMeasureObject(DistanceMeasure measure, ExampleSet exampleSet) {
		this.measure = measure;
		this.exampleSet = exampleSet;
	}

	public DistanceMeasure getDistanceMeasure() {
		return measure;
	}

	public ExampleSet getExampleSet() {
		return exampleSet;
	}

	public String getExtension() {
		return "sim";
	}

	public String getFileDescription() {
		return "Similarity Measure";
	}

	@Override
	public String toString() {
		return "Similarity based upon " + measure.toString();
	}
}
