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
package com.rapidminer.template.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.dnd.TransferableOperator;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceAction.IconType;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.ButtonDecotrator;
import com.rapidminer.gui.tools.components.FancyButton;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.operator.nio.AbstractDataImportWizard;
import com.rapidminer.operator.nio.CSVImportWizard;
import com.rapidminer.operator.nio.ExcelImportWizard;
import com.rapidminer.operator.nio.model.DataResultSet;
import com.rapidminer.operator.nio.model.WizardState;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.template.RoleRequirement;
import com.rapidminer.template.Template;
import com.rapidminer.template.TemplateController;
import com.rapidminer.template.TemplateState;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;


/**
 * Area to display a data set or a friendly area where the user can drop a data set.
 *
 * @author Simon Fischer
 */
public class InputDataArea extends JPanel {

	private static final long serialVersionUID = 1L;

	private final TemplateController controller;

	private CardLayout cards = new CardLayout();

	private ExampleSetTable dataTable = new ExampleSetTable(false);

	/** True iff the data loading screen is shown. */
	private boolean isLoading = false;

	private StopTemplateAction stopAction;

	public InputDataArea(final TemplateController controller) {
		super();
		this.controller = controller;
		this.stopAction = new StopTemplateAction(controller);
		this.controller.getModel().addObserver(new Observer() {

			@Override
			public void update(Observable o, Object arg) {
				if (TemplateState.OBSERVER_EVENT_INPUT.equals(arg) || TemplateState.OBSERVER_EVENT_ROLES.equals(arg)) {
					ExampleSet inputData = InputDataArea.this.controller.getInputWithAssignedRoles();
					if (inputData != null) {
						dataTable.setExampleSet(inputData);
						if (!isLoading) {
							cards.show(InputDataArea.this, "data");
						}
					} else {
						dataTable.setExampleSet(null);
						if (!isLoading) {
							showEmpty();
						}
					}
				}
			}
		});

		dataTable.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 1) {
					int col = dataTable.getSelectedColumn();
					if (col != -1) {
						ExampleSetTableModel model = (ExampleSetTableModel) dataTable.getModel();
						AttributeRole role = model.getAttributesByIndex().get(col);
						TemplateState state = controller.getModel();
						Template template = state.getTemplate();
						if (template.getRoleRequirements().size() == 1) {
							RoleRequirement roleRequirement = template.getRoleRequirement(0);
							if (TemplateController.isCompatible(role.getAttribute(), roleRequirement)) {
								controller.assignRole(roleRequirement, role.getAttribute().getName(), null);
							}
						}
					}
				}
			}
		});

		setBackground(Color.WHITE);
		setLayout(cards);

		// BEGIN Drop area: "Drop your data here" plus two buttons below
		JPanel dropAreaPanel = new JPanel();
		dropAreaPanel.setLayout(new BoxLayout(dropAreaPanel, BoxLayout.Y_AXIS));

		dropAreaPanel.add(Box.createVerticalGlue());

		JLabel dropDataLabel = new JLabel(
				"<html><div style=\"font-weight:bold;text-align:center;margin:auto auto auto auto;\"><div style=\"font-size:18px;\">Drop your data</div><div style=\"font-size:52px;\">HERE</div><div>Use Excel, CSV, or repository</div></div></html>");
		dropDataLabel.setHorizontalAlignment(SwingConstants.CENTER);
		dropDataLabel.setForeground(Color.LIGHT_GRAY);
		dropDataLabel.setFont(dropDataLabel.getFont().deriveFont(Font.BOLD, dropDataLabel.getFont().getSize()));
		dropDataLabel.setAlignmentX(CENTER_ALIGNMENT);
		dropAreaPanel.add(dropDataLabel);

		dropAreaPanel.add(Box.createRigidArea(new Dimension(0, 15)));

		JLabel orLabel = new JLabel(
				"<html><div style=\"font-weight:bold;text-align:center;margin:auto auto auto auto;font-size:15px\">OR</div></html>");
		orLabel.setHorizontalAlignment(SwingConstants.CENTER);
		orLabel.setForeground(Color.LIGHT_GRAY);
		orLabel.setAlignmentX(CENTER_ALIGNMENT);
		orLabel.setAlignmentX(CENTER_ALIGNMENT);
		orLabel.setFont(orLabel.getFont().deriveFont(Font.BOLD, orLabel.getFont().getSize()));
		dropAreaPanel.add(orLabel);

		dropAreaPanel.add(Box.createRigidArea(new Dimension(0, 15)));

		JPanel dataImportPanel = new JPanel();
		dataImportPanel.setLayout(new BoxLayout(dataImportPanel, BoxLayout.X_AXIS));
		dataImportPanel.add(Box.createHorizontalGlue());
		FancyButton demoData = new FancyButton(new ResourceAction(48, "template.use_demo_data", IconType.FLAT) {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				ExampleSet data = controller.getModel().getTemplate().getDemoData();
				if (controller.getModel().getTemplate() != null) {
					ActionStatisticsCollector.getInstance().log(ActionStatisticsCollector.TYPE_TEMPLATE,
							controller.getModel().getTemplate().getName(), "demo_data");
				}
				controller.setInput(data);
			}
		});
		demoData.setDrawArrow(false);
		demoData.setFont(demoData.getFont().deriveFont(Font.BOLD, 28f));
		decorateWithInsets(demoData);
		dataImportPanel.add(demoData);
		dataImportPanel.add(Box.createRigidArea(new Dimension(20, 0)));
		FancyButton importButton = new FancyButton(new ResourceAction(48, "template.import_file", IconType.FLAT) {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				final File file = SwingTools.chooseFile(RapidMinerGUI.getMainFrame(), "import_data", null, true, false,
						new String[] { "xls", "xlsx", "csv", "tsv", "xml" }, new String[] { "Excel spreadsheet",
					"Excel  2007 spreadsheet", "Comma separated values", "Tab separated values", "XML files" },
					false);
				if (file == null) {
					return; // no selection->abort
				}
				importFile(file);
			}
		});

		importButton.setDrawArrow(false);
		importButton.setFont(importButton.getFont().deriveFont(Font.BOLD, 28f));
		decorateWithInsets(importButton);
		dataImportPanel.add(importButton);

		dataImportPanel.add(Box.createHorizontalGlue());
		dropAreaPanel.add(dataImportPanel);

		dropAreaPanel.add(Box.createVerticalGlue());
		dropAreaPanel.setBorder(new RoundTitledBorder(2, I18N.getMessage(I18N.getGUIBundle(),
				"gui.label.template.load_data.label")));
		add(dropAreaPanel, "empty");
		// END Drop area

		// BEGIN Loading panel with progress indicator
		JPanel loadingDataPanel = createProgressPanel(2,
				I18N.getMessage(I18N.getGUIBundle(), "gui.label.template.loading_data.label"), null);
		add(loadingDataPanel, "loading");
		// END Loading panel

		// BEGIN Data panel: Header with title and close icon, data table, and role selector / run
		// button at the south

		// BEGIN Header: Title and clear
		JPanel headerPanel = new JPanel();
		headerPanel.setBackground(Color.WHITE);
		headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.X_AXIS));
		JLabel dataLabel = new ResourceLabel("template.your_data");
		dataLabel.setFont(dataLabel.getFont().deriveFont(18f).deriveFont(Font.BOLD));
		headerPanel.add(dataLabel);

		headerPanel.add(Box.createRigidArea(new Dimension(30, 0)));
		headerPanel.add(Box.createHorizontalGlue());

		JButton clearData = new JButton(new ResourceAction(true, "template.clear_data", IconType.FLAT) {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				controller.setInput(null);
			}
		});
		ButtonDecotrator.decorateAsLinkButton(clearData);

		clearData.setAlignmentX(RIGHT_ALIGNMENT);
		JPanel rightPanel = new JPanel(new BorderLayout());
		rightPanel.setBackground(Color.WHITE);
		rightPanel.add(clearData, BorderLayout.LINE_END);
		headerPanel.add(rightPanel);
		// END Header: Title and clear

		// BEGIN actual data
		dataTable.getTableHeader().setReorderingAllowed(false);
		JScrollPane dataScrollPane = new JScrollPane(dataTable);
		dataScrollPane.setBackground(Color.WHITE);
		dataScrollPane.setForeground(Color.WHITE);
		dataScrollPane.getViewport().setBackground(Color.WHITE);
		dataScrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
		// END actual data

		// BEGIN downsampling: label plus checkbox
		final JPanel downsamplingPanel = new JPanel(new GridLayout(2, 1, 0, 0));
		downsamplingPanel.setBackground(Color.WHITE);
		downsamplingPanel.add(new ResourceLabel("template.downsampling_recommended"));
		JCheckBox downsamplingCheckbox = new JCheckBox(new ResourceAction("template.downsampling_checkbox") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				controller.getModel().setDownsamplingEnabled(!((JCheckBox) e.getSource()).isSelected());
			}
		});
		downsamplingCheckbox.setSelected(!controller.getModel().isDownsamplingEnabled());
		downsamplingCheckbox.setBackground(Color.WHITE);
		controller.getModel().addObserver(new Observer() {

			@Override
			public void update(Observable o, Object arg) {
				if (TemplateState.OBSERVER_EVENT_INPUT.equals(arg)) {
					downsamplingPanel.setVisible(controller.getModel().getInputData() != null
							&& controller.getModel().getInputData().size() > TemplateController.MAX_RECOMMENDED_DATA_SIZE);
				}
			}
		});
		downsamplingPanel.add(downsamplingCheckbox);
		// END downsampling

		// BEGIN Role selection and template execution
		JPanel southPanel = new JPanel(new GridLayout(1, 2, 30, 30));

		southPanel.setBackground(Color.WHITE);
		RoleRequirementSelector roleSelector = new RoleRequirementSelector(controller);
		southPanel.add(roleSelector);

		final CardLayout resultCards = new CardLayout();
		final JPanel runTemplateCardsPanel = new JPanel(resultCards);
		JPanel runTemplatePanel = new JPanel(new GridBagLayout());
		runTemplatePanel.setBackground(Color.WHITE);
		runTemplatePanel.setBorder(BorderFactory.createCompoundBorder(
				new RoundTitledBorder(4, I18N.getMessage(I18N.getGUIBundle(), "gui.label.template.run_template.label")),
				BorderFactory.createEmptyBorder(20, 20, 20, 20)));

		GridBagConstraints rt = new GridBagConstraints();
		rt.anchor = GridBagConstraints.FIRST_LINE_START;
		rt.weightx = 1;
		rt.weighty = 1;
		rt.insets = new Insets(20, 20, 0, 20);
		rt.gridwidth = GridBagConstraints.REMAINDER;

		final FancyButton runButton = new FancyButton(new ResourceAction(64, "template.run_template", IconType.FLAT) {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				stopAction.setEnabled(true);
				resultCards.show(runTemplateCardsPanel, "progress");
				new ProgressThread("template.running", false) {

					@Override
					protected void executionCancelled() {
						controller.stop();
					};

					@Override
					public void run() {
						try {
							InputDataArea.this.controller.run();
						} catch (Exception e1) {
							LogService.getRoot().log(Level.WARNING,
									"com.rapidminer.template.gui.DropDataArea.process_error", e1);
							if (e1 instanceof ProcessStoppedException) {
								if (controller.getModel().getTemplate() != null) {
									ActionStatisticsCollector.getInstance().log(ActionStatisticsCollector.TYPE_TEMPLATE,
											controller.getModel().getTemplate().getName(), "run_stop");
								}
							} else {
								if (controller.getModel().getTemplate() != null) {
									ActionStatisticsCollector.getInstance().log(ActionStatisticsCollector.TYPE_TEMPLATE,
											controller.getModel().getTemplate().getName(), "run_error");
								}
								SwingTools.showVerySimpleErrorMessage("template.process_error");
							}
						} finally {
							resultCards.show(runTemplateCardsPanel, "resultsNavigation");
						}
					}
				}.start();
			}
		});
		this.controller.getModel().addObserver(new Observer() {

			@Override
			public void update(Observable o, Object arg) {
				if (TemplateState.OBSERVER_EVENT_ROLES.equals(arg) || TemplateState.OBSERVER_EVENT_INPUT.equals(arg)) {
					runButton.setEnabled(controller.isRoleAssignmentComplete());
				}
			}
		});
		runButton.setEnabled(controller.isRoleAssignmentComplete());
		runButton.setDrawArrow(false);
		runButton.setFont(runButton.getFont().deriveFont(Font.BOLD, 28f));
		decorateWithInsets(runButton);

		rt.weighty = 2;
		rt.insets = new Insets(0, 0, 0, 0); // 40, 20, 50, 20);
		rt.anchor = GridBagConstraints.CENTER;
		rt.gridwidth = GridBagConstraints.RELATIVE;
		runTemplatePanel.add(runButton, rt);
		runTemplateCardsPanel.add(runTemplatePanel, "resultsNavigation");

		FancyButton stopButton = new FancyButton(stopAction);
		stopButton.setVerticalTextPosition(SwingConstants.BOTTOM);
		stopButton.setHorizontalTextPosition(SwingConstants.CENTER);
		stopButton.setDrawArrow(false);
		stopButton.setText(null);
		stopButton.setBorder(BorderFactory.createCompoundBorder(stopButton.getBorder(),
				BorderFactory.createEmptyBorder(0, 18, 0, 8)));
		JPanel progressIndicatorPanel = createProgressPanel(5,
				I18N.getMessage(I18N.getGUIBundle(), "gui.label.template.running_template.label"), stopButton);
		runTemplateCardsPanel.add(progressIndicatorPanel, "progress");
		resultCards.show(runTemplateCardsPanel, "resultsNavigation");

		southPanel.add(runTemplateCardsPanel);
		// END Role selection and template execution

		// Now compose entire data tab
		JPanel dataTab = new JPanel(new GridBagLayout());
		dataTab.setBackground(Color.WHITE);
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.gridheight = 1;
		c.weightx = 1;
		c.weighty = 0;
		c.insets = new Insets(0, 0, 12, 0);
		dataTab.add(headerPanel, c);

		c.weighty = 1;
		c.insets = new Insets(0, 0, 0, 0);
		dataTab.add(dataScrollPane, c);

		c.weighty = 0;
		c.insets = new Insets(0, 0, 12, 0);
		dataTab.add(downsamplingPanel, c);

		c.weighty = 0.4;
		c.fill = GridBagConstraints.BOTH;
		dataTab.add(southPanel, c);

		add(dataTab, "data");
		cards.show(this, "empty");

		setTransferHandler(new TransferHandler() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean canImport(TransferSupport support) {
				return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
						|| support.isDataFlavorSupported(TransferableOperator.LOCAL_TRANSFERRED_REPOSITORY_LOCATION_FLAVOR);
			}

			@Override
			public boolean importData(JComponent comp, final Transferable t) {
				try {
					if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
						@SuppressWarnings("unchecked")
						final List<File> files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
						if (!files.isEmpty()) {
							File file = files.get(0);
							importFile(file);
							return true;
						}
						return false;
					} else if (t.isDataFlavorSupported(TransferableOperator.LOCAL_TRANSFERRED_REPOSITORY_LOCATION_FLAVOR)) {
						startLoadingProgress();
						new ProgressThread("importing_data", false) {

							@Override
							public void run() {
								try {
									RepositoryLocation loc = null;
									try {
										loc = (RepositoryLocation) t
												.getTransferData(TransferableOperator.LOCAL_TRANSFERRED_REPOSITORY_LOCATION_FLAVOR);
									} catch (UnsupportedFlavorException e1) {
										// cannot happen, we checked that
										throw new RuntimeException("Unsupported flavour: " + e1, e1);
									} catch (IOException e1) {
										SwingTools.showSimpleErrorMessage("template.repository_read_failed", e1, loc,
												e1.getMessage());
									}
									try {
										if (loc != null) {
											Entry entry = loc.locateEntry();
											if (entry instanceof IOObjectEntry) {
												IOObjectEntry iooe = (IOObjectEntry) entry;
												IOObject input = iooe.retrieveData(null);
												if (input instanceof ExampleSet) {
													InputDataArea.this.controller.setInput((ExampleSet) input);
												}
											}
										}
									} catch (RepositoryException e) {
										LogService.getRoot().log(Level.WARNING,
												"com.rapidminer.template.gui.DropDataArea.import_failed", e);
									}
								} finally {
									stopLoadingProgress();
								}
							}
						}.start();

						return true;
					} else {
						return super.importData(comp, t);
					}
				} catch (UnsupportedFlavorException | IOException e) {
					LogService.getRoot().log(Level.WARNING, "com.rapidminer.template.gui.DropDataArea.import_failed", e);
					return false;
				}
			}

		});
	}

	private void startLoadingProgress() {
		isLoading = true;
		cards.show(InputDataArea.this, "loading");
	}

	private void stopLoadingProgress() {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				if (controller.getModel().getInputData() == null) {
					cards.show(InputDataArea.this, "empty");
				} else {
					cards.show(InputDataArea.this, "data");
				}
				isLoading = false;
			}
		});
	}

	private JPanel createProgressPanel(int step, String message, JComponent additionalComponent) {
		JPanel progressIndicatorPanel = new JPanel(new GridBagLayout());
		progressIndicatorPanel.setBackground(Color.WHITE);
		progressIndicatorPanel.setBorder(BorderFactory.createCompoundBorder(new RoundTitledBorder(step, message),
				BorderFactory.createEmptyBorder(20, 20, 20, 20)));
		GridBagConstraints progressConstraints = new GridBagConstraints();
		progressConstraints.gridwidth = GridBagConstraints.REMAINDER;
		progressConstraints.fill = GridBagConstraints.BOTH;
		progressConstraints.insets = new Insets(30, 30, 30, 30);
		progressConstraints.weightx = 1;
		progressConstraints.weighty = 1;
		progressIndicatorPanel.add(new ProgressIndicator(), progressConstraints);

		if (additionalComponent != null) {
			progressConstraints.fill = GridBagConstraints.NONE;
			progressConstraints.anchor = GridBagConstraints.CENTER;
			progressConstraints.weightx = 0;
			progressConstraints.weighty = 0;
			progressConstraints.insets = new Insets(0, 0, 0, 0);
			progressIndicatorPanel.add(additionalComponent, progressConstraints);
		}
		return progressIndicatorPanel;
	}

	public void showEmpty() {
		cards.show(this, "empty");
	}

	private void importFile(final File file) {
		if (!file.exists()) {
			SwingTools.showVerySimpleErrorMessage("template.file_not_existing");
			return;
		}
		startLoadingProgress();
		new ProgressThread("template.importing_data") {

			@Override
			public void run() {
				final ExampleSet input;
				try {
					input = InputDataArea.this.controller.importFile(file);
				} finally {
					stopLoadingProgress();
				}
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						if (input != null) {
							InputDataArea.this.controller.setInput(input);
							return;
						} else {
							LogService.getRoot().log(Level.INFO, "Failed to import: " + file);
							if (controller.getModel().getTemplate() != null) {
								ActionStatisticsCollector.getInstance().log(ActionStatisticsCollector.TYPE_TEMPLATE,
										controller.getModel().getTemplate().getName(), "import_wizard");
							}
							AbstractDataImportWizard wizard = null;
							try {
								if (file.getName().endsWith(".csv") || file.getName().endsWith("*.tsv")) {
									wizard = new CSVImportWizard(file, null);
								} else if (file.getName().endsWith("*.xls") || file.getName().endsWith("*.xlsx")) {
									wizard = new ExcelImportWizard(file, null);
								}
							} catch (OperatorException e1) {
								// should not happen if operator == null
								throw new RuntimeException("Failed to create wizard.", e1);
							}
							if (wizard != null) {
								wizard.setModal(true); // need to wait for wizard to complete
								wizard.setVisible(true);
								final WizardState state = wizard.getState();
								startLoadingProgress();
								new ProgressThread("template.importing_data") {

									@Override
									public void run() {
										try (DataResultSet resultSet = state.getDataResultSetFactory().makeDataResultSet(
												null)) {
											state.getTranslator().clearErrors();
											final ExampleSet exampleSet = state.readNow(resultSet, false,
													getProgressListener());
											if (controller.getModel().getTemplate() != null) {
												ActionStatisticsCollector.getInstance().log(
														ActionStatisticsCollector.TYPE_TEMPLATE,
														controller.getModel().getTemplate().getName(),
														"import_wizard_success");
											}
											SwingUtilities.invokeLater(new Runnable() {

												@Override
												public void run() {
													InputDataArea.this.controller.setInput(exampleSet);
												}
											});
										} catch (OperatorException e) {
											if (controller.getModel().getTemplate() != null) {
												ActionStatisticsCollector.getInstance().log(
														ActionStatisticsCollector.TYPE_TEMPLATE,
														controller.getModel().getTemplate().getName(), "import_error");
											}
											SwingTools.showSimpleErrorMessage("importwizard.unknown_error", e);
										} finally {
											stopLoadingProgress();
										}
									}
								}.start();
							} else {
								SwingTools.showVerySimpleErrorMessage("template.unknown_file_format");
							}
						}
					}
				});
			}
		}.start();
	}

	private void decorateWithInsets(JComponent comp) {
		comp.setBorder(BorderFactory.createCompoundBorder(comp.getBorder(), BorderFactory.createEmptyBorder(8, 10, 8, 0)));
	}
}
