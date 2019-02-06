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
package com.rapidminer.tools.expression.internal.antlr;

/**
 * {@link RuntimeException} thrown when the {@link CapitulatingFunctionExpressionLexer} or the
 * {@link CapitulatingErrorStrategy} encounters an error it does not want to recover from.
 *
 * @author Gisa Schaefer
 *
 */
class CapitulatingRuntimeException extends RuntimeException {

	private static final long serialVersionUID = -4281221436108519452L;

	/**
	 * Creates a {@link RuntimeException} that marks that a parser or lexer has encountered an error
	 * it does not want to recover from.
	 */
	CapitulatingRuntimeException() {
		super();
	}

}
