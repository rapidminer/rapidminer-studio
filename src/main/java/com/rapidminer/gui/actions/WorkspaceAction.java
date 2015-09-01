/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.actions;

import com.rapidminer.gui.ApplicationPerspectives;
import com.rapidminer.gui.Perspective;
import com.rapidminer.gui.tools.ResourceAction;

import java.awt.event.ActionEvent;


/**
 * 
 * @author Simon Fischer
 */
public class WorkspaceAction extends ResourceAction {

	private static final long serialVersionUID = 1L;

	private final ApplicationPerspectives perspectives;

	private final Perspective perspective;

	public WorkspaceAction(ApplicationPerspectives perspectives, Perspective perspective, String name) {
		super(true, "workspace_" + name);
		this.perspective = perspective;
		this.perspectives = perspectives;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		perspectives.showPerspective(perspective.getName());
	}

	/**
	 * Returns the {@link Perspective} for this action.
	 */
	public Perspective getPerspective() {
		return perspective;
	}
}
