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
package com.rapidminer.example;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import com.rapidminer.example.table.ExampleTable;
import com.rapidminer.operator.ResultObject;


/**
 * Interface definition for all example sets. Usually, example sets do not contain any data but are
 * only views on an example table (for example for sampling or feature selection purposes). It
 * should be possible to create a layered view on the data, hence the name multi-layered data view.
 * ExampleSet implementation should support this view concept.
 *
 * @author Ingo Mierswa
 */
public interface ExampleSet extends ResultObject, Cloneable, Iterable<Example> {

	/** necessary since default method was added */
	static final long serialVersionUID = 4100925167567270064L;

	// ------------- Misc -----------------------------

	/** Clones the example set. */
	public Object clone();

	/** True if all attributes are equal. */
	@Override
	public boolean equals(Object o);

	/**
	 * Returns the hash code. Two example sets must deliver the same hash code if they are equal.
	 */
	@Override
	public int hashCode();

	/**
	 * Frees unused resources, if supported by the implementation. Does nothing by default.
	 *
	 * Should only be used on freshly {@link #clone}ed {@link ExampleSet}s to ensure that the
	 * cleaned up resources are not requested afterwards.
	 *
	 * @since 7.3
	 */
	public default void cleanup() {
		// does nothing by default
	}

	// -------------------- attributes --------------------

	/**
	 * Returns the data structure holding all attributes. NOTE! if you intend to iterate over all
	 * Attributes of this ExampleSet then you need to create an Iterator by calling
	 * {@link Attributes#allAttributes getAttributes().allAttributes()} and use it instead.
	 */
	public Attributes getAttributes();

	// -------------------- Examples --------------------

	/**
	 * Returns the number of examples in this example set. This number should not be used to create
	 * for-loops to iterate through all examples.
	 */
	public int size();

	/**
	 * Returns the underlying example table. Most operators should operate on the example set and
	 * manipulate example to change table data instead of using the table directly.
	 */
	public ExampleTable getExampleTable();

	/**
	 * Returns the example with the given id value. If the example set does not contain an id
	 * attribute this method should return null. Call {@link #remapIds()} before using this method.
	 */
	public Example getExampleFromId(double value);

	/**
	 * Returns all examples which have the given id. Should return null in the case that there are
	 * no examples matching that id.
	 */
	public int[] getExampleIndicesFromId(double value);

	/**
	 * Returns the i-th example. It is not guaranteed that asking for an example by using the index
	 * in the example table is efficiently implemented. Therefore for-loops for iterations are not
	 * an option and an {@link ExampleReader} should be used.
	 */
	public Example getExample(int index);

	/**
	 * Remaps all ids. This method should be invoked before the method
	 * {@link #getExampleFromId(double)} is used.
	 */
	public void remapIds();

	// -------------------- File Writing --------------------

	/** Writes the data and the attribute description to a file. */
	public void writeDataFile(File dataFile, int fractionDigits, boolean quoteNominal, boolean zipped, boolean append,
			Charset encoding) throws IOException;

	/**
	 * Writes the attribute meta descriptions into a file. The data file is used in order to
	 * determine the relative file positions and is not allowed to be null.
	 */
	public void writeAttributeFile(File attFile, File dataFile, Charset encoding) throws IOException;

	/**
	 * Writes the data and the attribute description to a sparse data file.
	 *
	 * @param dataFile
	 *            the file to write the data to
	 * @param format
	 *            specified by {@link com.rapidminer.operator.io.SparseFormatExampleSource}
	 * @param fractionDigits
	 *            the number of fraction digits (-1 for all possible digits)
	 */
	public void writeSparseDataFile(File dataFile, int format, int fractionDigits, boolean quoteNominal, boolean zipped,
			boolean append, Charset encoding) throws IOException;

	/**
	 * Writes the attribute meta descriptions for a sparse data file into a file. The data file is
	 * used in order to determine the relative file positions and is not allowed to be null.
	 *
	 * @param format
	 *            specified by {@link com.rapidminer.operator.io.SparseFormatExampleSource}
	 */
	public void writeSparseAttributeFile(File attFile, File dataFile, int format, Charset encoding) throws IOException;

	// ------------------- Statistics ---------------

	/** Recalculate all attribute statistics. */
	public void recalculateAllAttributeStatistics();

	/** Recalculate the attribute statistics of the given attribute. */
	public void recalculateAttributeStatistics(Attribute attribute);

	/**
	 * Returns the desired statistic for the given attribute. This method should be preferred over
	 * the deprecated method Attribute#getStatistics(String) since it correctly calculates and keep
	 * the statistics for the current example set and does not overwrite the statistics in the
	 * attribute.
	 */
	public double getStatistics(Attribute attribute, String statisticsName);

	/**
	 * Returns the desired statistic for the given attribute. This method should be preferred over
	 * the deprecated method Attribute#getStatistics(String) since it correctly calculates and keep
	 * the statistics for the current example set and does not overwrite the statistics in the
	 * attribute.
	 */
	public double getStatistics(Attribute attribute, String statisticsName, String statisticsParameter);

}
