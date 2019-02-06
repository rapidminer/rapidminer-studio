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

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.flow.processrendering.model.ProcessRendererModel;
import com.rapidminer.gui.flow.processrendering.view.ProcessRendererView;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.operator.Operator;


/**
 * Deletes either a selected connection or {@link Operator}.
 *
 * @author Simon Fischer
 * @since 6.4.0
 *
 */
public class DeleteSelectedConnectionAction extends ResourceAction {

	private static final long serialVersionUID = 1L;

	private ProcessRendererView view;

	public DeleteSelectedConnectionAction(ProcessRendererView view) {
		super("delete_selected_connection");

		this.view = view;
	}

	@Override
	public void loggedActionPerformed(final ActionEvent e) {
		ProcessRendererModel model = view.getModel();
		if (model.getSelectedConnectionSource() != null) {
			if (model.getSelectedConnectionSource().isConnected()) {
				model.getSelectedConnectionSource().disconnect();
			}
			model.setSelectedConnectionSource(null);
			model.fireMiscChanged();
		} else {
			// try to delete selected annotation
			if (model.getSelectedOperators().size() == 1
					&& model.getSelectedOperators().get(0).equals(model.getDisplayedChain())) {
				if (RapidMinerGUI.getMainFrame().getProcessPanel().getAnnotationsHandler().isActive()) {
					RapidMinerGUI.getMainFrame().getProcessPanel().getAnnotationsHandler().deleteSelected();
				}
			}
			for (Operator selectedOperator : model.getSelectedOperators()) {
				// don't delete if we have selected surrounding operator in subprocess so the whole
				// subprocess would get deleted
				if (selectedOperator.equals(model.getDisplayedChain())) {
					return;
				}
			}
			RapidMinerGUI.getMainFrame().getActions().DELETE_OPERATOR_ACTION.actionPerformed(e);
			model.setConnectingPortSource(null);
			model.fireMiscChanged();
		}
	}
}
