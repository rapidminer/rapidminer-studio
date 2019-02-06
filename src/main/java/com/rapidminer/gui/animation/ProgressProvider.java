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
package com.rapidminer.gui.animation;

/**
 * Interface for a provider for a progress between 0 and 100.
 *
 * @author Gisa Schaefer
 * @since 7.1.0
 */
public interface ProgressProvider {

	/**
	 * Returns the current progress or 0 if the progress is indeterminate.
	 *
	 * @return the current progress (between 0 and 100)
	 */
	int getProgress();
}
