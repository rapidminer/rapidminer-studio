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
import java.util.concurrent.Callable;


/**
 * Interface for a container that holds Represents intermediate results when building a
 * {@link Expression} via a {@link ExpressionParser}.
 *
 * @author Gisa Schaefer
 * @since 6.5.0
 */
public interface ExpressionEvaluator {

	/**
	 * Returns the {@link ExpressionType}.
	 *
	 * @return the type of the expression
	 */
	public ExpressionType getType();

	/**
	 * Returns whether the result of the callable in this container is always constant
	 *
	 * @return {@code true} if the result of the callable is constant
	 */
	public boolean isConstant();

	/**
	 * Returns the stored String callable, can be {@code null} if the type is not compatible with a
	 * String callable.
	 *
	 * @return a String callable, can be {@code null}
	 */
	public Callable<String> getStringFunction();

	/**
	 * Returns the stored double callable, can be {@code null} if the type is not compatible with a
	 * double callable.
	 *
	 * @return a double callable, can be {@code null}
	 */
	public DoubleCallable getDoubleFunction();

	/**
	 * Returns the stored Date callable, can be {@code null} if the type is not compatible with a
	 * Date callable.
	 *
	 * @return a Date callable, can be {@code null}
	 */
	public Callable<Date> getDateFunction();

	/**
	 * Returns the stored Boolean callable, can be {@code null} if the type is not compatible with a
	 * Boolean callable.
	 *
	 * @return a Boolean callable, can be {@code null}
	 */
	public Callable<Boolean> getBooleanFunction();

}
