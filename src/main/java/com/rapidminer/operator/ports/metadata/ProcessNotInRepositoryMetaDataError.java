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

import java.util.Collections;
import java.util.List;

import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.quickfix.QuickFix;


/**
 * An Error that occurs if the process is not registered to a repository but relative repository paths are used.
 * 
 * @author Marco Boeck
 * @since 8.2
 */
public class ProcessNotInRepositoryMetaDataError extends SimpleMetaDataError {

	/**
	 * Constructor for an error. Please note, that the i18nKey will be appended to "metadata.error."
	 * to form the final key.
	 */
	public ProcessNotInRepositoryMetaDataError(Severity severity, Port port, String i18nKey, Object... i18nArgs) {
		super(severity, port, Collections.emptyList(), i18nKey, i18nArgs);
	}

	/**
	 * Constructor for an error. Please note, that the i18nKey will be appended to "metadata.error."
	 * to form the final key.
	 */
	public ProcessNotInRepositoryMetaDataError(Severity severity, Port port, List<? extends QuickFix> fixes, String i18nKey, Object... args) {
		super(severity, port, fixes, i18nKey, args);
	}

}
