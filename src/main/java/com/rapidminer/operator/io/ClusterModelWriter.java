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

import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.clustering.ClusterModelInterface;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.GZIPOutputStream;


/**
 * Write a single cluster model to a file.
 * 
 * @author Sebastian Land
 * 
 */
public class ClusterModelWriter extends AbstractWriter<ClusterModelInterface> {

	/** The parameter name for &quot;the file to which the cluster model is stored&quot; */
	public static final String PARAMETER_CLUSTER_MODEL_FILE = "cluster_model_file";

	public ClusterModelWriter(OperatorDescription description) {
		super(description, ClusterModelInterface.class);

	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeFile(PARAMETER_CLUSTER_MODEL_FILE,
				"the file to which the cluster model is stored", "clm", false);
		type.setExpert(false);
		types.add(type);
		return types;
	}

	@Override
	public ClusterModelInterface write(ClusterModelInterface model) throws OperatorException {

		File file = getParameterAsFile(PARAMETER_CLUSTER_MODEL_FILE, true);
		OutputStream out = null;
		try {
			out = new GZIPOutputStream(new FileOutputStream(file));
			model.write(out);
		} catch (IOException e) {
			throw new UserError(this, e, 303, new Object[] { file, e.getMessage() });
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					logError("Cannot close stream to file " + file);
				}
			}
		}
		return model;
	}
}
