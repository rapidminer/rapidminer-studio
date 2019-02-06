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
package com.rapidminer.gui.plotter.charts;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.Map;

import org.jfree.chart.labels.AbstractCategoryItemLabelGenerator;
import org.jfree.chart.labels.CategoryItemLabelGenerator;
import org.jfree.data.category.CategoryDataset;
import org.jfree.util.PublicCloneable;


/**
 * This is the item label generator for the Pareto chart plotter.
 * 
 * @author Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class ParetoChartItemLabelGenerator extends AbstractCategoryItemLabelGenerator implements CategoryItemLabelGenerator,
		Cloneable, PublicCloneable, Serializable {

	private static final long serialVersionUID = 3475336746667135258L;

	/** The default format string. */
	public static final String DEFAULT_LABEL_FORMAT_STRING = "{2}";

	private Map<String, String> itemLabels;

	/**
	 * Creates a new generator with a default number formatter.
	 */
	public ParetoChartItemLabelGenerator(Map<String, String> itemLabels) {
		super(DEFAULT_LABEL_FORMAT_STRING, NumberFormat.getInstance());
		this.itemLabels = itemLabels;
	}

	/**
	 * Generates the label for an item in a dataset. Note: in the current dataset implementation,
	 * each row is a series, and each column contains values for a particular category.
	 * 
	 * @param dataset
	 *            the dataset (<code>null</code> not permitted).
	 * @param row
	 *            the row index (zero-based).
	 * @param column
	 *            the column index (zero-based).
	 * 
	 * @return The label (possibly <code>null</code>).
	 */
	@Override
	public String generateLabel(CategoryDataset dataset, int row, int column) {
		String key = (String) dataset.getColumnKey(column);
		return itemLabels.get(key);
	}
}
