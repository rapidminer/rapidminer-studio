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
package com.rapidminer.gui.actions;

import java.util.List;

import javax.swing.Action;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;


/**
 * Context for {@link Operator} specific {@link Action}s.
 *
 * @author Michael Knopf
 * @since 6.5
 */
public interface OperatorActionContext {

	/**
	 * Return the currently displayed {@link OperatorChain}.
	 *
	 * @return the displayed operator chain
	 * @since 6.5
	 */
	Operator getDisplayedChain();

	/**
	 * Returns the list of operators covered by the context. I.e., the operators the action would be
	 * applied to.
	 * <p>
	 * May be empty (but never {@code null}. May include the displayed {@link OperatorChain} (see
	 * {@link #getDisplayedChain}).
	 *
	 * @return the list of operators
	 * @since 6.5
	 */
	List<Operator> getOperators();
}
