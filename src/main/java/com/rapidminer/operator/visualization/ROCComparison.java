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
package com.rapidminer.operator.visualization;

import com.rapidminer.operator.ResultObjectAdapter;
import com.rapidminer.tools.math.ROCData;

import java.util.List;
import java.util.Map;


/**
 * This object can usually not be passed to other operators but can simply be used for the inline
 * visualization of a ROC comparison plot (without a dialog).
 * 
 * @author Ingo Mierswa
 */
public class ROCComparison extends ResultObjectAdapter {

	private static final long serialVersionUID = 9181453276271041294L;

	private Map<String, List<ROCData>> rocData;

	public ROCComparison(Map<String, List<ROCData>> rocData) {
		this.rocData = rocData;
	}

	@Override
	public String getName() {
		return "ROC Comparison";
	}

	@Override
	public String toString() {
		return "A comparison visualization based on the ROC plots for different classification schemes.";
	}

	public String getExtension() {
		return "roc";
	}

	public String getFileDescription() {
		return "ROC comparison files";
	}

	public Map<String, List<ROCData>> getRocData() {
		return this.rocData;
	}
}
