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

import com.rapidminer.gui.OperatorDocLoader;
import com.rapidminer.gui.OperatorDocViewer;
import com.rapidminer.gui.tools.ResourceAction;

import java.awt.event.ActionEvent;


/**
 * @author Miguel Buescher
 */
public class RefreshHelpTextFromWikiAction extends ResourceAction {

	private static final long serialVersionUID = 1L;

	private OperatorDocViewer operatorDocViewer;

	public RefreshHelpTextFromWikiAction(boolean smallIcon, String i18nKey, Object[] i18nArgs,
			OperatorDocViewer operatorDocViewer) {
		super(smallIcon, i18nKey, i18nArgs);
		this.operatorDocViewer = operatorDocViewer;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		OperatorDocLoader.clearOperatorCache();
		this.operatorDocViewer.refresh();
	}

}
