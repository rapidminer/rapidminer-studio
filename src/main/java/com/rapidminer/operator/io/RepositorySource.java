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

import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.InvalidRepositoryEntryError;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.SimpleProcessSetupError;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
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
	}

	@Override
	public MetaData getGeneratedMetaData() throws OperatorException {
		IOObjectEntry entry;
		try {
			entry = getRepositoryEntry();
		} catch (RepositoryException | UndefinedParameterError e) {
			handleSetupError(e);
			return super.getGeneratedMetaData();
		}
		try {
			MetaData metaData = entry.retrieveMetaData().clone();
			// We reduce the number of nominal values to a limit here to keep meta data
			// transformations fast.
			if (metaData instanceof ExampleSetMetaData) {
				for (AttributeMetaData amd : ((ExampleSetMetaData) metaData).getAllAttributes()) {
					if (amd.isNominal()) {
						amd.shrinkValueSet();
					}
				}
			}
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

	private IOObjectEntry getRepositoryEntry() throws RepositoryException, UserError {
		RepositoryLocation location = getParameterAsRepositoryLocation(PARAMETER_REPOSITORY_ENTRY);
		Entry entry = location.locateEntry();
		if (entry == null) {
			throw new RepositoryEntryNotFoundException("Entry '" + location + "' does not exist.");
		} else if (entry instanceof IOObjectEntry) {
			return (IOObjectEntry) entry;
		} else {
			throw new RepositoryEntryWrongTypeException("Entry '" + location + "' is not a data entry, but " + entry.getType());
		}
	}

	/**
	 * Handles the given exception by adding an appropriate {@link com.rapidminer.operator.ProcessSetupError ProcessSetupError}
	 * to this operator if applicable.
	 * <p>
	 * Exceptions that are not {@link RepositoryException RepositoryExceptions} will be ignored.
	 * A specific error is added for entries that can not be found or are of the wrong type. Otherwise a generic error is added.
	 *
	 * @since 9.2.0
	 */
	private void handleSetupError(Exception e) throws UserError{
		if (!(e instanceof RepositoryException)) {
			return;
		}
		if (e instanceof RepositoryEntryNotFoundException || e instanceof RepositoryEntryWrongTypeException) {
			addError(new InvalidRepositoryEntryError(Severity.WARNING, getPortOwner(), PARAMETER_REPOSITORY_ENTRY,
					Collections.singletonList(new ParameterSettingQuickFix(getPortOwner().getOperator(), PARAMETER_REPOSITORY_ENTRY)),
					REPO_ERROR_KEYS.get(e.getClass()), getParameterAsRepositoryLocation(PARAMETER_REPOSITORY_ENTRY),
					e.getMessage()));
			return;
		}
		addError(new SimpleProcessSetupError(Severity.WARNING, getPortOwner(), "repository_access_error",
				getParameterAsRepositoryLocation(PARAMETER_REPOSITORY_ENTRY), e.getMessage()));
	}

	@Override
	public IOObject read() throws OperatorException {
		try {
			final IOObject data = getRepositoryEntry().retrieveData(null);
			data.getAnnotations().setAnnotation(Annotations.KEY_SOURCE, getRepositoryEntry().getLocation().toString());
			return data;
		} catch (RepositoryException e) {
			throw new UserError(this, e, 312, getParameterAsString(PARAMETER_REPOSITORY_ENTRY), e.getMessage());
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
