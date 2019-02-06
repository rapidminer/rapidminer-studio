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
package com.rapidminer;

import com.rapidminer.operator.Operator;


/**
 * Listens to events during the run of an process.
 * 
 * @author Ingo Mierswa Exp $
 */
public interface ProcessListener {

	/** Will be invoked during process start. */
	public void processStarts(Process process);

	/** Will be invoked every time another operator is started in the process. */
	public void processStartedOperator(Process process, Operator op);

	/** Will be invoked every time an operator is finished */
	public void processFinishedOperator(Process process, Operator op);

	/** Will invoked when the process was successfully finished. */
	public void processEnded(Process process);

}
