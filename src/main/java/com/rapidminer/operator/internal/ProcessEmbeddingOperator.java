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
package com.rapidminer.operator.internal;

/**
 * Marker interface for <em>Execute Process</em>.
 *
 * @author Michael Knopf
 * @since 6.5.0
 */
public interface ProcessEmbeddingOperator {

	/** Key used to identify <em>Execute Process</em>. */
	public static final String OPERATOR_KEY = "productivity:execute_process";

	/** The parameter name for the process file which should be encapsulated by this operator. */
	public static final String PARAMETER_PROCESS_FILE = "process_location";

}
