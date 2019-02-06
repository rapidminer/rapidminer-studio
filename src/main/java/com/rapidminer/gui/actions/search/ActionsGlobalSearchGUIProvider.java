/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.actions.search;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.dnd.DragGestureListener;
import java.awt.event.ActionEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.logging.Level;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.apache.lucene.document.Document;
import org.apache.lucene.util.BytesRef;

import com.rapidminer.gui.actions.ToggleAction;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.search.GlobalSearchGUIUtilities;
import com.rapidminer.gui.search.GlobalSearchableGUIProvider;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.search.GlobalSearchUtilities;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.plugin.Plugin;


/**
 * Provides UI elements to display {@link com.rapidminer.gui.tools.ResourceAction}s in the Global Search results for the {@link ActionsGlobalSearch}.
 *
 * @author Marco Boeck
 * @since 8.1
 */
public class ActionsGlobalSearchGUIProvider implements GlobalSearchableGUIProvider {

	private static final ImageIcon SELECTED_ICON = SwingTools.createIcon("16/check.png");
	private static final Border ACTION_COMPOUND_BORDER = BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Colors.BUTTON_BORDER), BorderFactory.createEmptyBorder(0, 0, 0, 5));
	private static final Border RESULT_EMPTY_BORDER = BorderFactory.createEmptyBorder(1, 0, 3, 0);
	private static final Border ICON_EMPTY_BORDER = BorderFactory.createEmptyBorder(0, 5, 0, 15);

	private static final float FONT_SIZE = 13f;
	private static final Color DESCRIPTION_COLOR = new Color(62, 62, 62);
	private static final float FONT_SIZE_DESCRIPTION = 11f;


	@Override
	public JComponent getGUIListComponentForDocument(Document document, String[] bestFragments) {
		ResourceAction action = createActionFromDocument(document);

		JPanel mainListPanel = new JPanel();
		mainListPanel.setOpaque(false);
		mainListPanel.setLayout(new BorderLayout());

		JPanel descriptionPanel = new JPanel();
		descriptionPanel.setOpaque(false);
		descriptionPanel.setLayout(new BorderLayout());

		JLabel descriptionLabel = new JLabel();
		descriptionLabel.setForeground(DESCRIPTION_COLOR);
		descriptionLabel.setFont(descriptionLabel.getFont().deriveFont(FONT_SIZE_DESCRIPTION));

		JLabel resultLabel = new JLabel();
		resultLabel.setFont(resultLabel.getFont().deriveFont(FONT_SIZE));

		descriptionPanel.add(resultLabel, BorderLayout.CENTER);
		descriptionPanel.add(descriptionLabel, BorderLayout.SOUTH);

		mainListPanel.add(descriptionPanel, BorderLayout.CENTER);

		// should not happen (because it is not indexed if action cannot be serialized), but just in case
		if (action == null) {
			resultLabel.setText(document.get(GlobalSearchUtilities.FIELD_NAME));
			return mainListPanel;
		}

		ImageIcon icon = null;
		try {
			icon = ((ImageIcon) action.getValue(Action.SMALL_ICON));
		} catch (ClassCastException e) {
			// no icon then.
		}
		String name = document.get(GlobalSearchUtilities.FIELD_NAME);
		name = GlobalSearchGUIUtilities.INSTANCE.createHTMLHighlightFromString(name, bestFragments);

		resultLabel.setText(name);

		JLabel iconListLabel = new JLabel();
		iconListLabel.setBorder(ICON_EMPTY_BORDER);
		iconListLabel.setIcon(icon);
		mainListPanel.add(iconListLabel, BorderLayout.WEST);

		// for dockable actions, display the description of the panel below the name
		if (action instanceof DockableAction) {
			Object description = action.getValue(ResourceAction.SHORT_DESCRIPTION);
			if (description != null) {
				descriptionLabel.setText("<html><div style=\"width:175px\">" + description + "</div></html>");
			} else {
				resultLabel.setBorder(RESULT_EMPTY_BORDER);
			}
			Object tooltip = action.getValue(ResourceAction.LONG_DESCRIPTION);
			if (tooltip != null) {
				mainListPanel.setToolTipText(String.valueOf(tooltip));
			}
		} else {
			// because we have no 2nd line in this case, we need a bit of empty border to reach a certain height
			// so that a single result fills the dialog
			resultLabel.setBorder(RESULT_EMPTY_BORDER);
			mainListPanel.setToolTipText(document.get(ActionsGlobalSearchManager.FIELD_DESCRIPTION));
		}

		// action is enabled either if it is enabled anyway, or if it was only disabled due to focus loss
		resultLabel.setEnabled(action.isEnabled() || action.isDisabledDueToFocusLost());

		// ToggleActions need to display their toggle state
		if (action instanceof ToggleAction) {
			boolean selected = ((ToggleAction) action).isSelected();
			if (selected) {
				JLabel selectedLabel = new JLabel(SELECTED_ICON);
				selectedLabel.setBorder(ACTION_COMPOUND_BORDER);
				mainListPanel.add(selectedLabel, BorderLayout.WEST);
			}
		}

		return mainListPanel;
	}

	@Override
	public String getI18nNameForSearchable() {
		return I18N.getMessage(I18N.getGUIBundle(), "gui.component.global_search.action.category.title");
	}

	@Override
	public void searchResultTriggered(Document document, Veto veto) {
		// try to deserialize action and trigger it
		ResourceAction action = createActionFromDocument(document);

		// action is enabled either if it is enabled anyway, or if it was only disabled due to focus loss
		if (action != null && (action.isEnabled() || action.isDisabledDueToFocusLost())) {
			SwingTools.invokeLater(() -> {
				ActionEvent event = new ActionEvent(action, ActionEvent.ACTION_PERFORMED, "");
				action.actionPerformed(event);
			});

			// update ToggleAction in index so that new state will be retrieved next time
			if (action instanceof ToggleAction) {
				action.addToGlobalSearch();
			}
		} else {
			// action could not be triggered, do not close search results to indicate that
			veto.veto();
		}
	}

	@Override
	public void searchResultBrowsed(Document document) {
		// ignore, cannot do anything meaningful
	}

	@Override
	public boolean isDragAndDropSupported(Document document) {
		return false;
	}

	@Override
	public DragGestureListener getDragAndDropSupport(Document document) {
		return null;
	}

	/**
	 * Deserializes the {@link ResourceAction} from the document.
	 *
	 * @param document
	 * 		the search document where the action is stored in
	 * @return the action instance or {@code null} if something goes wrong
	 */
	private ResourceAction createActionFromDocument(Document document) {
		BytesRef binaryValue = document.getBinaryValue(ActionsGlobalSearchManager.FIELD_SERIALIZED_ACTION);

		// restore action with the plugin classloader, otherwise plugin actions would not be found
		try (ByteArrayInputStream byteInputStream = new ByteArrayInputStream(binaryValue.bytes)) {

				ObjectInputStream ois = new ObjectInputStream(byteInputStream) {

					@Override
					protected Class resolveClass(ObjectStreamClass desc) throws ClassNotFoundException {
						try {
							return super.resolveClass(desc);
						} catch (ClassNotFoundException | IOException e) {
							return Class.forName(desc.getName(), false, Plugin.getMajorClassLoader());
						}
					}
				};
		 	return (ResourceAction) ois.readObject();
		} catch (Exception e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.actions.search.ActionsGlobalSearchManager.error.deserialize_action", e);
			return null;
		}
	}

}
