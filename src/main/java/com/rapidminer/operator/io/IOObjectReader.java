/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.operator.io;

import com.rapidminer.operator.AbstractIOObject;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.OperatorService;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;


/**
 * Generic reader for all types of IOObjects. Reads an IOObject from a file.
 * 
 * @author Ingo Mierswa
 */
public class IOObjectReader extends AbstractReader<IOObject> {

	/** The parameter name for &quot;Filename of the object file.&quot; */
	public static final String PARAMETER_OBJECT_FILE = "object_file";

	static {
		AbstractReader.registerReaderDescription(new ReaderDescription("ioo", IOObjectReader.class, PARAMETER_OBJECT_FILE));
	}

	/** The parameter name for &quot;The class of the object(s) which should be saved.&quot; */
	public static final String PARAMETER_IO_OBJECT = "io_object";

	public static final String PARAMETER_IGNORE_TYPE = "ignore_type";

	private String[] objectArray = null;

	public IOObjectReader(OperatorDescription description) {
		super(description, IOObject.class);
	}

	@Override
	public MetaData getGeneratedMetaData() throws OperatorException {
		if (getParameterAsBoolean(PARAMETER_IGNORE_TYPE)) {
			return super.getGeneratedMetaData();
		} else {
			try {
				return new MetaData(getSelectedClass());
			} catch (UndefinedParameterError e) {
				return super.getGeneratedMetaData();
			}
		}
	}

	@Override
	public IOObject read() throws OperatorException {
		getParameter(PARAMETER_OBJECT_FILE);
		AbstractIOObject.InputStreamProvider inputStreamProvider = new AbstractIOObject.InputStreamProvider() {

			@Override
			public InputStream getInputStream() throws IOException {
				try {
					return getParameterAsInputStream(PARAMETER_OBJECT_FILE);
				} catch (UndefinedParameterError e) {
					throw new IOException(e);
				} catch (UserError e) {
					throw new IOException(e);
				}
			}
		};
		IOObject object;
		try {
			object = AbstractIOObject.read(inputStreamProvider);
		} catch (IOException e) {
			throw new UserError(this, e, 302, getParameter(PARAMETER_OBJECT_FILE), e);
		}

		if (object == null) {
			throw new UserError(this, 302, new Object[] { getParameter(PARAMETER_OBJECT_FILE), "cannot load object file" });
		} else {
			Class clazz = getSelectedClass();
			if (!(clazz.isInstance(object)) && !getParameterAsBoolean(PARAMETER_IGNORE_TYPE)) {
				throw new UserError(this, 942, new Object[] { getParameter(PARAMETER_OBJECT_FILE), clazz.getSimpleName(),
						object.getClass().getSimpleName() });
			} else {
				return object;
			}
		}
	}

	private Class<? extends IOObject> getSelectedClass() throws UndefinedParameterError {
		int ioType = getParameterAsInt(PARAMETER_IO_OBJECT);
		if (getIOObjectNames() != null) {
			if (ioType != -1) {
				return OperatorService.getIOObjectClass(getIOObjectNames()[ioType]);
			} else {
				return IOObject.class;
			}

		} else {
			return null;
		}
	}

	private String[] getIOObjectNames() {
		if (this.objectArray == null) {
			Set<String> ioObjects = OperatorService.getIOObjectsNames();
			this.objectArray = ioObjects.toArray(new String[ioObjects.size()]);
		}
		return objectArray;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeFile(PARAMETER_OBJECT_FILE, "Filename of the object file.", "ioo", false));
		types.add(new ParameterTypeBoolean(PARAMETER_IGNORE_TYPE,
				"Indicates if the execution should be aborted if type of read object does not match selected type.", false));
		ParameterType type = new ParameterTypeCategory(PARAMETER_IO_OBJECT,
				"The class of the object(s) which should be saved.", getIOObjectNames(), 0, false);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_IGNORE_TYPE, false, false));
		types.add(type);

		return types;
	}
}
