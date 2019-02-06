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
package com.rapidminer.gui.tools;

import com.rapidminer.tools.ProgressListener;


/**
 * @author Tobias Malbrecht
 */
class ProgressDisplay {

	private int total = 100;
	private int completed = 0;
	private String message;

	private final ProgressListener progressListener = new ProgressListener() {

		@Override
		public void setCompleted(int completed) {
			checkCancelled();
			ProgressDisplay.this.completed = completed;
			ProgressThreadDialog instance = ProgressThreadDialog.getInstance();
			if (instance != null) {
				instance.updateProgress(progressThread);
			}
		}

		@Override
		public void setTotal(int total) {
			ProgressDisplay.this.total = total;
			setCompleted(completed);
			ProgressThreadDialog instance = ProgressThreadDialog.getInstance();
			if (instance != null) {
				instance.updateProgress(progressThread);
			}
		}

		@Override
		public void complete() {
			setCompleted(total);
		}

		@Override
		public void setMessage(String message) {
			checkCancelled();
			ProgressDisplay.this.message = message;
			ProgressThreadDialog instance = ProgressThreadDialog.getInstance();
			if (instance != null) {
				instance.updateProgressMessage(progressThread);
			}
		}

	};
	private ProgressThread progressThread;

	public ProgressDisplay(String label, ProgressThread progressThread) {
		super();
		this.progressThread = progressThread;
	}

	public ProgressListener getListener() {
		return progressListener;
	}

	public int getCompleted() {
		return completed;
	}

	public int getTotal() {
		return total;
	}

	public String getMessage() {
		return message;
	}

	private void checkCancelled() {
		if (progressThread != null) {
			progressThread.checkCancelled();
		}
	}
}
