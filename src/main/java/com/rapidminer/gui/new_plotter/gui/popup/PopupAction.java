/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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
package com.rapidminer.gui.new_plotter.gui.popup;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ResourceAction;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.JDialog;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;


/**
 * This action can be used to show a component as a popup on the screen. If the popup loses the
 * focus to another component in the containing window it will be hidden.
 * <p>
 * The action's settings are taken from a .properties file being part of the GUI Resource bundles of
 * RapidMiner. These might be accessed using the I18N class.
 * <p>
 * A resource action needs a key specifier, which will be used to build the complete keys of the
 * form:
 * <ul>
 * <li>gui.action.-key-.label = Which will be the caption</li>
 * <li>gui.action.-key-.icon = The icon of this action. For examples used in menus or buttons</li>
 * <li>gui.action.-key-.acc = The accelerator key used for menu entries</li>
 * <li>gui.action.-key-.tip = Which will be the tool tip</li>
 * <li>gui.action.-key-.mne = Which will give you access to the mnemonics key. Please make it the
 * same case as in the label</li>
 * </ul>
 * 
 * @author Nils Woehler
 * 
 */
public class PopupAction extends ResourceAction implements PopupComponentListener, ComponentListener {

	public enum PopupPosition {
		HORIZONTAL, VERTICAL
	}

	private class ContainerPopupDialog extends JDialog {

		private static final long serialVersionUID = 1L;

		public ContainerPopupDialog(Window owner, Component comp, Point point) {
			super(owner != null ? owner : RapidMinerGUI.getMainFrame());
			this.add(comp);
			this.setLocation(point);
			this.setUndecorated(true);
			pack();
		}

	}

	private static final long serialVersionUID = 1L;

	private final PopupPanel popupComponent;
	private Component actionSource = null;

	private ContainerPopupDialog popup = null;

	private PopupPosition position = PopupPosition.VERTICAL;

	private static final int BORDER_OFFSET = 9;

	private Window containingWindow;

	private long hideTime = 0;

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
	public synchronized void actionPerformed(ActionEvent e) {
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
		int xSource = source.getLocationOnScreen().x;
		int ySource = source.getLocationOnScreen().y;

		// get size of popup
		Dimension popupSize = ((Component) popupComponent).getSize();
		if (popupSize.width == 0) {
			popupSize = ((Component) popupComponent).getPreferredSize();
		}

		int xPopup = 0;
		int yPopup = 0;

		// get max x and y window positions
		Window focusedWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
		int maxX = focusedWindow.getLocationOnScreen().x + focusedWindow.getWidth();
		int maxY = focusedWindow.getLocationOnScreen().y + focusedWindow.getHeight();

		switch (position) {
			case VERTICAL:

				// place popup at sources' x position
				xPopup = xSource;

				// check if popup is outside active window
				if (xPopup + popupSize.width > maxX) {

					// move popup x position to the left
					// to fit inside the active window
					xPopup = maxX - popupSize.width - BORDER_OFFSET;
				}

				// place popup always below source (to avoid overlapping)
				yPopup = ySource + source.getHeight();

				// if the popup now would be moved outside of RM Studio to the left it would look
				// silly, so in that case just show it at its intended position and let it be cut
				// off on the right side as we cannot do anything about it
				if (xPopup < focusedWindow.getLocationOnScreen().x
						|| (xPopup - focusedWindow.getLocationOnScreen().x) + popupSize.width > focusedWindow.getWidth()) {
					xPopup = xSource;
				}

				break;
			case HORIZONTAL:

				// place popup always to the right side of the source (to avoid overlapping)
				xPopup = xSource + source.getWidth();

				yPopup = ySource;

				// check if popup is outside active window
				if (yPopup + popupSize.height > maxY) {

					// move popup upwards to fit into active window
					yPopup = maxY - popupSize.height - BORDER_OFFSET;
				}

				// if the popup now would be moved outside of RM Studio at the top it would look
				// silly, so in that case just show it at its intended position and let it be cut
				// off on the bottom side as we cannot do anything about it
				if (yPopup < focusedWindow.getLocationOnScreen().y
						|| (yPopup - focusedWindow.getLocationOnScreen().y) + popupSize.height > focusedWindow.getHeight()) {
					yPopup = ySource;
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
		popupComponent.startTracking(containingWindow, actionSource);
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
		// Unnecessary to hide on move event. Causes trouble on Linux
		// hidePopup();
	}

	@Override
	public void componentShown(ComponentEvent e) {
		return; // Nothing to be done
	}

	@Override
	public void componentHidden(ComponentEvent e) {
		hidePopup();
	}

}
