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
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.gui.flow.processrendering.view.ProcessRendererView;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;


/**
 * Selects all {@link Operator}s.
 *
 * @author Simon Fischer
 * @since 6.4.0
 *
 */
public class SelectAllAction extends ResourceAction {

	private static final long serialVersionUID = 1L;

	private ProcessRendererView view;

	public SelectAllAction(ProcessRendererView view) {
		super("select_all");

		this.view = view;
	}

	@Override
	public void loggedActionPerformed(final ActionEvent e) {
		view.getModel().clearOperatorSelection();
		List<Operator> selected = new LinkedList<>();
		for (ExecutionUnit unit : view.getModel().getProcesses()) {
			selected.addAll(unit.getOperators());
		}

		// On an empty process, select the displayed operator chain instead
		if (selected.isEmpty()) {
			selected.add(view.getModel().getDisplayedChain());
		}

		view.getModel().addOperatorsToSelection(selected);
		view.getModel().fireOperatorSelectionChanged(selected);
	}
}
