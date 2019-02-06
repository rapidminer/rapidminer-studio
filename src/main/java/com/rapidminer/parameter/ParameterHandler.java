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
package com.rapidminer.parameter;

import com.rapidminer.Process;
import com.rapidminer.gui.renderer.Renderer;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.UserError;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;


/**
 * This interface defines that instance are able to handle parameters. In RapidMiner, this if for
 * example true for the class {@link Operator} but also for the {@link Renderer}s.
 * 
 * @author Ingo Mierswa
 */
public interface ParameterHandler {

	/** Returns a collection of all parameters of this parameter handler. */
	public Parameters getParameters();

	/**
	 * Sets all parameters of this operator. The given parameters are not allowed to be null and
	 * must correspond to the parameter types defined by this parameter handler.
	 */
	public void setParameters(Parameters parameters);

	/**
	 * Sets the given single parameter to the Parameters object of this operator. For parameter list
	 * the method {@link #setListParameter(String, List)} should be used.
	 */
	public void setParameter(String key, String value);

	/**
	 * Sets the given parameter list to the Parameters object of this operator. For single
	 * parameters the method {@link #setParameter(String, String)} should be used.
	 */
	public void setListParameter(String key, List<String[]> list);

	/**
	 * Returns a single parameter retrieved from the {@link Parameters} of this Operator.
	 */
	public Object getParameter(String key) throws UndefinedParameterError;

	/** Returns true iff the parameter with the given name is set. */
	public boolean isParameterSet(String key) throws UndefinedParameterError;

	/** Returns a single named parameter and casts it to String. */
	public String getParameterAsString(String key) throws UndefinedParameterError;

	/** Returns a single named parameter and casts it to char. */
	public char getParameterAsChar(String key) throws UndefinedParameterError;

	/** Returns a single named parameter and casts it to int. */
	public int getParameterAsInt(String key) throws UndefinedParameterError;

	/** Returns a single named parameter and casts it to double. */
	public double getParameterAsDouble(String key) throws UndefinedParameterError;

	/** Returns a single named parameter and casts it to long. */
	public long getParameterAsLong(String key) throws UndefinedParameterError;

	/**
	 * Returns a single named parameter and casts it to boolean. This method never throws an
	 * exception since there are no non-optional boolean parameters.
	 */
	public boolean getParameterAsBoolean(String key);

	/**
	 * Returns a single named parameter and casts it to List. The list returned by this method
	 * contains the user defined key-value pairs. Each element is an String array of length 2. The
	 * first element is the key, the second the parameter value.
	 */
	public List<String[]> getParameterList(String key) throws UndefinedParameterError;

	/**
	 * Returns a Pair of Strings, the Strings are in the order of type definition of the subtypes.
	 */
	public String[] getParameterTupel(String key) throws UndefinedParameterError;

	/** Returns a single named parameter and casts it to Color. */
	public java.awt.Color getParameterAsColor(String key) throws UndefinedParameterError;

	/**
	 * Returns a single named parameter and tries to handle it as URL. If this works, this method
	 * creates an input stream from this URL and delivers it. If not, this method tries to cast the
	 * parameter value to a file. This file is already resolved against the process definition file.
	 * If the parameter name defines a non-optional parameter which is not set and has no default
	 * value, a UndefinedParameterError will be thrown. If the parameter is optional and was not set
	 * this method returns null. Operators should always use this method instead of directly using
	 * the method {@link Process#resolveFileName(String)}.
	 * 
	 * @throws DirectoryCreationError
	 * @throws UserError
	 */
	public InputStream getParameterAsInputStream(String key) throws IOException, UserError;

	/**
	 * Returns a single named parameter and casts it to File. This file is already resolved against
	 * the process definition file. If the parameter name defines a non-optional parameter which is
	 * not set and has no default value, a UndefinedParameterError will be thrown. If the parameter
	 * is optional and was not set this method returns null. Operators should always use this method
	 * instead of directly using the method {@link Process#resolveFileName(String)}.
	 * 
	 * @throws DirectoryCreationError
	 * @throws UserError
	 */
	public java.io.File getParameterAsFile(String key) throws UserError;

	/**
	 * Returns a single named parameter and casts it to File. This file is already resolved against
	 * the process definition file. If the parameter name defines a non-optional parameter which is
	 * not set and has no default value, a UndefinedParameterError will be thrown. If the parameter
	 * is optional and was not set this method returns null. Operators should always use this method
	 * instead of directly using the method {@link Process#resolveFileName(String)}.
	 * 
	 * @throws DirectoryCreationError
	 * @throws UserError
	 */
	public java.io.File getParameterAsFile(String key, boolean createMissingDirectories) throws UserError;

	/** Returns a single named parameter and casts it to a double matrix. */
	public double[][] getParameterAsMatrix(String key) throws UndefinedParameterError;

	/** Returns a list of all defined parameter types for this handler. */
	public List<ParameterType> getParameterTypes();

}
