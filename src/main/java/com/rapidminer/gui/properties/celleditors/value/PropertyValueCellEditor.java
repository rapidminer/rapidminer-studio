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
package com.rapidminer.gui.properties.celleditors.value;

import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterType;


/**
 * Interface for all value cell editors of property / parameter tables. Please note that the objects
 * of this interface are constructed via reflection and that a one-argument constructor taking the
 * {@link ParameterType} must be provided. Directly after initialization the method
 * {@link #setOperator(Operator)} is invoked.
 * 
 * @author Ingo Mierswa, Simon Fischer Exp $
 */
public interface PropertyValueCellEditor extends TableCellEditor, TableCellRenderer {

	/** This method can be implemented to perform operator specific settings. */
	void setOperator(Operator operator);

	/**
	 * Returns true if this editor should also be used as renderer. Should not be the case for
	 * components with frames around the component like JTextFields.
	 */
	boolean useEditorAsRenderer();

	/**
	 * Indicates whether this editor renders the parameter type key and does not need to be rendered
	 * by the PropertyPanel.
	 */
	boolean rendersLabel();

	/**
	 * Activates this editor as if the user chose the main action (e.g. clicked the only button). Does nothing by default.
	 * This is used for interacting with parameter editors, e.g. by double-clicking in the process view on an operator for the primary parameter type.
	 * <p>
	 * If your custom parameter type should be able to use the primary parameter functionality via {@link ParameterType#setPrimary(boolean)}, your editor must overwrite this method.
	 * </p>
	 *
	 * @since 8.2
	 */
	default void activate() {
		// does nothing by default
	}
}
