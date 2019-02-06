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
package com.rapidminer.gui.tools;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.Action;


/**
 * Encapsulates an action. Transmits all actionPerformed calls to that action.
 * 
 * @author Tobias Malbrecht
 * 
 */
public class ResourceActionTransmitter extends ResourceAction {

	private static final long serialVersionUID = -6756599447920780982L;

	private final Action action;

	private final Component eventSource;

	public ResourceActionTransmitter(String i18nKey, Action action, Object... i18nArgs) {
		this(false, i18nKey, action, null, i18nArgs);
	}

	public ResourceActionTransmitter(String i18nKey, Action action, Component fixedEventSource, Object... i18nArgs) {
		this(false, i18nKey, action, fixedEventSource, i18nArgs);

	}

	public ResourceActionTransmitter(boolean smallIcon, String i18nKey, Action action, Object... i18nArgs) {
		this(smallIcon, i18nKey, action, null, i18nArgs);
	}

	public ResourceActionTransmitter(boolean smallIcon, String i18nKey, Action action, Component fixedEventSource,
			Object... i18nArgs) {
		super(smallIcon, i18nKey, i18nArgs);
		this.action = action;
		this.eventSource = fixedEventSource;
	}

	@Override
	public void loggedActionPerformed(ActionEvent e) {
		if (eventSource != null) {
			e.setSource(eventSource);
		}
		action.actionPerformed(e);
	}
}
