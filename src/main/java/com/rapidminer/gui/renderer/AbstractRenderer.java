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
package com.rapidminer.gui.renderer;

import com.rapidminer.gui.look.Colors;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.visualization.dependencies.NumericalMatrix;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeColor;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeTupel;
import com.rapidminer.parameter.Parameters;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.WebServiceTools;
import com.rapidminer.tools.math.StringToMatrixConverter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;


/**
 * This is the abstract renderer superclass for all renderers which provide some basic methods for
 * parameter handling.
 * 
 * @author Ingo Mierswa
 */
public abstract class AbstractRenderer implements Renderer {

	private Parameters parameters;

	public AbstractRenderer() {}

	@Override
	public final List<ParameterType> getParameterTypes() {
		return getParameterTypes(null);
	}

	public List<ParameterType> getParameterTypes(InputPort inputPort) {
		return new LinkedList<ParameterType>();
	}

	@Override
	public String getParameter(String key) throws UndefinedParameterError {
		return getParameters().getParameter(key);
	}

	@Override
	public String toString() {
		return getName();
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
		if (parameterValue.length() > 0) {
			return parameterValue.charAt(0);
		}
		return 0;
	}

	@Override
	public Color getParameterAsColor(String key) throws UndefinedParameterError {
		return ParameterTypeColor.string2Color(getParameter(key));
	}

	@Override
	public double getParameterAsDouble(String key) throws UndefinedParameterError {
		return Double.valueOf(getParameter(key));
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
		return new File(getParameter(key));
	}

	@Override
	public int getParameterAsInt(String key) throws UndefinedParameterError {
		ParameterType type = parameters.getParameterType(key);
		String value = getParameter(key);
		if (type != null) {
			if (type instanceof ParameterTypeCategory) {
				String parameterValue = value;
				try {
					return Integer.valueOf(parameterValue);
				} catch (NumberFormatException e) {
					ParameterTypeCategory categoryType = (ParameterTypeCategory) type;
					return categoryType.getIndex(parameterValue);
				}
			}
		}
		try {
			return Integer.valueOf(value);
		} catch (NumberFormatException e) {
			throw new UndefinedParameterError(key, "Expected integer but found '" + value + "'.");
		}
	}

	/** Returns a single named parameter and casts it to long. */
	@Override
	public long getParameterAsLong(String key) throws UndefinedParameterError {
		ParameterType type = this.getParameters().getParameterType(key);
		String value = getParameter(key);
		if (type != null) {
			if (type instanceof ParameterTypeCategory) {
				String parameterValue = value;
				try {
					return Long.valueOf(parameterValue);
				} catch (NumberFormatException e) {
					ParameterTypeCategory categoryType = (ParameterTypeCategory) type;
					return categoryType.getIndex(parameterValue);
				}
			}
		}
		try {
			return Long.valueOf(value);
		} catch (NumberFormatException e) {
			throw new UndefinedParameterError(key, "Expected integer but found '" + value + "'.");
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
		return ParameterTypeList.transformString2List(getParameter(key));
	}

	@Override
	public String[] getParameterTupel(String key) throws UndefinedParameterError {
		return ParameterTypeTupel.transformString2Tupel(getParameter(key));
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
		getParameters().setParameter(key, value);
	}

	/** Do nothing. */
	@Override
	public void setParameters(Parameters parameters) {
		this.parameters = parameters;
	}

	/** Returns null. */
	@Override
	public Parameters getParameters() {
		if (this.parameters == null) {
			this.parameters = new Parameters(getParameterTypes());
		}
		return this.parameters;
	}

	/** Returns null. */
	@Override
	public Parameters getParameters(InputPort inputPort) {
		if (this.parameters == null) {
			updateParameters(inputPort);
		}
		return this.parameters;
	}

	/**
	 * This method overrides all existing parameters. It must be used to ensure, that input Port
	 * referencing attributes are connected to the correct port, since they are only created once
	 * and might be initialized from another operator.
	 */
	@Override
	public void updateParameters(InputPort inputPort) {
		this.parameters = new Parameters(getParameterTypes(inputPort));
	}

	/**
	 * Adds a warning label for a specified {@link com.rapidminer.operator.IOObject},
	 * e.g. {@link NumericalMatrix NumericalMatrices} that were not computed since
	 * there were not enough valid attributes available.
	 *
	 * @param baseComponent
	 * 		the base visualization component
	 * @param showWarning
	 * 		whether to add a warning
	 * @param i18n
	 * 		the i18n key of the warning to show
	 * @param arguments
	 * 		the i18n arguments
	 * @return the base component or a new {@link JPanel} with the base component and a warning label
	 * @see #getVisualizationComponent(Object, IOContainer)
	 * @since 8.2
	 */
	protected static Component addWarningPanel(Component baseComponent, boolean showWarning, String i18n, Object... arguments) {
		if (!showWarning) {
			return baseComponent;
		}
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(baseComponent, BorderLayout.CENTER);
		JLabel warningLabel = new JLabel(I18N.getGUILabel(i18n, arguments));
		warningLabel.setHorizontalAlignment(SwingConstants.CENTER);
		warningLabel.setFont(warningLabel.getFont().deriveFont(14f));
		warningLabel.setOpaque(true);
		warningLabel.setBackground(Colors.WARNING_COLOR);
		warningLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		panel.add(warningLabel, BorderLayout.NORTH);
		return panel;
	}
}
