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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ListHoverHelper;
import com.rapidminer.gui.tools.MenuShortcutJList;


/**
 * Container in which the user can switch the displayed children by clicking on an icon in a bar on
 * the left hand side.
 *
 * Titles and icons for the elements representing the individual cards are taken from the GUI
 * properties gui.cards.PANEL_KEY.CARD_KEY.title and gui.cards.PANEL_KEY.CARD_KEY.icon where
 * PANEL_KEY is the key passed to {@link #ButtonBarCardPanel(String)} and CARD_KEY is the one passed
 * to {@link #addCard(String, JComponent)}.
 *
 * @author Florian Ziegler, David Arnu, Nils Woehler
 *
 */
public class ButtonBarCardPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private final DefaultListModel<Card> cardListModel = new DefaultListModel<>();

	private JPanel content;

	private JList<Card> navigation;

	private CardLayout cardLayout;

	private Map<String, Component> keyToComponentMap;

	private boolean showCards = true;

	private final Set<String> noCardKeys;

	private ExtendedJScrollPane navigationScrollPane;

	/**
	 * Constructor that creates a {@link ButtonBarCardPanel} which always shows the list of cards.
	 */
	public ButtonBarCardPanel() {
		this(new HashSet<String>(), true);
	}

	/**
	 * Constructor that creates a {@link ButtonBarCardPanel} with cards shown.
	 *
	 * @param showCards
	 *            if set to <code>false</code> the cards are not shown and the user cannot select
	 *            the shown card manually
	 */
	public ButtonBarCardPanel(Set<String> noCardKeys, boolean showCards) {
		this.noCardKeys = noCardKeys;
		this.showCards = showCards;

		navigation = new MenuShortcutJList<Card>(cardListModel, false) {

			private static final long serialVersionUID = -5414386397971825656L;

			@Override
			protected void paintComponent(Graphics g) {
				g.setColor(Color.WHITE);
				g.fillRect(0, 0, getWidth(), getHeight());
				g.setColor(Color.LIGHT_GRAY);
				g.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
				super.paintComponent(g);
			}
		};

		navigation.setOpaque(true);
		DefaultListSelectionModel listSelectionModel = new DefaultListSelectionModel() {

			private static final long serialVersionUID = 1L;

			@Override
			public void removeSelectionInterval(final int index0, final int index1) {
				// deselecting is not allowed
				return;
			}
		};
		ListHoverHelper.install(navigation);
		navigation.setSelectionModel(listSelectionModel);
		navigation.setBounds(5, 5, navigation.getWidth(), navigation.getHeight());
		navigation.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.LIGHT_GRAY));
		navigation.setFixedCellHeight(100);
		navigation.setBackground(Colors.PANEL_BACKGROUND);
		navigation.setSelectionForeground(Color.BLACK);
		navigation.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		navigation.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				// this prevents firing of multiple events for the same thing
				if (e.getValueIsAdjusting()) {
					return;
				}
				Card card = navigation.getSelectedValue();
				if (card != null) {
					cardLayout.show(content, card.getKey());
					fireCardSelectedEvent(card.getKey());
				}
			}
		});
		navigation.setCellRenderer(new CardCellRenderer());

		cardLayout = new CardLayout();
		content = new JPanel(cardLayout);
		setLayout(new BorderLayout(0, 25));

		navigationScrollPane = new ExtendedJScrollPane(navigation);
		navigationScrollPane.setBorder(BorderFactory.createEmptyBorder());
		navigationScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		navigationScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		navigationScrollPane.setPreferredSize(new Dimension(105, 0));

		// set component orientation to get scrollbar on left side
		navigationScrollPane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

		add(navigationScrollPane, BorderLayout.WEST);

		add(content, BorderLayout.CENTER);
		keyToComponentMap = new HashMap<>();
	}

	/**
	 * Adds a card to the {@link ButtonBarCardPanel}
	 *
	 * @param card
	 *            the card referring to the component
	 * @param componentToAdd
	 *            the component that is shown once the card is selected
	 */
	public void addCard(Card card, Component componentToAdd) {
		JPanel borderPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		borderPanel.add(componentToAdd, gbc);

		content.add(borderPanel, card.getKey());
		keyToComponentMap.put(card.getKey(), componentToAdd);

		// in the below cases an unnecessary additional panel is suppressed
		if (noCardKeys.contains(card.getKey()) || !showCards) {
			navigationScrollPane.setPreferredSize(new Dimension(0, navigation.getHeight()));
		}

		cardListModel.addElement(card);
		if (cardListModel.size() == 1) {
			navigation.setSelectedIndex(0);
		}
	}

	/**
	 *
	 * @return the component of this ButtonBarCardPanel that is currently shown
	 */
	public Component getShownComponent() {
		return keyToComponentMap.get(navigation.getSelectedValue().getKey());
	}

	/**
	 * @return the currently selected card
	 */
	public Card getSelectedCard() {
		return navigation.getSelectedValue();
	}

	/**
	 * @param index
	 *            changes the selected card to the card at the specified index
	 */
	public void setSelectedCard(int index) {
		navigation.setSelectedIndex(index);
	}

	/**
	 * @param key
	 *            Sets the selections to a specific card identified by it's key. If the key is not
	 *            present, nothings happens.
	 */
	public void selectCard(String key) {
		for (int i = 0; i < navigation.getModel().getSize(); i++) {
			if (navigation.getModel().getElementAt(i).getKey().equals(key)) {
				navigation.setSelectedIndex(i);
			}

		}
	}

	public void addCardSelectionListener(CardSelectionListener l) {
		listenerList.add(CardSelectionListener.class, l);
	}

	public void removeCardSelectionListener(CardSelectionListener l) {
		listenerList.remove(CardSelectionListener.class, l);
	}

	private void fireCardSelectedEvent(String key) {
		for (CardSelectionListener listener : getListeners(CardSelectionListener.class)) {
			CardSelectionEvent e = new CardSelectionEvent(this, key);
			listener.cardSelected(e);
		}
	}
}
