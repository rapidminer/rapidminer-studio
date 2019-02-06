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
package com.rapidminer.tools.config.actions;

/**
 * Interface for the result of an action. This indicates if the result of the action as well as a
 * message for the user.
 * 
 * @author Marco Boeck
 * 
 */
public interface ActionResult {

	/**
	 * Outcome possiblities of an action. If the action has no clear success/failure state, use
	 * {@link Result#NONE}.
	 * 
	 */
	public enum Result {
		/** indicates that the action was successful */
		SUCCESS,

		/** indicates that the action has failed */
		FAILURE,

		/** indicates the action had no clear success/failure state */
		NONE
	}

	/**
	 * Returns the {@link Result} of the action.
	 * 
	 * @return
	 */
	public Result getResult();

	/**
	 * Returns a human readable message for the user which can be displayed once an action is
	 * complete.
	 * <p>
	 * The message should be <strong>short</strong> and precise, otherwise it might not fit in the
	 * UI.
	 * </p>
	 * 
	 * @return
	 */
	public String getMessage();
}
