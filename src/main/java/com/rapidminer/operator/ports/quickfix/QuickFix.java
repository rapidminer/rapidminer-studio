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
package com.rapidminer.operator.ports.quickfix;

import com.rapidminer.operator.ports.PortException;

import javax.swing.Action;


/**
 * A quick fix that can be used to fix a meta data error.
 * 
 * @author Simon Fischer
 * */
public interface QuickFix extends Comparable<QuickFix> {

	public static final int MAX_RATING = 10;
	public static final int MIN_RATING = 1;

	/**
	 * Get an action to display the quick fix in a menu. The actions actionPerformed() method must
	 * call {@link #apply()} on this object.
	 */
	public Action getAction();

	/**
	 * Applies the quick fix. May require GUI interaction.
	 * 
	 * @throws PortException
	 */
	public void apply();

	/**
	 * Returns true if the fix requires user interaction. Quick fixes that can be applied
	 * non-interactively can be used to repair a whole process setup.
	 */
	public boolean isInteractive();

	/**
	 * Returns a number between {@link #MIN_RATING} and {@link #MAX_RATING} that rates the quick fix
	 * with respect to the presumed quality of the obtained solution. Quick fixes with larger rating
	 * will be listed first.
	 */
	public int getRating();

}
