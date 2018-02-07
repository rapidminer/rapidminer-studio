/**
 * Copyright (C) 2001-2018 by RapidMiner and the contributors
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
package com.rapidminer.gui;

import com.rapidminer.Process;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.UpdateQueue;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;

import javax.swing.SwingUtilities;


/**
 * This queue updates the meta data on any update received from a process.
 * 
 * @author Simon Fischer
 * 
 */
public class MetaDataUpdateQueue extends UpdateQueue {

	private final MainFrame mainFrame;

	public MetaDataUpdateQueue(MainFrame mainFrame) {
		super("MetaDataValidation");
		this.mainFrame = mainFrame;
		this.setPriority(MIN_PRIORITY);
	}

	/**
	 * Enqueues a tasks to validate the given process.
	 * 
	 * @param force
	 *            if false, process will be validated only if validate automatically is selected.
	 */
	public void validate(final Process process, final boolean force) {
		execute(new Runnable() {

			@Override
			public void run() {
				new ProgressThread("validate_process") {

					@Override
					public void run() {
						getProgressListener().setTotal(100);
						getProgressListener().setCompleted(10);
						if (force) {
							process.getRootOperator().checkAll();
						} else {
							process.getRootOperator().checkAllExcludingMetaData();
						}
						getProgressListener().setCompleted(90);
						try {
							SwingUtilities.invokeAndWait(new Runnable() {

								@Override
								public void run() {
									mainFrame.fireProcessUpdated();
								}
							});
						} catch (InterruptedException e) {
						} catch (InvocationTargetException e) {
							// LogService.getRoot().log(Level.WARNING,
							// "While updating process editors: "+e, e);
							LogService.getRoot().log(
									Level.WARNING,
									I18N.getMessage(LogService.getRoot().getResourceBundle(),
											"com.rapidminer.gui.MetaDataUpdateQueue.error_while_updating", e), e);

						}
						getProgressListener().setCompleted(100);
						getProgressListener().complete();
					}
				}.startAndWait();
			}
		});
	}

}
