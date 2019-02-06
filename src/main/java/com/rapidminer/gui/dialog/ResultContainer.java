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
package com.rapidminer.gui.dialog;

import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.MissingIOObjectException;
import com.rapidminer.operator.performance.PerformanceCriterion;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.tools.math.TestGroup;


/**
 * This container class contains an operator tree and the results delivered by the tree. It will be
 * used by the class {@link ResultHistory}.
 * 
 * @author Ingo Mierswa
 */
public class ResultContainer {

	private String name;
	private String processXML;
	private String resultString;

	private TestGroup group = null;

	public ResultContainer(String name, String processXML, IOContainer ioContainer) {
		this.name = name;
		this.processXML = processXML;
		this.resultString = ioContainer.toString();
		try {
			PerformanceVector performanceVector = ioContainer.get(PerformanceVector.class);
			PerformanceCriterion criterion = performanceVector.getMainCriterion();
			if (criterion != null) {
				this.group = new TestGroup(criterion.getExampleCount(), criterion.getAverage(), criterion.getVariance());
			} else {
				this.group = null;
			}
		} catch (MissingIOObjectException e) {
			// tries to find a performance. Ok if this does not work
		}
	}

	public String getName() {
		return this.name;
	}

	public String getProcess() {
		return this.processXML;
	}

	public String getResults() {
		return this.resultString;
	}

	public TestGroup getTestGroup() {
		return this.group;
	}

	@Override
	public String toString() {
		return this.name;
	}
}
