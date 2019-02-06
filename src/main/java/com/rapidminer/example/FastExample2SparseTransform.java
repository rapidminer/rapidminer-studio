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

import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.SparseDataRow;
import com.rapidminer.operator.UserError;

import java.util.Arrays;


/**
 * This class can be used for the efficient generation of sparse example formats. The constructor
 * creates a mapping between example set and example table attribute indices which only need to be
 * performed once. The sparse data can then be queried efficiently by using the methods
 * {@link #getNonDefaultAttributeIndices(Example)} and
 * {@link #getNonDefaultAttributeValues(Example)}. Please note that this filter should be
 * reinstatiated for new example sets. Furthermore, a gain in performance is only achieved for
 * examples with underlying {@link SparseDataRow}s.
 * 
 * @author Julien Nioche, Ingo Mierswa ingomierswa Exp $
 */
public class FastExample2SparseTransform {

	/**
	 * The mapping between the attribute indices in the data row / example table on the attribute
	 * indices of the given example set. This mapping is only necessary for examples backed up by
	 * {@link SparseDataRow}s.
	 */
	private int[] mapping;

	/**
	 * This attribute array is necessary since in example sets only iterators are provided for
	 * attributes.
	 */
	private Attribute[] attributes;

	/**
	 * The complete array of all attribute indices which can be used for data rows which do not
	 * implement {@link SparseDataRow}.
	 */
	private int[] allIndices;

	/**
	 * Returns for a table giving the equivalence between the positions of the Attributes in the
	 * ExampleTable and the number of the regular Attributes in the ExampleSet. A value of -1
	 * indicates that the Attribute is not regular or has been deleted (is null). This is used in
	 * order to optimize the access to sparse DataRows (e.g. SVM implementations or for Weka), which
	 * is important when the number of Attributes is large.
	 * 
	 * @throws UserError
	 */
	public FastExample2SparseTransform(ExampleSet es) throws UserError {
		// init
		this.mapping = new int[es.getExampleTable().getNumberOfAttributes()];
		for (int i = 0; i < mapping.length; i++) {
			mapping[i] = -1;
		}

		// create mappings
		int pos = 0;
		this.attributes = new Attribute[es.getAttributes().size()];
		this.allIndices = new int[es.getAttributes().size()];
		for (Attribute attribute : es.getAttributes()) {
			int tableIndex = attribute.getTableIndex();
			if (tableIndex != Attribute.VIEW_ATTRIBUTE_INDEX) {
				this.mapping[attribute.getTableIndex()] = pos;
				this.attributes[pos] = attribute;
				this.allIndices[pos] = pos;
				pos++;
			} else {
				throw new UserError(null, 140);
			}
		}

		// trunkate allIndices array
		this.allIndices = Arrays.copyOf(this.allIndices, pos);

		// trim is necessary in order to allow fast mapping!
		for (Example e : es) {
			e.getDataRow().trim();
		}
	}

	/**
	 * Returns a list with the indices of the regular Attributes with non-default values. This can
	 * be used for a faster construction of sparse dataset representations when the number of
	 * Attributes is large. The positions of attributes are sorted by ascending number.
	 */
	public int[] getNonDefaultAttributeIndices(Example example) {
		int numberNonDefaultAttributes = 0;
		DataRow data = example.getDataRow();
		if (data instanceof SparseDataRow) {
			int[] nonDefaultInd = ((SparseDataRow) (data)).getNonDefaultIndices();
			int[] tempArray = new int[nonDefaultInd.length];
			// map between the positive indices in the table
			// and the corresponding attribute positions
			for (int i = 0; i < nonDefaultInd.length; i++) {
				int nextPos = mapping[nonDefaultInd[i]];
				if (nextPos != -1) {
					tempArray[numberNonDefaultAttributes++] = nextPos;
				}
			}
			// trim the array and sort it
			int[] finalArray = new int[numberNonDefaultAttributes];
			System.arraycopy(tempArray, 0, finalArray, 0, numberNonDefaultAttributes);
			// the positions have to be sorted for the sparse data
			Arrays.sort(finalArray);
			return finalArray;
		} else {
			int[] tempArray = new int[allIndices.length];
			for (Attribute a : example.getAttributes()) {
				int nextPos = mapping[a.getTableIndex()];
				// check for view attribute and zero value
				// default value should not be used, since both libsvm and fast large margin solve
				// LPs, so a value other than 0 would make an impact
				if (nextPos != -1 && example.getValue(a) != 0) {
					tempArray[numberNonDefaultAttributes++] = nextPos;
				}
			}
			// trim the array and sort it
			int[] finalArray = new int[numberNonDefaultAttributes];
			System.arraycopy(tempArray, 0, finalArray, 0, numberNonDefaultAttributes);
			// the positions have to be sorted for the sparse data
			Arrays.sort(finalArray);
			return finalArray;
		}
	}

	/**
	 * Returns an array of non-default values of the given example. These are only the values of
	 * regular attributes. Simply invokes {@link #getNonDefaultAttributeValues(Example, int[])} with
	 * the array of non-default indices for the given example.
	 */
	public double[] getNonDefaultAttributeValues(Example example) {
		return getNonDefaultAttributeValues(example, getNonDefaultAttributeIndices(example));
	}

	/**
	 * Returns an array of non-default values of the given example. These are only the values of
	 * regular attributes. The size of the returned array is the same as the size of the given
	 * indices array.
	 */
	public double[] getNonDefaultAttributeValues(Example example, int[] nonDefaultIndices) {
		double[] result = new double[nonDefaultIndices.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = example.getValue(this.attributes[nonDefaultIndices[i]]);
		}
		return result;
	}
}
