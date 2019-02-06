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
package com.rapidminer.repository.gui;

import com.rapidminer.tools.ProgressListener;


/**
 * Progress listener to handle the progress of sub-tasks. These will use only an absolute section
 * (min ... max) of the respective parent progress listener. The progress values of the parent
 * progress are used to control the progress state.
 *
 * @author Simon Fischer, Adrian Wilke
 */
public class RescalingProgressListener implements ProgressListener {

	// Parent related
	private ProgressListener parentListener;
	private int parentMinAbsolute, parentMaxAbsolute;

	// Local
	private Double total = 0.0;

	/**
	 * Constructs progress listener which uses just a part of its parent progress listener.
	 *
	 * @param parentListener
	 *            The parent progress listener.
	 * @param parentMinAbsolute
	 *            Minimum progress value for this progress. Used to set parent progress listener.
	 * @param parentMaxAbsolute
	 *            Maximum progress value for this progress. Used to set parent progress listener.
	 */
	RescalingProgressListener(ProgressListener parentListener, int parentMinAbsolute, int parentMaxAbsolute) {
		this.parentListener = parentListener;
		this.parentMinAbsolute = parentMinAbsolute;
		this.parentMaxAbsolute = parentMaxAbsolute;
	}

	@Override
	public void setTotal(int total) {
		// Maximum value for this progress
		this.total = new Double(total);
	}

	@Override
	public void setCompleted(int completed) {

		// Deny values greater than the total value
		if (completed > total) {
			completed = total.intValue();
		}

		// Compute locally completed amount: [0 ... 1]
		// Deny division by zero
		double locallyCompleted = 0;
		if (total > 0) {
			locallyCompleted = completed / total;
		} else {
			locallyCompleted = 1;
		}

		// Maximum value to add is parent-max - parent-min.
		// Value to add depends on local progress [0 ... 1].
		// It starts at parent-min.
		Double totalCompleted = (parentMaxAbsolute - parentMinAbsolute) * locallyCompleted + parentMinAbsolute;

		parentListener.setCompleted(totalCompleted.intValue());
	}

	@Override
	public void complete() {
		setCompleted(total.intValue());
	}

	@Override
	public void setMessage(String message) {
		parentListener.setMessage(message);
	}
}
