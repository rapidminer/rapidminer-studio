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
package com.rapidminer.example.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.IntToDoubleFunction;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.SimpleExampleSet;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.table.DataRowReader;
import com.rapidminer.example.table.ExampleTable;


/**
 * Builds an {@link ExampleSet} from the given data starting with the given attributes. The
 * instructions to fill the underlying {@link ExampleTable} are applied in the following order:
 * <ul>
 * <li>rows in the order with which they are added by {@link #addDataRow} or {@link #addRow}</li>
 * <li>rows given by the reader specified by {@link #withDataRowReader}</li>
 * <li>the blank size {@link #withBlankSize}</li>
 * <li>columns are filled as specified by {@link #withColumnFiller}</li>
 * </ul>
 *
 * To prevent memory strain, the data rows added by {@link #addDataRow} and {@link #addRow} are
 * immediately written to the underlying data table instead of being stored in the builder. The
 * other data (set by {@link #withDataRowReader}, {@link #withBlankSize} or
 * {@link #withColumnFiller}) that fills the table is stored and only used on {@link #build}.
 *
 * If a special data management optimization type should be used
 * {@link #withOptimizationHint(DataManagment)} must be called before adding any rows.
 *
 * @author Gisa Schaefer
 * @since 7.3
 */
public abstract class ExampleSetBuilder {

	/**
	 * Data management optimization options that can be used with the builder option
	 * {@link ExampleSetBuilder#withOptimizationHint(DataManagement)}.
	 */
	public enum DataManagement {

		/**
		 * Always use the fastest data representation, regardless of memory usage.
		 */
		SPEED_OPTIMIZED,

		/**
		 * Automatically detects very sparse columns and compress those.
		 */
		AUTO,

		/**
		 * Decreases memory usage by compressing sparse columns as much as possible.
		 */
		MEMORY_OPTIMIZED;
	}

	/** all the attributes in the example set */
	private final List<Attribute> attributes;

	/** roles for some of the attributes */
	private final Map<Attribute, String> specialAttributes = new LinkedHashMap<>();

	/**
	 * Creates a builder based on the given attributes. The given attributes will be the attributes
	 * in the {@link ExampleTable} constructed with this builder. If the given attributes are
	 * {@code null}, the example will have no attributes.
	 *
	 * @param attributes
	 *            the {@link Attribute}s that the {@link ExampleSet} should contain, can be
	 *            {@code null}
	 */
	ExampleSetBuilder(List<Attribute> attributes) {
		this.attributes = new ArrayList<>();
		if (attributes != null) {
			this.attributes.addAll(attributes);
		}
	}

	/**
	 * Creates a builder based on the given attributes. The given attributes will be the attributes
	 * in the {@link ExampleTable} constructed with this builder.
	 *
	 * @param attributes
	 *            the {@link Attribute}s that the {@link ExampleSet} should contain
	 */
	ExampleSetBuilder(Attribute... attributes) {
		this.attributes = Arrays.asList(attributes);
	}

	/**
	 * Determines which of the attributes used to construct the builder should have a special role
	 * in the {@link ExampleSet} that is build.
	 *
	 * @param specialAttributes
	 *            map from attributes to their role names
	 * @return the builder
	 */
	public ExampleSetBuilder withRoles(Map<Attribute, String> specialAttributes) {
		this.specialAttributes.putAll(specialAttributes);
		return this;
	}

	/**
	 * Sets the role for the given attribute, which is one of the attributes used to construct the
	 * builder.
	 *
	 * @param attribute
	 *            the attribute that has a special role, must not be {@code null}
	 * @param role
	 *            the role name for the attribute
	 * @return the builder
	 */
	public ExampleSetBuilder withRole(Attribute attribute, String role) {
		if (attribute == null) {
			throw new IllegalArgumentException("The attribute must not be null");
		}
		specialAttributes.put(attribute, role);
		return this;
	}

	/**
	 * Sets the expected number of rows to numberOfRows. Does not construct any rows but ensures
	 * that the container for the rows has the right size and does not need to be resized while rows
	 * are added.
	 *
	 * @param numberOfRows
	 *            the expected number of rows
	 * @return the builder
	 */
	public abstract ExampleSetBuilder withExpectedSize(int numberOfRows);

	/**
	 * Adds the data row to the existing data rows of the data table. Will be applied before all the
	 * other table fillers ({@link #withDataRowReader}, {@link #withBlankSize},
	 * {@link #withColumnFiller} ).
	 *
	 * @param dataRow
	 *            the data row to add
	 * @return the builder
	 * @throws RuntimeException
	 *             May be thrown if the data row does not fit the attributes of the underlying
	 *             table, depending on the data row implementation.
	 */
	public abstract ExampleSetBuilder addDataRow(DataRow dataRow);

	/**
	 * Adds the data of the row as a new data row to the data table. Has the same effect as
	 * {@code addDataRow(new DoubleArrayDataRow(row))} but might be more efficient if supported by
	 * the underlying data structure. Will be applied before all the other table fillers (
	 * {@link #withDataRowReader}, {@link #withBlankSize}, {@link #withColumnFiller} ).
	 *
	 * @param row
	 *            the data to add
	 * @return the builder
	 */
	public abstract ExampleSetBuilder addRow(double[] row);

	/**
	 * Adds the rows supplied by the reader to the data table. Will be applied after any rows added
	 * by {@link #addDataRow} and {@link #addRow} but before creating blank rows specified by
	 * {@link #withBlankSize} and filling columns specified by {@link #withColumnFiller}. Calling
	 * the method a second time will overwrite the previous reader.
	 *
	 * @param reader
	 *            a {@link DataRowReader} providing rows for the data table
	 * @return the builder
	 */
	public abstract ExampleSetBuilder withDataRowReader(DataRowReader reader);

	/**
	 * Constructs numberOfRows blank rows. Creates rows of type
	 * {@link DataRowFactory.TYPE_DOUBLE_ARRAY} if supported by the underlying data structure.
	 *
	 * @param numberOfRows
	 *            the number of blank rows to create
	 * @return the builder
	 */
	public abstract ExampleSetBuilder withBlankSize(int numberOfRows);

	/**
	 * Fills the column in the data table associated with the given attribute with the values
	 * generated by the valueForRow function.
	 *
	 * Only has an effect if there are rows already constructed by {@link #addRow},
	 * {@link #addDataRow}, {@link #withDataRowReader} and {@link #withBlankSize}. Will be applied
	 * after those so it overwrites the values already set.
	 *
	 * @param attribute
	 *            the attribute for which the column should be filled by valueForRow
	 * @param valueForRow
	 *            the function to fill the column associated with the attribute
	 * @return the builder
	 */
	public abstract ExampleSetBuilder withColumnFiller(Attribute attribute, IntToDoubleFunction valueForRow);

	/**
	 * A hint for optimization type to use in the data table. Must be set before
	 * {@link #addDataRow(DataRow)} or {@link #addRow(double[])} is called. When this method is not
	 * used, {@link DataManagment#AUTO} is selected by default. May be ignored if not supported by
	 * the underlying data structure.
	 *
	 * @param management
	 *            the data management optimization to use
	 * @return the builder
	 */
	public abstract ExampleSetBuilder withOptimizationHint(DataManagement management);

	/**
	 * Builds the example set.
	 *
	 * @return the {@link ExampleSet} build from the specified data
	 */
	public ExampleSet build() {
		// Cannot provide map of special attributes directly to the constructor used below, because
		// we have to ensure that all attributes are being added to the regular attributes first so
		// that a bug in Attributes#getRole() does not take effect. This way,
		// Attributes#setSpecial() deletes the same attribute and not some other attribute (with
		// same role as first attribute's name).
		ExampleSet set = new SimpleExampleSet(getExampleTable(), attributes, null);
		Attributes attributes = set.getAttributes();
		for (Entry<Attribute, String> entry : specialAttributes.entrySet()) {
			attributes.setSpecialAttribute(entry.getKey(), entry.getValue());
		}
		return set;
	}

	/**
	 * @return the specified attributes
	 */
	protected List<Attribute> getAttributes() {
		return attributes;
	}

	/**
	 * Fetches the example table to use for building the example set. Will only be called once when
	 * the example set is build.
	 *
	 * @return the example table to use for the example set
	 */
	protected abstract ExampleTable getExampleTable();

	/**
	 * Sets the table indices so that {@code dataRow.set(attribute,value)} can be used for the
	 * attributes. Calling {@code new MemoryExampleTable(getAttributes)} or
	 * {@code new ColumnarExampleTable(getAttribute)} has the same effect.
	 */
	protected void setTableIndices() {
		int i = 0;
		for (Attribute attribute : getAttributes()) {
			attribute.setTableIndex(i++);
		}
	}

}
