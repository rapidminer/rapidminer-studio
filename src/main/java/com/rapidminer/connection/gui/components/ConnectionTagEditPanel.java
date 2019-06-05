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
package com.rapidminer.connection.gui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import org.jdesktop.swingx.WrapLayout;

import com.rapidminer.connection.util.ConnectionI18N;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.FontTools;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;


/**
 * The panel to edit connection tags.
 *
 * @author Jonas Wilms-Pfau
 * @since 9.3
 */
public class ConnectionTagEditPanel extends JPanel {

	private static final String CLOSE_SYMBOL = "\uf2d7";
	private static final Font CLOSE_FONT = new Font("Ionicons", Font.PLAIN, 20);
	private static final Color TAG_BACKGROUND = new Color(204, 203, 203);
	private static final Color TAG_FOREGROUND = new Color(74, 74, 74);
	private static final Color TAG_BACKGROUND_DISABLED = new Color(225, 225, 225);
	private static final Color TAG_FOREGROUND_DISABLED = ConnectionInfoPanel.UNKNOWN_TYPE_COLOR;
	private static final Font OPEN_SANS_12 = FontTools.getFont("Open Sans", Font.PLAIN, 12);
	private static final Font OPEN_SANS_13 = FontTools.getFont("Open Sans", Font.PLAIN, 13);

	private final boolean editable;
	private final transient Consumer<List<String>> setTags;
	private final LinkedHashMap<JComponent, String> tagToValue = new LinkedHashMap<>();
	private final boolean isEnabled;

	ConnectionTagEditPanel(ObservableList<String> tags, Consumer<List<String>> setTags, final boolean editable, final boolean enabled) {
		super(new WrapLayout(FlowLayout.LEFT, editable ? 11 : 0, editable ? 11 : 0));
		this.editable = editable;
		this.setTags = setTags;
		this.isEnabled = enabled;

		if (tags.isEmpty() && !editable) {
			JLabel noTagLabel = new JLabel(ConnectionI18N.getConnectionGUILabel("no_tags"));
			noTagLabel.setFont(OPEN_SANS_12);
			if (!enabled) {
				noTagLabel.setForeground(ConnectionInfoPanel.UNKNOWN_TYPE_COLOR);
			}
			add(noTagLabel);
		}

		for (String s : tags) {
			addTag(s, -1);
		}

		if (!editable) {
			return;
		}
		// EDITABLE ONLY

		// Empty border to as margin for elements
		setBorder(new EmptyBorder(0, 0, 2, 2));
		setBackground(Color.WHITE);

		// Allow insertion of new tags
		JTextField addField = new JTextField(5);
		addField.setFont(OPEN_SANS_13);
		addField.setBorder(new EmptyBorder(1, 1, 1, 1));
		addField.addActionListener(e -> SwingTools.invokeLater(() -> createTag(setTags, addField)));
		addField.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				createTag(setTags, addField);
			}
		});
		add(addField);
		setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
		// Show tooltip and focus input field on click
		setToolTipText(ConnectionI18N.getConnectionGUILabel("add_tag_hint"));
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				addField.requestFocus();
			}
		});

		// Check for external modifications
		tags.addListener((ListChangeListener<String>) (l -> {
			if (!l.getList().equals(new ArrayList<>(tagToValue.values()))) {
				SwingTools.invokeLater(() -> {
					tagToValue.keySet().forEach(this::remove);
					for (String s : l.getList()) {
						addTag(s, -1);
					}
					revalidate();
					repaint();
				});
			}
		}));
	}

	/**
	 * Creates a new tag.
	 */
	private void createTag(Consumer<List<String>> setTags, JTextField addField) {
		String tag = addField.getText().trim();
		if (tag.isEmpty()) {
			return;
		}
		addField.setText("");
		addTag(tag, tagToValue.size());
		setTags.accept(new ArrayList<>(tagToValue.values()));
		revalidate();
		repaint();
		// Scroll to the end of the panel
		scrollRectToVisible(new Rectangle(getX(), Integer.MAX_VALUE / 2, 1, 1));
	}

	/**
	 * Adds a tag UI element for the given String
	 *
	 * @param tag
	 * 		the tag to create
	 * @param pos
	 * 		the position at which to insert the tag, or -1 to append the component to the end
	 */
	private void addTag(String tag, int pos) {
		JLabel tagLabel = new JLabel(tag);
		tagLabel.setFont(OPEN_SANS_13);
		tagLabel.setForeground(isEnabled ? TAG_FOREGROUND : TAG_FOREGROUND_DISABLED);
		// Add border for more space
		Border border = BorderFactory.createEmptyBorder(2, editable ? 3 : 10, 3, 11);

		JPanel roundBackground = new JPanel(new BorderLayout()) {
			@Override
			public void paintComponent(Graphics g) {
				super.paintComponent(g);
				Graphics2D g2 = (Graphics2D) g;
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setPaint(isEnabled ? TAG_BACKGROUND : TAG_BACKGROUND_DISABLED);
				// Prevent it from being cut off
				g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 6, 6));
			}
		};

		tagLabel.setBorder(border);
		roundBackground.add(tagLabel, BorderLayout.CENTER);
		JButton removeTag = new JButton(CLOSE_SYMBOL);

		if (editable) {
			roundBackground.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			roundBackground.setBackground(getBackground());
			removeTag.setFont(CLOSE_FONT.deriveFont(1f * removeTag.getFont().getSize()));
			removeTag.setContentAreaFilled(false);
			removeTag.setBorderPainted(false);
			removeTag.setBorder(BorderFactory.createLineBorder(getBackground()));
			roundBackground.add(removeTag, BorderLayout.WEST);
			tagToValue.put(roundBackground, tag);
			removeTag.addActionListener(e -> SwingTools.invokeLater(() -> {
				tagToValue.remove(roundBackground);
				remove(roundBackground);
				setTags.accept(new ArrayList<>(tagToValue.values()));
				revalidate();
				repaint();
			}));
			add(roundBackground, pos);
		} else {
			JPanel spacer = new JPanel(new BorderLayout());
			spacer.setBackground(getBackground());
			spacer.add(roundBackground, BorderLayout.CENTER);
			// Don't use FlowLayout gaps in view mode since they are also added to the top and left side
			spacer.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 5));
			add(spacer, pos);
		}

	}
}
