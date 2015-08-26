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

import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.clustering.ClusterModel;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.tools.OperatorService;

import java.util.List;


/**
 * Reads a single cluster model from a file.
 * 
 * @author Sebastian Land
 * 
 */
public class ClusterModelReader extends AbstractReader<ClusterModel> {

	/** The parameter name for &quot;the file from which the cluster model is read&quot; */
	public static final String PARAMETER_CLUSTER_MODEL_FILE = "cluster_model_file";
	public static final String PARAMETER_IS_HIERARCHICAL_MODEL_FILE = "is_hierarchical_model_file";

	public ClusterModelReader(OperatorDescription description) {
		super(description, ClusterModel.class);
	}

	@Override
	public ClusterModel read() throws OperatorException {
		try {
			IOObjectReader ioReader = OperatorService.createOperator(IOObjectReader.class);
			ioReader.setParameter(IOObjectReader.PARAMETER_OBJECT_FILE, getParameterAsString(PARAMETER_CLUSTER_MODEL_FILE));
			if (getParameterAsBoolean(PARAMETER_IS_HIERARCHICAL_MODEL_FILE)) {
				ioReader.setParameter(IOObjectReader.PARAMETER_IO_OBJECT, "HierarchicalClusterModel");
			} else {
				ioReader.setParameter(IOObjectReader.PARAMETER_IO_OBJECT, "ClusterModel");
			}
			return (ClusterModel) ioReader.read();
		} catch (OperatorCreationException e) {
			throw new OperatorException("Cannot create IOObjectReader");
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeFile(PARAMETER_CLUSTER_MODEL_FILE, "the file from which the cluster model is read",
				"clm", false));
		types.add(new ParameterTypeBoolean(PARAMETER_IS_HIERARCHICAL_MODEL_FILE,
				"indicates that the stored model file is a hierarchical cluster model", false));
		return types;
	}
}
