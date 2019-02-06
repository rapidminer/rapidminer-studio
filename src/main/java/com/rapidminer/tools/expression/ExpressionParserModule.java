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

import java.util.List;

import com.rapidminer.gui.properties.ExpressionPropertyDialog;


/**
 * A module that can be used to construct an {@link ExpressionParser} via
 * {@link ExpressionParserBuilder#withModule(ExpressionParserModule)} or
 * {@link ExpressionParserBuilder#withModules(List)}.
 *
 * @author Gisa Schaefer
 * @since 6.5.0
 */
public interface ExpressionParserModule {

	/**
	 * Returns the key associated to this module. The constants of this module are shown in the
	 * {@link ExpressionPropertyDialog} under the category defined by
	 * "gui.dialog.function_input.key.constant_category".
	 *
	 * @return the key for the module
	 */
	public String getKey();

	/**
	 * Returns all {@link Constant}s stored in this module. The constants are shown in the
	 * {@link ExpressionPropertyDialog} under the category defined by
	 * "gui.dialog.function_input.key.constant_category" where key is defined by {@link #getKey()}.
	 *
	 * @return all constants known by this module
	 */
	public List<Constant> getConstants();

	/**
	 * Returns all {@link Function}s stored in this module.
	 *
	 * @return all functions known by this module
	 */
	public List<Function> getFunctions();

}
