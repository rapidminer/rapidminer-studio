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
package com.rapidminer.gui.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.Border;

import com.rapidminer.gui.RapidMinerGUI;


/**
 * This component is based on a {@link JWindow}. Once {@link #setVisible(boolean)} is called, the
 * popup will display and fade away after the specified amount of time if the GraphicsDevice
 * supports it. To use this component with minimal effort, you can utilize the static
 * {@link #showFadingPopup(JPanel, JComponent, PopupLocation)} methods.
 *
 * @author Marco Boeck
 *
 */
public class NotificationPopup extends JWindow {

	/**
	 * Listener for {@link NotificationPopup}s.
	 *
	 */
	public static interface NotificationPopupListener {

		/**
		 * Triggered when the popup is gone, either by having faded out or being closed via
		 * mouse-click.
		 *
		 * @param popup
		 */
		public void popupClosed(NotificationPopup popup);
	}

	/**
	 * Used to specifiy the location of the {@link NotificationPopup} on the parent component.
	 *
	 */
	public static enum PopupLocation {
		CENTER, CENTER_RIGHT, CENTER_LEFT, UPPER_LEFT, UPPER_CENTER, UPPER_RIGHT, LOWER_LEFT, LOWER_CENTER, LOWER_RIGHT;
	}

	private static final long serialVersionUID = 3620229881624404350L;

	/** the default time before the popup fades away in ms */
	public static final int DEFAULT_DELAY = 3000;

	/** the timer used to fade the popup in */
	private Timer fadeInTimer;

	/** the timer used to fade the popup out */
	private Timer fadeOutTimer;

	/** the mouse listener which allows closing of the notification with a click */
	private MouseListener mouseListener;

	/** the alpha value */
	private float alpha;

	/** flag indicating if we can fade the popup out or not */
	private boolean isOpacitySupported;

	/** the listener for this popup */
	private NotificationPopupListener listener;

	/**
	 * Creates a new {@link NotificationPopup} instance with the given content panel and the given
	 * timeout in ms before the popup fades away.
	 *
	 * @param content
	 *            the content panel to display
	 * @param delay
	 *            the time in milliseconds before the popup will start fading away after
	 *            {@link #show(java.awt.Component, int, int)} has been called
	 * @param listener
	 *            the listener which is called for notification popup events; can be
	 *            <code>null</code>
	 */
	private NotificationPopup(final JPanel content, final int delay, final NotificationPopupListener listener) {
		super(RapidMinerGUI.getMainFrame());

		// determine what the default GraphicsDevice can support
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice gd = ge.getDefaultScreenDevice();
		isOpacitySupported = gd.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.TRANSLUCENT);

		this.listener = listener;
		alpha = 0.0f;
		fadeOutTimer = new Timer(10, new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				if (alpha <= 0.0f) {
					fadeOutTimer.stop();
					dispose();
					return;
				}

				alpha = Math.max(0.0f, alpha - 0.01f);
				if (isOpacitySupported) {
					NotificationPopup.this.setOpacity(alpha);
				}
			}
		});
		fadeOutTimer.setInitialDelay(delay);
		fadeInTimer = new Timer(10, new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				if (alpha >= 1.0f) {
					fadeInTimer.stop();
					fadeOutTimer.start();
					return;
				}

				alpha = Math.min(1.0f, alpha + 0.10f);
				if (isOpacitySupported) {
					NotificationPopup.this.setOpacity(alpha);
				}
			}
		});
		fadeInTimer.setInitialDelay(0);

		// allow closing of the notification with the mouse
		mouseListener = new MouseAdapter() {

			@Override
			public void mousePressed(final MouseEvent e) {
				dispose();
			}

			@Override
			public void mouseEntered(final MouseEvent e) {
				if (!fadeInTimer.isRunning()) {
					alpha = 1.0f;
					if (isOpacitySupported) {
						NotificationPopup.this.setOpacity(alpha);
					}
					fadeOutTimer.stop();
				}
			}

			@Override
			public void mouseExited(final MouseEvent e) {
				if (SwingTools.isMouseEventExitedToChildComponents(NotificationPopup.this, e)) {
					// not really exited, only moved mouse to child component
					return;
				}
				fadeOutTimer.restart();
			}

		};
		if (isOpacitySupported) {
			NotificationPopup.this.setOpacity(alpha);
		}
		setLayout(new BorderLayout());
		add(content, BorderLayout.CENTER);
		pack();
	}

	/**
	 * Return the {@link MouseListener} which allows closing of the popup when clicking on it.
	 *
	 * @return
	 */
	private MouseListener getMouseListener() {
		return mouseListener;
	}

	@Override
	public void setVisible(final boolean b) {
		// start fade timer when setting to visible
		if (b && !fadeInTimer.isRunning()) {
			fadeInTimer.start();
		}

		super.setVisible(b);
	}

	@Override
	public void dispose() {
		if (listener != null) {
			listener.popupClosed(NotificationPopup.this);
			listener = null;
		}

		super.dispose();
	}

	/**
	 * Shows a fading popup panel which displays the given {@link JPanel} content on the parent
	 * {@link JComponent} in the specified {@link PopupLocation}. The popup is shown immediately and
	 * remains visible for the default delay. It can be closed by clicking on it or waiting the
	 * default delay. If the parent component is not showing, the popup is not shown either.
	 *
	 * @param content
	 *            the panel containing what should be shown in the popup
	 * @param invoker
	 *            the parent component on which the popup should be shown
	 * @param location
	 *            the location where on the parent component the popup should be shown
	 * @return the popup or <code>null</code> if it is not showing
	 */
	public static NotificationPopup showFadingPopup(final JPanel content, final Component invoker,
			final PopupLocation location) {
		return showFadingPopup(content, invoker, location, DEFAULT_DELAY);
	}

	/**
	 * Shows a fading popup panel which displays the given {@link JPanel} content on the parent
	 * {@link JComponent} in the specified {@link PopupLocation}. The popup is shown immediately and
	 * remains visible for the specified delay. It can be closed by clicking on it or waiting the
	 * specified delay. If the parent component is not showing, the popup is not shown either.
	 *
	 * @param content
	 *            the panel containing what should be shown in the popup
	 * @param invoker
	 *            the parent component on which the popup should be shown
	 * @param location
	 *            the location where on the parent component the popup should be shown
	 * @param delay
	 *            the delay in milliseconds before the popup starts to fade out
	 * @return the popup or <code>null</code> if it is not showing
	 */
	public static NotificationPopup showFadingPopup(final JPanel content, final Component invoker,
			final PopupLocation location, final int delay) {
		return showFadingPopup(content, invoker, location, delay, 0, 0);
	}

	/**
	 * Shows a fading popup panel which displays the given {@link JPanel} content on the parent
	 * {@link JComponent} in the specified {@link PopupLocation}. The popup is shown immediately and
	 * remains visible for the specified delay. It can be closed by clicking on it or waiting the
	 * specified delay. If the parent component is not showing, the popup is not shown either.
	 *
	 * @param content
	 *            the panel containing what should be shown in the popup
	 * @param invoker
	 *            the parent component on which the popup should be shown
	 * @param location
	 *            the location where on the parent component the popup should be shown
	 * @param delay
	 *            the delay in milliseconds before the popup starts to fade out
	 * @param paddingX
	 *            the distance in pixels from the horizontal parent component side
	 * @param paddingY
	 *            the distance in pixels from the vertical parent component side
	 * @return the popup or <code>null</code> if it is not showing
	 */
	public static NotificationPopup showFadingPopup(final JPanel content, final Component invoker,
			final PopupLocation location, final int delay, final int paddingX, final int paddingY) {
		return showFadingPopup(content, invoker, location, delay, paddingX, paddingY,
				BorderFactory.createLineBorder(Color.BLACK, 1, false));
	}

	/**
	 * Shows a fading popup panel which displays the given {@link JPanel} content on the parent
	 * {@link JComponent} in the specified {@link PopupLocation}. The popup is shown immediately and
	 * remains visible for the specified delay. It can be closed by clicking on it or waiting the
	 * specified delay. If the parent component is not showing, the popup is not shown either.
	 *
	 * @param content
	 *            the panel containing what should be shown in the popup
	 * @param invoker
	 *            the parent component on which the popup should be shown
	 * @param location
	 *            the location where on the parent component the popup should be shown
	 * @param delay
	 *            the delay in milliseconds before the popup starts to fade out
	 * @param paddingX
	 *            the distance in pixels from the horizontal parent component side
	 * @param paddingY
	 *            the distance in pixels from the vertical parent component side
	 * @param border
	 *            the border to display around the notification
	 * @return the popup or <code>null</code> if it is not showing
	 */
	public static NotificationPopup showFadingPopup(final JPanel content, final Component invoker,
			final PopupLocation location, final int delay, final int paddingX, final int paddingY, final Border border) {
		return showFadingPopup(content, invoker, location, delay, paddingX, paddingY,
				BorderFactory.createLineBorder(Color.BLACK, 1, false), null);
	}

	/**
	 * Shows a fading popup panel which displays the given {@link JPanel} content on the parent
	 * {@link JComponent} in the specified {@link PopupLocation}. The popup is shown immediately and
	 * remains visible for the specified delay. It can be closed by clicking on it or waiting the
	 * specified delay. If the parent component is not showing, the popup is not shown either.
	 *
	 * @param content
	 *            the panel containing what should be shown in the popup
	 * @param invoker
	 *            the parent component on which the popup should be shown
	 * @param location
	 *            the location where on the parent component the popup should be shown
	 * @param delay
	 *            the delay in milliseconds before the popup starts to fade out
	 * @param paddingX
	 *            the distance in pixels from the horizontal parent component side
	 * @param paddingY
	 *            the distance in pixels from the vertical parent component side
	 * @param border
	 *            the border to display around the notification
	 * @param listener
	 *            listener which is notified on events
	 * @return the popup or <code>null</code> if it is not showing
	 */
	public static NotificationPopup showFadingPopup(final JPanel content, final Component invoker,
			final PopupLocation location, final int delay, final int paddingX, final int paddingY, final Border border,
			final NotificationPopupListener listener) {
		if (content == null) {
			throw new IllegalArgumentException("content must not be null!");
		}
		if (invoker == null) {
			throw new IllegalArgumentException("invoker must not be null!");
		}
		if (!invoker.isShowing()) {
			return null;
		}

		// if we are in a scrollpane somewhere, we need to actually display on the scrollpane
		// otherwise the placement can be off completely or even outside of the screen
		Component actualInvoker = invoker;
		Container scrollpane = SwingUtilities.getAncestorOfClass(JScrollPane.class, invoker);
		if (scrollpane != null) {
			actualInvoker = scrollpane;
		}

		// border first because #pack() is called afterwards
		content.setBorder(border);
		final NotificationPopup popup = new NotificationPopup(content, delay, listener);
		content.addMouseListener(popup.getMouseListener());

		int x, y;
		switch (location) {
			case UPPER_LEFT:
				x = 0 + paddingX;
				y = 0 + paddingY;
				break;
			case UPPER_CENTER:
				x = (actualInvoker.getWidth() - popup.getPreferredSize().width) / 2 + paddingX;
				y = 0 + paddingY;
				break;
			case UPPER_RIGHT:
				x = actualInvoker.getWidth() - popup.getPreferredSize().width - paddingX;
				y = 0 + paddingY;
				break;
			case LOWER_LEFT:
				x = 0 + paddingX;
				y = actualInvoker.getHeight() - popup.getPreferredSize().height - paddingY;
				break;
			case LOWER_CENTER:
				x = (actualInvoker.getWidth() - popup.getPreferredSize().width) / 2 + paddingX;
				y = actualInvoker.getHeight() - popup.getPreferredSize().height - paddingY;
				break;
			case LOWER_RIGHT:
				x = actualInvoker.getWidth() - popup.getPreferredSize().width - paddingX;
				y = actualInvoker.getHeight() - popup.getPreferredSize().height - paddingY;
				break;
			case CENTER:
				x = (actualInvoker.getWidth() - popup.getPreferredSize().width) / 2 + paddingX;
				y = (actualInvoker.getHeight() - popup.getPreferredSize().height) / 2 + paddingY;
				break;
			case CENTER_LEFT:
				x = 0 + paddingX;
				y = (actualInvoker.getHeight() - popup.getPreferredSize().height) / 2 + paddingY;
				break;
			case CENTER_RIGHT:
				x = actualInvoker.getWidth() - popup.getPreferredSize().width - paddingX;
				y = (actualInvoker.getHeight() - popup.getPreferredSize().height) / 2 + paddingY;
				break;
			default:
				x = 0 + paddingX;
				y = 0 + paddingY;
		}
		x += actualInvoker.getLocationOnScreen().getX();
		y += actualInvoker.getLocationOnScreen().getY();

		popup.setLocation(x, y);
		popup.setFocusableWindowState(false); // does not take focus away from the currently focused
		// component
		popup.setVisible(true);
		popup.setFocusableWindowState(true); // afterwards it should be focusable

		return popup;
	}

	/**
	 * Restarts the timer until fadeout if notification has not started to fade out.
	 *
	 * @return {@code true} if the restart was successful
	 */
	public boolean restartTimer() {
		if (fadeInTimer.isRunning()) {
			// fadeOutTimer not started yet
			return true;
		}
		if (fadeOutTimer.isRunning() && alpha >= 1.0f) {
			fadeOutTimer.restart();
			return true;
		}
		return false;
	}
}
