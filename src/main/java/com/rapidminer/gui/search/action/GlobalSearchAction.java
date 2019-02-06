/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.search.action;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.search.GlobalSearchDialog;
import com.rapidminer.gui.search.GlobalSearchPanel;
import com.rapidminer.gui.tools.ResourceAction;


/**
 * The action that opens the {@link GlobalSearchDialog}.
 *
 * @author Marco Boeck
 * @since 8.1
 */
public class GlobalSearchAction extends ResourceAction {

	private static final String GLOBAL_SEARCH = "global_search";


	public GlobalSearchAction() {
		super(true, GLOBAL_SEARCH);
		InputMap inputMap = ApplicationFrame.getApplicationFrame().getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		ActionMap actionMap = ApplicationFrame.getApplicationFrame().getRootPane().getActionMap();

		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), GLOBAL_SEARCH);
		actionMap.put(GLOBAL_SEARCH, this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		GlobalSearchPanel.getInstance().requestFocusInWindow();
	}

}
