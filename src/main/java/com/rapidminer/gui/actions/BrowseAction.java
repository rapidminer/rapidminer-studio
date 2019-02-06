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
package com.rapidminer.gui.actions;

import java.awt.event.ActionEvent;
import java.net.URI;

import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.tools.RMUrlHandler;


/**
 * Opens a browser.
 *
 * @author Simon Fischer
 *
 */
public class BrowseAction extends ResourceAction {

	private static final long serialVersionUID = 1L;

	private URI uri;

	public BrowseAction(String i18nKey, URI uri) {
		super(i18nKey);
		this.uri = uri;
		setCondition(EDIT_IN_PROGRESS, DONT_CARE);
	}

	@Override
	public void loggedActionPerformed(ActionEvent e) {
		RMUrlHandler.openInBrowser(uri);
	}
}
