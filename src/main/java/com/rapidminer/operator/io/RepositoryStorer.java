/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeRepositoryLocation;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryManager;

import java.util.List;


/**
 * This operator stores IOObjects at a location in a repository.
 * 
 * @author Simon Fischer
 * 
 */
public class RepositoryStorer extends AbstractWriter<IOObject> {

	public static final String PARAMETER_REPOSITORY_ENTRY = "repository_entry";

	public RepositoryStorer(OperatorDescription description) {
		super(description, IOObject.class);
	}

	@Override
	public IOObject write(IOObject ioobject) throws OperatorException {
		try {
			RepositoryLocation location = getParameterAsRepositoryLocation(PARAMETER_REPOSITORY_ENTRY);
			return RepositoryManager.getInstance(null).store(ioobject, location, this);
		} catch (RepositoryException e) {
			throw new UserError(this, e, 315, getParameterAsString(PARAMETER_REPOSITORY_ENTRY), e.getMessage());
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterTypeRepositoryLocation type = new ParameterTypeRepositoryLocation(PARAMETER_REPOSITORY_ENTRY,
				"Repository entry.", true, false, false, false, true, true);
		type.setExpert(false);
		types.add(type);
		return types;
	}
}
