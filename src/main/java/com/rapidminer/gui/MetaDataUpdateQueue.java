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
package com.rapidminer.gui;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import javax.swing.SwingUtilities;

import com.rapidminer.Process;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ProgressThreadStateListener;
import com.rapidminer.gui.tools.UpdateQueue;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;


/**
 * This queue updates the meta data on any update received from a process.
 * 
 * @author Simon Fischer
 * 
 */
public class MetaDataUpdateQueue extends UpdateQueue {

	/** @since 9.2.0 */
	static final String VALIDATE_PROCESS_KEY = "validate_process";
	/** @since 9.2.0 */
	static final String REVALIDATE_PROCESS_KEY = "re" + VALIDATE_PROCESS_KEY;
	/** @since 9.2.0 */
	private static final Map<Process, List<MDGenerationChecker>> MD_GENERATION_CHECKERS =
			Collections.synchronizedMap(new WeakHashMap<>());

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
		execute(new ProgressThread(VALIDATE_PROCESS_KEY) {

			@Override
			public void run() {
				getProgressListener().setTotal(100);
				getProgressListener().setCompleted(10);
				MDGenerationChecker checker;
				if (force) {
					checker = new MDGenerationChecker();
					MD_GENERATION_CHECKERS.computeIfAbsent(process, p -> new ArrayList<>()).add(checker);
					process.getRootOperator().checkAll();
					if (checker.needsRevalidation()) {
						// trigger new validation cycle if at least one long running MD generation was found
						new ProgressThread(REVALIDATE_PROCESS_KEY) {

							@Override
							protected boolean isBlockedByOther() {
								return !checker.isDone();
							}

							@Override
							public void run() {
								checker.destroy();
								if (mainFrame.getProcess() == process) {
									validate(process, true);
								}
							}
						}.start();
					} else {
						checker.destroy();
					}
				} else {
					checker = null;
					process.getRootOperator().checkAllExcludingMetaData();
				}
				getProgressListener().setCompleted(90);
				try {
					SwingUtilities.invokeAndWait(mainFrame::fireProcessUpdated);
				} catch (InterruptedException e) {
				} catch (InvocationTargetException e) {
					LogService.getRoot().log(
							Level.WARNING,
							I18N.getMessage(LogService.getRoot().getResourceBundle(),
									"com.rapidminer.gui.MetaDataUpdateQueue.error_while_updating", e), e);

				} finally {
					MD_GENERATION_CHECKERS.getOrDefault(process, new ArrayList<>()).remove(checker);
					getProgressListener().complete();
				}
			}
		}::startAndWait);
	}

	/**
	 * Register a {@link com.rapidminer.operator.ports.metadata.MetaData} generating {@link ProgressThread} for the
	 * given {@link Process}. Will do nothing if a validation was not triggered for the process.
	 */
	public static void registerMDGeneration(Process process, ProgressThread generation) {
		MD_GENERATION_CHECKERS.getOrDefault(process, new ArrayList<>()).forEach(l -> l.registerThread(generation));
	}

	/**
	 * A {@link ProgressThreadStateListener} that listens to its registered {@link ProgressThread ProgressThreads}.
	 * As soon as at least one progress thread is registered, the {@link #needsRevalidation()} return value indicates
	 * that another validation run is needed.
	 *
	 * @author Jan Czogalla
	 * @since 9.2.0
	 */
	private static final class MDGenerationChecker implements ProgressThreadStateListener {

		private final Set<ProgressThread> relatedThreads = new HashSet<>();

		private boolean needsRevalidation = false;

		/** Creates a new instance and registers it with the {@link ProgressThread} */
		private MDGenerationChecker() {
			ProgressThread.addProgressThreadStateListener(this);
		}

		@Override
		public void progressThreadStarted(ProgressThread pg) {/* noop*/}

		@Override
		public void progressThreadQueued(ProgressThread pg) {/* noop*/}

		@Override
		public synchronized void progressThreadCancelled(ProgressThread pg) {
			relatedThreads.remove(pg);
		}

		@Override
		public synchronized void progressThreadFinished(ProgressThread pg) {
			relatedThreads.remove(pg);
		}

		/** Register a {@link ProgressThread} with this listener */
		private synchronized void registerThread(ProgressThread pg) {
			needsRevalidation = true;
			relatedThreads.add(pg);
		}

		/** Returns {@code true}, if at least one {@link ProgressThread} was registered */
		private synchronized boolean needsRevalidation() {
			return needsRevalidation;
		}

		/** Returns {@code true} if not waiting for any more threads */
		private synchronized boolean isDone() {
			return relatedThreads.size() <= 1 && relatedThreads.stream().noneMatch(ProgressThread.getCurrentThreads()::contains);
		}

		/** Unregister this listener from {@link ProgressThread} */
		private synchronized void destroy() {
			relatedThreads.clear();
			ProgressThread.removeProgressThreadStateListener(this);
		}
	}

}
