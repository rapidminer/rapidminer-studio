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
package com.rapidminer;

import com.rapidminer.tools.XMLException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;


/**
 * <p>
 * WARNING: This class is now deprecated. Please use the class {@link Process} instead!.
 * </p>
 * 
 * <p>
 * This class was introduced to avoid confusing handling of operator maps and other stuff when a new
 * experiment is created. It is also necessary for file name resolving and breakpoint handling.
 * </p>
 * 
 * <p>
 * If you want to use RapidMiner from your own application the best way is often to create an
 * experiment from the scratch (by adding the complete operator tree to the experiment root
 * operator) or from a file (for example created with the GUI beforehand) and start it by invoking
 * the {@link #run()} method.
 * </p>
 * 
 * @deprecated Please use the new class {@link Process} instead
 * @author Ingo Mierswa
 */
@Deprecated
public class Experiment extends Process {

	/**
	 * Constructs an experiment consisting only of a SimpleOperatorChain.
	 * 
	 * @deprecated Please use class {@link Process} now
	 */
	@Deprecated
	public Experiment() {
		super();
	}

	/**
	 * Creates a new experiment from the given URL. #
	 * 
	 * @deprecated Please use class {@link Process} now
	 */
	@Deprecated
	public Experiment(URL url) throws IOException, XMLException {
		super(url);
	}

	/**
	 * Creates a new experiment from the given experiment file. This might have been created with
	 * the GUI beforehand.
	 * 
	 * @deprecated Please use class {@link Process} now
	 */
	@Deprecated
	public Experiment(File file) throws IOException, XMLException {
		super(file);
	}

	/**
	 * Reads an experiment configuration from an XML String.
	 * 
	 * @deprecated Please use class {@link Process} now
	 */
	@Deprecated
	public Experiment(String xmlString) throws IOException, XMLException {
		super(xmlString);
	}

	/**
	 * Reads an experiment configuration from the given file.
	 * 
	 * @deprecated Please use class {@link Process} now
	 */
	@Deprecated
	public Experiment(InputStream in) throws IOException, XMLException {
		super(in);
	}
}
