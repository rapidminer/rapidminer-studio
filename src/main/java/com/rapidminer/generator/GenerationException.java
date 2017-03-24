/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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
package com.rapidminer.generator;

import com.rapidminer.operator.OperatorException;


/**
 * This exception will be thrown if an error occurs during the generation of new features.
 * 
 * @author Ingo Mierswa, Simon Fischer Exp $
 */
public class GenerationException extends OperatorException {

	private static final long serialVersionUID = -2760374156089530715L;

	public GenerationException(String str) {
		super(str);
	}

	public GenerationException(String str, Exception e) {
		super(str, e);
	}

}
