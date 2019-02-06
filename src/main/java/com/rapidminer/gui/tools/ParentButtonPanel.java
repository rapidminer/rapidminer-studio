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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.LinkedList;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.event.EventListenerList;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.rapidminer.gui.LoggedAbstractAction;
import com.rapidminer.gui.tools.components.LinkLocalButton;
import com.rapidminer.tools.I18N;


/**
 *
 * @author Tobias Malbrecht
 */
public class ParentButtonPanel<T> extends ExtendedJToolBar {

	private static final String BREADCRUMB_ABBREVIATION = "...";

	private static final int MAX_BREADCRUMB_LENGTH = 28;

	private static final long serialVersionUID = 1L;

	private static final String RIGHT_ARROW = "<html><span style=\"color: 4F4F4F;\">" + Ionicon.ARROW_RIGHT_B.getHtml()
			+ "</span></html>";

	private static final String DOWN_ARROW = "<html><span style=\"color: 4F4F4F;\">" + Ionicon.ARROW_DOWN_B.getHtml()
			+ "</span></html>";

	private static final int HISTORY_SIZE = 20;

	private LinkedList<T> backward = new LinkedList<>();

	private LinkedList<T> forward = new LinkedList<>();

	private ParentButtonModel<T> model;

	private T currentNode;

	private T selectedNode;

	private boolean selectionByHistory = false;

	private JButton upButton = new JButton(new ResourceAction(true, "select_parent") {

		private static final long serialVersionUID = -5411675828764033039L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			selectedNode = model.getParent(currentNode);
			fireAction();
		}
	});

	public ParentButtonPanel() {
		this(null);
	}

	public ParentButtonPanel(ParentButtonModel<T> model) {
		setModel(model);
		setOpaque(false);

	}

	private final MouseListener borderListener = new MouseAdapter() {

		@Override
		public void mouseEntered(MouseEvent e) {
			if (e.getComponent() instanceof AbstractButton) {
				((AbstractButton) e.getComponent()).setBorderPainted(true);
			}
		}

		@Override
		public void mouseExited(MouseEvent e) {
			if (e.getComponent() instanceof AbstractButton) {
				((AbstractButton) e.getComponent()).setBorderPainted(false);
			}
		}
	};

	@Override
	public Component add(Component c) {
		Component result = super.add(c);
		c.addMouseListener(borderListener);
		return result;
	}

	public void setSelectedNode(T node) {
		this.currentNode = node;
		upButton.setEnabled(model.getParent(currentNode) != null);
		setup();
	}

	public void setModel(ParentButtonModel<T> model) {
		this.model = model;
		setup();
	}

	private void setup() {
		removeAll();
		addSeparator();
		addSeparator();
		add(upButton);

		if (model != null) {

			LinkedList<T> nodes = new LinkedList<>();
			T node = currentNode;
			while (node != null) {
				nodes.addFirst(node);
				node = model.getParent(node);
			}

			boolean maxDepthReached = false;
			for (T n : nodes) {
				if (n.equals(nodes.getFirst()) || n.equals(nodes.getLast()) || n.equals(nodes.get(nodes.size() - 2))) {
					// always show the first, n-th and (n-1)th button
					add(makeButton(n));

					// only show last drop down button in case any children are available
					if (model.getNumberOfChildren(n) != 0) {
						add(makeDropdownButton(n));
					}
				} else if (!maxDepthReached) {
					JButton emptyButton = new JButton(BREADCRUMB_ABBREVIATION);
					// remove all mouse actions for this button, so it can't be clicked or
					// highlighted
					MouseListener[] actions = emptyButton.getMouseListeners();
					for (MouseListener a : actions) {
						emptyButton.removeMouseListener(a);
					}
					add(emptyButton);
					maxDepthReached = true;
				} else {
					// don't add anymore buttons
				}
			}
		}
		revalidate();
		repaint();
	}

	private LinkLocalButton makeButton(final T node) {
		String name = model.toString(node);
		if (name.length() > MAX_BREADCRUMB_LENGTH) {
			name = name.substring(0, MAX_BREADCRUMB_LENGTH - BREADCRUMB_ABBREVIATION.length()) + BREADCRUMB_ABBREVIATION;
		}

		if (node.equals(currentNode)) {
			Action action = new LoggedAbstractAction("<span style=\"font-weight: bold; text-decoration: none; color: #000000\">"
					+ name + "</span>") {

				private static final long serialVersionUID = 1L;

				@Override
				public void loggedActionPerformed(ActionEvent e) {
					selectedNode = node;
					fireAction();
				}
			};
			LinkLocalButton button = new LinkLocalButton(action);
			button.setToolTipText(I18N.getGUIMessage("gui.button.process_panel.breadcrumbs.current.tip"));
			button.setBorder(BorderFactory.createEmptyBorder(7, 0, 0, 0));
			return button;
		} else {
			Action action = new LoggedAbstractAction(name) {

				private static final long serialVersionUID = 1L;

				@Override
				public void loggedActionPerformed(ActionEvent e) {
					selectedNode = node;
					fireAction();
				}
			};
			LinkLocalButton button = new LinkLocalButton(action);
			button.setToolTipText(I18N.getGUIMessage("gui.button.process_panel.breadcrumbs.any.tip"));
			button.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
			return button;
		}
	}

	private JButton makeDropdownButton(final T node) {
		final JButton button = new JButton(RIGHT_ARROW);
		button.setOpaque(false);
		button.setBorderPainted(false);
		button.setMargin(new Insets(0, 0, 0, 0));
		button.setPreferredSize(new Dimension(16, 16));
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// hack to prevent filter popup from opening itself again when you click the button
				// to actually close it while it is open
				try {
					long lastClose = Long.parseLong(String.valueOf(button.getClientProperty("lastCloseTime")));
					if (System.currentTimeMillis() - lastClose < 250) {
						return;
					}
				} catch (NumberFormatException e1) {
					// ignore
				}

				JPopupMenu menu = makeMenu(node);
				button.setText(DOWN_ARROW);
				menu.addPopupMenuListener(new PopupMenuListener() {

					@Override
					public void popupMenuCanceled(PopupMenuEvent e) {}

					@Override
					public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
						button.setText(RIGHT_ARROW);
						button.putClientProperty("lastCloseTime", System.currentTimeMillis());
					}

					@Override
					public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}
				});
				menu.show(button, 0, button.getHeight() - 1);
			}
		});
		return button;
	}

	private JPopupMenu makeMenu(final T node) {
		JPopupMenu menu = new JPopupMenu();
		for (int i = 0; i < model.getNumberOfChildren(node); i++) {
			final T child = model.getChild(node, i);
			menu.add(new LoggedAbstractAction(model.toString(child)) {

				private static final long serialVersionUID = 7232177147279985209L;

				{
					putValue(SMALL_ICON, model.getIcon(child));
				}

				@Override
				public void loggedActionPerformed(ActionEvent e) {
					selectedNode = child;
					fireAction();
				}
			});
		}
		return menu;
	}

	public T getSelectedNode() {
		return selectedNode;
	}

	private EventListenerList listeners = new EventListenerList();

	public void addActionListener(ActionListener l) {
		listeners.add(ActionListener.class, l);
	}

	public void removeActionListener(ActionListener l) {
		listeners.remove(ActionListener.class, l);
	}

	private int eventId = 0;

	public void clearHistory() {
		backward.clear();
		forward.clear();
	}

	public void addToHistory(T node) {
		if (node == null) {
			return;
		}
		if (backward.size() > 0) {
			if (backward.getFirst() != node) {
				backward.remove(node);
				backward.addFirst(node);
				if (!selectionByHistory) {
					forward.clear();
				}
			}
		} else {
			backward.remove(node);
			backward.addFirst(node);
			if (!selectionByHistory) {
				forward.clear();
			}
		}
		while (backward.size() > HISTORY_SIZE) {
			backward.removeLast();
		}
		selectionByHistory = false;
	}

	private void fireAction() {
		ActionEvent e = new ActionEvent(this, eventId++, "select");
		for (ActionListener l : listeners.getListeners(ActionListener.class)) {
			l.actionPerformed(e);
		}
		upButton.setEnabled(model.getParent(currentNode) != null);
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
	}
}
