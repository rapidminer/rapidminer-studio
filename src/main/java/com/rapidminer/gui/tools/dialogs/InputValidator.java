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
package com.rapidminer.gui.tools.dialogs;

/**
 * Simple validation interface for generic input.
 *
 * @param <T>
 *            the type of the input
 * @author Marcel Michel
 * @since 7.0.0
 */
public interface InputValidator<T> {

	/**
	 * Validates the given input string.
	 *
	 * @param input
	 *            the input which should be validated
	 * @return An human readable error message if the input is not valid, otherwise {@code null}
	 */
	public String validateInput(T input);

}
