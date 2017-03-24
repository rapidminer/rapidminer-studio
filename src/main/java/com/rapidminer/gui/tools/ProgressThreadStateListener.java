/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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

import java.util.EventListener;


/**
 * Listener which will be informed when a new {@link ProgressThread} is queued, executed, cancelled
 * or finished.
 * 
 * @author Marco Boeck
 * 
 */
public interface ProgressThreadStateListener extends EventListener {

	/**
	 * Called when the {@link ProgressThread} has started execution.
	 * 
	 * @param pg
	 */
	public void progressThreadStarted(ProgressThread pg);

	/**
	 * Called when the {@link ProgressThread} has been queued.
	 * 
	 * @param pg
	 */
	public void progressThreadQueued(ProgressThread pg);

	/**
	 * Called when the {@link ProgressThread} has been cancelled.
	 * 
	 * @param pg
	 */
	public void progressThreadCancelled(ProgressThread pg);

	/**
	 * Called when the {@link ProgressThread} has finished execution.
	 * 
	 * @param pg
	 */
	public void progressThreadFinished(ProgressThread pg);
}
