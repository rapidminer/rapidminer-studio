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
package com.rapidminer.connection.util;

import com.rapidminer.tools.ProgressListener;


/**
 * An abstract adapter class for receiving {@link ProgressListener progress listener} events. The methods in the class
 * are empty. This class exists as convenience for creating listener objects.
 * <p>
 * Extend this class to create a {@link ProgressListener} listener and override the methods for the events of interest.
 * (If you implement the {@link ProgressListener} interface, you have to define all of the methods in it. This abstract
 * class defines empty methods for them all, so you can only have to define methods for events you care about.)
 * </p>
 *
 * @author Jonas Wilms-Pfau
 * @since 9.3
 */
abstract class ProgressAdapter implements ProgressListener {

	@Override
	public void setTotal(int total) {
		// do nothing
	}

	@Override
	public void setCompleted(int completed) {
		// do nothing
	}

	@Override
	public void complete() {
		// do nothing
	}

	@Override
	public void setMessage(String message) {
		// do nothing
	}
}
