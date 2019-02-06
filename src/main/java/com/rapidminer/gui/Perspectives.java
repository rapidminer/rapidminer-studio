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
package com.rapidminer.gui;

import com.vlsolutions.swing.docking.DockingContext;


/**
 * Collection of {@link Perspective}s that can be applied, saved, created.
 *
 * @author Simon Fischer
 * @deprecated use {@link PerspectiveController} instead
 */
@Deprecated
public class Perspectives extends ApplicationPerspectives {

	public static final String WELCOME = "design";
	public static final String RESULT = "result";
	public static final String DESIGN = "design";
	public static final String BUSINESS = "design";

	public Perspectives(PerspectiveController perspectiveController) {
		super(perspectiveController);
	}

	public Perspectives(DockingContext context) {
		super(context);
	}

	@Override
	protected void makePredefined() {
		// nothing to do
	}

	@Override
	protected void restoreDefault(String perspectiveName) {
		perspectiveController.getModel().restoreDefault(perspectiveName);
	}
}
