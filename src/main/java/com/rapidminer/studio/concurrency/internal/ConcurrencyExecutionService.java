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
package com.rapidminer.studio.concurrency.internal;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import com.rapidminer.Process;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.studio.concurrency.internal.util.BackgroundExecution;
import com.rapidminer.studio.concurrency.internal.util.BackgroundExecutionServiceListener;
import com.rapidminer.studio.concurrency.internal.util.ProcessBackgroundExecution;


/**
 * Interface for the concurrent process and operator execution service.
 * <p>
 * Note that this part of the API is only temporary and might be removed in future versions again.
 * </p>
 *
 * @author Marco Boeck
 * @since 7.4
 */
public interface ConcurrencyExecutionService {

	/**
	 * This method executes the given process in the background. This method does
	 * <strong>not</strong> block.
	 * </p>
	 *
	 * @param process
	 *            The process to be executed in the background
	 */
	void executeProcess(Process process) throws UserError;

	/**
	 * This method executes a process from the repository under certain input and macros. It can be
	 * used to run entire processes in the background from a process.
	 * <p>
	 * This method does <strong>not</strong> block.
	 * </p>
	 *
	 * @param process
	 * @param container
	 * @param macroSettings
	 */
	void executeProcess(Process process, IOContainer container, Map<String, String> macroSettings) throws UserError;

	/**
	 * This method will prepare an operator with subprocesses, that should be performed in parallel
	 * as specified by the given {@link Callable}. The operator will be added to an otherwise empty
	 * {@link BackgroundExecutionProcess}, that will capture all the side effects. If
	 * synchronizeSideEffects is true, these side effects will be merged back into the
	 * parentProcess.
	 *
	 * @param <T>
	 *
	 * @param parentProcess
	 *            The process which contains the parallelized operator
	 * @param clonedOperator
	 *            A clone of this parallelized operator
	 * @param applyCount
	 *            The apply count for this current parallelized version
	 * @param synchronizeSideEffects
	 *            Whether side effects should be merged, usually only during last iteration
	 * @param task
	 *            The actual sub task of the operator to execute
	 * @return
	 */
	<V, T> Callable<V> prepareOperatorTask(Process parentProcess, Operator clonedOperator, int applyCount,
			boolean synchronizeSideEffects, Callable<V> task);

	/**
	 * Executes the tasks for the given operator.
	 *
	 * @param operator
	 *            the operator which executes the given tasks
	 * @param tasks
	 *            the tasks which should be executed
	 * @return the execution result
	 * @throws OperatorException
	 *             if something goes wrong during the task execution
	 */
	<T> List<T> executeOperatorTasks(Operator operator, List<Callable<T>> tasks) throws OperatorException;

	/**
	 * Submits the task for the given operator for execution.
	 *
	 * @param operator
	 *            the operator which executes the given task
	 * @param task
	 *            the task which should be executed
	 * @return a future which can be used to later access the result
	 * @since 7.3
	 */
	<T> Future<T> submitOperatorTask(Operator operator, Callable<T> task);

	/**
	 * Waits for the results of the given {@link Future}s, returning their results upon completion.
	 * <p>
	 * The method blocks until all {@code Future}s have their results.
	 * <p>
	 * This is an advanced method which should only be used if you know what you are doing. Usage of
	 * {@link #call(List)} is recommended instead.
	 *
	 * @param <T>
	 *            the type of the values returned from the futures
	 * @param operator
	 *            the operator which executes the given tasks
	 * @param futures
	 *            the {@link Future}s for which their results should be collected
	 * @return a list containing the results of the futures
	 * @throws OperatorException
	 *             if something goes wrong during the task execution
	 * @since 7.3
	 */
	<T> List<T> collectResults(Operator operator, List<Future<T>> futures) throws OperatorException;

	/**
	 * This method removes the task from the list of currently executed things. If all tasks are
	 * removed the the corresponding UI element will be removed as well.
	 *
	 * @param parentProcess
	 * @param operatorName
	 * @param applyCount
	 */
	void removeOperatorTask(Process parentProcess, String operatorName, int applyCount);

	/**
	 * This copies the list of background executions and returns the copy.
	 *
	 * @return
	 */
	List<BackgroundExecution> getExecutions();

	/**
	 * This method will execute the given {@link Callable} by calling it's run method within the
	 * current thread.
	 *
	 * @param callable
	 * @throws Exception
	 */
	<T> T executeBlockingTask(Callable<T> callable) throws Exception;

	/**
	 * This stops the execution of a given process execution in the background.
	 *
	 * @param execution
	 */
	void stopProcessExecution(ProcessBackgroundExecution execution);

	/**
	 * This drops the execution from the manageable data structures. Please notice, that this will
	 * not stop nor release any resources. To stop execution, you will need to call stopExecution
	 * before.
	 *
	 * @param execution
	 */
	void removeProcessExecution(ProcessBackgroundExecution execution);

	/**
	 * The provided listener will be notified if any background execution is added or removed
	 *
	 * @param listener
	 *            This listener is being added to the listener pool
	 */
	void addListener(BackgroundExecutionServiceListener listener);

	/**
	 * Notifies every listener that a new process has been added to the executions
	 */
	void newProcessEvent(BackgroundExecution execution);

	/**
	 * Notifies every listener that a process has been removed from the executions
	 */
	void removedProcessEvent(BackgroundExecution execution);

}
