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
package com.rapidminer.tools.expression;

import java.util.Date;


/**
 * Interface for a constant used inside an expression that is parsed by an {@link ExpressionParser}.
 *
 * @author Gisa Schaefer
 * @since 6.5.0
 */
public interface Constant {

	/**
	 * @return the {@link ExpressionType}
	 */
	public ExpressionType getType();

	/**
	 * @return the name
	 */
	public String getName();

	/**
	 * @return the string value if the constant has type {@link ExpressionType#STRING}
	 * @throws IllegalStateException
	 *             if the type is not {@link ExpressionType#STRING}
	 */
	public String getStringValue();

	/**
	 * @return the double value if the constant has type {@link ExpressionType#DOUBLE}
	 * @throws IllegalStateException
	 *             if the type is not {@link ExpressionType#DOUBLE}
	 */
	public double getDoubleValue();

	/**
	 * @return the boolean value if the constant has type {@link ExpressionType#BOOLEAN}
	 * @throws IllegalStateException
	 *             if the type is not {@link ExpressionType#BOOLEAN}
	 */
	public boolean getBooleanValue();

	/**
	 * @return the Date value if the constant has type {@link ExpressionType#DATE}
	 * @throws IllegalStateException
	 *             if the type is not {@link ExpressionType#DATE}
	 */
	public Date getDateValue();

	/**
	 * Returns the annotation of this constant, for example a description of a constant or where it
	 * is used.
	 *
	 * @return the annotation
	 */
	public String getAnnotation();

	/**
	 * @return is this {@link Constant} should be visible in the UI
	 */
	public boolean isInvisible();
}
