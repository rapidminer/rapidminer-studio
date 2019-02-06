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

import java.awt.event.ActionEvent;


/**
 * Implements an empty {@link #actionPerformed(ActionEvent)} method.
 * 
 * @author Simon Fischer
 */
public class ResourceActionAdapter extends ResourceAction {

	private static final long serialVersionUID = -2739419154938092834L;

	public ResourceActionAdapter(String i18nKey, Object... i18nArgs) {
		this(false, i18nKey, i18nArgs);
	}

	public ResourceActionAdapter(boolean smallIcon, String key) {
		super(smallIcon, key);
	}

	public ResourceActionAdapter(boolean smallIcon, String i18nKey, Object... i18nArgs) {
		super(smallIcon, i18nKey, i18nArgs);
	}

}
