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
package com.rapidminer.gui.flow.processrendering.annotations;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.TextAction;


/**
 * This action is used to hook into workflow annotation editor pane paste events. It replaces the
 * original paste action by acting as proxy and delegating events to the original action.
 *
 * @author Marco Boeck
 * @since 6.4.0
 *
 */
final class PasteAnnotationProxyAction extends TextAction {

	private static final long serialVersionUID = 1L;

	private final AnnotationsDecorator decorator;

	/** original paste action */
	private final Action action;

	/**
	 * Creates a new proxy which calls the original paste action and then makes sure the editor
	 * content is updated by calling {@link AnnotationsDecorator#updateEditorContent()}.
	 *
	 * @param action
	 *            the original paste action which is called by this action
	 */
	public PasteAnnotationProxyAction(final Action action, final AnnotationsDecorator decorator) {
		super(DefaultEditorKit.pasteAction);
		this.action = action;
		this.decorator = decorator;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		action.actionPerformed(e);
		decorator.updateEditorContent();
	}

}
