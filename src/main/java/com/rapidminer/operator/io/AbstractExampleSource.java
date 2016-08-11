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

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;


/**
 * Super class of all operators requiring no input and creating an {@link ExampleSet}.
 *
 * @author Simon Fischer
 */
public abstract class AbstractExampleSource extends AbstractReader<ExampleSet> {

	public AbstractExampleSource(final OperatorDescription description) {
		super(description, ExampleSet.class);
	}

	@Override
	public MetaData getGeneratedMetaData() throws OperatorException {
		return new ExampleSetMetaData();
	}

	/** Creates (or reads) the ExampleSet that will be returned by {@link #apply()}. */
	public abstract ExampleSet createExampleSet() throws OperatorException;

	@Override
	public ExampleSet read() throws OperatorException {
		return createExampleSet();
	}

}
