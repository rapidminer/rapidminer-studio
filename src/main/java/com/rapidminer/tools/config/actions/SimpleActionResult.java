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
 * Simple implementation of the {@link ActionResult} interface which can be used to describe the
 * outcome of an action.
 * 
 * @author Marco Boeck
 * 
 */
public class SimpleActionResult implements ActionResult {

	/** the result of an action */
	private Result result;

	/** the message to describe the result */
	private String message;

	/**
	 * Creates a new {@link SimpleActionResult} instance.
	 * 
	 * @param message
	 * @param result
	 */
	public SimpleActionResult(String message, Result result) {
		this.message = message;
		this.result = result;
	}

	@Override
	public Result getResult() {
		return result;
	}

	@Override
	public String getMessage() {
		return message;
	}

}
