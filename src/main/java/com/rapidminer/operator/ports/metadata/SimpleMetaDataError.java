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
package com.rapidminer.operator.ports.metadata;

import com.rapidminer.operator.SimpleProcessSetupError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.quickfix.QuickFix;

import java.util.Collections;
import java.util.List;


/**
 * An Error that can be registered to an {@link InputPort} to show up in the GUI. This errors are
 * created during the MetaDataTransformation of a process and should give the use the ability to
 * find errors and problems before executing the process.
 * 
 * @author Simon Fischer
 */
public class SimpleMetaDataError extends SimpleProcessSetupError implements MetaDataError {

	private Port port;

	/**
	 * Constructor for an error. Please note, that the i18nKey will be appended to "metadata.error."
	 * to form the final key.
	 */
	public SimpleMetaDataError(Severity severity, Port port, String i18nKey, Object... i18nArgs) {
		this(severity, port, Collections.<QuickFix> emptyList(), i18nKey, i18nArgs);
	}

	/**
	 * Constructor for an error. Please note, that the i18nKey will be appended to "metadata.error."
	 * to form the final key.
	 */
	public SimpleMetaDataError(Severity severity, Port port, List<? extends QuickFix> fixes, String i18nKey, Object... args) {
		super(severity, port == null ? null : port.getPorts().getOwner(), fixes, true, "metadata.error." + i18nKey, args);
		this.port = port;
	}

	@Override
	public Port getPort() {
		return port;
	}
}
