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
package com.rapidminer.operator.nio.model;

import javax.swing.table.TableModel;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.tools.ProgressListener;


/**
 * This is the interface for the factories of {@link DataResultSet}s. They usually store information
 * needed to create the result set like for example resource identifiers, queries and so on. From
 * that, they construct the result set.
 *
 * @author Sebastian Land, Simon Fischer
 */
public interface DataResultSetFactory extends AutoCloseable {

	/** Creates a result set. Make sure to call {@link #close()} after using this method. */
	public DataResultSet makeDataResultSet(Operator operator) throws OperatorException;

	/**
	 * This method has to return a table model that can be used for showing a preview.
	 */
	public TableModel makePreviewTableModel(ProgressListener listener) throws OperatorException, ParseException;

	/**
	 * Returns the human readable name of the resource read (most often, this will be a file or
	 * URL).
	 */
	public String getResourceName();

	/**
	 * Makes initial meta data. Only the number of rows should be filled in here. All other
	 * information will later be added by {@link DataResultSetTranslationConfiguration}
	 */
	public ExampleSetMetaData makeMetaData();

	/** Sets the configuration parameters in the given reader operator. */
	public void setParameters(AbstractDataResultSetReader reader);

	/** Closes all resources associated with this factory without throwing an exception. */
	@Override
	public void close();
}
