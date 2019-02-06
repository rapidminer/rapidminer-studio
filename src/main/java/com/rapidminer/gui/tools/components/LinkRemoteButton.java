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
package com.rapidminer.gui.tools.components;

import javax.swing.Action;

import com.rapidminer.gui.look.Colors;


/**
 * Can be used as a label that triggers a remote action event (aka opens a website in the browser)
 * on every link activation click. To use a link button which triggers an in-application action, use
 * {@link LinkLocalButton} instead. The remote and the local link button may use different color
 * schemes or other indicates so the user knows what is about to happen before he clicks the link.
 * <p>
 * The {@link Action#NAME} property of the action will be the label text. It can either contain no
 * HTML tags in which case the entire label will become the link or it can contain an <a href=#> tag
 * and regular text in which case only the content of the tag will become the link. The icon
 * property of the action is ignored.
 * </p>
 *
 * @author Marco Boeck
 * @since 7.0.0
 *
 */
public class LinkRemoteButton extends AbstractLinkButton {

	private static final long serialVersionUID = 1L;

	public LinkRemoteButton(final Action action) {
		super(action, Colors.LINKBUTTON_REMOTE);
	}

}
