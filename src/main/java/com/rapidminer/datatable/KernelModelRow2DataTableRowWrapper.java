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
package com.rapidminer.datatable;

import com.rapidminer.operator.learner.functions.kernel.KernelModel;


/**
 * This class wraps the data row of a kernel model.
 * 
 * @author Ingo Mierswa
 */
public class KernelModelRow2DataTableRowWrapper implements DataTableRow {

	static final String[] SPECIAL_COLUMN_NAMES = { "counter", "label", "function value", "alpha", "abs(alpha)",
			"support vector" };

	public static final int COUNTER = 0;
	public static final int LABEL = 1;
	public static final int FUNCTION_VALUE = 2;
	public static final int ALPHA = 3;
	public static final int ABS_ALPHA = 4;
	public static final int SUPPORT_VECTOR = 5;

	public static final int NUMBER_OF_SPECIAL_COLUMNS = 6;

	private KernelModel kernelModel;
	private DataTableKernelModelAdapter adapter;
	private int index;

	public KernelModelRow2DataTableRowWrapper(KernelModel kernelModel, DataTableKernelModelAdapter adapter, int index) {
		this.kernelModel = kernelModel;
		this.adapter = adapter;
		this.index = index;
	}

	@Override
	public String getId() {
		return this.kernelModel.getId(index);
	}

	@Override
	public int getNumberOfValues() {
		return kernelModel.getNumberOfAttributes() + NUMBER_OF_SPECIAL_COLUMNS;
	}

	@Override
	public double getValue(int column) {
		switch (column) {
			case COUNTER:
				return index;
			case LABEL:
				if (this.kernelModel.isClassificationModel()) {
					String label = this.kernelModel.getClassificationLabel(index);
					return adapter.mapString(LABEL, label);
				} else {
					return this.kernelModel.getRegressionLabel(index);
				}
			case FUNCTION_VALUE:
				return this.kernelModel.getFunctionValue(index);
			case ALPHA:
				return this.kernelModel.getAlpha(index);
			case ABS_ALPHA:
				return Math.abs(this.kernelModel.getAlpha(index));
			case SUPPORT_VECTOR:
				return Math.abs(this.kernelModel.getAlpha(index)) != 0.0d ? 1 : 0;
			default:
				return this.kernelModel.getAttributeValue(index, column - NUMBER_OF_SPECIAL_COLUMNS);
		}
	}
}
