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
package com.rapidminer.operator;

import java.util.Collections;
import java.util.List;

import com.rapidminer.operator.ports.PortOwner;
import com.rapidminer.operator.ports.quickfix.QuickFix;


/**
 * Error that is used when a repository entry is either of the wrong type or does not exist.
 *
 * @author Marco Boeck
 * @since 8.2
 */
public class InvalidRepositoryEntryError extends SimpleProcessSetupError {

	private final String parameterKey;


	public InvalidRepositoryEntryError(Severity severity, PortOwner owner, String parameterKey, String i18nKey, Object... i18nArgs) {
		super(severity, owner, Collections.emptyList(), false, i18nKey, i18nArgs);

		if (parameterKey == null) {
			throw new IllegalArgumentException("parameterKey must not be null!");
		}
		this.parameterKey = parameterKey;
	}

	public InvalidRepositoryEntryError(Severity severity, PortOwner owner, String parameterKey, List<? extends QuickFix> fixes, String i18nKey,
									   Object... i18nArgs) {
		super(severity, owner, fixes, false, i18nKey, i18nArgs);

		if (parameterKey == null) {
			throw new IllegalArgumentException("parameterKey must not be null!");
		}
		this.parameterKey = parameterKey;
	}

	public InvalidRepositoryEntryError(Severity severity, PortOwner portOwner, String parameterKey, List<? extends QuickFix> fixes,
									   boolean absoluteKey, String i18nKey, Object... i18nArgs) {
		super(severity, portOwner, fixes, absoluteKey, i18nKey, i18nArgs);

		if (parameterKey == null) {
			throw new IllegalArgumentException("parameterKey must not be null!");
		}
		this.parameterKey = parameterKey;
	}

	/**
	 * Returns the parameter key that caused this error.
	 *
	 * @return the parameter key that caused this, never {@code null}
	 */
	public String getParameterKey() {
		return parameterKey;
	}
}
