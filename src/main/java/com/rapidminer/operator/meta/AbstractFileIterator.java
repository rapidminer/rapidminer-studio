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
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.nio.file.FileObject;
import com.rapidminer.operator.ports.CollectingPortPairExtender;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.PortPairExtender;
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
 * @author Sebastian Land, Ingo Mierswa, Marius Helf
 */
public abstract class AbstractFileIterator extends OperatorChain {

	private static final OperatorVersion OPERATOR_VERSION_OUT_PORTS = new OperatorVersion(5, 1, 1);
	private final PortPairExtender inputExtender = new PortPairExtender("in", getInputPorts(), getSubprocess(0)
			.getInnerSources());
	private final CollectingPortPairExtender outputExtender = new CollectingPortPairExtender("out", getSubprocess(0)
			.getInnerSinks(), getOutputPorts());

	private final String PORT_INNER_FILE_SOURCE = "file object";
	private final OutputPort innerFileSource = getSubprocess(0).getInnerSources().createPort(PORT_INNER_FILE_SOURCE);

	public static final String PARAMETER_FILTER = "filter";

	public static final String PARAMETER_FILE_NAME_MACRO = "file_name_macro";
	public static final String PARAMETER_FILE_PATH_MACRO = "file_path_macro";
	public static final String PARAMETER_PARENT_PATH_MACRO = "parent_path_macro";

	public static final String PARAMETER_RECURSIVE = "recursive";

	public static final String PARAMETER_ITERATE_OVER_SUBDIRS = "iterate_over_subdirs";
	public static final String PARAMETER_ITERATE_OVER_FILES = "iterate_over_files";

	public static final String PARAMETER_FILTERED_STRING = "filtered_string";

	public static final int FILTERED_STRING_FILE_NAME = 0;
	public static final int FILTERED_STRING_FILE_PATH = 1;
	public static final int FILTERED_STRING_PARENT_PATH = 2;
	public static final String[] FILTERED_STRINGS = { "file name (last part of the path)",
			"full path (including file name)", "parent path" };

	public AbstractFileIterator(OperatorDescription description) {
		super(description, "Nested Process");
		inputExtender.start();
		outputExtender.start();
		getTransformer().addGenerationRule(innerFileSource, FileObject.class);
		getTransformer().addRule(inputExtender.makePassThroughRule());
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

		fileNameMacro = getParameterAsString(PARAMETER_FILE_NAME_MACRO);
		pathNameMacro = getParameterAsString(PARAMETER_FILE_PATH_MACRO);
		parentPathMacro = getParameterAsString(PARAMETER_PARENT_PATH_MACRO);

		boolean recursive = getParameterAsBoolean(PARAMETER_RECURSIVE);

		boolean iterateFiles = getParameterAsBoolean(PARAMETER_ITERATE_OVER_FILES);
		boolean iterateSubDirs = getParameterAsBoolean(PARAMETER_ITERATE_OVER_SUBDIRS);

		macroHandler = getProcess().getMacroHandler();

		iterate(null, filter, iterateSubDirs, iterateFiles, recursive);
	}

	MacroHandler macroHandler;
	String fileNameMacro;
	String pathNameMacro;
	String parentPathMacro;

	abstract protected void iterate(Object currentParent, Pattern filter, boolean iterateSubDirs, boolean iterateFiles,
			boolean recursive) throws OperatorException;

	protected void doWorkForSingleIterationStep(String fileName, String fullPath, String parentPath, FileObject fileObject)
			throws OperatorException {
		clearAllInnerSinks();
		macroHandler.addMacro(fileNameMacro, fileName);
		macroHandler.addMacro(pathNameMacro, fullPath);
		macroHandler.addMacro(parentPathMacro, parentPath);
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
				case FILTERED_STRING_PARENT_PATH:
					matchedName = parentPath;
					break;
				default:
					assert false;	// illegal parameter value for filtered string
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

		types.add(new ParameterTypeRegexp(
				PARAMETER_FILTER,
				"Specifies a regular expression which is used as filter for the file and directory names, e.g. 'a.*b' for all files starting with 'a' and ending with 'b'. Ignored if empty.",
				true, false));

		ParameterType type = new ParameterTypeCategory(PARAMETER_FILTERED_STRING,
				"Indicates which part of the file name is matched against the filter expression.", FILTERED_STRINGS,
				FILTERED_STRING_FILE_NAME, true);
		types.add(type);

		types.add(new ParameterTypeString(
				PARAMETER_FILE_NAME_MACRO,
				"Specifies the name of the macro, which delievers the current file name without path. Use %{macro_name} to use the file name in suboperators.",
				"file_name", false));
		types.add(new ParameterTypeString(
				PARAMETER_FILE_PATH_MACRO,
				"Specifies the name of the macro containing the absolute path and file name of the current file. Use %{macro_name} to address the file in suboperators.",
				"file_path", false));
		types.add(new ParameterTypeString(
				PARAMETER_PARENT_PATH_MACRO,
				"Specifies the name of the macro containing the absolute path of the current file's directory. Use %{macro_name} to address the file in suboperators.",
				"parent_path", false));

		types.add(new ParameterTypeBoolean(PARAMETER_RECURSIVE,
				"Indicates if the operator will also deliver the files / directories of subdirectories (resursively).",
				false, false));

		types.add(new ParameterTypeBoolean(
				PARAMETER_ITERATE_OVER_FILES,
				"If checked, the operator will iterate over files in the given directory and set their path and name macros.",
				true, false));
		types.add(new ParameterTypeBoolean(
				PARAMETER_ITERATE_OVER_SUBDIRS,
				"If checked, the operator will iterate over subdirectories in the given directory and set their path and name macros.",
				false, false));
		return types;
	}

	protected class EntryContainer {

		public String fileName;
		public String fullPath;
		public String parentPath;
		public FileObject fileObject;

		public EntryContainer(String file, String full, String parent, FileObject object) {
			this.fileName = file;
			this.fullPath = full;
			this.parentPath = parent;
			this.fileObject = object;
		}
	}
}
