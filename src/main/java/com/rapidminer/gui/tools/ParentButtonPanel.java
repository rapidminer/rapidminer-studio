/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
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
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.event.EventListenerList;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.rapidminer.gui.tools.components.DropDownButton;
import com.rapidminer.tools.I18N;


/**
 *
 * @author Tobias Malbrecht
 */
public class ParentButtonPanel<T> extends ExtendedJToolBar {

	private static final int HISTORY_SIZE = 20;

	private static final long serialVersionUID = 7115627292465260984L;

	private LinkedList<T> backward = new LinkedList<>();

	private LinkedList<T> forward = new LinkedList<>();

	private ParentButtonModel<T> model;

	private T currentNode;

	private T selectedNode;

	private boolean selectionByHistory = false;

	private DropDownButton backwardButton = new DropDownButton(new ResourceAction(true, "select_backward") {

		private static final long serialVersionUID = 3096873810683015968L;

		@Override
		public void actionPerformed(ActionEvent e) {
			if (backward.size() > 1) {
				forward.addFirst(backward.removeFirst());
				selectedNode = backward.getFirst();
				selectionByHistory = true;
				fireAction();
			}
		}
	}) {

		private static final long serialVersionUID = -770314831566061205L;

		@Override
		protected JPopupMenu getPopupMenu() {
			return createBackwardMenu();
		}
	};

	private DropDownButton forwardButton = new DropDownButton(new ResourceAction(true, "select_forward") {

		private static final long serialVersionUID = -5189987068889279439L;

		@Override
		public void actionPerformed(ActionEvent e) {
			if (forward.size() > 0) {
				backward.addFirst(forward.removeFirst());
				selectedNode = backward.getFirst();
				selectionByHistory = true;
				fireAction();
			}
		}
	}) {

		private static final long serialVersionUID = -770314831566061205L;

		@Override
		protected JPopupMenu getPopupMenu() {
			return createForwardMenu();
		}
	};

	private JButton upButton = new JButton(new ResourceAction(true, "select_parent") {

		private static final long serialVersionUID = -5411675828764033039L;

		@Override
		public void actionPerformed(ActionEvent e) {
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
			if (!(e.getComponent() instanceof Separator)) {
				((AbstractButton) e.getComponent()).setBorderPainted(true);
			}
		}

		@Override
		public void mouseExited(MouseEvent e) {
			if (!(e.getComponent() instanceof Separator)) {
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
		backwardButton.setEnabled(backward.size() > 1);
		forwardButton.setEnabled(forward.size() > 0);
		setup();
	}

	public void setModel(ParentButtonModel<T> model) {
		this.model = model;
		setup();
	}

	private void setup() {
		removeAll();
		backwardButton.addToToolBar(this);
		forwardButton.addToToolBar(this);
		add(upButton);

		// add some space between the buttons and the actual breadcrumbs
		addSeparator();
		addSeparator();
		addSeparator();

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
					JButton emptyButton = new JButton("...");
					// remove all mouse actions for this button, so it can't be clicked or
					// highlighted
					MouseListener[] actions = emptyButton.getMouseListeners();
					for (MouseListener a : actions) {
						emptyButton.removeMouseListener(a);
					}
					add(emptyButton);
					ArrowButton arrow = new ArrowButton(SwingConstants.EAST);
					actions = arrow.getMouseListeners();
					for (MouseListener a : actions) {
						arrow.removeMouseListener(a);
					}
					add(arrow);
					maxDepthReached = true;
				} else {
					// don't add anymore buttons
				}
			}
		}
		revalidate();
		repaint();
	}

	private JPopupMenu createBackwardMenu() {
		JPopupMenu menu = new JPopupMenu("History");
		boolean first = true;
		for (final T node : backward) {
			if (first) {
				first = false;
				continue;
			}
			menu.add(new AbstractAction(model.toString(node)) {

				private static final long serialVersionUID = 1L;

				{
					putValue(SMALL_ICON, model.getIcon(node));
				}

				@Override
				public void actionPerformed(ActionEvent e) {
					selectedNode = node;
					Iterator<T> iterator = backward.iterator();
					while (iterator.hasNext()) {
						T nextNode = iterator.next();
						if (nextNode == node) {
							break;
						}
						iterator.remove();
						forward.addFirst(nextNode);
					}
					selectionByHistory = true;
					fireAction();
				}
			});
		}
		return menu;
	}

	private JPopupMenu createForwardMenu() {
		JPopupMenu menu = new JPopupMenu("History");
		for (final T node : forward) {
			menu.add(new AbstractAction(model.toString(node)) {

				private static final long serialVersionUID = 1L;

				{
					putValue(SMALL_ICON, model.getIcon(node));
				}

				@Override
				public void actionPerformed(ActionEvent e) {
					selectedNode = node;
					Iterator<T> iterator = forward.iterator();
					while (iterator.hasNext()) {
						T nextNode = iterator.next();
						iterator.remove();
						backward.addFirst(nextNode);
						if (nextNode == node) {
							break;
						}
					}
					selectionByHistory = true;
					fireAction();
				}
			});
		}
		return menu;
	}

	private AbstractButton makeButton(final T node) {
		JToggleButton button = new JToggleButton(model.toString(node), model.getIcon(node));
		button.setOpaque(false);
		button.setBorderPainted(false);
		button.setToolTipText(I18N.getGUIMessage("gui.button.process_panel.breadcrumbs.any.tip"));
		button.setMargin(new Insets(0, 0, 0, 0));
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				selectedNode = node;
				fireAction();
			}
		});
		if (node.equals(currentNode)) {
			button.setSelected(true);
			button.setFont(button.getFont().deriveFont(java.awt.Font.BOLD));
			button.setToolTipText(I18N.getGUIMessage("gui.button.process_panel.breadcrumbs.current.tip"));
		}
		return button;
	}

	private JButton makeDropdownButton(final T node) {
		final ArrowButton button = new ArrowButton(SwingConstants.EAST);
		button.setOpaque(false);
		button.setBorderPainted(false);
		button.setMargin(new Insets(0, 0, 0, 0));
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JPopupMenu menu = makeMenu(node);
				button.setDirection(SwingConstants.SOUTH);
				menu.addPopupMenuListener(new PopupMenuListener() {

					@Override
					public void popupMenuCanceled(PopupMenuEvent e) {}

					@Override
					public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
						button.setDirection(SwingConstants.EAST);
					}

					@Override
					public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}
				});
				menu.show(button, 0, button.getHeight());
			}
		});
		return button;
	}

	private JPopupMenu makeMenu(final T node) {
		JPopupMenu menu = new JPopupMenu();
		for (int i = 0; i < model.getNumberOfChildren(node); i++) {
			final T child = model.getChild(node, i);
			menu.add(new AbstractAction(model.toString(child)) {

				private static final long serialVersionUID = 7232177147279985209L;

				{
					putValue(SMALL_ICON, model.getIcon(child));
				}

				@Override
				public void actionPerformed(ActionEvent e) {
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
		backwardButton.setEnabled(backward.size() > 1);
		forwardButton.setEnabled(forward.size() > 0);
	}

	private void fireAction() {
		ActionEvent e = new ActionEvent(this, eventId++, "select");
		for (ActionListener l : listeners.getListeners(ActionListener.class)) {
			l.actionPerformed(e);
		}
		upButton.setEnabled(model.getParent(currentNode) != null);
		backwardButton.setEnabled(backward.size() > 1);
		forwardButton.setEnabled(forward.size() > 0);
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
	}
}
