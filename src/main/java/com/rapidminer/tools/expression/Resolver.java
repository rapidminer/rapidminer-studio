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

import java.util.Collection;
import java.util.Date;


/**
 * Resolver for variables and scope constants used in expressions. Handles some variables, their
 * meta data and their values.
 *
 * @author Gisa Schaefer
 * @since 6.5.0
 */
public interface Resolver {

	/**
	 * Returns the {@link FunctionInput}s of all variables known to this resolver.
	 *
	 * @return the {@link FunctionInput}s of all known variables
	 */
	public Collection<FunctionInput> getAllVariables();

	/**
	 * Returns the {@link ExpressionType} of the variable with name variableName or {@code null} if
	 * this variable does not exist.
	 *
	 * @param variableName
	 *            the name of the variable
	 * @return the type of the variable variableName or {@code null}
	 */
	public ExpressionType getVariableType(String variableName);

	/**
	 * Returns the String value of the variable variableName, if this variable has a String value.
	 * Check the expression type of the variable using {@link #getExpressionType()} before calling
	 * this method.
	 *
	 * @param variableName
	 *            the name of the variable
	 * @return the String value of the variable variableName, if this variable has a String value
	 * @throws IllegalStateException
	 *             if the variable is not of type {@link ExpressionType#STRING}
	 */
	public String getStringValue(String variableName);

	/**
	 * Returns the double value of the variable variableName, if this variable has a double value.
	 * Check the expression type of the variable using {@link #getExpressionType()} before calling
	 * this method.
	 *
	 * @param variableName
	 *            the name of the variable
	 * @return the double value of the variable variableName, if this variable has a double value
	 * @throws IllegalStateException
	 *             if the variable is not of type {@link ExpressionType#INTEGER} or
	 *             {@link ExpressionType#DOUBLE}
	 */
	public double getDoubleValue(String variableName);

	/**
	 * Returns the boolean value of the variable variableName, if this variable has a boolean value.
	 * Check the expression type of the variable using {@link #getExpressionType()} before calling
	 * this method.
	 *
	 * @param variableName
	 *            the name of the variable
	 * @return the boolean value of the variable variableName, if this variable has a boolean value
	 * @throws IllegalStateException
	 *             if the variable is not of type {@link ExpressionType#BOOLEAN}
	 */
	public boolean getBooleanValue(String variableName);

	/**
	 * Returns the Date value of the variable variableName, if this variable has a Date value. Check
	 * the expression type of the variable using {@link #getExpressionType()} before calling this
	 * method.
	 *
	 * @param variableName
	 *            the name of the variable
	 * @return the Date value of the variable variableName, if this variable has a Date value
	 * @throws IllegalStateException
	 *             if the variable is not of type {@link ExpressionType#DATE}
	 */
	public Date getDateValue(String variableName);
}
