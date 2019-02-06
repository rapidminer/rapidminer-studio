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
package com.rapidminer.gui.new_plotter;

import com.rapidminer.RapidMiner;
import com.rapidminer.gui.new_plotter.listener.events.PlotConfigurationChangeEvent;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.ParameterService;

import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;


/**
 * @author Marius Helf, Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class PlotConfigurationError {

	private static ResourceBundle messages = I18N.getGUIBundle();
	private static final MessageFormat formatter = new MessageFormat("");

	private static final String NAME_KEY = "name";
	private static final String MESSAGE_KEY = "message";
	private static final String EXPLANATION_KEY = "explanation";

	private List<PlotConfigurationQuickFix> possiblesFixes = new LinkedList<PlotConfigurationQuickFix>();

	/**
	 * An identifier for the error (see PlotterMessages.properties, prefix "gui.plotter.error")
	 */
	private String errorId;
	private Object[] messageParameters;

	// /**
	// * The change that caused the error, if any
	// */
	// private PlotConfigurationChangeEvent cause;

	// /**
	// * The object that created this error
	// */
	// private Object location;

	public PlotConfigurationError(String errorId, Object... messageParameters) {
		super();
		this.errorId = errorId;
		this.messageParameters = messageParameters;
	}

	public PlotConfigurationError(String errorId, PlotConfigurationQuickFix quickFix, Object... messageParameters) {
		super();
		this.errorId = errorId;
		this.messageParameters = messageParameters;
		possiblesFixes.add(quickFix);
	}

	public PlotConfigurationError(String errorId, PlotConfigurationChangeEvent changeForQuickFix,
			Object... messageParameters) {
		super();
		this.errorId = errorId;
		this.messageParameters = messageParameters;
		possiblesFixes.add(new PlotConfigurationQuickFix(changeForQuickFix));
	}

	public void addQuickFix(PlotConfigurationQuickFix quickFix) {
		possiblesFixes.add(quickFix);
	}

	public List<PlotConfigurationQuickFix> getQuickFixes() {
		return possiblesFixes;
	}

	public String getErrorName() {
		String deflt = "Unnamed plotter error.";
		if (Boolean.valueOf(ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_DEBUGMODE))) {
			deflt += " (" + errorId + ")";
		}

		return getResourceString(errorId, NAME_KEY, deflt);
	}

	public String getErrorMessage() {
		return getErrorMessage(errorId, messageParameters);
	}

	public Object[] getMessageParameters() {
		return messageParameters;
	}

	public String getErrorId() {
		return errorId;
	}

	public String getErrorDescription() {
		return getResourceString(errorId, EXPLANATION_KEY, "");
	}

	/**
	 * This returns a resource message of the internationalized error messages identified by an id.
	 * Compared to the legacy method {@link #getResourceString(int, String, String)} this supports a
	 * more detailed identifier. This makes it easier to ensure extensions don't reuse already
	 * defined core errors. It is common sense to add the extensions namespace identifier as second
	 * part of the key, just after error. For example: error.rmx_web.operator.unusable = This
	 * operator {0} is unusable.
	 * 
	 * @param id
	 *            The identifier of the error. "gui.plotter.error." will be automatically prepended-
	 * @param key
	 *            The part of the error description that should be shown.
	 * @param deflt
	 *            The default if no resource bundle is available.
	 */
	public static String getResourceString(String id, String key, String deflt) {
		if (messages == null) {
			return deflt;
		}
		try {
			return messages.getString("gui.plotter.error." + id + "." + key);
		} catch (java.util.MissingResourceException e) {
			return deflt;
		}
	}

	public static String getErrorMessage(String identifier, Object[] arguments) {
		String message = getResourceString(identifier, MESSAGE_KEY, "No message.");
		try {
			formatter.applyPattern(message);
			String formatted = formatter.format(arguments);
			return formatted;
		} catch (Throwable t) {
			return message;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof PlotConfigurationError)) {
			return false;
		}

		return true;
	}
}
