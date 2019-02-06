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
package com.rapidminer.gui.tools;

import java.awt.event.MouseEvent;
import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import javax.swing.text.html.HTMLEditorKit;


/**
 * A LinkController for {@link JEditorPane}s that also allows clicking on a link if in edit mode.
 *
 * @author Marco Boeck
 * @since 9.0.0
 */
public class EditableLinkController extends HTMLEditorKit.LinkController {

	@Override
	public void mouseClicked(MouseEvent e) {
		JEditorPane editor = (JEditorPane) e.getSource();

		if (editor.isEditable() && SwingUtilities.isLeftMouseButton(e) && e.getClickCount() >= 2) {
			// make sure we do not start editing twice to avoid internal Java explosions
			editor.setEditable(false);
			super.mouseClicked(e);
			editor.setEditable(true);
		}

	}
}
