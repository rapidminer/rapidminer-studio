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
package com.rapidminer.repository.internal.remote;

import java.util.Collection;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.internal.remote.model.ExecutionResponse;
import com.rapidminer.repository.internal.remote.model.ProcessContextWrapper;
import com.rapidminer.repository.internal.remote.model.ProcessResponse;
import com.rapidminer.repository.internal.remote.model.Response;
import com.rapidminer.tools.PasswordInputCanceledException;


/**
 *
 * The {@link RemoteScheduler} allows to schedule Process executions on RapidMiner Server.
 *
 * @author Nils Woehler
 * @since 6.5.0
 * @deprecated Use new REST APIs of the Execution Backend instead.
 */
@Deprecated
public interface RemoteScheduler {

	/**
	 * Schedules a process to be executed as soon as possible. Works with all process service
	 * versions.
	 *
	 * @throws RepositoryException
	 *             on fail
	 * @throws PasswordInputCanceledException
	 *             if the user canceled the login dialog
	 */
	ExecutionResponse executeProcessSimple(String executeProcess, XMLGregorianCalendar xmlGregorianCalendar,
										   ProcessContextWrapper processContextWrapper) throws RepositoryException, PasswordInputCanceledException;

	/**
	 * Schedules a process to be executed as soon as possible. If Process Service version is not at
	 * least 1.3 queueName will not be considered.
	 *
	 * @throws RepositoryException
	 *             on fail
	 * @throws PasswordInputCanceledException
	 *             if the user canceled the login dialog
	 */
	ExecutionResponse executeProcessSimple(String path, XMLGregorianCalendar xmlGregorianCalendar,
			ProcessContextWrapper processContextWrapper, String queueName) throws RepositoryException,
			PasswordInputCanceledException;

	/**
	 * Executes a process with an provided offset.
	 *
	 * @return <code>null</code> if function is not supported.
	 * @throws RepositoryException
	 *             on fail
	 * @throws PasswordInputCanceledException
	 *             if the user canceled the login dialog
	 */
	ExecutionResponse executeProcessWithOffset(String path, Long offset, ProcessContextWrapper processContextWrapper,
			String queueName) throws RepositoryException, PasswordInputCanceledException;

	/**
	 * Executes a process with an provided offset. Works with all process service versions.
	 *
	 * @throws RepositoryException
	 *             on fail
	 * @throws PasswordInputCanceledException
	 *             if the user canceled the login dialog
	 */
	ExecutionResponse executeProcessCron(final String processName, final String cronExpression,
			final XMLGregorianCalendar start, final XMLGregorianCalendar end, final ProcessContextWrapper context)
			throws RepositoryException, PasswordInputCanceledException;

	/**
	 * If Process Service version is not at least 1.3 queueName will not be considered.
	 *
	 * @throws RepositoryException
	 *             on fail
	 * @throws PasswordInputCanceledException
	 *             if the user canceled the login dialog
	 */
	ExecutionResponse executeProcessCron(String path, String cronExpression, XMLGregorianCalendar start,
			XMLGregorianCalendar end, ProcessContextWrapper processContextWrapper, String queueName)
					throws RepositoryException, PasswordInputCanceledException;

	Response stopProcess(int id) throws RepositoryException, PasswordInputCanceledException;

	/**
	 * Note: This method returns info about the job associated with the given id of a
	 * link ScheduledProcess, not of the link ProcessExecutionParameters object returned from
	 * link ProcessService_1_3#executeProcessSimple(String, Date) when submitting the job.
	 *
	 * @throws RepositoryException
	 *             on fail
	 * @throws PasswordInputCanceledException
	 *             if the user canceled the login dialog
	 */
	ProcessResponse getRunningProcessesInfo(int processId) throws RepositoryException, PasswordInputCanceledException;

	/**
	 * Retrieve the IDs of running processes since a specified date.
	 *
	 * @param since
	 *            the date used to lookup running processes
	 * @return a collection of process IDs that are running since the specified date
	 * @throws RepositoryException
	 *             on fail
	 * @throws PasswordInputCanceledException
	 *             if the user canceled the login dialog
	 */
	Collection<Integer> getRunningProcesses(XMLGregorianCalendar since) throws RepositoryException,
	PasswordInputCanceledException;

	/**
	 * Queries the server for process IDs for the specified job ID.
	 *
	 * @param jobId
	 *            the job ID which should be used to lookup process IDs
	 * @return a list of process IDs for the specified job ID
	 * @throws RepositoryException
	 *             on fail
	 * @throws PasswordInputCanceledException
	 *             if the user canceled the login dialog
	 */
	List<Integer> getProcessIdsForJobId(int jobId) throws RepositoryException, PasswordInputCanceledException;

	/**
	 * Returns a list of available execution queue names. If the process service version is prior to
	 * version 1.3, <code>null</code> will be returned.
	 *
	 * @throws RepositoryException
	 *             on fail
	 * @throws PasswordInputCanceledException
	 *             if the user canceled the login dialog
	 */
	List<String> getQueueNames() throws RepositoryException, PasswordInputCanceledException;

}
