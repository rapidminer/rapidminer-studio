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
package com.rapidminer.gui.popup;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ResourceAction;


/**
 * This action can be used to show a component as a popup on the screen. If the popup loses the focus to another
 * component in the containing window it will be hidden.
 * <p>
 * The action's settings are taken from a .properties file being part of the GUI Resource bundles of RapidMiner. These
 * might be accessed using the I18N class.
 * <p>
 * A resource action needs a key specifier, which will be used to build the complete keys of the form:
 * <ul>
 * <li>gui.action.-key-.label = Which will be the caption</li>
 * <li>gui.action.-key-.icon = The icon of this action. For examples used in menus or buttons</li>
 * <li>gui.action.-key-.acc = The accelerator key used for menu entries</li>
 * <li>gui.action.-key-.tip = Which will be the tool tip</li>
 * <li>gui.action.-key-.mne = Which will give you access to the mnemonics key. Please make it the
 * same case as in the label</li>
 * </ul>
 * <p>
 * Since 9.4.0: If a popup needs to be dynamically resized while it is being displayed, call {@link
 * Component#firePropertyChange(String, boolean, boolean)} on the component with {@code pack} as the property name. This
 * will trigger a {@link JDialog#pack()} call on the popup, changing its size to the currently preferred size of the component.
 * </p>
 *
 * @author Nils Woehler
 */
public class PopupAction extends ResourceAction implements PopupComponentListener, ComponentListener {

	public enum PopupPosition {
		HORIZONTAL, VERTICAL
	}

	private class ContainerPopupDialog extends JDialog {

		private static final long serialVersionUID = 1L;

		private Component component;

		ContainerPopupDialog(Window owner, Component comp, Point point) {
			super(owner != null ? owner : RapidMinerGUI.getMainFrame());
			this.component = comp;
			this.add(comp);
			this.setLocation(point);
			this.setUndecorated(true);
			pack();

			ResourceAction closeAction = new ResourceAction("close") {

				@Override
				public void loggedActionPerformed(ActionEvent e) {
					hidePopup();
				}

			};
			getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
					.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "CANCEL");
			getRootPane().getActionMap().put("CANCEL", closeAction);
		}

		@Override
		public boolean requestFocusInWindow() {
			return component != null ? component.requestFocusInWindow() : super.requestFocusInWindow();
		}

	}

	private static final long serialVersionUID = 1L;
	private static final String PACK_EVENT = "pack";

	private final PopupPanel popupComponent;

	private Component actionSource = null;

	private ContainerPopupDialog popup = null;

	private PopupPosition position = PopupPosition.VERTICAL;

	private static final int BORDER_OFFSET = 5;

	private Window containingWindow;

	private long hideTime = 0;

	/** listening for custom resize event */
	private PropertyChangeListener propertyChangeListener;


	public PopupAction(boolean smallIcon, String i18nKey, Component component, Object... i18nArgs) {
		super(smallIcon, i18nKey, i18nArgs);

		this.popupComponent = new PopupPanel(component);
		popupComponent.addListener(this);
	}

	public PopupAction(boolean smallIcon, String i18nKey, Component component, PopupPosition position, Object... i18nArgs) {
		super(smallIcon, i18nKey, i18nArgs);

		this.position = position;
		this.popupComponent = new PopupPanel(component);
		popupComponent.addListener(this);
	}

	public PopupAction(String i18nKey, Component component, Object... i18nArgs) {
		super(i18nKey, i18nArgs);
		this.popupComponent = new PopupPanel(component);
		popupComponent.addListener(this);
	}

	public PopupAction(String i18nKey, Component component, PopupPosition position, Object... i18nArgs) {
		super(i18nKey, i18nArgs);
		this.position = position;
		this.popupComponent = new PopupPanel(component);
		popupComponent.addListener(this);
	}

	@Override
	public synchronized void loggedActionPerformed(ActionEvent e) {
		if (System.currentTimeMillis() - hideTime > 150) {
			if (hidePopup()) {
				return;
			}
			showPopup((Component) e.getSource());
		} else {
			Object source = e.getSource();
			if (source instanceof JToggleButton) {
				((JToggleButton) e.getSource()).setSelected(false);
			}
		}
	}

	private Point calculatePosition(Component source) {
		if (!source.isShowing()) {
			// should not happen, but better safe than sorry
			return new Point(0, 0);
		}

		int xSource = source.getLocationOnScreen().x;
		int ySource = source.getLocationOnScreen().y;

		// get size of popup
		Dimension popupSize = popupComponent.getSize();
		if (popupSize.width == 0) {
			popupSize = ((Component) popupComponent).getPreferredSize();
		}

		int xPopup = 0;
		int yPopup = 0;

		// get max x and y screen coordinates
		Window focusedWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
		if (focusedWindow == null) {
			// should not happen, but better safe than sorry
			return new Point(xSource, ySource);
		}
		GraphicsConfiguration graphicsConfig = focusedWindow.getGraphicsConfiguration();
		if (graphicsConfig == null) {
			// should not happen, but better safe than sorry
			return new Point(xSource, ySource);
		}
		Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(graphicsConfig);
		int maxScreenX = graphicsConfig.getDevice().getDisplayMode().getWidth() - screenInsets.right;
		int maxScreenY = graphicsConfig.getDevice().getDisplayMode().getHeight() - screenInsets.bottom;

		switch (position) {
			case VERTICAL:

				// place popup at sources' x position
				xPopup = xSource;
				// place popup always below source (to avoid overlapping)
				yPopup = ySource + source.getHeight();

				// check if popup is outside active window
				if (xPopup + popupSize.width > maxScreenX) {

					// move popup x position to the left
					// to fit inside the active window
					xPopup = maxScreenX - popupSize.width - BORDER_OFFSET;
				}

				// if the popup now would be moved outside of screen to the left it would look
				// silly, so in that case just show it at its intended position and let it be cut
				// off on the right side as we cannot do anything about it
				if (xPopup < screenInsets.left) {
					xPopup = screenInsets.left;
				}

				break;
			case HORIZONTAL:

				// place popup always to the right side of the source (to avoid overlapping)
				xPopup = xSource + source.getWidth();

				// place popup at sources' y position
				yPopup = ySource;

				// check if popup is outside active window
				if (yPopup + popupSize.height > maxScreenY) {

					// move popup upwards to fit into active window
					yPopup = maxScreenY - popupSize.height - BORDER_OFFSET;
				}

				// if the popup now would be moved outside of screen at the top it would look
				// silly, so in that case just show it at top of screen and let it be cut
				// off on the bottom side as we cannot do anything about it
				if (yPopup < screenInsets.top) {
					yPopup = screenInsets.top;
				}

				break;
		}

		return new Point(xPopup, yPopup);

	}

	/**
	 * Creates the popup, calculates position and sets focus
	 */
	private void showPopup(Component source) {

		actionSource = source;
		actionSource.addComponentListener(this);

		if (actionSource instanceof JToggleButton) {
			JToggleButton toggleSource = (JToggleButton) actionSource;
			toggleSource.setSelected(true);
		}

		containingWindow = SwingUtilities.windowForComponent(actionSource);
		containingWindow.addComponentListener(this);

		Point position = calculatePosition(source);
		popupComponent.setLocation(position);
		popupComponent.setVisible(true);
		popup = new ContainerPopupDialog(containingWindow, popupComponent, position);
		popup.setVisible(true);
		popup.requestFocus();
		popupComponent.startTracking(containingWindow);

		if (propertyChangeListener != null) {
			popupComponent.getComponent().removePropertyChangeListener(PACK_EVENT, propertyChangeListener);
		}
		propertyChangeListener =  e -> {
			if (popup != null) {
				popup.pack();

				// best position may change due to changed dimensions, recalculate and set
				Point popupPosition = calculatePosition(source);
				popup.setLocation(popupPosition);
			}
		};
		popupComponent.getComponent().addPropertyChangeListener(PACK_EVENT, propertyChangeListener);
	}

	/**
	 * Hides the popup component.
	 */
	private boolean hidePopup() {
		if (actionSource instanceof JToggleButton) {
			JToggleButton toggleSource = (JToggleButton) actionSource;
			toggleSource.setSelected(false);
		}

		if (containingWindow != null) {
			containingWindow.removeComponentListener(this);
			containingWindow.requestFocusInWindow();
			containingWindow = null;
		}

		if (actionSource != null) {
			actionSource.removeComponentListener(this);
			actionSource = null;
		}

		// Check if popup is visible
		if (popup != null) {
			popupComponent.setVisible(false);
			popupComponent.stopTracking();
			popupComponent.getComponent().removePropertyChangeListener(PACK_EVENT, propertyChangeListener);

			// hide popup and reset
			popup.dispose();
			popup = null;

			hideTime = System.currentTimeMillis();
			return true;
		}

		return false;
	}

	@Override
	public void focusLost() {
		hidePopup();
	}

	@Override
	public void componentResized(ComponentEvent e) {
		hidePopup();
	}

	@Override
	public void componentMoved(ComponentEvent e) {
		// Nothing to be done
	}

	@Override
	public void componentShown(ComponentEvent e) {
		// Nothing to be done
	}

	@Override
	public void componentHidden(ComponentEvent e) {
		hidePopup();
	}

}
