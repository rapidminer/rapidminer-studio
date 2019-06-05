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
package com.rapidminer.gui.processeditor.results;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import com.rapidminer.core.license.ProductConstraintManager;
import com.rapidminer.gui.CleanupRequiringComponent;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.actions.CloseAllResultsAction;
import com.rapidminer.gui.actions.CloseAllResultsExceptCurrentResultAction;
import com.rapidminer.gui.actions.StoreInRepositoryAction;
import com.rapidminer.gui.renderer.RendererService;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.license.ConstraintNotRestrictedException;
import com.rapidminer.license.LicenseConstants;
import com.rapidminer.license.LicenseEvent;
import com.rapidminer.license.LicenseEvent.LicenseEventType;
import com.rapidminer.license.LicenseManagerListener;
import com.rapidminer.license.LicenseManagerRegistry;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.ResultObject;
import com.rapidminer.tools.Tools;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;
import com.vlsolutions.swing.docking.DockableActionCustomizer;


/**
 * Dockable containing a single result.
 *
 * @author Simon Fischer
 *
 */
public class ResultTab extends JPanel implements Dockable {

	private static final long serialVersionUID = 1L;

	public static final String DOCKKEY_PREFIX = "result_";

	/** the max length of dockkey names */
	private static final int MAX_DOCKNAME_LENGTH = 80;

	private Component label;
	private JPanel component;
	private final DockKey dockKey;
	private final String id;
	private ResultObject resultObject = null;
	private LicenseManagerListener licenseListener;

	public ResultTab(String id) {
		setLayout(new BorderLayout());
		this.id = id;
		this.dockKey = new DockKey(id, "Result " + id);
		this.dockKey.setDockGroup(MainFrame.DOCK_GROUP_RESULTS);
		this.dockKey.setName(id);
		this.dockKey.setFloatEnabled(true);
		this.dockKey.setIconDisplayed(true);
		// results can be closed with ctrl+w
		this.dockKey.putProperty(DockKey.PROPERTY_SHORTCUT_CLOSING_ENABLED, Boolean.TRUE);
		DockableActionCustomizer customizer = new DockableActionCustomizer() {

			@Override
			public void visitTabSelectorPopUp(JPopupMenu popUpMenu, Dockable dockable) {
				popUpMenu.add(new JMenuItem(new CloseAllResultsExceptCurrentResultAction(RapidMinerGUI.getMainFrame(), dockKey.getKey())));
				popUpMenu.add(new JMenuItem(new CloseAllResultsAction(RapidMinerGUI.getMainFrame())));
				popUpMenu.addSeparator();
				popUpMenu.add(new JMenuItem(new StoreInRepositoryAction(resultObject)));
			}
		};
		customizer.setTabSelectorPopUpCustomizer(true); // enable tabbed dock custom popup menu
		// entries
		this.dockKey.setActionCustomizer(customizer);
		label = makeStandbyLabel();
		add(label, BorderLayout.CENTER);

		addLicenseLimitListener();

	}

	/**
	 * Adds a license listener that updates all {@ResultLimitPanel}s which might be contained in
	 * subcomponents.
	 */
	private void addLicenseLimitListener() {
		licenseListener = new LicenseManagerListener() {

			@Override
			public <S, C> void handleLicenseEvent(LicenseEvent<S, C> event) {
				if (event.getType() == LicenseEventType.ACTIVE_LICENSE_CHANGED) {
					Integer licenseLimit;
					try {
						licenseLimit = LicenseManagerRegistry.INSTANCE.get().getConstraintValue(
								ProductConstraintManager.INSTANCE.getProduct(), LicenseConstants.DATA_ROW_CONSTRAINT);
					} catch (ConstraintNotRestrictedException e) {
						licenseLimit = null;
					}

					if (component != null) {
						searchAllChildren(component, licenseLimit);
					}
				}
			}

			/**
			 * Go recursively through all components in search of {@link ResultLimitPanel}s which
			 * need to update their text.
			 */
			private void searchAllChildren(Container component, Integer licenseLimit) {
				for (Component child : component.getComponents()) {
					if (child instanceof ResultLimitPanel) {
						SwingTools.invokeLater(() -> {
							((ResultLimitPanel) child).licenseUpdated(licenseLimit);
						});
					} else if (child instanceof Container) {
						searchAllChildren((Container) child, licenseLimit);
					}
				}
			}

		};
		LicenseManagerRegistry.INSTANCE.get().registerLicenseManagerListener(licenseListener);
	}

	/**
	 * Creates a component for this object and displays it. This method does not have to be called
	 * on the EDT. It executes a time consuming task and should be called from a
	 * {@link ProgressThread}.
	 */
	public void showResult(final ResultObject resultObject) {
		if (resultObject != null) {
			this.resultObject = resultObject;
		}
		SwingUtilities.invokeLater(() -> {
			if (label != null) {
				remove(label);
				label = null;
			}
			if (resultObject != null) {
				String name = resultObject.getName();
				if (resultObject.getSource() != null) {
					name += " (" + resultObject.getSource() + ")";
				}

				// without this, the name could be too long and cut off the close button (and
				// even exit the screen)
				name = SwingTools.getShortenedDisplayName(name, MAX_DOCKNAME_LENGTH);

				dockKey.setName(name);
				dockKey.setTooltip(Tools.toString(resultObject.getProcessingHistory(), " \u2192 "));
				label = makeStandbyLabel();
				add(label, BorderLayout.CENTER);
			} else {
				if (id.startsWith(DOCKKEY_PREFIX + "process_")) {
					String number = id.substring((DOCKKEY_PREFIX + "process_").length());
					label = new ResourceLabel("resulttab.cannot_be_restored_process_result", number);
					dockKey.setName("Result #" + number);
				} else {
					label = new ResourceLabel("resulttab.cannot_be_restored");
					dockKey.setName("Result " + id);
				}
				((JComponent) label).setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
				add(label, BorderLayout.CENTER);
			}
			// remove old component
			if (component != null) {
				remove(component);
			}
		});

		if (resultObject != null) {
			final JPanel newComponent = createComponent(resultObject, null);
			SwingUtilities.invokeLater(() -> {
				if (label != null) {
					remove(label);
					label = null;
				}
				component = newComponent;
				add(component, BorderLayout.CENTER);
				dockKey.setIcon((Icon) component.getClientProperty(ResultDisplayTools.CLIENT_PROPERTY_RAPIDMINER_RESULT_ICON));
			});
		}
	}

	private static JComponent makeStandbyLabel() {
		Box labelBox = new Box(BoxLayout.Y_AXIS);
		labelBox.add(Box.createVerticalGlue());
		Box horizontalBox = Box.createHorizontalBox();
		horizontalBox.add(Box.createHorizontalGlue());
		horizontalBox.add(new ResourceLabel("resulttab.creating_display"));
		horizontalBox.add(Box.createHorizontalGlue());
		labelBox.add(horizontalBox);
		labelBox.add(Box.createVerticalGlue());
		return labelBox;
	}

	/**
	 * Creates an appropriate name, appending a number to make names unique, and calls
	 * {@link ResultDisplayTools#createVisualizationComponent(IOObject, IOContainer, String)}.
	 */
	private JPanel createComponent(ResultObject resultObject, IOContainer resultContainer) {
		final String resultName = RendererService.getName(resultObject.getClass());
		String usedResultName = resultObject.getName();
		if (usedResultName == null) {
			usedResultName = resultName;
		}
		this.resultObject = resultObject;
		return ResultDisplayTools.createVisualizationComponent(resultObject, resultContainer, id + ": " + usedResultName);
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public DockKey getDockKey() {
		return dockKey;
	}

	/**
	 * Free up any resources held by this result tab. Also checks every child component for instances of {@link
	 * CleanupRequiringComponent} and calls their clean-up methods.
	 */
	public void freeResources() {
		if (component != null) {
			cleanUpRecursively(component);
			remove(component);
			component = null;
			resultObject = null;
		}
		LicenseManagerRegistry.INSTANCE.get().removeLicenseManagerListener(licenseListener);
	}

	/**
	 * Looks recursively for components implementing the  {@link CleanupRequiringComponent} interface and calls their
	 * cleanUp() method.
	 *
	 * @param component
	 * 		the component whose children should be searched
	 */
	private void cleanUpRecursively(Container component) {
		for (Component child : component.getComponents()) {
			if (child instanceof CleanupRequiringComponent) {
				((CleanupRequiringComponent) child).cleanUp();
			} else if (child instanceof Container) {
				cleanUpRecursively((Container) child);
			}
		}
	}

	/**
	 * @return the panel which displays the actual result
	 */
	public JPanel getResultViewComponent() {
		return component;
	}
}
