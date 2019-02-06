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
package com.rapidminer.tools;

/**
 * Interface for time consuming tasks.
 * 
 * @author Simon Fischer
 */
public interface ProgressListener {

	/** Sets the total amount of work to do, on an arbitrary scale. */
	public void setTotal(int total);

	/** Sets the amount of work completed, in the range [0, {@link #getTotal()}. */
	public void setCompleted(int completed);

	/** Notifies the listener that the task is complete. */
	public void complete();

	/** An optional message to display to the user. */
	public void setMessage(String message);
}
