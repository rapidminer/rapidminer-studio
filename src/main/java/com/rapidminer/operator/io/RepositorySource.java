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
package com.rapidminer.operator.io;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.rapidminer.connection.ConnectionInformationContainerIOObject;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.InvalidRepositoryEntryError;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.SimpleProcessSetupError;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.UserSetupError;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.quickfix.ParameterSettingQuickFix;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeRepositoryLocation;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.RepositoryEntryNotFoundException;
import com.rapidminer.repository.RepositoryEntryWrongTypeException;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;


/**
 * 
 * @author Sebastian Land
 */
public class RepositorySource extends AbstractReader<IOObject> {

	public static final String PARAMETER_REPOSITORY_ENTRY = "repository_entry";

	private static final Map<Class<?>, String> REPO_ERROR_KEYS = new HashMap<>();
	static {
		REPO_ERROR_KEYS.put(RepositoryEntryNotFoundException.class, "repository_location_does_not_exist");
		REPO_ERROR_KEYS.put(RepositoryEntryWrongTypeException.class, "repository_location_wrong_type");
	}

	public RepositorySource(OperatorDescription description) {
		super(description, IOObject.class);
		// reuse precheck thread
		getTransformer().addRule(getPrecheckThread()::start);
	}

	@Override
	public MetaData getGeneratedMetaData() throws OperatorException {
		IOObjectEntry entry;
		try {
			entry = getRepositoryEntry();
		} catch (RepositoryException e) {
			throw handleSetupError(e);
		} catch (UndefinedParameterError e) {
			return super.getGeneratedMetaData();
		}
		try {
			MetaData metaData = entry.retrieveMetaData();
			metaData.getAnnotations().setAnnotation(Annotations.KEY_SOURCE, entry.getLocation().toString());
			return metaData;
		} catch (RepositoryException e) {
			getLogger().log(Level.INFO, "Error retrieving meta data from " + entry.getLocation() + ": " + e, e);
			return super.getGeneratedMetaData();
		}
	}

	/** @return {@code true} */
	@Override
	protected boolean isMetaDataCacheable() {
		return true;
	}

	/**
	 * Returns a {@link ProgressThread} that checks if the repository entry selected for the parameter
	 * {@value #PARAMETER_REPOSITORY_ENTRY} exists and compares that state to the previous one. Will mark the cache as
	 * dirty if the state changed.
	 *
	 * @return the progress thread
	 * @see #getCachedMetaData()
	 * @since 9.3
	 */
	private ProgressThread getPrecheckThread() {
		ProgressThread precheckThread = new ProgressThread("RepositorySource.precheck_metadata", false, getName()) {

			@Override
			public void run() {
				String repoLocationParam = null;
				boolean wasBrokenBefore = false;
				String unreplacedParameter = null;
				try {
					repoLocationParam = getParameter(PARAMETER_REPOSITORY_ENTRY);
					// cannot use the repoLocationParam to make the cache dirty via setParameter below: Macros are
					// already resolved there -> create unreplaced parameter
					unreplacedParameter = getParameters().getParameter(PARAMETER_REPOSITORY_ENTRY);
					wasBrokenBefore = getCachedMetaData() == null || IOObject.class.equals(getCachedMetaData().getObjectClass());
					checkCancelled();
					IOObjectEntry entry = getRepositoryEntry();
					// retrieve the meta data to prevent an infinite update loop in case the entry is there but the meta data is not
					entry.retrieveMetaData();
					checkCancelled();
					if (wasBrokenBefore) {
						// make cache dirty by setting the parameter to its current state; no change in value happens
						setParameter(PARAMETER_REPOSITORY_ENTRY, unreplacedParameter);
						transformMetaData();
					}
				} catch (RepositoryException e) {
					checkCancelled();
					if (!wasBrokenBefore && repoLocationParam != null) {
						// make cache dirty by setting the parameter to its current state; no change in value happens
						setParameter(PARAMETER_REPOSITORY_ENTRY, unreplacedParameter);
						transformMetaData();
					}
				} catch (UserError e) {
					// ignore; this will be handled elsewhere
				}
			}
		};
		precheckThread.addDependency(TRANSFORMER_THREAD_KEY);
		precheckThread.setIndeterminate(true);
		return precheckThread;
	}

	private IOObjectEntry getRepositoryEntry() throws RepositoryException, UserError {
		RepositoryLocation location = getParameterAsRepositoryLocation(PARAMETER_REPOSITORY_ENTRY);
		Entry entry = location.locateEntry();
		if (entry == null) {
			throw new RepositoryEntryNotFoundException(location);
		} else if (entry instanceof IOObjectEntry) {
			return (IOObjectEntry) entry;
		} else {
			throw new RepositoryEntryWrongTypeException(location, IOObjectEntry.TYPE_NAME, entry.getType());
		}
	}

	/**
	 * Handles the given exception by returning an {@link UserSetupError} containing an appropriate
	 * {@link com.rapidminer.operator.ProcessSetupError ProcessSetupError} for this operator.
	 * <p>
	 * A specific error is created for entries that can not be found or are of the wrong type.
	 * Otherwise a generic error is created.
	 *
	 * @since 9.2.0
	 */
	private UserSetupError handleSetupError(RepositoryException e) throws UserError {
		if (e instanceof RepositoryEntryNotFoundException || e instanceof RepositoryEntryWrongTypeException) {
			return new UserSetupError(this, new InvalidRepositoryEntryError(Severity.WARNING, getPortOwner(),
					PARAMETER_REPOSITORY_ENTRY,
					Collections.singletonList(new ParameterSettingQuickFix(this, PARAMETER_REPOSITORY_ENTRY)),
					REPO_ERROR_KEYS.get(e.getClass()), getParameterAsRepositoryLocation(PARAMETER_REPOSITORY_ENTRY),
					e.getMessage()));
		}
		return new UserSetupError(this, new SimpleProcessSetupError(Severity.WARNING, getPortOwner(),
				"repository_access_error", getParameterAsRepositoryLocation(PARAMETER_REPOSITORY_ENTRY),
				e.getMessage()));
	}

	@Override
	public IOObject read() throws OperatorException {
		try {
			final IOObject data = getRepositoryEntry().retrieveData(null);
			data.getAnnotations().setAnnotation(Annotations.KEY_SOURCE, getRepositoryEntry().getLocation().toString());
			logConnection(data);
			return data;
		} catch (RepositoryException e) {
			throw new UserError(this, e, 312, getParameterAsString(PARAMETER_REPOSITORY_ENTRY), e.getMessage());
		}
	}

	/**
	 * Logs if the data is a connection.
	 */
	private void logConnection(IOObject data) {
		if (data instanceof ConnectionInformationContainerIOObject) {
			ActionStatisticsCollector.INSTANCE.logNewConnection(this,
					((ConnectionInformationContainerIOObject) data).getConnectionInformation());
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterTypeRepositoryLocation type = new ParameterTypeRepositoryLocation(PARAMETER_REPOSITORY_ENTRY,
				"Repository entry.", false);
		type.setExpert(false);
		type.setPrimary(true);
		types.add(type);
		return types;
	}
}
