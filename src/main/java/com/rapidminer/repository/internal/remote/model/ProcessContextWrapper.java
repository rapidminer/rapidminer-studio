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
package com.rapidminer.repository.internal.remote.model;

import java.util.ArrayList;
import java.util.List;


/**
 * Container for Studio process context
 *
 * @author Andreas Timm
 * @since 9.5.0
 */
public class ProcessContextWrapper {

	protected List<String> inputRepositoryLocations;
	protected List<MacroDefinition> macros;
	protected List<String> outputRepositoryLocations;

	/**
	 * The repository locations of process input
	 *
	 * @return list of repository locations as strings, never {@code null}
	 */
	public List<String> getInputRepositoryLocations() {
		if (inputRepositoryLocations == null) {
			inputRepositoryLocations = new ArrayList<>();
		}
		return inputRepositoryLocations;
	}

	public void setInputRepositoryLocations(List<String> inputRepositoryLocations) {
		this.inputRepositoryLocations = inputRepositoryLocations;
	}

	/**
	 * The process macros
	 *
	 * @return list of {@link MacroDefinition MacroDefinitions}, never {@code null}
	 */
	public List<MacroDefinition> getMacros() {
		if (macros == null) {
			macros = new ArrayList<>();
		}
		return macros;
	}

	public void setMacros(List<MacroDefinition> macros) {
		this.macros = macros;
	}

	/**
	 * The repository locations of process output
	 *
	 * @return list of repository locations as strings, never {@code null}
	 */
	public List<String> getOutputRepositoryLocations() {
		if (outputRepositoryLocations == null) {
			outputRepositoryLocations = new ArrayList<>();
		}
		return outputRepositoryLocations;
	}

	public void setOutputRepositoryLocations(List<String> outputRepositoryLocations) {
		this.outputRepositoryLocations = outputRepositoryLocations;
	}
}
