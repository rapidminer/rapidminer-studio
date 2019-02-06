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

import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.quickfix.QuickFix;

import java.util.Collections;
import java.util.List;


/**
 * @author Simon Fischer
 */
public abstract class AbstractPrecondition implements Precondition {

	private final InputPort inputPort;

	public AbstractPrecondition(InputPort inputPort) {
		this.inputPort = inputPort;
	}

	protected InputPort getInputPort() {
		return inputPort;
	}

	protected void createError(Severity severity, String i18nKey, Object... i18nArgs) {
		createError(severity, Collections.<QuickFix> emptyList(), i18nKey, i18nArgs);
	}

	protected void createError(Severity severity, final List<? extends QuickFix> fixes, String i18nKey, Object... i18nArgs) {
		getInputPort().addError(new SimpleMetaDataError(severity, getInputPort(), fixes, i18nKey, i18nArgs));
	}

	@Override
	public String toString() {
		return getDescription();
	}
}
