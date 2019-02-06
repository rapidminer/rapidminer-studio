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
package com.rapidminer.operator.meta;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.rapidminer.MacroHandler;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.nio.file.FileObject;
import com.rapidminer.operator.ports.CollectingPortPairExtender;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.PortPairExtender;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SubprocessTransformRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeRegexp;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;


/**
 * This operator is the base class for operators which loop over a file structure.
 *
 * @author Vaclav Uher
 */
public abstract class AbstractRepositoryIterator extends OperatorChain {

	private static final OperatorVersion OPERATOR_VERSION_OUT_PORTS = new OperatorVersion(5, 1, 1);

	private final PortPairExtender inputExtender = new PortPairExtender("in", getInputPorts(), getSubprocess(0)
			.getInnerSources());
	private final CollectingPortPairExtender outputExtender = new CollectingPortPairExtender("out", getSubprocess(0)
			.getInnerSinks(), getOutputPorts());

	private final String PORT_INNER_FILE_SOURCE = "repository object";
	private final OutputPort innerFileSource = getSubprocess(0).getInnerSources().createPort(PORT_INNER_FILE_SOURCE);

	public static final String PARAMETER_ENTRY_TYPE = "entry_type";
	public static final String PARAMETER_FILTER = "filter";

	public static final String PARAMETER_ENTRY_NAME_MACRO = "entry_name_macro";
	public static final String PARAMETER_REPOSITORY_PATH_MACRO = "repository_path_macro";
	public static final String PARAMETER_PARENT_FOLDER_MACRO = "parent_folder_macro";

	public static final String PARAMETER_RECURSIVE = "recursive";

	public static final String PARAMETER_FILTERED_STRING = "filtered_string";

	public static final String[] PARAMETERS_ENTRY_TYPE = new String[] { "IOObject", "blob" };
	public static final int IO_OBJECT = 0;
	public static final int BLOB = 1;

	public static final int FILTERED_STRING_FILE_NAME = 0;
	public static final int FILTERED_STRING_FILE_PATH = 1;
	public static final int FILTERED_STRING_PARENT_FOLDER_NAME = 2;
	public static final String[] FILTERED_STRINGS = { "file name (last part of the path)",
			"full path (including file name)", "parent folder name" };

	public AbstractRepositoryIterator(OperatorDescription description) {
		super(description, "Nested Process");
		inputExtender.start();
		outputExtender.start();
		getTransformer().addRule(inputExtender.makePassThroughRule());

		getTransformer().addRule(new GenerateNewMDRule(innerFileSource, IOObject.class) {

			@Override
			public MetaData modifyMetaData(MetaData unmodifiedMetaData) {
				int iterateOverType;
				try {
					iterateOverType = getParameterAsInt(PARAMETER_ENTRY_TYPE);
				} catch (UndefinedParameterError e) {
					// cannot happen because parameter has a default value
					return unmodifiedMetaData;
				}
				if (iterateOverType == BLOB) {
					return new MetaData(FileObject.class);
				} else {
					return unmodifiedMetaData;
				}
			}
		});

		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));

		getTransformer().addRule(outputExtender.makePassThroughRule());
	}

	@Override
	public void doWork() throws OperatorException {

		Pattern filter = null;
		if (isParameterSet(PARAMETER_FILTER)) {
			String filterString = getParameterAsString(PARAMETER_FILTER);
			filter = Pattern.compile(filterString);
		}
		fileNameMacro = getParameterAsString(PARAMETER_ENTRY_NAME_MACRO);
		pathNameMacro = getParameterAsString(PARAMETER_REPOSITORY_PATH_MACRO);
		parentFolderMacro = getParameterAsString(PARAMETER_PARENT_FOLDER_MACRO);

		boolean recursive = getParameterAsBoolean(PARAMETER_RECURSIVE);

		int type = getParameterAsInt(PARAMETER_ENTRY_TYPE);

		iterate(null, filter, recursive, type);
	}

	private String fileNameMacro;
	private String pathNameMacro;
	private String parentFolderMacro;

	abstract protected void iterate(Object currentParent, Pattern filter, boolean recursive, int type)
			throws OperatorException;

	protected void doWorkForSingleIterationStep(String fileName, String fullPath, String parentName, IOObject fileObject)
			throws OperatorException {
		clearAllInnerSinks();

		MacroHandler macroHandler = getProcess().getMacroHandler();
		macroHandler.addMacro(fileNameMacro, fileName);
		macroHandler.addMacro(pathNameMacro, fullPath);
		macroHandler.addMacro(parentFolderMacro, parentName);
		innerFileSource.deliver(fileObject);
		inputExtender.passDataThrough();
		super.doWork();
		outputExtender.collect();
	}

	@Override
	public boolean shouldAutoConnect(InputPort inputPort) {
		if (getCompatibilityLevel().isAtMost(OPERATOR_VERSION_OUT_PORTS)) {
			return false;
		}
		return true;
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return new OperatorVersion[] { OPERATOR_VERSION_OUT_PORTS };
	}

	protected boolean matchesFilter(Pattern filter, String fileName, String fullPath, String parentPath) {
		if (filter != null) {
			int matchWhat;
			try {
				matchWhat = getParameterAsInt(PARAMETER_FILTERED_STRING);
			} catch (UndefinedParameterError e) {
				matchWhat = FILTERED_STRING_FILE_NAME;
			}

			String matchedName = null;
			switch (matchWhat) {
				case FILTERED_STRING_FILE_NAME:
					matchedName = fileName;
					break;
				case FILTERED_STRING_FILE_PATH:
					matchedName = fullPath;
					break;
				case FILTERED_STRING_PARENT_FOLDER_NAME:
					matchedName = parentPath.substring(parentPath.lastIndexOf("/") + 1);
					break;
				default:
					throw new RuntimeException("Illegal parameter value for filtered string");
			}
			Matcher matcher = filter.matcher(matchedName);
			return matcher.matches();
		} else {
			return true;
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType type = new ParameterTypeCategory(PARAMETER_ENTRY_TYPE, "Type of object in repository to loop.",
				PARAMETERS_ENTRY_TYPE, BLOB, false);
		type.setExpert(false);
		types.add(type);

		types.add(new ParameterTypeRegexp(
				PARAMETER_FILTER,
				"Specifies a regular expression which is used as filter for the file and directory names, e.g. 'a.*b' for all files starting with 'a' and ending with 'b'. Ignored if empty.",
				true, false));

		ParameterType typeCategory = new ParameterTypeCategory(PARAMETER_FILTERED_STRING,
				"Indicates which part of the file name is matched against the filter expression.", FILTERED_STRINGS,
				FILTERED_STRING_FILE_NAME, true);
		types.add(typeCategory);

		types.add(new ParameterTypeString(
				PARAMETER_ENTRY_NAME_MACRO,
				"Specifies the name of the macro, which delievers the current file name without path. Use %{macro_name} to use the file name in suboperators.",
				"entry_name", false));
		types.add(new ParameterTypeString(
				PARAMETER_REPOSITORY_PATH_MACRO,
				"Specifies the name of the macro containing the absolute path and file name of the current file. Use %{macro_name} to address the file in suboperators.",
				"repository_path", false));
		types.add(new ParameterTypeString(
				PARAMETER_PARENT_FOLDER_MACRO,
				"Specifies the name of the macro containing the parent folder name of the current file's directory. Use %{macro_name} to address the file in suboperators.",
				"parent_folder", false));

		types.add(new ParameterTypeBoolean(PARAMETER_RECURSIVE,
				"Indicates if the operator will also deliver the files / directories of subdirectories (resursively).",
				true, false));

		return types;
	}
}
