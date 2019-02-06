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
package com.rapidminer.operator.nio.file.compression;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.nio.file.FileObject;
import com.rapidminer.operator.nio.file.compression.ArchiveFileObject.FuzzyCompressionLevel;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.InputPortExtender;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;

import java.util.List;


/**
 * An operator which adds one or more files to an archive file. Archive files are files like zip
 * files etc.
 * 
 * @author Marius Helf
 * 
 */
public class AddEntryToArchiveFile extends Operator {

	public static final String PARAMETER_OVERRIDE_COMPRESSION_LEVEL = "override_compression_level";
	public static final String PARAMETER_COMPRESSION_LEVEL = "compression_level";
	public static final String PARAMETER_DIRECTORY = "directory";

	public static final String[] COMPRESSION_LEVELS = { "best compression", "fastest compression", "no compression",
			"default compression" };
	public static final int COMPRESSION_LEVEL_BEST = 0;
	public static final int COMPRESSION_LEVEL_FASTEST = 1;
	public static final int COMPRESSION_LEVEL_NONE = 2;
	public static final int COMPRESSION_LEVEL_DEFAULT = 3;

	private InputPort archiveFileInput = getInputPorts().createPort("archive file", new MetaData(ZipFileObject.class));
	private InputPortExtender fileInput = new InputPortExtender("file input", getInputPorts(),
			new MetaData(FileObject.class), true);
	private OutputPort archiveFileOutput = getOutputPorts().createPort("archive file");

	public AddEntryToArchiveFile(OperatorDescription description) {
		super(description);
		getTransformer().addPassThroughRule(archiveFileInput, archiveFileOutput);
		fileInput.start();
	}

	@Override
	public void doWork() throws OperatorException {
		ZipFileObject archiveFile = archiveFileInput.getData(ZipFileObject.class);
		List<FileObject> files = fileInput.getData(FileObject.class, true);
		boolean overrideCompressionLevel = getParameterAsBoolean(PARAMETER_OVERRIDE_COMPRESSION_LEVEL);

		FuzzyCompressionLevel compressionLevel = null;
		if (overrideCompressionLevel) {
			switch (getParameterAsInt(PARAMETER_COMPRESSION_LEVEL)) {
				case COMPRESSION_LEVEL_BEST:
					compressionLevel = FuzzyCompressionLevel.BEST;
					break;
				case COMPRESSION_LEVEL_FASTEST:
					compressionLevel = FuzzyCompressionLevel.FASTEST;
					break;
				case COMPRESSION_LEVEL_NONE:
					compressionLevel = FuzzyCompressionLevel.NONE;
					break;
				case COMPRESSION_LEVEL_DEFAULT:
				default:
					compressionLevel = FuzzyCompressionLevel.DEFAULT;
					break;
			}
			if (!archiveFile.supportsComppressionLevel(compressionLevel)) {
				overrideCompressionLevel = false;
			}
		}

		String directory = getParameterAsString(PARAMETER_DIRECTORY);
		for (FileObject file : files) {
			if (overrideCompressionLevel) {
				archiveFile.addEntry(file, directory, compressionLevel);
			} else {
				archiveFile.addEntry(file, directory);
			}
			checkForStop();
		}
		archiveFileOutput.deliver(archiveFile);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeString(
				PARAMETER_DIRECTORY,
				"This parameter specifies the directory where the entry will be stored inside the archive file. Specify it in the form 'my/sub/directory', or leave it empty to store the entry in the root folder.",
				"", false));
		types.add(new ParameterTypeBoolean(
				PARAMETER_OVERRIDE_COMPRESSION_LEVEL,
				"This parameter allows to override the default compression	level of the archive file object for the entries created by this operator. The default level is set by the Create Archive File operator	that created the archive file object. This is useful, if you are adding	pre-compressed files to	the archive, such as zip files, jar files etc. These files cannot be further compressed, so you can save some execution time by setting the compression level for new entries of this kind to a low value.",
				false, true));
		ParameterType type = new ParameterTypeCategory(
				PARAMETER_COMPRESSION_LEVEL,
				"The compression level of the newly created entries is specified through this parameter.	In general, higher compression levels result also in a higher runtime.",
				COMPRESSION_LEVELS, COMPRESSION_LEVEL_DEFAULT, true);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_OVERRIDE_COMPRESSION_LEVEL, true,
				true));
		types.add(type);

		return types;
	}
}
