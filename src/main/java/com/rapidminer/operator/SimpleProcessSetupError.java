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

import com.rapidminer.operator.ports.PortOwner;
import com.rapidminer.operator.ports.quickfix.QuickFix;
import com.rapidminer.tools.I18N;

import java.util.Collections;
import java.util.List;


/**
 * @author Simon Fischer
 */
public class SimpleProcessSetupError implements ProcessSetupError {

	private String i18nKey;
	private final Object[] i18nArgs;
	private final PortOwner owner;
	private final List<? extends QuickFix> fixes;
	private final Severity severity;

	public SimpleProcessSetupError(Severity severity, PortOwner owner, String i18nKey, Object... i18nArgs) {
		this(severity, owner, Collections.emptyList(), false, i18nKey, i18nArgs);
	}

	public SimpleProcessSetupError(Severity severity, PortOwner owner, List<? extends QuickFix> fixes, String i18nKey,
			Object... i18nArgs) {
		this(severity, owner, fixes, false, i18nKey, i18nArgs);
	}

	public SimpleProcessSetupError(Severity severity, PortOwner portOwner, List<? extends QuickFix> fixes,
			boolean absoluteKey, String i18nKey, Object... i18nArgs) {
		super();
		if (absoluteKey) {
			this.i18nKey = i18nKey;
		} else {
			this.i18nKey = "process.error." + i18nKey;
		}
		this.i18nArgs = i18nArgs;
		this.owner = portOwner;
		this.fixes = fixes;
		this.severity = severity;
	}

	@Override
	public final String getMessage() {
		if (i18nArgs == null) {
			return I18N.getErrorBundle().getString(i18nKey);
		} else {
			return I18N.getMessage(I18N.getErrorBundle(), i18nKey, i18nArgs);
		}
	}

	@Override
	public final PortOwner getOwner() {
		return owner;
	}

	@Override
	public List<? extends QuickFix> getQuickFixes() {
		return fixes;
	}

	@Override
	public final Severity getSeverity() {
		return severity;
	}

	@Override
	public final String toString() {
		if (i18nArgs == null) {
			return I18N.getErrorBundle().getString(i18nKey);
		} else {
			return I18N.getMessage(I18N.getErrorBundle(), i18nKey, i18nArgs);
		}
	}
}
