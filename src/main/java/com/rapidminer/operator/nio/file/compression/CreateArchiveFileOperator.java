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
import com.rapidminer.operator.nio.file.compression.ArchiveFileObject.FuzzyCompressionLevel;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;

import java.util.List;


/**
 * An operator which creates an archive file, like e.g. a zip file.
 * 
 * Currently only zip files are supported, but the operator could be extended to support also other
 * types of archives like rar, tar, ...
 * 
 * @author Marius Helf
 * 
 */
public class CreateArchiveFileOperator extends Operator {

	public static final String[] BUFFER_TYPES = { "memory", "file" };
	public static final int BUFFER_TYPE_MEMORY = 0;
	public static final int BUFFER_TYPE_FILE = 1;

	public static final String PARAMETER_BUFFER_TYPE = "buffer_type";
	public static final String PARAMETER_USE_DEFAULT_COMPRESSION_LEVEL = "use_default_compression_level";
	public static final String PARAMETER_COMPRESSION_LEVEL = "compression_level";

	public static final String[] COMPRESSION_LEVELS = { "best compression", "fastest compression", "no compression",
			"default compression" };
	public static final int COMPRESSION_LEVEL_BEST = 0;
	public static final int COMPRESSION_LEVEL_FASTEST = 1;
	public static final int COMPRESSION_LEVEL_NONE = 2;
	public static final int COMPRESSION_LEVEL_DEFAULT = 3;

	OutputPort archiveFileOuputPort = getOutputPorts().createPort("archive file");

	/**
	 * @param description
	 */
	public CreateArchiveFileOperator(OperatorDescription description) {
		super(description);
		getTransformer().addRule(new GenerateNewMDRule(archiveFileOuputPort, ZipFileObject.class));
	}

	@Override
	public void doWork() throws OperatorException {
		ZipFileObject archiveFileObject = null;
		switch (getParameterAsInt(PARAMETER_BUFFER_TYPE)) {
			case BUFFER_TYPE_FILE:
				archiveFileObject = new ZipFileObject(ZipFileObject.BufferType.FILE);
				break;
			case BUFFER_TYPE_MEMORY:
				archiveFileObject = new ZipFileObject(ZipFileObject.BufferType.MEMORY);
				break;
			default:
				throw new RuntimeException("illegal parameter value for " + PARAMETER_BUFFER_TYPE);
		}

		if (!getParameterAsBoolean(PARAMETER_USE_DEFAULT_COMPRESSION_LEVEL)) {
			switch (getParameterAsInt(PARAMETER_COMPRESSION_LEVEL)) {
				case COMPRESSION_LEVEL_BEST:
					archiveFileObject.setCompressionLevel(FuzzyCompressionLevel.BEST);
					break;
				case COMPRESSION_LEVEL_FASTEST:
					archiveFileObject.setCompressionLevel(FuzzyCompressionLevel.FASTEST);
					break;
				case COMPRESSION_LEVEL_NONE:
					archiveFileObject.setCompressionLevel(FuzzyCompressionLevel.NONE);
					break;
				case COMPRESSION_LEVEL_DEFAULT:
				default:
					archiveFileObject.setCompressionLevel(FuzzyCompressionLevel.DEFAULT);
					break;
			}
		}
		archiveFileOuputPort.deliver(archiveFileObject);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeCategory(
				PARAMETER_BUFFER_TYPE,
				"Defines where the buffer for the archive file will be created. A memory buffered archive file will usually perform faster in terms of execution time, but the complete archive file must be kept in memory, which can lead to problems if large files or a large amount of files is added to the archive.",
				BUFFER_TYPES, BUFFER_TYPE_FILE, true));
		types.add(new ParameterTypeBoolean(
				PARAMETER_USE_DEFAULT_COMPRESSION_LEVEL,
				"This parameter allows you to override the default compression level. The default compression level depends on the host machine, but usually offers a reasonable trade-off between execution time and compression factor.",
				true, true));
		ParameterType type = new ParameterTypeCategory(
				PARAMETER_COMPRESSION_LEVEL,
				"The default compression level of the created zip file is specified by this parameter. This level may be overridden in the subsequent Add Entry to Zip File operators on a per-entry base. A compression level of 0 stands for no compression, whereas the highest level 9 means best compression. In general, higher compression levels result also in a higher runtime.",
				COMPRESSION_LEVELS, COMPRESSION_LEVEL_DEFAULT, true);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_DEFAULT_COMPRESSION_LEVEL, true,
				false));
		types.add(type);

		return types;
	}
}
