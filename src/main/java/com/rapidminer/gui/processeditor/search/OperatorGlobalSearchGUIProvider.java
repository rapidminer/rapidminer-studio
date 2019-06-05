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
package com.rapidminer.gui.processeditor.search;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collections;
import java.util.logging.Level;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import org.apache.lucene.document.Document;

import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.PerspectiveModel;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.dnd.TransferableOperator;
import com.rapidminer.gui.flow.processrendering.model.ProcessRendererModel;
import com.rapidminer.gui.search.GlobalSearchGUIUtilities;
import com.rapidminer.gui.search.GlobalSearchableGUIProvider;
import com.rapidminer.gui.tools.MultiSwingWorker;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.search.GlobalSearchUtilities;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.documentation.GroupDocumentation;
import com.rapidminer.tools.usagestats.DefaultUsageLoggable;


/**
 * Provides UI elements to display operators in the Global Search results for the {@link OperatorGlobalSearch}.
 *
 * @author Marco Boeck
 * @since 8.1
 */
public class OperatorGlobalSearchGUIProvider implements GlobalSearchableGUIProvider {

	/**
	 * Drag & Drop support for operators.
	 */
	private static final class OperatorDragGesture extends DefaultUsageLoggable implements DragGestureListener {

		private final Operator operator;

		private OperatorDragGesture(Operator operator) {
			this.operator = operator;
		}


		@Override
		public void dragGestureRecognized(DragGestureEvent event) {
			// only allow dragging with left mouse button
			if (event.getTriggerEvent().getModifiers() != InputEvent.BUTTON1_MASK) {
				return;
			}

			// change cursor to drag move
			Cursor cursor = null;
			if (event.getDragAction() == DnDConstants.ACTION_COPY) {
				cursor = DragSource.DefaultCopyDrop;
			}

			// set the recommended operator as the Transferable
			TransferableOperator transferable = new TransferableOperator(new Operator[]{operator});
			if (usageLogger != null) {
				transferable.setUsageStatsLogger(usageLogger);
			} else if (usageObject != null) {
				transferable.setUsageObject(usageObject);
			}
			event.startDrag(cursor, transferable);
		}

	}

	private static final Border ICON_EMPTY_BORDER = BorderFactory.createEmptyBorder(0, 5, 0, 15);

	private static final Color LOCATION_COLOR = new Color(128, 128, 128);

	private static final Color BLACKLISTED_OPERATOR_NAME_COLOR = new Color(150, 150, 150);
	private static final Icon BLACKLISTED_ICON = SwingTools.createIcon("16/" + I18N.getGUILabel("operator.blacklisted.icon"), true);
	private static final Color BLACKLISTED_LOCATION_COLOR = LOCATION_COLOR.brighter();

	private static final float FONT_SIZE_LOCATION = 9f;
	private static final float FONT_SIZE_NAME = 14f;


	@Override
	public JComponent getGUIListComponentForDocument(final Document document, final String[] bestFragments) {
		JPanel mainListPanel = new JPanel();
		mainListPanel.setLayout(new BorderLayout());
		mainListPanel.setOpaque(false);
		JPanel descriptionPanel = new JPanel();
		descriptionPanel.setOpaque(false);
		descriptionPanel.setLayout(new BorderLayout());

		JLabel nameListLabel = new JLabel();
		nameListLabel.setFont(nameListLabel.getFont().deriveFont(Font.BOLD).deriveFont(FONT_SIZE_NAME));
		JLabel locationListLabel = new JLabel();
		locationListLabel.setFont(locationListLabel.getFont().deriveFont(FONT_SIZE_LOCATION));
		JLabel iconListLabel = new JLabel();
		iconListLabel.setBorder(ICON_EMPTY_BORDER);

		descriptionPanel.add(nameListLabel, BorderLayout.CENTER);
		descriptionPanel.add(locationListLabel, BorderLayout.SOUTH);

		mainListPanel.add(iconListLabel, BorderLayout.WEST);
		mainListPanel.add(descriptionPanel, BorderLayout.CENTER);

		OperatorDescription opDesc = OperatorService.getOperatorDescription(document.get(GlobalSearchUtilities.FIELD_UNIQUE_ID));
		String name = opDesc.getName();
		mainListPanel.setToolTipText(name);
		name = GlobalSearchGUIUtilities.INSTANCE.createHTMLHighlightFromString(name, bestFragments);

		// add the "drag here" label to the process panel
		MouseListener hoverAdapter = new MouseAdapter() {

			@Override
			public void mouseEntered(MouseEvent e) {
				handleHoverOverOperator(true);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				handleHoverOverOperator(false);
			}
		};
		mainListPanel.addMouseListener(hoverAdapter);

		// remove hover listener when ancestor is being removed
		// reason is that it takes enough time after inserting an operator for the original hover listener to fire again
		mainListPanel.addAncestorListener(new AncestorListener() {
			@Override
			public void ancestorAdded(AncestorEvent event) {
				// not needed
			}

			@Override
			public void ancestorRemoved(AncestorEvent event) {
				mainListPanel.removeMouseListener(hoverAdapter);
			}

			@Override
			public void ancestorMoved(AncestorEvent event) {
				// not needed
			}
		});

		nameListLabel.setText(name);
		iconListLabel.setIcon(opDesc.getSmallIcon());
		locationListLabel.setText(createFullGroupName(opDesc.getGroup()));
		locationListLabel.setForeground(LOCATION_COLOR);

		if (OperatorService.isOperatorBlacklisted(opDesc.getKey())) {
			nameListLabel.setText(opDesc.getName());
			nameListLabel.setForeground(BLACKLISTED_OPERATOR_NAME_COLOR);
			iconListLabel.setIcon(BLACKLISTED_ICON);
			locationListLabel.setForeground(BLACKLISTED_LOCATION_COLOR);
			mainListPanel.setToolTipText(I18N.getGUILabel("operator.blacklisted.tip"));
		}
		return mainListPanel;
	}

	@Override
	public String getI18nNameForSearchable() {
		return I18N.getMessage(I18N.getGUIBundle(), "gui.component.global_search.operators.category.title");
	}

	@Override
	public void searchResultTriggered(final Document document, final Veto veto) {
		// try to insert operator into process
		String operatorKey = document.get(GlobalSearchUtilities.FIELD_UNIQUE_ID);
		if (operatorKey == null) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.processeditor.global_search.OperatorSearchManager.error.no_key");
			return;
		}

		try {
			Operator operator = OperatorService.getOperatorDescription(operatorKey).createOperatorInstance();
			MainFrame mainFrame = RapidMinerGUI.getMainFrame();
			mainFrame.getActions().insert(Collections.singletonList(operator));
			mainFrame.getPerspectiveController().showPerspective(PerspectiveModel.DESIGN);

			// make sure "drop here" message vanishes after insert
			handleHoverOverOperator(false);

			// move focus over to the freshly inserted operator, so you can edit it with your keyboard immediately
			RapidMinerGUI.getMainFrame().getProcessPanel().getProcessRenderer().requestFocusInWindow();
		} catch (OperatorCreationException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.processeditor.global_search.OperatorSearchManager.error.operator_creation_error", e.getMessage());
		}
	}

	@Override
	public void searchResultBrowsed(final Document document) {
		String operatorKey = document.get(GlobalSearchUtilities.FIELD_UNIQUE_ID);
		if (operatorKey == null) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.processeditor.global_search.OperatorSearchManager.error.no_key");
			return;
		}

		new MultiSwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() {
				try {
					Operator operator = OperatorService.getOperatorDescription(operatorKey).createOperatorInstance();
					RapidMinerGUI.getMainFrame().getOperatorDocViewer().setDisplayedOperator(operator);
				} catch (OperatorCreationException e) {
					LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.processeditor.global_search.OperatorSearchManager.error.operator_browse_error", e.getMessage());
				}
				return null;
			}
		}.start();
	}

	@Override
	public boolean isDragAndDropSupported(final Document document) {
		return true;
	}

	@Override
	public DragGestureListener getDragAndDropSupport(final Document document) {
		String operatorKey = document.get(GlobalSearchUtilities.FIELD_UNIQUE_ID);
		if (operatorKey == null) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.processeditor.global_search.OperatorSearchManager.error.no_key");
			return null;
		}

		try {
			return new OperatorDragGesture(OperatorService.getOperatorDescription(operatorKey).createOperatorInstance());
		} catch (OperatorCreationException e) {
			return null;
		}
	}

	/**
	 * Handle the hover-over-operator events.
	 *
	 * @param hovered
	 * 		{@code true} if hovering over an operator; {@code false} otherwise
	 */
	private void handleHoverOverOperator(final boolean hovered) {
		ProcessRendererModel modelRenderer = RapidMinerGUI.getMainFrame().getProcessPanel().getProcessRenderer()
				.getModel();

		modelRenderer.setOperatorSourceHovered(hovered);
		modelRenderer.fireMiscChanged();
	}

	/**
	 * Creates the full group path for an operator in human-readable form.
	 *
	 * @param groupKey
	 * 		the full group path in the format goupname.groupothername.finalgroup
	 * @return the human-readable key, never {@code null}
	 */
	private static String createFullGroupName(final String groupKey) {
		String[] groups = groupKey.split("\\.");
		StringBuilder sb = new StringBuilder();
		for (String group : groups) {
			sb.append(GroupDocumentation.keyToUpperCase(group));
			sb.append('/');
		}
		if (sb.length() > 0) {
			sb.deleteCharAt(sb.length() - 1);
		} else {
			sb.append(GroupDocumentation.keyToUpperCase(groupKey));
		}
		return sb.toString();
	}
}
