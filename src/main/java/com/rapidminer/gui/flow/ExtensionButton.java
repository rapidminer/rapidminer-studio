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
package com.rapidminer.gui.flow;

import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.JButton;

import com.rapidminer.gui.flow.processrendering.model.ProcessRendererModel;
import com.rapidminer.gui.flow.processrendering.view.ProcessRendererView;
import com.rapidminer.gui.look.ui.ExtensionButtonUI;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.tools.I18N;


/**
 * Button which can be used to add/remove a subprocess from an {@link OperatorChain} in the
 * {@link ProcessRendererView}.
 *
 * @author Simon Fischer
 */
public class ExtensionButton extends JButton {

	private static final long serialVersionUID = -3435398786000739458L;

	private final int subprocessIndex;
	private final boolean add;

	public ExtensionButton(final ProcessRendererModel model, final OperatorChain chain, final int subprocessIndex,
			final boolean add) {
		super(new ResourceAction(true, add ? "add_subprocess" : "delete_subprocess") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				if (add) {
					chain.addSubprocess(subprocessIndex + 1);
				} else {
					chain.removeSubprocess(subprocessIndex);
				}
				model.fireDisplayedChainChanged();
			}
		});
		this.setUI(new ExtensionButtonUI());
		this.subprocessIndex = subprocessIndex;
		this.add = add;
		setText(null);
		if (add) {
			if (subprocessIndex >= 0 && subprocessIndex < chain.getSubprocesses().size()) {
				setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.action.add_subprocess.tip.after", chain
						.getSubprocess(subprocessIndex).getName()));
			}
			if (subprocessIndex == -1) {
				setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.action.add_subprocess.tip.before", chain
						.getSubprocess(0).getName()));
			}
		} else {
			if (subprocessIndex >= 0 && subprocessIndex < chain.getSubprocesses().size()) {
				setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.action.delete_subprocess.tip.after", chain
						.getSubprocess(subprocessIndex).getName()));
			}
		}
		setMargin(new Insets(0, 0, 0, 0));
		setContentAreaFilled(false);
	}

	public int getSubprocessIndex() {
		return subprocessIndex;
	}

	public boolean isAdd() {
		return add;
	}

}
