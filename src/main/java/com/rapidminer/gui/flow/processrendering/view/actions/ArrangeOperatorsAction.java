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
package com.rapidminer.gui.flow.processrendering.view.actions;

import java.awt.event.ActionEvent;

import com.rapidminer.gui.flow.processrendering.view.ProcessRendererController;
import com.rapidminer.gui.flow.processrendering.view.ProcessRendererView;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.operator.Operator;


/**
 * Automatically arranges all {@link Operator}s.
 *
 * @author Simon Fischer
 * @since 6.4.0
 *
 */
public class ArrangeOperatorsAction extends ResourceAction {

	private static final long serialVersionUID = 1L;

	private ProcessRendererView view;
	private ProcessRendererController controller;

	public ArrangeOperatorsAction(ProcessRendererView view, ProcessRendererController controller) {
		super(true, "arrange_operators");

		this.view = view;
		this.controller = controller;
	}

	@Override
	public void loggedActionPerformed(final ActionEvent e) {
		controller.autoArrange(view.getModel().getProcesses());
	}
}
