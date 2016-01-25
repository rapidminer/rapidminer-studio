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
package com.rapidminer.tools.config;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.rapidminer.operator.OperatorException;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeColor;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeTupel;
import com.rapidminer.parameter.Parameters;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.WebServiceTools;
import com.rapidminer.tools.math.StringToMatrixConverter;


/**
 * The default {@link ParameterHandler} implementation for {@link Configurator}s.
 *
 * The method {@link #getParameterTypes()} has to be implemented to handle {@link ParameterType}
 * dependencies of {@link Configurable}s.
 *
 * @since 6.1.1 Extracted to public class
 *
 * @author Simon Fischer, Dominik Halfkann, Marco Boeck, Adrian Wilke
 */
public abstract class ConfiguratorParameterHandler implements ParameterHandler {

	private Parameters parameters;

	@Override
	public String getParameter(String key) throws UndefinedParameterError {
		return this.parameters.getParameter(key);
	}

	@Override
	public boolean getParameterAsBoolean(String key) {
		try {
			return Boolean.valueOf(getParameter(key));
		} catch (UndefinedParameterError e) {
			return false;
		}
	}

	@Override
	public char getParameterAsChar(String key) throws UndefinedParameterError {
		String parameterValue = getParameter(key);
		if (parameterValue != null && parameterValue.length() > 0) {
			return parameterValue.charAt(0);
		}
		return 0;
	}

	@Override
	public Color getParameterAsColor(String key) throws UndefinedParameterError {
		String parameterValue = getParameter(key);
		if (parameterValue == null) {
			return Color.BLACK;
		}
		return ParameterTypeColor.string2Color(parameterValue);
	}

	@Override
	public double getParameterAsDouble(String key) throws UndefinedParameterError {
		String value = getParameter(key);
		if (value == null) {
			throw new UndefinedParameterError(key);
		}
		try {
			return Double.valueOf(value);
		} catch (NumberFormatException e) {
			throw new UndefinedParameterError(key, "Expected real number but found '" + value + "'.");
		}
	}

	@Override
	public InputStream getParameterAsInputStream(String key) throws UndefinedParameterError, IOException {
		String urlString = getParameter(key);
		if (urlString == null) {
			return null;
		}

		try {
			URL url = new URL(urlString);
			InputStream stream = WebServiceTools.openStreamFromURL(url);
			return stream;
		} catch (MalformedURLException e) {
			// URL did not work? Try as file...
			File file = getParameterAsFile(key);
			if (file != null) {
				return new FileInputStream(file);
			} else {
				return null;
			}
		}
	}

	@Override
	public File getParameterAsFile(String key) throws UndefinedParameterError {
		return getParameterAsFile(key, false);
	}

	@Override
	public File getParameterAsFile(String key, boolean createMissingDirectories) throws UndefinedParameterError {
		String filename = getParameter(key);
		if (filename == null) {
			return null;
		}
		if (!Files.exists(Paths.get(filename))) {
			return null;
		}
		return new File(filename);
	}

	@Override
	public int getParameterAsInt(String key) throws UndefinedParameterError {
		ParameterType type = this.parameters.getParameterType(key);
		String value = getParameter(key);
		if (type != null) {
			if (type instanceof ParameterTypeCategory) {
				try {
					return Integer.valueOf(value);
				} catch (NumberFormatException e) {
					ParameterTypeCategory categoryType = (ParameterTypeCategory) type;
					return categoryType.getIndex(value);
				}
			}
		}
		try {
			return Integer.valueOf(value);
		} catch (NumberFormatException e) {
			throw new UndefinedParameterError(key, "Expected integer but found '" + value + "'.");
		}
	}

	@Override
	public long getParameterAsLong(String key) throws UndefinedParameterError {
		ParameterType type = this.parameters.getParameterType(key);
		String value = getParameter(key);
		if (type != null) {
			if (type instanceof ParameterTypeCategory) {
				try {
					return Long.valueOf(value);
				} catch (NumberFormatException e) {
					ParameterTypeCategory categoryType = (ParameterTypeCategory) type;
					return categoryType.getIndex(value);
				}
			}
		}
		try {
			return Long.valueOf(value);
		} catch (NumberFormatException e) {
			throw new UndefinedParameterError(key, "Expected long but found '" + value + "'.");
		}
	}

	@Override
	public double[][] getParameterAsMatrix(String key) throws UndefinedParameterError {
		String matrixLine = getParameter(key);
		try {
			return StringToMatrixConverter.parseMatlabString(matrixLine);
		} catch (OperatorException e) {
			throw new UndefinedParameterError(e.getMessage());
		}
	}

	@Override
	public String getParameterAsString(String key) throws UndefinedParameterError {
		return getParameter(key);
	}

	@Override
	public List<String[]> getParameterList(String key) throws UndefinedParameterError {
		String value = getParameter(key);
		if (value == null) {
			throw new UndefinedParameterError(key);
		}
		return ParameterTypeList.transformString2List(value);
	}

	@Override
	public String[] getParameterTupel(String key) throws UndefinedParameterError {
		String value = getParameter(key);
		if (value == null) {
			throw new UndefinedParameterError(key);
		}
		return ParameterTypeTupel.transformString2Tupel(value);
	}

	@Override
	public boolean isParameterSet(String key) throws UndefinedParameterError {
		return getParameter(key) != null;
	}

	@Override
	public void setListParameter(String key, List<String[]> list) {
		this.parameters.setParameter(key, ParameterTypeList.transformList2String(list));
	}

	@Override
	public void setParameter(String key, String value) {
		this.parameters.setParameter(key, value);
	}

	@Override
	public void setParameters(Parameters parameters) {
		this.parameters = parameters;
	}

	@Override
	public Parameters getParameters() {
		if (this.parameters == null) {
			this.parameters = new Parameters(getParameterTypes());
		}
		return this.parameters;
	}
}
