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

import java.awt.Window;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;

import com.rapidminer.RapidMiner;
import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.processeditor.results.ResultDisplay;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;


/**
 * <p>
 * {@link Runnable}s implementing this class can be execute in a dedicated thread (cached thread
 * pool) and automatically display their progress in the status bar. To use this class, define a
 * property "gui.progress.KEY.label" in the GUI properties file, and pass KEY to the constructor.
 * Then, from within the {@link #run()} method, use {@link #getProgressListener()} to report any
 * progress the task makes.
 * </p>
 * <p>
 * By default, {@link ProgressThread}s are executed in parallel. However sometimes a dependency
 * (Thread B should wait for Thread A to finish before being executed) is required. This can be
 * achieved by setting an optional ID dependency {@link String} via
 * {@link #addDependency(String...)}. If a {@link ProgressThread} is started via {@link #start()} or
 * {@link #startAndWait()} and there is already a {@link ProgressThread} running or in the queue
 * with an ID matching one of the dependencies of the new task, it will wait until they have
 * finished execution before being executed itself.
 * </p>
 * <p>
 * This can also be used to queue multiple instances of the same task which should run one after
 * another. As long as they all have the same ID and a dependency on said ID, they are executed in
 * the order they were queued via {@link #start()} or {@link #startAndWait()}.
 * </p>
 * 
 * @author Simon Fischer, Marco Boeck
 */
public abstract class ProgressThread implements Runnable {

	/**
	 * amount of time the blocking {@link #startAndWait()} method waits between each check for
	 * dependencies
	 */
	private static final int BUSY_WAITING_INTERVAL = 500;

	/** the currently running tasks */
	private static List<ProgressThread> currentThreads = Collections.synchronizedList(new ArrayList<ProgressThread>());

	/**
	 * the queue of {@link ProgressThread}s which await execution because they depend on other
	 * currently running/queued tasks
	 */
	private static List<ProgressThread> queuedThreads = Collections.synchronizedList(new ArrayList<ProgressThread>());

	/** the list of event listeners */
	private static EventListenerList listener = new EventListenerList();

	/** this is the {@link ExecutorService} from which all {@link ProgressThread}s are started */
	private static ExecutorService EXECUTOR = Executors.newCachedThreadPool(new ThreadFactory() {

		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r, "ProgressThread");
			thread.setDaemon(true);
			thread.setPriority(Thread.MIN_PRIORITY);
			return thread;
		}
	});

	private static final Object LOCK = new Object();

	/** the {@link ProgressDisplay} instance for this task */
	private final ProgressDisplay display;

	/** the human readable name for this task */
	private String name;

	/**
	 * if this flag is <code>true</code>, the progress UI is in the foreground while the task is
	 * running
	 */
	private boolean runInForeground;

	/** flag indicating if {@link #startAndWait()} has been called */
	private boolean isWaiting = false;

	/** the i18n key */
	private final String key;

	/** the dependencies. Every {@link String} here is checked against other task IDs */
	private List<String> dependencies;

	/** the ProgressThreadListeners */
	private final Set<ProgressThreadListener> listeners = new CopyOnWriteArraySet<>();

	/** <code>true</code> if the task was cancelled */
	private boolean cancelled = false;

	/** <code>true</code> if the task is started. (Remains true after canceling.) */
	private boolean started = false;

	/**
	 * If {@link #startDialogShowTimer} is set to <code>true</code> and {@link #runInForeground} is
	 * set to <code>false</code> when starting the progress thread, the progress dialog will be
	 * shown after this defined amount of time if the progress thread has not finished yet by then.
	 * The default value is set to 2000 milliseconds (2 seconds).
	 */
	private long showDialogTimerDelay = 2000;

	/**
	 * If set to <code>true</code> and {@link #runInForeground} is set to <code>false</code>, the
	 * progress dialog will be shown after the amount of time defined by
	 * {@link #showDialogTimerDelay} if the progress thread has not finished yet by then. The
	 * amount if time can be defined by changing {@link #setShowDialogTimerDelay(long)} which by
	 * default is set to 2000 milliseconds (2 seconds).
	 */
	private boolean startDialogShowTimer = false;

	/**
	 * If set to <code>false</code> the user is not allowed to cancel the progress thread
	 */
	private boolean isCancelable = true;

	/**
	 * If set to <code>true</code> the progress bar will not have a determinate state
	 */
	private boolean indeterminate = false;;

	/**
	 * If set to {@code true} and a progress thread is cancelled, there is a popup about cancelling dependent tasks.
	 */
	private boolean dependencyPopups = true;

	/**
	 * Creates a new {@link ProgressThread} instance with the specified {@link I18N} key. Uses its
	 * I18N key as an ID to allow other ProgressThreads to depend on it.
	 * 
	 * @param i18nKey
	 *            used to retrieve the name of the progress thread from GUI properties file. The
	 *            i18N key has to look like this: gui.progress.$i18nKey$.label. The i18N key is also
	 *            used as progress thread ID.
	 */
	public ProgressThread(String i18nKey) {
		this(i18nKey, false);
	}

	/**
	 * Creates a new {@link ProgressThread} instance with the specified {@link I18N} key. Also opens
	 * the window showing currently active {@link ProgressThread}s once it is started. Uses its I18N
	 * key as an ID to allow other ProgressThreads to depend on it.
	 * 
	 * @param i18nKey
	 *            used to retrieve the name of the progress thread from GUI properties file. The
	 *            i18N key has to look like this: gui.progress.$i18nKey$.label. The i18N key is also
	 *            used as progress thread ID.
	 * @param runInForeground
	 *            if set to <code>true</code> the progress thread dialog will be shown when the
	 *            progress thread is started
	 */
	public ProgressThread(String i18nKey, boolean runInForeground) {
		this(i18nKey, runInForeground, new Object[] {});
	}

	/**
	 * Creates a new {@link ProgressThread} instance with the specified {@link I18N} key and ID. The
	 * ID can be used by other {@link ProgressThread}s as a dependency. Also opens the window
	 * showing currently active {@link ProgressThread}s once it is started if runInForeground is set
	 * to <code>true</code>.
	 * 
	 * @param i18nKey
	 *            the key for I18N and ID used for dependency handling
	 * @param runInForeground
	 *            if <code>true</code>, the dialog will be shown in the foreground
	 * @param arguments
	 *            the I18N arguments for the I18N key
	 */
	public ProgressThread(String i18nKey, boolean runInForeground, Object... arguments) {
		if (i18nKey == null || "".equals(i18nKey.trim())) {
			throw new IllegalArgumentException("i18nKey must not be null!");
		}
		this.name = I18N.getMessage(I18N.getGUIBundle(), "gui.progress." + i18nKey + ".label", arguments);
		this.key = i18nKey;
		this.runInForeground = runInForeground && !RapidMiner.getExecutionMode().isHeadless();
		this.display = new ProgressDisplay(name, this);
		this.dependencies = new ArrayList<>();
	}

	/**
	 * Returns the human readable name.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name + (cancelled ? " (cancelled)" : "");
	}

	/**
	 * Returns the {@link ProgressListener} of the {@link ResultDisplay}.
	 * 
	 * @return
	 */
	public ProgressListener getProgressListener() {
		checkCancelled();
		return display.getListener();
	}

	/**
	 * Returns the ID of this task.
	 */
	public String getID() {
		return key;
	}

	/**
	 * This call adds the specified ID(s) as a dependency to this task. What this means is that as
	 * long as there are other tasks running/in the queue which have an ID which matches one of the
	 * dependencies, this task will not be executed. Only after all tasks which have been queued
	 * before and have an ID matching one of the dependencies, this task will be executed. A task
	 * can have as many dependencies as required.
	 * 
	 * @param dependencyIDs
	 *            the ID(s) of another {@link ProgressThread} (see {@link #getID()} which must
	 *            finish execution before this task can run
	 */
	public void addDependency(String... dependencyIDs) {
		if (dependencyIDs == null) {
			throw new IllegalArgumentException("dependencyIDs must not be null!");
		}
		for (String dependencyID : dependencyIDs) {
			if (dependencyID == null) {
				throw new IllegalArgumentException("dependencyID must not be null!");
			}
			this.dependencies.add(dependencyID);
		}
	}

	/**
	 * Returns the dependencies of this task.
	 * 
	 * @return
	 */
	public List<String> getDependencies() {
		return new ArrayList<>(dependencies);
	}

	/**
	 * Checks whether this {@link ProgressThread} is blocked by anything else but dependencies.
	 *
	 * @return {@code false} by default
	 * @see #isBlockedByDependencies()
	 * @see #getDependencies()
	 * @since 9.2
	 */
	protected boolean isBlockedByOther() {
		return false;
	}

	/**
	 * Returns the {@link ResultDisplay}.
	 * 
	 * @return
	 */
	public ProgressDisplay getDisplay() {
		return display;
	}

	/**
	 * Changes the human readable name for the progress display UI.
	 * 
	 * @param i18nKey
	 */
	public void setDisplayLabel(String i18nKey) {
		name = I18N.getMessage(I18N.getGUIBundle(), "gui.progress." + i18nKey + ".label");
	}

	/**
	 * Note that this method has nothing to do with Thread.start. It merely enqueues this Runnable
	 * in the Executor's queue. It sets cancelled and started to false so that progress threads can be reused.
	 */
	public void start() {
		cancelled = false;
		started = false;
		// see if task is blocked, if not start it immediately
		boolean blocked = false;
		synchronized (LOCK) {
			blocked = isBlockedByDependencies();
		}
		if (!blocked) {
			EXECUTOR.execute(makeWrapper(true));
		} else {
			// otherwise add to queue, which is checked once another task finishes execution
			synchronized (LOCK) {
				queuedThreads.add(this);
			}
			taskQueued(this);
		}
	}

	/**
	 * Enqueues this task and waits for its completion. If you call this method, you probably want
	 * to set the runInForeground flag in the constructor to true. This methods sets cancelled and started to false at
	 * the beginning so that progress threads can be reused.
	 * <p>
	 * Be careful when using this method for {@link ProgressThread}s with dependencies, this call
	 * might block for a long time.
	 * </p>
	 */
	public void startAndWait() {
		cancelled = false;
		started = false;
		// set flag indicating we are a busy waiting task - these are not started automatically by
		// #checkQueueForDependenciesAndExecuteUnblockedTasks()
		isWaiting = true;
		try {
			boolean blocked;
			synchronized (LOCK) {
				blocked = isBlockedByDependencies();
			}
			// no dependency -> start immediately
			if (!blocked) {
				EXECUTOR.submit(makeWrapper(true)).get();
				return;
			}
			synchronized (LOCK) {
				queuedThreads.add(this);
			}
			taskQueued(this);
			// because this method waits, we can't just queue and leave. Instead we check on a
			// regular basis and see if it can be executed now.
			do {
				synchronized (LOCK) {
					blocked = isBlockedByDependencies();
				}
				if (!blocked) {
					// no longer blocked? Execute and wait and afterwards leave loop
					synchronized (LOCK) {
						queuedThreads.remove(this);
						currentThreads.add(this);
					}
					EXECUTOR.submit(makeWrapper(false)).get();
					break;
				}
				Thread.sleep(BUSY_WAITING_INTERVAL);
			} while (true);
		} catch (InterruptedException e) {
			LogService.getRoot().log(
					Level.SEVERE,
					I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.gui.tools.ProgressThread.executing_error", name), e);

		} catch (ExecutionException e) {
			LogService.getRoot().log(
					Level.SEVERE,
					I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.gui.tools.ProgressThread.executing_error", name), e);
		}
	}

	/** Returns true if the thread was cancelled. */
	public final boolean isCancelled() {
		return cancelled;
	}

	/**
	 * If the thread is currently active, calls {@link #executionCancelled()} to notify children. If
	 * not active, removes the thread from the queue so it won't become active.
	 */
	public final void cancel() {
		boolean dependentThreads = false;
		synchronized (LOCK) {
			dependentThreads = checkQueuedThreadDependOnCurrentThread();
		}
		if (dependentThreads) {
			if (dependencyPopups && ConfirmDialog.OK_OPTION != SwingTools.showConfirmDialog(getCancellationOwner(),
					"cancel_pg_with_dependencies", ConfirmDialog.OK_CANCEL_OPTION)) {
				return;
			} else {
				synchronized (LOCK) {
					removeQueuedThreadsWithDependency(getID());
				}
			}
		}
		synchronized (LOCK) {
			cancelled = true;
			if (started) {
				executionCancelled();
				currentThreads.remove(this);
			} else {
				// cancel and not started? Can be in queue or already in current
				boolean found = queuedThreads.remove(this);
				if (!found) {
					//handle the case that run method not already started
					currentThreads.remove(this);
				}
			}
		}
		taskCancelled(this);
	}

	/**
	 * Gets the owner for the dependency cancellation popup. This is the progress thread dialog if it is visible so
	 * that the cancellation is not shown behind it.
	 */
	private Window getCancellationOwner() {
		ProgressThreadDialog dialog = ProgressThreadDialog.getInstance();
		if (dialog == null || !dialog.isVisible()) {
			return ApplicationFrame.getApplicationFrame();
		}
		return dialog;
	}

	/**
	 * <p>
	 * <strong>ATTENTION: Make sure this is only called from inside a synchronized block!</strong>
	 * </p>
	 * 
	 * @return returns <code>true</code> if any of the queued progress threads depend on this
	 *         progress thread
	 * 
	 * 
	 */
	private final boolean checkQueuedThreadDependOnCurrentThread() {
		for (ProgressThread pg : queuedThreads) {
			if (pg.getDependencies().contains(getID())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Removes all queued threads that depend on the progress threads with the provided IDs. Also
	 * all thread that depend on the threads that have been removed are removed recursively.
	 * <p>
	 * <strong>ATTENTION: Make sure this is only called from inside a synchronized block!</strong>
	 * </p>
	 * 
	 * @param ids
	 *            the progress thread IDs the queued progress threads should be checked for
	 */
	private static final void removeQueuedThreadsWithDependency(String... ids) {
		Iterator<ProgressThread> iterator = queuedThreads.iterator();

		// iterator over queued threads and remove the remove the ones that depend on one of the
		// provided IDs
		Set<String> cancelledThreads = new HashSet<>();
		while (iterator.hasNext()) {
			ProgressThread pg = iterator.next();
			if (!Collections.disjoint(Arrays.asList(ids), pg.getDependencies())) {
				iterator.remove();
				cancelledThreads.add(pg.getID());
			}
		}
		// also remove all the ones depending on the ones that have been cancelled.
		if (!cancelledThreads.isEmpty()) {
			removeQueuedThreadsWithDependency(cancelledThreads.toArray(new String[cancelledThreads.size()]));
		}
	}

	/** Adds a new ProgressThreadListener **/
	public final void addProgressThreadListener(final ProgressThreadListener listener) {
		listeners.add(listener);
	}

	/** Removes a ProgressThreadListener **/
	public final void removeProgressThreadListener(final ProgressThreadListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Subclasses can implemented this method if they want to be notified about cancellation of this
	 * thread. In most cases, this is not necessary. Subclasses can ask {@link #isCancelled()}
	 * whenever cancelling is possible, or, even easier, directly call {@link #checkCancelled()}.
	 */
	protected void executionCancelled() {}

	/** If cancelled, throws a RuntimeException to stop the thread. */
	protected void checkCancelled() throws ProgressThreadStoppedException {
		if (cancelled) {
			throw new ProgressThreadStoppedException();
		}
	}

	/**
	 * Creates a wrapper that executes this class' run method, sets {@link #current} and
	 * subsequently removes it from the list of pending tasks and shows a
	 * {@link ProgressThreadDialog} if necessary. As a side effect, calling this method also results
	 * in adding this ProgressThread to the list of pending tasks if addToCurrent is {@code true}.
	 * */
	private Runnable makeWrapper(boolean addToCurrent) {
		// show dialog if wanted
		if (runInForeground) {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					if (!ProgressThreadDialog.getInstance().isVisible()) {
						ProgressThreadDialog.getInstance().setVisible(false, true);
					}
				};
			});
		}
		if (addToCurrent) {
			synchronized (LOCK) {
				currentThreads.add(ProgressThread.this);
			}
		}
		taskStarted(this);
		return new Runnable() {

			@Override
			public void run() {
				synchronized (LOCK) {
					if (cancelled) {
						LogService.getRoot().log(Level.INFO, "com.rapidminer.gui.tools.ProgressThread.task_cancelled",
								getName());
						return;
					}
					started = true;
				}
				Timer showProgressTimer = null;
				if (!isRunInForegroundFlagSet() && isStartDialogShowTimer() && !RapidMiner.getExecutionMode().isHeadless()) {
					showProgressTimer = new Timer("show-pg-timer", true);
					final TimerTask showProgressTask = new TimerTask() {

						@Override
						public void run() {
							SwingUtilities.invokeLater(() -> {
								runInForeground = true;
								if (!ProgressThreadDialog.getInstance().isVisible()) {
									ProgressThreadDialog.getInstance().setVisible(false, true);
								}
							});
						}
					};
					showProgressTimer.schedule(showProgressTask, getShowDialogTimerDelay());
				}
				try {
					ActionStatisticsCollector.getInstance().startTimer(this, ActionStatisticsCollector.TYPE_PROGRESS_THREAD, key,
							"runtime");
					ActionStatisticsCollector.getInstance().log(ActionStatisticsCollector.TYPE_PROGRESS_THREAD, key,
							"started");
					ProgressThread.this.run();
					if (showProgressTimer != null) {
						showProgressTimer.cancel();
					}
					ActionStatisticsCollector.getInstance().log(ActionStatisticsCollector.TYPE_PROGRESS_THREAD, key,
							"completed");
				} catch (ProgressThreadStoppedException e) {
					if (showProgressTimer != null) {
						showProgressTimer.cancel();
					}
					ActionStatisticsCollector.getInstance().log(ActionStatisticsCollector.TYPE_PROGRESS_THREAD, key,
							"stopped");
					LogService.getRoot().log(Level.FINE,
							"com.rapidminer.gui.tools.ProgressThread.progress_thread_aborted",
							getName());
				} catch (Exception e) {
					if (showProgressTimer != null) {
						showProgressTimer.cancel();
					}
					ActionStatisticsCollector.getInstance().log(ActionStatisticsCollector.TYPE_PROGRESS_THREAD, key,
							"failed");
					LogService.getRoot().log(
							Level.WARNING,
							I18N.getMessage(LogService.getRoot().getResourceBundle(),
									"com.rapidminer.gui.tools.ProgressThread.error_executing_background_job", name, e), e);

					SwingTools.showSimpleErrorMessage("error_executing_background_job", e, name, e);
				} finally {
					ActionStatisticsCollector.getInstance().stopTimer(this);
					if (!ProgressThread.this.isCancelled()) {
						ProgressThread.this.getProgressListener().complete();
					}
					synchronized (LOCK) {
						currentThreads.remove(ProgressThread.this);
					}
					for (ProgressThreadListener listener : listeners) {
						listener.threadFinished(ProgressThread.this);
					}
					checkQueueForDependenciesAndExecuteUnblockedTasks();
					taskFinished(ProgressThread.this);
				}
			}
		};
	}

	/**
	 * If <code>true</code>, this task has been started via {@link #startAndWait()}.
	 * 
	 * @return
	 */
	private boolean isWaiting() {
		return isWaiting;
	}

	/**
	 * If <code>true</code>, the runInForegrund flag has been set.
	 * 
	 * @return
	 */
	private boolean isRunInForegroundFlagSet() {
		return runInForeground;
	}

	/**
	 * Returns <code>true</code> if this task is blocked by its dependencies; <code>false</code>
	 * otherwise.
	 * <p>
	 * A task is blocked by dependencies if a task with an ID matching one or more of the
	 * dependencies is running or in the queue before this one.
	 * </p>
	 * <p>
	 * <strong>ATTENTION: Make sure this is only called from inside a synchronized block!</strong>
	 * </p>
	 * 
	 * @return
	 */
	private boolean isBlockedByDependencies() {
		if (isBlockedByOther()) {
			return true;
		}
		List<String> currentDependencies = getDependencies();
		if (currentDependencies.isEmpty()) {
			return false;
		}
		for (ProgressThread pg : currentThreads) {
			if (currentDependencies.contains(pg.getID())) {
				return true;
			}
		}
		// now check tasks in queue as there might be a dependency waiting for one himself
		for (ProgressThread pg : queuedThreads) {
			// loop over queued tasks until we reach ourself, if no dependencies have been found
			// by then, we can start!
			if (pg.equals(this)) {
				break;
			}
			if (currentDependencies.contains(pg.getID())) {
				return true;
			}
		}

		return false;
	}

	/**
	 * @return the time that must have passed before the progress thread dialog is shown
	 */
	public long getShowDialogTimerDelay() {
		return showDialogTimerDelay;
	}

	/**
	 * Allows to define the time in milliseconds after which the progress thread dialog should be
	 * shown if the task has not finished yet by then. This will have effect only if
	 * {@link #runInForeground} is set to <code>false</code> and {@link #startDialogShowTimer} is
	 * set to <code>true</code>. The time is specified in milliseconds.<br/>
	 * <b>Note:</b> Changing this value will take effect only before starting the progress thread.
	 */
	public void setShowDialogTimerDelay(long delay) {
		if (delay <= 0) {
			throw new IllegalArgumentException("Only values above 0 are allowed.");
		}
		this.showDialogTimerDelay = delay;
	}

	/**
	 * @return defines if the progress thread dialog should be shown if the progress thread has not
	 *         yet finished after the time defined by {@link #showDialogTimerDelay}.
	 */
	public boolean isStartDialogShowTimer() {
		return startDialogShowTimer;
	}

	/**
	 * Allows to define whether the progress thread dialog should be shown if the progress thread
	 * has not yet finished after the time specified by {@link #showDialogTimerDelay}. The default
	 * value is set to 2 seconds and can be changed by calling
	 * {@link #setShowDialogTimerDelay(long)}.<br/>
	 * <b>Note:</b> Changing this value will take effect only before starting the progress thread.
	 */
	public void setStartDialogShowTimer(boolean startDialogShowTimer) {
		this.startDialogShowTimer = startDialogShowTimer;
	}

	/**
	 * @return whether the progress bar for this progress thread should be in indeterminate mode
	 */
	public boolean isIndeterminate() {
		return indeterminate;
	}

	/**
	 * To indicate that a task of unknown length is executing, you can put a progress bar into
	 * indeterminate mode. While the bar is in indeterminate mode, it animates constantly to show
	 * that work is occurring. </br><b>Note:</b> Changing this value will take effect only before
	 * starting the progress thread.
	 */
	public void setIndeterminate(boolean indeterminate) {
		this.indeterminate = indeterminate;

		// In case of indeterminate progress thread the message change to a hint that it actually is
		// doing something
		if (this.isIndeterminate()) {
			getProgressListener().setMessage(I18N.getGUILabel("indeterminate.progress"));
		}
	}

	/**
	 * Allows to define whether the user should be able to cancel the progress thread.
	 * </br><b>Note:</b> Changing this will only have effect before starting the progress thread.
	 */
	public void setCancelable(boolean isCancelable) {
		this.isCancelable = isCancelable;
	}

	/**
	 * @return whether the progress thread should be cancelable or not
	 */
	public boolean isCancelable() {
		return isCancelable;
	}

	/**
	 * Allows to define whether a popup is shown if the user cancels a progress thread that has dependent progress
	 * threads. By default this is {@code true}.
	 * </br><b>Note:</b> Changing this is only guaranteed to have an effect before starting the progress thread.
	 *
	 * @since 9.3.0
	 */
	public void setDependencyPopups(boolean showDependencyPopups) {
		this.dependencyPopups = showDependencyPopups;
	}

	/**
	 * @return whether the progress thread opens a popup when it has dependent progress threads and is cancelled or not
	 * @since 9.3.0
	 */
	public boolean showsDependencyPopups() {
		return dependencyPopups;
	}

	/**
	 * @return the currently executed tasks.
	 */
	public static Collection<ProgressThread> getCurrentThreads() {
		return new ArrayList<>(currentThreads);
	}

	/**
	 * @return the currently queued tasks
	 */
	public static Collection<ProgressThread> getQueuedThreads() {
		return new ArrayList<>(queuedThreads);
	}

	/**
	 * @return <code>true</code> if a {@link ProgressThread} is neither being executed nor queued;
	 *         <code>false</code> otherwise.
	 */
	public static boolean isEmpty() {
		return getCurrentThreads().isEmpty() && getQueuedThreads().isEmpty();
	}

	/**
	 * @return <code>true</code> if a {@link ProgressThread} is currently running which has the
	 *         inForeground flag set; <code>false</code> otherwise.
	 */
	public static boolean isForegroundRunning() {
		for (ProgressThread pg : getCurrentThreads()) {
			if (pg.isRunInForegroundFlagSet()) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Adds the specified {@link ProgressThreadStateListener} which will be informed of any changes.
	 * 
	 * @param l
	 */
	public static void addProgressThreadStateListener(ProgressThreadStateListener l) {
		listener.add(ProgressThreadStateListener.class, l);
	}

	/**
	 * Removes the specified {@link ProgressThreadStateListener}.
	 * 
	 * @param l
	 */
	public static void removeProgressThreadStateListener(ProgressThreadStateListener l) {
		listener.remove(ProgressThreadStateListener.class, l);
	}

	/**
	 * Checks the currently queued tasks if there are ones which are no longer blocked by
	 * dependencies and executes them.
	 */
	private static final void checkQueueForDependenciesAndExecuteUnblockedTasks() {
		// a task has finished, now check tasks in queue if there are ones which are no
		// longer blocked
		synchronized (LOCK) {
			for (ProgressThread pg : new ArrayList<>(queuedThreads)) {
				if (!pg.isBlockedByDependencies()) {
					// busy waiting tasks should not be started here, they will notice themselves
					if (!pg.isWaiting()) {
						queuedThreads.remove(pg);
						EXECUTOR.execute(pg.makeWrapper(true));
					}
				}
			}
		}

	}

	/**
	 * Notify listeners that a task was queued.
	 * 
	 * @param task
	 */
	private static void taskQueued(ProgressThread task) {
		for (ProgressThreadStateListener l : listener.getListeners(ProgressThreadStateListener.class)) {
			l.progressThreadQueued(task);
		}
	}

	/**
	 * Notify listeners that a task was started.
	 * 
	 * @param task
	 */
	private static void taskStarted(ProgressThread task) {
		for (ProgressThreadStateListener l : listener.getListeners(ProgressThreadStateListener.class)) {
			l.progressThreadStarted(task);
		}
	}

	/**
	 * Notify listeners that a task was cancelled.
	 * 
	 * @param task
	 */
	private static void taskCancelled(ProgressThread task) {
		for (ProgressThreadStateListener l : listener.getListeners(ProgressThreadStateListener.class)) {
			l.progressThreadCancelled(task);
		}
	}

	/**
	 * Notify listeners that a task was finished.
	 * 
	 * @param task
	 */
	private static void taskFinished(ProgressThread task) {
		for (ProgressThreadStateListener l : listener.getListeners(ProgressThreadStateListener.class)) {
			l.progressThreadFinished(task);
		}
	}

}
