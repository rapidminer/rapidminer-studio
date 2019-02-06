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
package com.rapidminer.operator;

import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;


/**
 * This interface extends IOObject and is hence an object which can be handled by operators.
 * Additionally this object is a result and can be of interest for a user. ResultWriters can write
 * the results in a result file.
 * 
 * @see com.rapidminer.operator.io.ResultWriter
 * @author Ingo Mierswa
 */
public interface ResultObject extends IOObject {

	/** Defines the name of this result object. */
	public abstract String getName();

	/** Result string will be displayed in result files written with a ResultWriter operator. */
	public abstract String toResultString();

	/** Returns an icon used for displaying the results. May return null. */
	public abstract Icon getResultIcon();

	/**
	 * Returns a list of actions (e.g. "save") that is displayed below (or near to) the
	 * visualisation component.
	 * 
	 * @deprecated Action concept for GUI components removed from result objects
	 */
	@Deprecated
	public abstract List<Action> getActions();

}
