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
package com.rapidminer.studio.concurrency.internal.util;

import com.rapidminer.operator.Operator;
import com.rapidminer.tools.Tools;


/**
 * Entity which holds an operator, the corresponding apply count and start time.
 * <p>
 * Note that this part of the API is only temporary and might be removed in future versions again.
 * </p>
 *
 * @author Sebastian Land
 * @since 7.4
 */
public class ProcessExecutionStackEntry {

	private String operatorName;
	private int operatorApplyCount;
	private long startTime;

	public ProcessExecutionStackEntry(Operator operator) {
		this.operatorName = operator.getName();
		this.operatorApplyCount = operator.getApplyCount();
		this.startTime = System.currentTimeMillis();
	}

	public String getOperatorName() {
		return operatorName;
	}

	public int getOperatorApplyCount() {
		return operatorApplyCount;
	}

	public String getRuntimeAsString() {
		return Tools.formatDuration(System.currentTimeMillis() - startTime);
	}
}
