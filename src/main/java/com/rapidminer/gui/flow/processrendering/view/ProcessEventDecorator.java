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
package com.rapidminer.gui.flow.processrendering.view;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawDecorator;
import com.rapidminer.operator.ExecutionUnit;


/**
 * Implementations of this interface can be registered to decorate the process renderer mouse and
 * keyboard event handling. The decorator is called during the {@link RenderPhase} it was registered
 * for.
 * <p>
 * If it makes no sense that the event is passed down to the earlier phases/decorators, call
 * {@link MouseEvent#consume()}.
 * </p>
 *
 * <p>
 * <strong>Attention:</strong> Drawing should work on a headless server, so implementing both this
 * interface and {@link ProcessDrawDecorator} in the same class should be avoided.
 * </p>
 *
 * @author Marco Boeck
 * @since 6.4.0
 * @see ProcessRendererView#addEventDecorator(ProcessEventDecorator, RenderPhase)
 *
 */
public interface ProcessEventDecorator {

	/**
	 * The type of mouse event a {@link ProcessEventDecorator} can be notified of.
	 *
	 * @since 6.4.0
	 */
	public static enum MouseEventType {
		/** see {@link MouseListener#mouseClicked(MouseEvent)} */
		MOUSE_CLICKED,

		/** see {@link MouseListener#mousePressed(MouseEvent)} */
		MOUSE_PRESSED,

		/** see {@link MouseListener#mouseReleased(MouseEvent)} */
		MOUSE_RELEASED,

		/** see {@link MouseMotionListener#mouseMoved(MouseEvent)} */
		MOUSE_MOVED,

		/** see {@link MouseMotionListener#mouseDragged(MouseEvent)} */
		MOUSE_DRAGGED,

		/** see {@link MouseListener#mouseEntered(MouseEvent)} */
		MOUSE_ENTERED,

		/** see {@link MouseListener#mouseExited(MouseEvent)} */
		MOUSE_EXITED;
	}

	/**
	 * The type of key event a {@link ProcessEventDecorator} can be notified of.
	 *
	 * @since 6.4.0
	 */
	public static enum KeyEventType {
		/** see {@link KeyListener#keyPressed(KeyEvent)} */
		KEY_PRESSED,

		/** see {@link KeyListener#keyReleased(KeyEvent)} */
		KEY_RELEASED,

		/** see {@link KeyListener#keyTyped(KeyEvent)} */
		KEY_TYPED;
	}

	/**
	 * Process the mouse event during the {@link RenderPhase} specified while registering.
	 * <p>
	 * If it makes no sense that the event is passed down to the earlier phases/decorators, call
	 * {@link MouseEvent#consume()}.
	 * </p>
	 *
	 * @param process
	 *            the process the mouse event happened over. Can be {@code null}!
	 * @param type
	 *            the mouse event type
	 * @param e
	 *            the mouse event to process
	 */
	public void processMouseEvent(ExecutionUnit process, MouseEventType type, MouseEvent e);

	/**
	 * Process the key event during the {@link RenderPhase} specified while registering.
	 * <p>
	 * If it makes no sense that the event is passed down to the earlier phases/decorators, call
	 * {@link KeyEvent#consume()}.
	 * </p>
	 *
	 * @param process
	 *            the process the mouse event happened over. Can be {@code null}!
	 * @param type
	 *            the key event type
	 * @param e
	 *            the key event to process
	 */
	public void processKeyEvent(ExecutionUnit process, KeyEventType type, KeyEvent e);
}
