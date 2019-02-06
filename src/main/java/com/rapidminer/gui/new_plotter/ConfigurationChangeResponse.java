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

import com.rapidminer.tools.Tools;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;


/**
 * @author Marius Helf, Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class ConfigurationChangeResponse {

	private List<PlotConfigurationError> errorList = new LinkedList<PlotConfigurationError>();
	private List<PlotConfigurationError> warningList = new LinkedList<PlotConfigurationError>();
	private static final URL ERROR_ICON_URL = Tools.getResource("icons/16/error.png");
	private static final URL WARNING_ICON_URL = Tools.getResource("icons/16/sign_warning.png");

	public boolean hasErrors() {
		return !errorList.isEmpty();
	}

	public boolean hasWarnings() {
		return !warningList.isEmpty();
	}

	public boolean hasErrorsOrWarnings() {
		return !errorList.isEmpty() || !warningList.isEmpty();
	}

	public List<PlotConfigurationError> getErrors() {
		return errorList;
	}

	public List<PlotConfigurationError> getWarnings() {
		return warningList;
	}

	public void addError(PlotConfigurationError error) {
		errorList.add(error);
	}

	public void addWarning(PlotConfigurationError warning) {
		warningList.add(warning);
	}

	public void clearWarnings() {
		warningList.clear();
	}

	public void clearErrors() {
		errorList.clear();
	}

	public void append(ConfigurationChangeResponse other) {
		errorList.addAll(other.errorList);
		warningList.addAll(other.warningList);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (hasErrors()) {
			builder.append("### Errors:\n");
			for (PlotConfigurationError error : getErrors()) {
				builder.append(" - ");
				builder.append(error.getErrorName()).append("\n");
				builder.append(error.getErrorMessage()).append("\n");
				builder.append(error.getErrorDescription()).append("\n");
				builder.append("\n");
			}
			builder.append("\n");
		}
		if (hasWarnings()) {
			builder.append("### Warnings:\n");
			for (PlotConfigurationError warning : getWarnings()) {
				builder.append(" - ");
				builder.append(warning.getErrorName()).append("\n");
				builder.append(warning.getErrorMessage()).append("\n");
				builder.append(warning.getErrorDescription()).append("\n");
				builder.append("\n");
			}
		}
		return builder.toString();
	}

	/**
	 * @return
	 */
	public String toHtmlString() {
		StringBuilder builder = new StringBuilder();
		if (hasErrors()) {
			builder.append("<div style=\"color:#cc0000;\">");
			for (PlotConfigurationError error : getErrors()) {
				builder.append("<img valign=\"middle\" style=\"vertical-align:middle;\" src=\"" + ERROR_ICON_URL
						+ "\"/>&nbsp;");
				builder.append("<strong>");
				builder.append(error.getErrorName());
				builder.append("</strong>");
				builder.append("<br>");
				builder.append(error.getErrorMessage()).append("<br>");
				builder.append(error.getErrorDescription()).append("<br>");
			}
			builder.append("</div>");
		}

		if (hasWarnings()) {
			builder.append("<div style=\"color:#cc6600;\">");
			for (PlotConfigurationError warning : getWarnings()) {
				builder.append("<img valign=\"middle\" style=\"vertical-align:middle;\" src=\"" + WARNING_ICON_URL
						+ "\"/>&nbsp;");
				builder.append("<strong>");
				builder.append(warning.getErrorName());
				builder.append("</strong>");
				builder.append("<br>");
				builder.append(warning.getErrorMessage()).append("<br>");
				builder.append(warning.getErrorDescription()).append("<br>");
			}
			builder.append("</div>");
		}
		return builder.toString();
	}

	@Override
	public boolean equals(Object obj) {

		if (obj == null || !(obj instanceof ConfigurationChangeResponse)) {
			return false;
		}

		ConfigurationChangeResponse other = (ConfigurationChangeResponse) obj;

		if (!other.getErrors().equals(errorList)) {
			return false;
		}

		if (!other.getWarnings().equals(warningList)) {
			return false;
		}

		return true;
	}

	public boolean isEmpty() {
		return !(hasErrors() || hasWarnings());
	}
}
