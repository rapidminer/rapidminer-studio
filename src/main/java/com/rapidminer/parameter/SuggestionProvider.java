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
package com.rapidminer.parameter;

import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.operator.Operator;
import com.rapidminer.tools.ProgressListener;

import java.util.List;


/**
 * Interface for providing suggestions for the {@link ParameterTypeSuggestion}.
 * 
 * @author Nils Woehler
 * @since 6.0.003
 */
public interface SuggestionProvider<R> {

	/**
	 * Called whenever the combo box of {@link ParameterTypeSuggestion} is opened. The method should
	 * return rather quick as it is blocking the GUI thread otherwise.
	 * 
	 * @param op
	 *            the operator being used to configure the suggestion provider. Might be
	 *            <code>null</code> if renderer is not used within the parameter panel.
	 * @param pl
	 *            the progress listener to report progress to
	 * @return the list of suggestions. The suggestions will be displayed in a combobox popup by
	 *         calling the {@link #toString()} method.
	 */
	 List<R> getSuggestions(Operator op, ProgressListener pl);

	/**
	 * @return a resource action being displayed right next to the combo box if defined. Might be
	 *         <code>null</code> in case no action should be displayed.
	 */
	default ResourceAction getAction(){
		return null;
	}
}
