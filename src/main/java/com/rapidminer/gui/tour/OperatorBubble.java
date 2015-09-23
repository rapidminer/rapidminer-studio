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
package com.rapidminer.gui.tour;

import java.awt.Point;
import java.awt.Window;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.JViewport;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.flow.ProcessInteractionListener;
import com.rapidminer.gui.flow.ProcessPanel;
import com.rapidminer.gui.flow.processrendering.view.ProcessRendererView;
import com.rapidminer.gui.tools.dialogs.ErrorDialog;
import com.rapidminer.gui.tour.comic.ComicManager;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.tools.LogService;


/**
 * This class creates a speech bubble-shaped JDialog, which can be attached to an Operator by using
 * its ID. The bubble triggers two events which are observable by the {@link BubbleListener}; either
 * if the close button was clicked, or if the corresponding button was used. The keys for the title
 * and the text must be of format gui.bubble.xxx.body or gui.bubble.xxx.title .
 *
 * @author Thilo Kamradt
 *
 */

public class OperatorBubble extends BubbleWindow {

	private static final long serialVersionUID = 7404582361212798730L;

	private int split;

	private Class<? extends Operator> operatorClass;
	private Operator onDisplay = null;
	private OperatorChain homeChain = null;

	private ProcessRendererView renderer = RapidMinerGUI.getMainFrame().getProcessPanel().getProcessRenderer();
	private JViewport viewport = RapidMinerGUI.getMainFrame().getProcessPanel().getViewPort();
	private ProcessInteractionListener rendererListener;
	private ChangeListener viewPortListener;
	private ProcessInteractionListener assistantRendererListener;
	private String notInChainKey = "operatorNotDisplayed";

	/**
	 * Creates a BubbleWindow which points to an Operator
	 *
	 * @param owner
	 *            the {@link Window} on which this {@link BubbleWindow} should be shown.
	 * @param preferredAlignment
	 *            offer for alignment but the Class will calculate by itself whether the position is
	 *            usable.
	 * @param i18nKey
	 *            of the message which should be shown
	 * @param toAttach
	 *            Class of the Operator which should be attached
	 * @param arguments
	 *            arguments to pass thought to the I18N Object
	 */
	public OperatorBubble(Window owner, AlignedSide preferredAlignment, String i18nKey, Class<? extends Operator> toAttach,
			Object... arguments) {
		this(owner, preferredAlignment, i18nKey, toAttach, 1, arguments);
	}

	/**
	 * Creates a BubbleWindow which points to an Operator
	 *
	 * @param owner
	 *            the {@link Window} on which this {@link BubbleWindow} should be shown.
	 * @param preferredAlignment
	 *            offer for alignment but the Class will calculate by itself whether the position is
	 *            usable.
	 * @param i18nKey
	 *            of the message which should be shown
	 * @param toAttach
	 *            Class of the Operator which should be attached
	 * @param split
	 *            if there are more than one Operator of the given Class the Operator at the
	 *            position of split(started to count by 1) will be chosen.(if there are less
	 *            Operator than the given split the last one will be chosen)
	 * @param arguments
	 *            arguments to pass thought to the I18N Object
	 */
	public OperatorBubble(Window owner, AlignedSide preferredAlignment, String i18nKey, Class<? extends Operator> toAttach,
			int split, Object... arguments) {
		this(owner, preferredAlignment, i18nKey, toAttach, split, null, arguments);
	}

	/**
	 * Creates a BubbleWindow which points to an Operator
	 *
	 * @param owner
	 *            the {@link Window} on which this {@link BubbleWindow} should be shown.
	 * @param preferredAlignment
	 *            offer for alignment but the Class will calculate by itself whether the position is
	 *            usable.
	 * @param i18nKey
	 *            of the message which should be shown
	 * @param toAttach
	 *            Class of the Operator which should be attached
	 * @param split
	 *            if there are more than one Operator of the given Class the Operator at the
	 *            position of split(started to count by 1) will be chosen.(if there are less
	 *            Operator than the given split the last one will be chosen)
	 * @param buttonsToAdd
	 *            array of JButton's which will be added to the Bubble (null instead of the array
	 *            won't throw an error).
	 * @param arguments
	 *            arguments to pass thought to the I18N Object
	 */
	public OperatorBubble(Window owner, AlignedSide preferredAlignment, String i18nKey, Class<? extends Operator> toAttach,
			int split, JButton[] buttonsToAdd, Object... arguments) {
		super(owner, preferredAlignment, i18nKey, ProcessPanel.PROCESS_PANEL_DOCK_KEY, buttonsToAdd, arguments);
		operatorClass = toAttach;
		this.split = split;
		Operator[] matchingOperators = this.getMatchingOperatorsInChain(operatorClass, renderer.getModel()
				.getDisplayedChain());
		if (matchingOperators.length == 0) {
			boolean operatorInProcess = false;
			for (Operator op : RapidMinerGUI.getMainFrame().getProcess().getAllOperators()) {
				if (operatorClass.isAssignableFrom(op.getClass())) {
					operatorInProcess = true;
				}
			}
			if (!operatorInProcess) {
				// only show error dialog if we are not running a comic, because then the error does
				// not help
				if (!ComicManager.getInstance().isEpisodeRunning()) {
					LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.tour.BubbleToOperator.operatorNotAvailable");
					new ErrorDialog(owner, "tour.OperatorNotAvailable").setVisible(true);
				}
				this.killBubble(true);
			} else {
				this.registerRegularListener();
				this.changeToAssistant(AssistantType.NOT_IN_CHAIN);
			}
		} else {
			onDisplay = matchingOperators[matchingOperators.length <= split ? matchingOperators.length - 1 : split - 1];
			homeChain = renderer.getModel().getDisplayedChain();
			renderer.scrollRectToVisible(renderer.getModel().getOperatorRect(onDisplay).getBounds());
			super.paint(false);
		}

	}

	/**
	 * collects all Operators in the given Chain with the same Class
	 *
	 * @param operatorClass
	 *            Class of the Operators which should be collected
	 * @param displayedChain
	 *            the Chain in which will be searched for the Operators of the given Class
	 * @return an Array of the found Operators
	 */
	private Operator[] getMatchingOperatorsInChain(Class<? extends Operator> operatorClass, OperatorChain displayedChain) {
		ArrayList<Operator> matching = new ArrayList<>();
		List<Operator> operatorsInChain = displayedChain.getAllInnerOperators();
		for (Operator operatorInChain : operatorsInChain) {
			if (operatorClass.isAssignableFrom(operatorInChain.getClass())) {
				matching.add(operatorInChain);
			}
		}
		return matching.toArray(new Operator[0]);
	}

	@Override
	protected void registerSpecificListener() {
		viewPortListener = new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				OperatorBubble.this.paint(false);
			}
		};
		viewport.addChangeListener(viewPortListener);
		rendererListener = new ProcessInteractionListener() {

			@Override
			public void portContextMenuWillOpen(JPopupMenu menu, Port port) {
				// do not care
			}

			@Override
			public void operatorMoved(Operator op) {
				if (op.equals(OperatorBubble.this.onDisplay)) {
					OperatorBubble.this.paint(false);
				}
			}

			@Override
			public void operatorContextMenuWillOpen(JPopupMenu menu, Operator operator) {
				// do not care

			}

			@Override
			public void displayedChainChanged(OperatorChain displayedChain) {
				if (homeChain != null && onDisplay != null) {
					if (!homeChain.equals(displayedChain)) {
						// Operator chain has changed, but it may be still the same process
						boolean stillContainsOperator = false;
						for (Operator op : homeChain.getSubprocess(0).getOperators()) {
							if (op.getClass().equals(getOperator().getClass())) {
								stillContainsOperator = true;
								break;
							}
						}
						if (!stillContainsOperator) {
							OperatorBubble.this.changeToAssistant(AssistantType.NOT_IN_CHAIN);
						}
					}
				} else {
					OperatorBubble.this.changeToAssistant(AssistantType.NOT_IN_CHAIN);
				}
			}
		};
		renderer.addProcessInteractionListener(rendererListener);
	}

	@Override
	protected void unregisterSpecificListeners() {
		renderer.removeProcessInteractionListener(rendererListener);
		viewport.removeChangeListener(viewPortListener);
	}

	@Override
	protected Point getObjectLocation() {
		// get all necessary parameters
		if (!getDockable().getComponent().isShowing()) {
			throw new RuntimeException("Component is not Screen. Can not attach to not showing Component");
		}

		// // calculate Location of the upper left corner of the Operator, in the visible Rect
		Rectangle2D rec = renderer.getModel().getOperatorRect(onDisplay);
		double xOperator = (rec.getMinX() + rec.getCenterX()) / 2 + 4;
		double yOperator = (rec.getMinY() + rec.getCenterY()) / 2;

		// calculate LocationOnScreen of the upper left corner of the Operator
		Point view = RapidMinerGUI.getMainFrame().getProcessPanel().getProcessRenderer().getVisibleRect().getLocation();
		Point toReturn = new Point((int) (viewport.getLocationOnScreen().x + (xOperator - view.x)),
				(int) (viewport.getLocationOnScreen().y + (yOperator - view.y)));

		// return validated Point
		return this.validatePointForBubbleInViewport(toReturn);

	}

	@Override
	protected int getObjectWidth() {
		return (int) Math.round(renderer.getModel().getOperatorRect(onDisplay).getWidth());
	}

	@Override
	protected int getObjectHeight() {
		return (int) Math.round(renderer.getModel().getOperatorRect(onDisplay).getHeight());
	}

	/**
	 * validates the position of a Bubble and manipulates the position so that the Bubble won't
	 * point to a Point outside of the Viewport if the Operator is not in the Viewport (the
	 * Alignment of the Bubble is considered).
	 *
	 * @param position
	 *            Point to validate
	 * @return returns a Point inside the {@link JViewport}
	 */
	private Point validatePointForBubbleInViewport(Point position) {
		// calculate Offset which is necessary to consider the Alignment
		int xOffset = 0;
		int yOffset = 0;
		int x = position.x;
		int y = position.y;
		if (getRealAlignment() != null) {
			switch (getRealAlignment()) {
				case LEFTBOTTOM:
				case LEFTTOP:
					xOffset = this.getObjectWidth();
				case RIGHTBOTTOM:
				case RIGHTTOP:
					yOffset = (int) (this.getObjectHeight() * 0.5);
					break;
				case TOPLEFT:
				case TOPRIGHT:
					yOffset = this.getObjectHeight();
				case BOTTOMLEFT:
				case BOTTOMRIGHT:
					xOffset = (int) (this.getObjectWidth() * 0.5);
					break;
				default:
			}
		}
		// manipulate invalid coordinates
		if (!(position.x + xOffset >= viewport.getLocationOnScreen().x)) {
			// left
			x = viewport.getLocationOnScreen().x - xOffset;
		}
		if (!(position.x + xOffset <= viewport.getLocationOnScreen().x + viewport.getSize().width)) {
			// right
			x = viewport.getLocationOnScreen().x + viewport.getSize().width - xOffset;
		}
		if (!(position.y + yOffset >= viewport.getLocationOnScreen().y)) {
			// top
			y = viewport.getLocationOnScreen().y - yOffset;
		}
		if (!(position.y + yOffset <= viewport.getLocationOnScreen().y + viewport.getSize().height)) {
			// bottom
			y = viewport.getLocationOnScreen().y + viewport.getSize().height - yOffset;
		}

		return new Point(x, y);
	}

	@Override
	protected void changeToAssistant(AssistantType type) {
		if (getAssistantBubble() != null) {
			return;
		}
		// leftover listener fires with a delay, prevent popup
		if (isKilled()) {
			return;
		}
		if (type == AssistantType.NOT_IN_CHAIN) {
			setAssistantBubble(new DockableBubble(getOwner(), AlignedSide.RIGHT, notInChainKey, getDockableKey(), true,
					new Object[] { operatorClass.getName() }));
			this.unregisterRegularListener();
			assistantRendererListener = new ProcessInteractionListener() {

				@Override
				public void displayedChainChanged(OperatorChain displayedChain) {
					if (onDisplay == null) {
						Operator[] matching = OperatorBubble.this.getMatchingOperatorsInChain(operatorClass, displayedChain);
						if (matching.length != 0) {
							onDisplay = matching[matching.length <= split ? matching.length - 1 : split - 1];
							homeChain = displayedChain;
							closeOperatorAssistant();
							OperatorBubble.this.changeToMainBubble();
						}
					} else if (homeChain.equals(displayedChain)) {
						OperatorBubble.this.changeToMainBubble();
						closeOperatorAssistant();
						OperatorBubble.this.paint(false);
						OperatorBubble.this.setVisible(true);
					}
				}

				@Override
				public void operatorContextMenuWillOpen(JPopupMenu menu, Operator operator) {
					// do not care

				}

				@Override
				public void portContextMenuWillOpen(JPopupMenu menu, Port port) {
					// do not care

				}

				@Override
				public void operatorMoved(Operator op) {
					// do not care

				}
			};
			renderer.addProcessInteractionListener(assistantRendererListener);
			getAssistantBubble().setVisible(true);
			setCurrentAssistantType(type);
		} else {
			super.changeToAssistant(type);
		}
	}

	@Override
	protected void changeToMainBubble() {
		if (getAssistantBubble() == null) {
			return;
		}
		if (getCurrentAssistantType() == AssistantType.NOT_IN_CHAIN) {
			closeOperatorAssistant();
			registerRegularListener();
			paint(false);
			setVisible(true);
		} else {
			super.changeToMainBubble();
		}
	}

	/** disposes the NOTINCHAIN-Assistant */
	private void closeOperatorAssistant() {
		if (getAssistantBubble() != null && getCurrentAssistantType() == AssistantType.NOT_IN_CHAIN) {
			getAssistantBubble().triggerFire();
			setAssistantBubble(null);
			renderer.removeProcessInteractionListener(assistantRendererListener);
			setCurrentAssistantType(AssistantType.NO_ASSISTANT_ACTIVE);
		}
	}

	@Override
	protected void closeAssistants() {
		super.closeAssistants();
		this.closeOperatorAssistant();
	}

	public Operator getOperator() {
		return onDisplay;
	}
}
