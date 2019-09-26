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
package com.rapidminer.tools.config.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.xml.ws.WebServiceException;

import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;
import org.jdesktop.swingx.painter.MattePainter;

import com.rapidminer.Process;
import com.rapidminer.connection.adapter.ConnectionAdapter;
import com.rapidminer.connection.adapter.ConnectionAdapterHandler;
import com.rapidminer.connection.gui.ConnectionCreationDialog;
import com.rapidminer.connection.gui.components.DeprecationWarning;
import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceActionAdapter;
import com.rapidminer.gui.tools.ScrollableJPopupMenu;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.UpdateQueue;
import com.rapidminer.gui.tools.components.TransparentGlassPanePanel;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.parameter.Parameters;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.internal.remote.RemoteRepository;
import com.rapidminer.repository.local.LocalRepository;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.config.AbstractConfigurable;
import com.rapidminer.tools.config.Configurable;
import com.rapidminer.tools.config.ConfigurationException;
import com.rapidminer.tools.config.ConfigurationManager;
import com.rapidminer.tools.config.actions.ActionResult;
import com.rapidminer.tools.config.actions.ActionResult.Result;
import com.rapidminer.tools.config.actions.ConfigurableAction;
import com.rapidminer.tools.config.gui.event.ConfigurableEvent;
import com.rapidminer.tools.config.gui.event.ConfigurableModelEventListener;
import com.rapidminer.tools.config.gui.model.ConfigurableModel;
import com.rapidminer.tools.config.gui.renderer.ConfigurableInfoLabelRenderer;
import com.rapidminer.tools.config.gui.renderer.ConfigurableInfoLabelRenderer.ConfigurableInfoLabelType;
import com.rapidminer.tools.config.gui.renderer.ConfigurableRenderer;


/**
 * Dialog which can be used to manage all {@link Configurable}s in RapidMiner Studio.
 *
 * @author Marco Boeck, Sabrina Kirstein
 *
 */
public class ConfigurableDialog extends ButtonDialog {

	private static final long serialVersionUID = 725095214927473103L;

	/** icon displayed in case of configurator success */
	private static final ImageIcon SUCCESS_ICON = SwingTools.createIcon("24/check.png");

	/** icon displayed in case of configurator action failure */
	private static final ImageIcon FAILURE_ICON = SwingTools.createIcon("24/error.png");

	/** icon displayed in case of running action */
	private static final ImageIcon WORKING_ICON = SwingTools.createIcon("24/hourglass.png");

	/** icon displayed in case of storing configurables */
	private static final ImageIcon WAITING_ICON = SwingTools.createIcon("48/rm_logo_loading.gif");

	/** size of the list of configurables */
	private static final Dimension CONFIG_LIST_SIZE = new Dimension(300, 500);

	/** the dimension for the spacer in the middle */
	private static final Dimension DIMENSION_SPACER_MIDDLE = new Dimension(10, 100);

	/** the dimension for the spacer on the right side of the params */
	private static final Dimension DIMENSION_SPACER_PARAMS = new Dimension(5, 100);

	/** the background color of the JLists with Configurables */
	private static final Color LIGHTER_GRAY = new Color(250, 250, 250);

	/** the background color of the name panel showing the header of a Configurable */
	private static final Color NAME_PANEL_GRAY = new Color(220, 220, 220);

	/** separator if the title has to be cut */
	public static final String SEPARATOR = "[...]";

	/** the model backing the info label list for connections */
	private DefaultListModel<String> localInfoLabelListModel;

	/** the model backing the configurable list for local connections */
	private DefaultListModel<Configurable> localConfigListModel;

	/** the model backing the info label list for server connections */
	private Map<String, DefaultListModel<String>> remoteInfoLabelListModels;

	/** the model backing the configurable list for server connections */
	private Map<String, DefaultListModel<Configurable>> remoteConfigListModels;

	/** the {@link JList} containing the local info labels */
	private JList<String> localInfoLabelList;

	/** the {@link JList} containing the local {@link Configurable}s */
	private JList<Configurable> localConfigList;

	/** the {@link JList} containing the remote info labels */
	private Map<String, JList<String>> remoteInfoLabelLists;

	/** the {@link JList} containing the remote {@link Configurable}s */
	private Map<String, JList<Configurable>> remoteConfigLists;

	/** the controller for the local connections in this view */
	private ConfigurableController localController;

	/** the controller for the remote connections in this view */
	private Map<String, ConfigurableController> remoteControllers;

	/** the listener on the localModel */
	private ConfigurableModelEventListener localListener;

	/** the listeners on the remote models */
	private Map<String, ConfigurableModelEventListener> remoteListener;

	/** the local configuration pane */
	private JXTaskPane localTaskPane;

	/** the remote configurations pane */
	private Map<String, JXTaskPane> remoteTaskPanes;

	/** the panel which holds the parameter GUI for the selected {@link Configurable} */
	private JPanel parameterPanel;

	/** the text saying that n users have access to the configurable */
	private String userAccessText = I18N.getGUILabel("configurable_dialog.source_access");

	/** the text saying that a single user has access to the configurable */
	private String singleUserAccessText = I18N.getGUILabel("configurable_dialog.source_access_single_user");

	/** the label displaying the user groups that have access to a remote connection */
	private JLabel userAccessLabel;

	/** the button which can be clicked to change the user group access to a remote connection */
	private JButton userAccessButton;

	/** the label displaying the test action results */
	private JLabel testLabel;

	/** the panel containing the test action, test button */
	private final JPanel actionPanel = new JPanel(new GridBagLayout());

	/** the button which can be clicked to perform the test action for a configurable */
	private JButton testButton;

	/**
	 * The button that triggers the conversion to a new
	 * {@link com.rapidminer.connection.ConnectionInformation ConnectionInformation} if possible
	 *
	 * @see Configurable#supportsNewConnectionManagement()
	 * @see Configurable#convert()
	 * @since 9.3
	 */
	private JButton convertButton;

	/** the outer panel of the parameters part */
	private JPanel outerPanel;

	/** the outer panel of the dialog */
	private JPanel realOuterPanel;

	/** the button to save all configurables */
	private JButton okButton;

	/** the button to cancel the dialog */
	private JButton cancelButton;

	/**
	 * the panel which holds the name of a configurable, the button to change it and a button to
	 * delete the configurable
	 */
	private JPanel namePanel;

	/** the label displaying the name of a configurable */
	private JLabel nameLabel;

	/** the button which displays the configurator actions for a configurable */
	private JButton configActionsButton;

	/** the current panel showing the parameters for the selected configurable */
	private ConfiguratorParameterPanel configParamPanel;

	/** the {@link Configurable} which is selected (or was selected while switching) */
	private Configurable previousConfigurable;

	/** the {@link JXTaskPaneContainer} containing the {@link Configurable}s */
	private JXTaskPaneContainer configContainer;

	/** the button to rename a Configurable */
	private JButton renameButton;

	/** the button to remove a Configurable */
	private JButton removeButton;

	/** the location of the opened process, null (local) or remote repository */
	private RemoteRepository processLocation;

	/** preferred type id for new configurables (needed for start out of the parameter view) */
	private String preferredTypeId;

	private Map<String, Boolean> configurablesFetching = new HashMap<>();

	private Map<String, Boolean> addingConfigurablesFirstTime = new HashMap<>();

	protected static boolean isAddingDialogOpened = false;

	private final ConfigurableModelEventListener modelEventListener = new ConfigurableModelEventListener() {

		@Override
		public void modelChanged(ConfigurableEvent e) {

			int index = 0;
			switch (e.getEventType()) {
				case CONFIGURABLES_CHANGED:

					// clear all models
					localConfigListModel.clear();
					for (DefaultListModel<Configurable> remoteConfigListModel : remoteConfigListModels.values()) {
						remoteConfigListModel.clear();
					}
					// fill list model with existing Configurables
					for (Configurable config : localController.getModel().getConfigurables()) {
						localConfigListModel.addElement(config);
					}
					for (String key : remoteConfigListModels.keySet()) {
						for (Configurable config : getRemoteConfigurables(key)) {
							remoteConfigListModels.get(key).addElement(config);
						}
					}
					break;

				case CONFIGURABLE_ADDED:
					// find the correct list
					if (e.getConfigurable().getSource() == null) {

						if (localController.getModel().getConfigurables().size() == 1) {
							localInfoLabelListModel.removeAllElements();
						}
						index = localController.getModel().getConfigurables().indexOf(e.getConfigurable());
						localConfigListModel.add(index, e.getConfigurable());
						localConfigList.setSelectedIndex(index);
						unselectAllOtherLists(null);

					} else {

						String source = e.getConfigurable().getSource().getName();

						boolean notYetManuallyConnected = addingConfigurablesFirstTime.get(source) == null
								|| !addingConfigurablesFirstTime.get(source).booleanValue();
						boolean notYetAutoConnected = remoteControllers.get(source).getModel().wasVersionCheckDone();
						lastSelected = e.getConfigurable();

						if (notYetManuallyConnected && notYetAutoConnected) {
							if (remoteControllers.get(source).getModel().getConfigurables().size() == 1) {
								remoteInfoLabelListModels.get(source).removeAllElements();
							}
							index = getRemoteConfigurables(source).indexOf(e.getConfigurable());
							remoteConfigListModels.get(source).add(index, e.getConfigurable());
							remoteConfigLists.get(source).setSelectedIndex(index);

							unselectAllOtherLists(e.getConfigurable().getSource());
						}
					}
					break;

				case CONFIGURABLE_REMOVED:

					// find the correct list
					if (e.getConfigurable().getSource() == null) {

						index = localConfigList.getSelectedIndex();
						localConfigListModel.removeElement(e.getConfigurable());
						if (localConfigListModel.size() > 0) {
							// select the next entry (or none if it was the last)
							index = Math.min(index, localConfigListModel.size() - 1);
						} else {

							localInfoLabelListModel.removeAllElements();
							localInfoLabelListModel.addElement(ConfigurableInfoLabelType.NO_CONNECTIONS.toString());
							localTaskPane.repaint();
						}
						localConfigList.setSelectedIndex(index);

					} else {
						String source = e.getConfigurable().getSource().getName();
						if (!remoteConfigListModels.containsKey(source)) {
							// nothing to be removed
							break;
						}
						index = remoteConfigLists.get(source).getSelectedIndex();
						remoteConfigListModels.get(source).removeElement(e.getConfigurable());

						if (remoteConfigListModels.get(source).size() > 0) {
							// select the next entry (or none if it was the last)
							index = Math.min(index, remoteConfigListModels.get(source).size() - 1);
						} else {
							// if there are no connections available
							remoteInfoLabelListModels.get(source).removeAllElements();
							remoteInfoLabelListModels.get(source)
									.addElement(ConfigurableInfoLabelType.NO_CONNECTIONS.toString());
							remoteTaskPanes.get(source).repaint();
						}
						remoteConfigLists.get(source).setSelectedIndex(index);
					}
					break;

				case LOADED_FROM_REPOSITORY:

					// new remote connections are loaded and displayed in one block instead
					// of flying in separately
					Configurable c = e.getConfigurable();

					if (c != null) {
						if (c.getSource() != null) {
							// check if the refresh button should be visible
							updateRefreshButton();
							updateSmallLoginButton();

							// remember remote repository
							RemoteRepository repo = e.getConfigurable().getSource();
							final String source = repo.getName();
							addingConfigurablesFirstTime.remove(source);
							configurablesFetching.remove(source);

							// update UI
							// remove loading label
							remoteInfoLabelListModels.get(source).removeAllElements();
							remoteTaskPanes.get(source).add(remoteConfigLists.get(source));

							if (!remoteConfigListModels.containsKey(source)) {
								remoteConfigListModels.put(source, new DefaultListModel<Configurable>());
							}
							int lastSelectedIndex = 0;
							for (Configurable config : getRemoteConfigurables(source)) {
								index = getRemoteConfigurables(source).indexOf(config);
								remoteConfigListModels.get(source).add(index, config);
								if (lastSelected != null) {
									if (config.getId() == lastSelected.getId()) {
										lastSelectedIndex = index;
									}
								}
							}
							if (!isAddingDialogOpened) {
								unselectAllOtherLists(repo);
							}
							if (getRemoteConfigurables(source).size() > 0) {
								if (!isAddingDialogOpened) {
									remoteConfigLists.get(source).setSelectedIndex(lastSelectedIndex);
								}
							} else {
								// if there are no connections available
								remoteInfoLabelListModels.get(source).removeAllElements();
								remoteInfoLabelListModels.get(source)
										.addElement(ConfigurableInfoLabelType.NO_CONNECTIONS.toString());
								remoteTaskPanes.get(source).repaint();
								updateParameterPanel(null);
							}
						}
					}
					break;

				default:
					throw new IllegalStateException("event not handled!" + e.getEventType());
			}

			Configurable configurable = getSelectedValue();
			updateParameterPanel(configurable);
		}
	};

	/** glass pane showing a saving icon and label */
	private TransparentGlassPanePanel savingGlassPane;

	/** glass pane showing a gray transparent background */
	private TransparentGlassPanePanel simpleGlassPane;

	/** layer above the {@link #parameterPanel} */
	private JLayer<JPanel> parameterLayer;

	/** update queue for progress threads */
	private final UpdateQueue updateQueue = new UpdateQueue("storeConfigurables");

	/** drop down button to refresh the configurables */
	private RefreshConfigurablesDropDownButton refreshButton;

	/** login button if the user of a remote repository is not admin and has no connections */
	private LoginAsAdminDropDownButton smallLoginButton;

	/** layer on top of the outer panel */
	private JLayer<JPanel> outerLayer;

	/** button panel to create */
	private JPanel buttonPanel;

	/** action that is executed, when a user clicks cancel */
	private Action cancelAction;

	/** configurable that was selected before a refresh */
	private Configurable lastSelected;

	public ConfigurableDialog() {
		this(RapidMinerGUI.getMainFrame().getProcess());
	}

	/**
	 * @param openProcess
	 *            the currently opened process
	 */
	public ConfigurableDialog(Process openProcess) {
		super(ApplicationFrame.getApplicationFrame(), "configurable_dialog", ModalityType.MODELESS, new Object[] {});

		Repository processLoc = null;
		try {
			processLoc = openProcess == null || openProcess.getRepositoryLocation() == null ? null
					: openProcess.getRepositoryLocation().getRepository();
			if (processLoc instanceof RemoteRepository) {
				processLocation = (RemoteRepository) processLoc;
			} else {
				processLocation = null;
			}
		} catch (RepositoryException e) {
			// failure -> keep null and open local taskpane
		}

		// setup remote maps
		remoteInfoLabelLists = new HashMap<String, JList<String>>();
		remoteConfigLists = new HashMap<String, JList<Configurable>>();
		remoteControllers = new HashMap<String, ConfigurableController>();
		remoteListener = new HashMap<String, ConfigurableModelEventListener>();

		// setup source info label list model
		localInfoLabelListModel = new DefaultListModel<String>();
		remoteInfoLabelListModels = new HashMap<String, DefaultListModel<String>>();
		// setup configurable list model
		localConfigListModel = new DefaultListModel<Configurable>();
		remoteConfigListModels = new HashMap<String, DefaultListModel<Configurable>>();

		localListener = getConfigurableModelEventListener();
		// setup model for this view
		ConfigurableModel localModel = new ConfigurableModel(null);
		localModel.registerEventListener(localListener);

		// setup controller for this view
		localController = new ConfigurableController(this, localModel);

		// fill list model with existing Configurables
		for (Configurable config : ConfigurationManager.getInstance().getAllConfigurables()) {

			// find the correct list
			if (config.getSource() == null) {

				localConfigListModel.addElement(config);

			} else {
				final String source = config.getSource().getName();
				if (!remoteConfigListModels.containsKey(source)) {
					remoteConfigListModels.put(source, new DefaultListModel<Configurable>());
				}
				if (!remoteInfoLabelListModels.containsKey(source)) {
					remoteInfoLabelListModels.put(source, new DefaultListModel<String>());
				}
				if (!remoteListener.containsKey(source)) {
					remoteListener.put(source, getConfigurableModelEventListener());
					// setup model
					final ConfigurableModel remoteModel = new ConfigurableModel(config.getSource());
					remoteModel.registerEventListener(remoteListener.get(source));
					if (config.getSource().isConnected()) {
						ProgressThread pt = new ProgressThread("check_server_version") {

							@Override
							public void run() {
								try {
									remoteModel.checkVersion();
									if (!remoteModel.wasVersionCheckDone()) {
										collapseRemoteTaskPane(source);
										try {
											remoteModel.resetConfigurables();
											remoteModel.resetConnection();
										} catch (RepositoryException e1) {
											// connection could not be established
										}
									}
								} catch (WebServiceException e) {
									collapseRemoteTaskPane(source);
									try {
										remoteModel.resetConfigurables();
										remoteModel.resetConnection();
									} catch (RepositoryException e1) {
										// connection could not be established
									}
								}
							}
						};
						pt.start();
					}

					// setup controller
					remoteControllers.put(source, new ConfigurableController(this, remoteModel));
				}
				remoteConfigListModels.get(source).addElement(config);
			}
		}

		List<RemoteRepository> remotes = RepositoryManager.getInstance(null).getRemoteRepositories();
		for (final RemoteRepository repository : remotes) {
			if (!remoteConfigListModels.containsKey(repository.getName())) {
				remoteConfigListModels.put(repository.getName(), new DefaultListModel<Configurable>());
			}
			if (!remoteInfoLabelListModels.containsKey(repository.getName())) {
				remoteInfoLabelListModels.put(repository.getName(), new DefaultListModel<String>());
			}
			if (!remoteListener.containsKey(repository.getName())) {
				remoteListener.put(repository.getName(), getConfigurableModelEventListener());
				// setup model
				final ConfigurableModel remoteModel = new ConfigurableModel(repository);
				remoteModel.registerEventListener(remoteListener.get(repository.getName()));
				if (repository.isConnected()) {
					ProgressThread pt = new ProgressThread("check_server_version") {

						@Override
						public void run() {
							try {
								remoteModel.checkVersion();
								if (!remoteModel.wasVersionCheckDone()) {
									collapseRemoteTaskPane(repository.getName());
									try {
										remoteModel.resetConfigurables();
										remoteModel.resetConnection();
									} catch (RepositoryException e1) {
										// connection could not be established
									}
								}
							} catch (WebServiceException e) {
								collapseRemoteTaskPane(repository.getName());
								try {
									remoteModel.resetConfigurables();
									remoteModel.resetConnection();
								} catch (RepositoryException e1) {
									// connection could not be established
								}
							}
						}
					};
					pt.start();
				}

				// setup controller
				remoteControllers.put(repository.getName(), new ConfigurableController(this, remoteModel));
			}
		}

		initGUI();
		localConfigList.addListSelectionListener(createListSelectionListener(localConfigList));
		for (JList<Configurable> remoteList : remoteConfigLists.values()) {
			remoteList.addListSelectionListener(createListSelectionListener(remoteList));
		}

		updateQueue.start();
	}

	private ListSelectionListener createListSelectionListener(final JList<Configurable> list) {
		return new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {

				if (e.getValueIsAdjusting()) {
					return;
				}
				Configurable configurable = list.getSelectedValue();
				updateParameterPanel(configurable);
				previousConfigurable = configurable;

				if (e.getSource() == list && configurable != null) {
					unselectAllOtherLists(configurable.getSource());
				}
				updateButtonState(true);
			}
		};
	}

	/**
	 * Initializes the GUI.
	 */
	private void initGUI() {
		realOuterPanel = new JPanel(new BorderLayout());

		outerLayer = new JLayer<JPanel>(realOuterPanel);
		savingGlassPane = new TransparentGlassPanePanel(WAITING_ICON,
				I18N.getGUILabel("configurable_dialog.saving_configurables"), getBackground(), 0.5f);
		outerLayer.setGlassPane(savingGlassPane);
		savingGlassPane.setVisible(false);

		JPanel pagePanel = new JPanel(new BorderLayout());

		// list of configurables
		JPanel configPanel = createConfigPanel();
		// force size so it does not resize itself depending on entered values
		configPanel.setMinimumSize(CONFIG_LIST_SIZE);
		configPanel.setMaximumSize(CONFIG_LIST_SIZE);
		configPanel.setPreferredSize(CONFIG_LIST_SIZE);
		buttonPanel = createConfigurableButtonPanel();
		// create middle spacer
		JLabel spacer = new JLabel();
		spacer.setMinimumSize(DIMENSION_SPACER_MIDDLE);
		spacer.setMaximumSize(DIMENSION_SPACER_MIDDLE);
		spacer.setPreferredSize(DIMENSION_SPACER_MIDDLE);
		// add both to an outer panel for layout reasons
		JPanel outerConfigPanel = new JPanel(new BorderLayout());
		outerConfigPanel.setBorder(BorderFactory.createMatteBorder(0, 1, 1, 1, Color.LIGHT_GRAY));
		outerConfigPanel.add(configPanel, BorderLayout.CENTER);
		outerConfigPanel.add(buttonPanel, BorderLayout.SOUTH);
		// another panel for layouting
		JPanel outermostConfigPanel = new JPanel(new BorderLayout());
		outermostConfigPanel.add(outerConfigPanel, BorderLayout.CENTER);
		outermostConfigPanel.add(spacer, BorderLayout.EAST);

		// glass pane showed if the user is not able to edit connections due to an old version of
		// the server
		simpleGlassPane = new TransparentGlassPanePanel(null, null, getBackground(), 0.5f);

		// panel displaying the selected configurable
		JPanel paramPanel = createParameterPanel();

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 0.3;
		c.gridwidth = GridBagConstraints.REMAINDER;

		// add panels to page panel
		pagePanel.add(outermostConfigPanel, BorderLayout.WEST);
		pagePanel.add(paramPanel, BorderLayout.CENTER);

		// add page and button panel to outer panel
		realOuterPanel.add(pagePanel, BorderLayout.CENTER);

		layoutDefault(outerLayer, makeSaveButton(), makeCancel());
		new DeprecationWarning("manage_configurables").addToDialog(this);
		setDefaultSize(ButtonDialog.HUGE);
		setLocationRelativeTo(ApplicationFrame.getApplicationFrame());
		setModal(true);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				cancelButton.doClick();
			}
		});

		updateButtonState(true);
	}

	/**
	 * Updates the parameter panel for the selected {@link Configurable}.
	 */
	private void updateParameterPanel(Configurable config) {
		parameterPanel.removeAll();

		// save previously edited parameters
		updateModel();
		if (config != null) {
			try {
				// stripped text depending on the size of the panel
				String text = SwingTools.getStrippedJComponentText(outerPanel, config.getName(), outerPanel.getWidth() - 100,
						0);
				nameLabel.setText("<html><b>" + text + "</b></html>");

				// Get parameters based on Configurator implementation
				Parameters parameters = ConfigurationManager.getInstance().getAbstractConfigurator(config.getTypeId())
						.getParameterHandler(config).getParameters();

				// fill in real values of configurable
				for (String key : config.getParameters().keySet()) {
					parameters.setParameter(key, config.getParameters().get(key));
				}
				// init param panel with real values
				configParamPanel = new ConfiguratorParameterPanel(this, parameters);

				parameterLayer = new JLayer<JPanel>(configParamPanel);
				parameterLayer.setGlassPane(simpleGlassPane);
				simpleGlassPane.setVisible(false);

				// add it to wrapper panel
				parameterPanel.add(parameterLayer, BorderLayout.CENTER);

				boolean editingAllowed = true;
				boolean editingPossible = true;

				if (config.getSource() != null) {
					if (config.getSource().isConnected()) {
						if (!remoteControllers.get(config.getSource().getName()).getModel().hasAdminRights()) {
							editingAllowed = false;
						}
						// only interesting if we want to edit remote connections
						editingPossible = remoteControllers.get(config.getSource().getName()).getModel().isEditingPossible();
					}
				}

				if (!editingPossible) {

					SwingTools.setEnabledRecursive(configParamPanel, false);
					simpleGlassPane.setVisible(true);

				} else {
					if (!editingAllowed) {

						SwingTools.setEnabledRecursive(configParamPanel, false);
						simpleGlassPane.setVisible(true);
						renameButton.setVisible(false);
						removeButton.setVisible(false);
						actionPanel.setVisible(false);

					} else {

						if (!configParamPanel.isEnabled()) {
							SwingTools.setEnabledRecursive(configParamPanel, true);
						}

						if (simpleGlassPane.isVisible()) {
							simpleGlassPane.setVisible(false);
						}

						// make editing components visible
						renameButton.setVisible(true);
						removeButton.setVisible(true);
						actionPanel.setVisible(true);
					}
				}
			} catch (Exception e) {
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.tools.config.gui.ConfigurableDialog.error_setting_parameters", e);

				// display error in GUI
				parameterPanel.removeAll();

				JLabel errorLabel = new JLabel(
						I18N.getGUIMessage(I18N
								.getGUIMessage("gui.dialog.configurable_dialog.error.display_stored_configurable.label")),
						FAILURE_ICON, SwingConstants.LEADING);
				errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
				parameterPanel.add(errorLabel, BorderLayout.CENTER);
			}
		}
		updateButtonState(true);
		parameterPanel.revalidate();
		parameterPanel.repaint();
	}

	/**
	 * Checks if there is at least one user without admin rights
	 *
	 * @return number of users without admin rights
	 */
	private int getNumberOfUsersWithoutAdminRights() {

		int users = 0;
		for (ConfigurableController controller : remoteControllers.values()) {
			ConfigurableModel model = controller.getModel();
			boolean notUpdating = addingConfigurablesFirstTime.get(model.getSource().getName()) == null
					|| !addingConfigurablesFirstTime.get(model.getSource().getName()).booleanValue();
			boolean notFetching = configurablesFetching.get(model.getSource().getName()) == null
					|| !configurablesFetching.get(model.getSource().getName()).booleanValue();

			if (model.getSource().isConnected()) {
				if (notUpdating && notFetching) {
					if (!model.hasAdminRights()) {
						users++;
					}
				}
			}
		}
		return users;
	}

	/**
	 * Creates the panel which display the available {@link Configurable}s and buttons to add/remove
	 * new ones.
	 */
	private JPanel createConfigPanel() {
		JPanel configPanel = new JPanel();
		configPanel.setLayout(new GridBagLayout());

		configContainer = new JXTaskPaneContainer();
		configContainer.setBackgroundPainter(new MattePainter(Color.white));

		// add local task pane
		localTaskPane = new JXTaskPane();
		localTaskPane.setName("localGroup");
		localTaskPane.setTitle("Local");
		localTaskPane.setAnimated(false);
		localInfoLabelList = createNewInfoLabelJList(null);
		localTaskPane.add(localInfoLabelList);
		localConfigList = createNewConfigurableJList(null);
		localTaskPane.add(localConfigList);
		if (processLocation == null) {
			localTaskPane.setCollapsed(false);
		}
		configContainer.add(localTaskPane);

		// add remote task panes
		remoteTaskPanes = new HashMap<>();
		int i = 1;

		for (final String source : remoteConfigListModels.keySet()) {
			final JXTaskPane remoteTaskPane = new JXTaskPane();
			remoteTaskPane.setName("remoteGroup" + i);

			remoteInfoLabelLists.put(source, createNewInfoLabelJList(source));
			remoteTaskPane.add(remoteInfoLabelLists.get(source));
			remoteConfigLists.put(source, createNewConfigurableJList(source));
			remoteTaskPane.add(remoteConfigLists.get(source));
			remoteTaskPane.setTitle(source);
			remoteTaskPane.setAnimated(false);
			remoteTaskPane.addComponentListener(new ComponentAdapter() {

				@Override
				public void componentResized(ComponentEvent e) {
					if (remoteConfigListModels.get(source).isEmpty()) {
						boolean notFetching = configurablesFetching.get(source) == null
								|| !configurablesFetching.get(source).booleanValue();
						if (notFetching) {

							ProgressThread pt = new ProgressThread("load_configurables") {

								@Override
								public void run() {

									configurablesFetching.put(source, true);
									getProgressListener().setTotal(100);
									getProgressListener().setCompleted(10);
									List<RemoteRepository> remotes = RepositoryManager.getInstance(null)
											.getRemoteRepositories();
									for (RemoteRepository repository : remotes) {
										if (repository.getName().equals(source)) {
											// if the remote task pane was opened
											if (!remoteTaskPane.isCollapsed()) {

												// if the repository is not yet connected
												if (!repository.isConnected()) {

													addingConfigurablesFirstTime.put(source, true);
													remoteInfoLabelListModels.get(source).removeAllElements();
													remoteInfoLabelListModels.get(source)
															.addElement(ConfigurableInfoLabelType.LOADING.toString());
													remoteTaskPane.repaint();

													// try to connect
													try {
														repository.setPasswortInputCanceled(false);
														getProgressListener().setCompleted(30);
														if (!repository.isReachable()) {
															throw new RepositoryException();
														}
														getProgressListener().setCompleted(80);

													} catch (RepositoryException e1) {

														// timeout or connection failed
														remoteInfoLabelListModels.get(source).removeAllElements();
														remoteInfoLabelListModels.get(source)
																.addElement(ConfigurableInfoLabelType.FAILED.toString());
														addingConfigurablesFirstTime.remove(source);
														configurablesFetching.remove(source);
													}
													remoteControllers.get(source).getModel().isEditingPossible();

													// update UI
													remoteTaskPane.repaint();

												} else {
													// if there are no connections available
													if (remoteControllers.get(source).getModel().getConfigurables()
															.isEmpty()) {
														remoteInfoLabelListModels.get(source).removeAllElements();
														remoteInfoLabelListModels.get(source).addElement(
																ConfigurableInfoLabelType.NO_CONNECTIONS.toString());
														remoteTaskPane.repaint();
													}
												}
											}
											break;
										}
									}
									getProgressListener().setCompleted(100);
									getProgressListener().complete();
									configurablesFetching.remove(source);
									remoteTaskPane.repaint();
								}
							};
							pt.start();
						}
					}
				}
			});

			if (processLocation == null) {
				remoteTaskPane.setCollapsed(true);
			} else {
				if (source.equals(processLocation.getName())) {
					remoteTaskPane.setCollapsed(false);
					localTaskPane.setCollapsed(true);
				} else {
					remoteTaskPane.setCollapsed(true);
				}
			}
			configContainer.add(remoteTaskPane);
			remoteTaskPanes.put(source, remoteTaskPane);
			i++;
		}

		configContainer.add(Box.createVerticalStrut(5));
		JScrollPane configScroll = new ExtendedJScrollPane(configContainer);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		configPanel.add(configScroll, gbc);

		return configPanel;
	}

	/**
	 * Creates a new JList for a given source of a configurable
	 *
	 * @param source
	 *            can be null for local configurables, otherwise name of the source
	 * @return the created JList
	 */
	private JList<Configurable> createNewConfigurableJList(String source) {

		final JList<Configurable> createdConfigList = new JList<>();
		createdConfigList.setModel(source == null ? localConfigListModel : remoteConfigListModels.get(source));
		createdConfigList.setCellRenderer(new ConfigurableRenderer());
		createdConfigList.setFixedCellHeight(40);
		createdConfigList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		createdConfigList.setBackground(LIGHTER_GRAY);

		return createdConfigList;
	}

	/**
	 * Creates a new JList for info labels of a source
	 *
	 * @param source
	 *            can be null for local source, otherwise name of the source
	 * @return the created JList
	 */
	private JList<String> createNewInfoLabelJList(String source) {
		final JList<String> createdInfoLabelList = new JList<>();
		createdInfoLabelList.setModel(source == null ? localInfoLabelListModel : remoteInfoLabelListModels.get(source));
		createdInfoLabelList.setCellRenderer(new ConfigurableInfoLabelRenderer());
		createdInfoLabelList.setFixedCellHeight(20);
		createdInfoLabelList.setBackground(LIGHTER_GRAY);
		return createdInfoLabelList;
	}

	/**
	 * Unselects all other lists without triggering events
	 *
	 * @param source
	 *            can be null for the local connection list, otherwise remote connection source
	 */
	private void unselectAllOtherLists(RemoteRepository source) {
		// if a local connection is selected, clear the selection of all remote connection lists
		if (source == null) {
			for (JList<Configurable> list : remoteConfigLists.values()) {
				clearSelection(list);
			}
		} else {
			// clear the selection of the local connection list
			clearSelection(localConfigList);

			// clear the selection of all other remote connection lists
			for (String key : remoteConfigLists.keySet()) {
				if (!source.getName().equals(key)) {
					JList<Configurable> list = remoteConfigLists.get(key);
					clearSelection(list);
				}
			}
		}
	}

	/**
	 * Clears the selection of a {@link JList} without triggering of {@link ListSelectionEvent}s
	 *
	 * @param list
	 * @param key
	 */
	private void clearSelection(JList<Configurable> list) {

		ListSelectionListener[] oldListSelectionListeners = list.getListSelectionListeners();
		for (ListSelectionListener exListener : oldListSelectionListeners) {
			list.removeListSelectionListener(exListener);
		}

		list.clearSelection();
		list.setFocusable(false);
		list.setFocusable(true);

		for (ListSelectionListener exListener : oldListSelectionListeners) {
			list.addListSelectionListener(exListener);
		}
	}

	/**
	 * Creates the panel which contains the parameters as well as the {@link ConfigurableAction} s.
	 *
	 * @return
	 */
	private JPanel createParameterPanel() {
		outerPanel = new JPanel(new BorderLayout());

		namePanel = new JPanel(new GridBagLayout());
		namePanel.setBackground(NAME_PANEL_GRAY);
		namePanel.setMinimumSize(new Dimension(namePanel.getMinimumSize().width, 50));
		namePanel.setPreferredSize(new Dimension(namePanel.getPreferredSize().width, 50));
		namePanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.insets = new Insets(5, 10, 5, 5);
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.VERTICAL;

		nameLabel = new JLabel();
		nameLabel.setToolTipText(I18N.getGUILabel("configurable_dialog.name_config.tip"));
		nameLabel.setVisible(true);
		gbc.anchor = GridBagConstraints.WEST;
		namePanel.add(nameLabel, gbc);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setBackground(NAME_PANEL_GRAY);
		renameButton = new JButton(new ResourceActionAdapter(true, "configurable_dialog.rename_config") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				boolean done = false;
				final Configurable config = getSelectedValue();
				if (config == null) {
					return;
				}

				boolean localConfigurable = config.getSource() == null;
				boolean editingPossible = localConfigurable;
				boolean editingAllowed = localConfigurable;

				if (!localConfigurable) {
					ConfigurableController remoteController = remoteControllers.get(config.getSource().getName());
					if (remoteController.getModel().isEditingPossible()) {
						editingPossible = true;
					}
					if (remoteController.getModel().hasAdminRights()) {
						editingAllowed = true;
					}
				}

				if (editingPossible && editingAllowed) {
					// show input while user enters invalid values, if user clicks cancel or
					// finished correctly, abort loop
					do {
						String name = SwingTools.showInputDialog(ConfigurableDialog.this, "configurable_dialog.rename",
								config.getName());
						if (name == null) {
							// user cancelled dialog
							break;
						}
						if (name.equals(config.getName())) {
							// user did not change the name
							break;
						}
						if ("".equals(name.trim())) {
							SwingTools.showVerySimpleErrorMessage(ConfigurableDialog.this,
									"configurable_creation_dialog.invalid_name");
							continue;
						}
						ConfigurableController controller = config.getSource() == null ? localController
								: remoteControllers.get(config.getSource().getName());
						boolean isUnique = controller.isNameUniqueForType(config.getTypeId(), name);
						if (!isUnique) {
							SwingTools.showVerySimpleErrorMessage(ConfigurableDialog.this,
									"configurable_creation_dialog.invalid_duplicate_name");
							continue;
						} else {
							controller.renameConfigurable(config, name);
							updateParameterPanel(config);
							done = true;
							localConfigList.repaint();
							for (JList<Configurable> remoteList : remoteConfigLists.values()) {
								remoteList.repaint();
							}
						}
					} while (!done);
				}
			}
		});

		// listen to F2 types and do the rename action
		KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0);
		buttonPanel.registerKeyboardAction(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				renameButton.doClick();
			}
		}, keyStroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
		renameButton.setBorderPainted(false);
		renameButton.setContentAreaFilled(false);
		renameButton.setRolloverEnabled(false);
		renameButton.setVisible(false);
		renameButton.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseExited(MouseEvent e) {
				renameButton.setBorderPainted(false);
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				renameButton.setBorderPainted(true);
			}
		});
		gbc.gridx += 1;
		gbc.anchor = GridBagConstraints.EAST;
		// gbc.weightx = 0.95;
		buttonPanel.add(renameButton);

		removeButton = new JButton(new ResourceActionAdapter(true, "configurable_dialog.remove_config") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				Configurable selectedValue = getSelectedValue();
				if (selectedValue == null) {
					return;
				}

				if (selectedValue.getSource() == null) {
					localController.removeConfigurable(localConfigList.getSelectedValue());
				} else {
					ConfigurableController remoteController = remoteControllers.get(selectedValue.getSource().getName());
					if (remoteController.getModel().isEditingPossible()) {
						if (remoteController.getModel().hasAdminRights()) {
							remoteController.removeConfigurable(
									remoteConfigLists.get(selectedValue.getSource().getName()).getSelectedValue());
						}
					}
				}
			}
		});

		// listen to DELETE types and do the remove action
		KeyStroke keyStrokeRemove = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
		buttonPanel.registerKeyboardAction(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				removeButton.doClick();
			}
		}, keyStrokeRemove, JComponent.WHEN_IN_FOCUSED_WINDOW);
		removeButton.setBorderPainted(false);
		removeButton.setContentAreaFilled(false);
		removeButton.setRolloverEnabled(false);
		removeButton.setVisible(false);
		removeButton.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseExited(MouseEvent e) {
				removeButton.setBorderPainted(false);
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				removeButton.setBorderPainted(true);
			}
		});
		gbc.gridx += 1;
		gbc.weightx = 0.95;
		buttonPanel.add(removeButton);
		namePanel.add(buttonPanel, gbc);

		// setup parameter panel
		parameterPanel = new JPanel(new BorderLayout());

		actionPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
		gbc.gridx = 0;
		gbc.weightx = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;

		convertButton = new JButton(new ResourceAction(false, "configurable_dialog.convert_configurable") {

			@Override
			protected void loggedActionPerformed(ActionEvent e) {
				// skip if we started this without configurable or is unconvertable
				Configurable selectedConfig = getSelectedValue();
				if (selectedConfig == null || !selectedConfig.supportsNewConnectionManagement()) {
					return;
				}
				ConnectionAdapterHandler<ConnectionAdapter> handler = ConnectionAdapterHandler.getHandler(selectedConfig.getTypeId());
				if (handler == null) {
					return;
				}
				// create copy and set current values
				Configurable configurable;
				try {
					configurable = handler.create(selectedConfig.getName(), selectedConfig.getParameters());
				} catch (ConfigurationException ce) {
					SwingTools.showSimpleErrorMessage(ConfigurableDialog.this,
							"configuration.dialog.general", ce, ce.getMessage());
					return;
				}
				localController.saveConfigurable(configurable, configParamPanel.getParameters());
				// prevent multiple invocations
				setEnabled(false);
				Repository repository = selectedConfig.getSource();
				ConfigurableDialog parent = ConfigurableDialog.this;
				ConnectionCreationDialog conversionDialog = new ConnectionCreationDialog(parent, repository);
				String type = handler.getType();
				conversionDialog.preFill(type, configurable.getName());
				conversionDialog.setConverter(configurable::convert);
				conversionDialog.setVisible(true);
				setEnabled(true);
			}
		});
		convertButton.setEnabled(false);
		actionPanel.add(convertButton, gbc);

		gbc.gridx++;
		configActionsButton = new JButton(new ResourceAction(false, "configurable_dialog.show_actions") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				// skip if we started this without configurable
				Configurable selectedValue = getSelectedValue();
				if (selectedValue == null) {
					return;
				}

				Configurable configValue = previousConfigurable;
				if (AbstractConfigurable.class.isAssignableFrom(configValue.getClass())) {
					AbstractConfigurable configurable = (AbstractConfigurable) configValue;
					if (configurable.getActions() != null && !configurable.getActions().isEmpty()) {
						JPopupMenu actionMenu = new ScrollableJPopupMenu();
						// create one menu item for each action defined by the configurable
						for (final ConfigurableAction action : configurable.getActions()) {
							JMenuItem actionItem = new JMenuItem();
							actionItem.setIcon(SwingTools.createIcon("24/" + action.getIconName()));
							actionItem.setText(action.getName());
							actionItem.setToolTipText(action.getTooltip());
							actionItem.addActionListener(event -> {
								// reset last results
								testLabel.setIcon(null);

								// store fresh changes
								previousConfigurable = selectedValue;
								if (previousConfigurable.getSource() == null) {
									localController.saveConfigurable(previousConfigurable,
											configParamPanel.getParameters());
									localController.executeConfigurableAction(action);
								} else {
									remoteControllers.get(previousConfigurable.getSource().getName())
											.saveConfigurable(previousConfigurable, configParamPanel.getParameters());
									remoteControllers.get(previousConfigurable.getSource().getName())
											.executeConfigurableAction(action);
								}
							});

							actionMenu.add(actionItem);
						}

						actionMenu.show(configActionsButton, 0, (int) configActionsButton.getSize().getHeight());
					}
				}
			}
		});

		configActionsButton.setEnabled(false);
		actionPanel.add(configActionsButton, gbc);

		gbc.gridx += 1;
		gbc.weightx = 0.0;
		testButton = new JButton(new ResourceAction(false, "configurable_dialog.test_configurable") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				// skip if we started this without configurable
				Configurable selectedValue = getSelectedValue();
				if (selectedValue == null) {
					return;
				}

				// prevent multiple tests from running
				testButton.setEnabled(false);

				// reset last results
				testLabel.setIcon(null);

				// store fresh changes
				previousConfigurable = selectedValue;
				if (previousConfigurable.getSource() == null) {
					localController.saveConfigurable(previousConfigurable, configParamPanel.getParameters());
				} else {
					remoteControllers.get(previousConfigurable.getSource().getName()).saveConfigurable(previousConfigurable,
							configParamPanel.getParameters());
				}

				Configurable configValue = previousConfigurable;
				if (AbstractConfigurable.class.isAssignableFrom(configValue.getClass())) {
					AbstractConfigurable configurable = (AbstractConfigurable) configValue;
					if (configurable.getTestAction() != null) {
						if (previousConfigurable.getSource() == null) {
							localController.executeConfigurableAction(configurable.getTestAction());
						} else {
							remoteControllers.get(previousConfigurable.getSource().getName())
									.executeConfigurableAction(configurable.getTestAction());
						}
					}
				} else {
					testButton.setEnabled(true);
				}
			}
		});
		testButton.setEnabled(false);
		actionPanel.add(testButton, gbc);

		gbc.gridx += 1;
		testLabel = new JLabel();
		actionPanel.add(testLabel, gbc);

		// create middle spacer
		gbc.gridx += 1;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		actionPanel.add(new JLabel(), gbc);

		gbc.gridx += 1;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.NONE;
		userAccessLabel = new JLabel("<html><font color=\"red\"><b>0</b> " + userAccessText + "</font></html>");
		userAccessLabel.setVisible(false);
		actionPanel.add(userAccessLabel, gbc);

		userAccessButton = new JButton(new ResourceAction(false, "configurable_dialog.source_access") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				Configurable configurable = getSelectedValue();
				ConfigurableUserAccessDialog accessDialog = new ConfigurableUserAccessDialog(ConfigurableDialog.this,
						configurable);
				accessDialog.setVisible(true);
				ConfigurationManager.getInstance().setPermittedGroupsForConfigurable(configurable,
						accessDialog.getPermittedUserGroups());
				updateSourceAccessLabel(true);
			}
		});
		userAccessButton.setVisible(false);
		gbc.gridx += 1;
		actionPanel.add(userAccessButton, gbc);

		final ExtendedJScrollPane paramScrollPane = new ExtendedJScrollPane(parameterPanel);
		paramScrollPane.setBorder(null);
		JLabel spacer = new JLabel();
		spacer.setMinimumSize(DIMENSION_SPACER_PARAMS);
		spacer.setMaximumSize(DIMENSION_SPACER_PARAMS);
		spacer.setPreferredSize(DIMENSION_SPACER_PARAMS);
		// construct outer panel
		outerPanel.add(spacer, BorderLayout.WEST);
		outerPanel.add(namePanel, BorderLayout.NORTH);
		outerPanel.add(paramScrollPane, BorderLayout.CENTER);
		outerPanel.add(actionPanel, BorderLayout.SOUTH);
		outerPanel.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.LIGHT_GRAY));
		return outerPanel;
	}

	/**
	 * Creates the button panel containing the buttons for the {@link Configurable} list.
	 *
	 * @param buttonPanel
	 */
	private JPanel createConfigurableButtonPanel() {
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridBagLayout());
		GridBagConstraints buttonGBC = new GridBagConstraints();

		final JButton addButton = new JButton(new ResourceActionAdapter(false, "configurable_dialog.add_config") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {

				Configurable selected = getSelectedValue();
				RemoteRepository source = selected == null ? null : selected.getSource();
				ConfigurableCreationDialog dialog = new ConfigurableCreationDialog(ConfigurableDialog.this, localController,
						remoteControllers, source, preferredTypeId);
				isAddingDialogOpened = true;
				dialog.setVisible(true);
			}
		});

		buttonGBC.gridx = 0;
		buttonGBC.fill = GridBagConstraints.HORIZONTAL;
		buttonGBC.weightx = 0.5f;
		buttonGBC.insets = new Insets(5, 5, 5, 0);
		buttonPanel.add(addButton, buttonGBC);

		JPanel panelRefreshButton = new JPanel(new GridLayout(1, 2, 5, 5));
		refreshButton = new RefreshConfigurablesDropDownButton(remoteControllers);
		smallLoginButton = new LoginAsAdminDropDownButton(this, remoteControllers);
		panelRefreshButton.add(refreshButton);
		panelRefreshButton.add(smallLoginButton);
		buttonGBC.insets = new Insets(0, 5, 0, 5);
		buttonGBC.gridx += 1;
		buttonPanel.add(panelRefreshButton, buttonGBC);

		buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));

		return buttonPanel;
	}

	/**
	 * Creates the Save button which saves all the changes the user has created.
	 *
	 * @return
	 */
	private AbstractButton makeSaveButton() {

		Action okAction = new ResourceAction("configurable_dialog.save_all") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {

				// now we need to save the changes the user made
				// in a separate thread

				localController.getModel().removeEventListener(localListener);
				for (String remote : remoteListener.keySet()) {
					remoteControllers.get(remote).getModel().removeEventListener(remoteListener.get(remote));
				}

				// show the glass pane
				okButton.setEnabled(false);
				cancelButton.setEnabled(false);
				savingGlassPane.setVisible(true);
				SwingTools.setEnabledRecursive(realOuterPanel, false);

				ProgressThread pt = new ProgressThread("store_configurables") {

					@Override
					public void run() {
						getProgressListener().setTotal(100);
						getProgressListener().setCompleted(10);

						// save edited parameters of current configurable
						Configurable config = getSelectedValue();
						if (configParamPanel != null && config != null) {
							if (config.getSource() == null) {
								localController.saveConfigurable(config, configParamPanel.getParameters());
							} else {
								remoteControllers.get(config.getSource().getName()).saveConfigurable(config,
										configParamPanel.getParameters());
							}
						}

						// check if current configurable is in valid state, i.e. all required
						// parameters
						// are
						// set. If not, switch to broken config and display problem to user
						for (final Configurable configurable : localController.getModel().getConfigurables()) {
							final ActionResult result = localController.checkConfigurableValidState(configurable);
							if (result.getResult().equals(Result.FAILURE)) {

								SwingUtilities.invokeLater(new Runnable() {

									@Override
									public void run() {
										okButton.setEnabled(true);
										cancelButton.setEnabled(true);
										savingGlassPane.setVisible(false);
										SwingTools.setEnabledRecursive(realOuterPanel, true);
										// switch to the broken one
										localConfigList.setSelectedValue(configurable, true);
										displayResult(result);
									}
								});

								return;
							}
						}
						for (final String source : remoteControllers.keySet()) {
							if (remoteControllers.get(source).getModel().getSource().isConnected()) {
								if (remoteControllers.get(source).getModel().isEditingPossible()) {

									for (final Configurable configurable : remoteControllers.get(source).getModel()
											.getConfigurables()) {
										final ActionResult result = remoteControllers.get(source)
												.checkConfigurableValidState(configurable);
										if (result.getResult().equals(Result.FAILURE)) {
											SwingUtilities.invokeLater(new Runnable() {

												@Override
												public void run() {
													okButton.setEnabled(true);
													cancelButton.setEnabled(true);
													savingGlassPane.setVisible(false);
													SwingTools.setEnabledRecursive(realOuterPanel, true);
													// switch to the broken one
													remoteConfigLists.get(source).setSelectedValue(configurable, true);
													displayResult(result);
												}
											});
											return;
										}
									}
								}
							}
						}

						localController.save();
						for (Entry<String, ConfigurableController> entry : remoteControllers.entrySet()) {
							entry.getValue().save();
							entry.getValue().getModel().resetCredentials();
						}

						getProgressListener().setCompleted(100);
						getProgressListener().complete();

						// run this in a GUI thread
						SwingUtilities.invokeLater(new Runnable() {

							@Override
							public void run() {
								savingGlassPane.setVisible(false);
								setConfirmed(true);
								ConfigurableDialog.super.dispose();
							}
						});
					}
				};
				updateQueue.executeBackgroundJob(pt);
			}
		};
		okButton = new JButton(okAction);
		return okButton;
	}

	/**
	 * Creates the Cancel button which discards all the changes the user made.
	 *
	 * @return
	 */
	private AbstractButton makeCancel() {
		cancelAction = new ResourceAction("cancel") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {

				// show the glass pane
				okButton.setEnabled(false);
				cancelButton.setEnabled(false);
				savingGlassPane.setText("");
				savingGlassPane.setVisible(true);
				SwingTools.setEnabledRecursive(realOuterPanel, false);

				// if dialog was closed via 'Cancel', revert any changes
				localController.revertChanges();
				for (ConfigurableController remoteController : remoteControllers.values()) {
					remoteController.revertChanges();
				}
				localController.getModel().removeEventListener(localListener);
				for (String remote : remoteListener.keySet()) {
					remoteControllers.get(remote).getModel().removeEventListener(remoteListener.get(remote));
				}

				new ProgressThread("refresh_configurables") {

					@Override
					public void run() {
						for (String remote : remoteListener.keySet()) {
							remoteControllers.get(remote).getModel().resetCredentials();
						}
						SwingUtilities.invokeLater(new Runnable() {

							@Override
							public void run() {
								ConfigurableDialog.super.dispose();
							}
						});
					}
				}.start();
			}
		};
		cancelButton = new JButton(cancelAction);

		// make ESC close dialog
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "CANCEL");
		getRootPane().getActionMap().put("CANCEL", cancelAction);

		return cancelButton;

	}

	/**
	 * Updates the state of the buttons and the test result label depending on the currently
	 * selected {@link Configurable}.
	 *
	 * @param resetMessage
	 */
	private void updateButtonState(boolean resetMessage) {

		Configurable configurable = getSelectedValue();
		boolean editingPossible = true;
		boolean editingAllowed = true;

		if (configurable != null) {
			if (configurable.getSource() != null) {
				if (configurable.getSource().isConnected()) {
					if (!remoteControllers.get(configurable.getSource().getName()).getModel().hasAdminRights()) {
						editingAllowed = false;
					}
					// only interesting if we want to edit remote connections
					editingPossible = remoteControllers.get(configurable.getSource().getName()).getModel()
							.isEditingPossible();
				}
			}
		}

		// show the actionPanel only if configurable != null and editing is allowed
		// show the name panel if a configurable is selected and show the rename and remove
		// button
		// if the editing is allowed
		namePanel.setVisible(configurable != null);
		renameButton.setVisible(configurable != null && editingAllowed && editingPossible);
		removeButton.setVisible(configurable != null && editingAllowed && editingPossible);
		actionPanel.setVisible(configurable != null);
		userAccessButton
				.setVisible(configurable != null && configurable.getSource() != null && editingAllowed && editingPossible);
		updateSourceAccessLabel(editingAllowed && editingPossible);

		if (resetMessage) {
			testLabel.setIcon(null);
		}
		if (configurable != null) {
			nameLabel.setIcon(SwingTools.createIcon("24/"
					+ ConfigurationManager.getInstance().getAbstractConfigurator(configurable.getTypeId()).getIconName()));

			if (AbstractConfigurable.class.isAssignableFrom(configurable.getClass())) {
				AbstractConfigurable abstractConfig = (AbstractConfigurable) configurable;
				configActionsButton.setEnabled(abstractConfig.getActions() != null);
				testButton.setEnabled(abstractConfig.getTestAction() != null);
			}
			convertButton.setEnabled(configurable.supportsNewConnectionManagement()
					&& ConnectionAdapterHandler.getHandler(configurable.getTypeId()) != null);
		}

		updateRefreshButton();
		updateSmallLoginButton();
	}

	private void updateRefreshButton() {

		// check if the refresh button should be visible (if there are connected remote
		// repositories)
		int counter = 0;
		for (ConfigurableController controller : remoteControllers.values()) {
			if (controller.getModel().getSource().isConnected()) {
				counter++;
			}
		}
		if (counter > 0) {
			if (!refreshButton.isVisible()) {
				refreshButton.setVisible(true);
			}
		} else {
			if (refreshButton.isVisible()) {
				refreshButton.setVisible(false);
			}
		}
	}

	private void updateSmallLoginButton() {

		// check if the small login button should be visible (if there are connected remote
		// repositories)
		int counter = getNumberOfUsersWithoutAdminRights();

		if (counter > 0) {
			if (!smallLoginButton.isVisible()) {
				smallLoginButton.setVisible(true);
			}
		} else {
			if (smallLoginButton.isVisible()) {
				smallLoginButton.setVisible(false);
			}
		}
	}

	/**
	 * Updates the text of the {@link ConfigurableDialog#userAccessLabel} depending on the currently
	 * selected {@link Configurable}.
	 *
	 */
	private void updateSourceAccessLabel(boolean editingEnabled) {

		Configurable configurable = getSelectedValue();

		if (configurable != null && configurable.getSource() != null && editingEnabled) {

			userAccessLabel.setVisible(true);

			int noOfAccessingUsers = ConfigurationManager.getInstance().getPermittedGroupsForConfigurable(configurable)
					.size();

			if (noOfAccessingUsers == 0) {
				userAccessLabel.setText(
						"<html><font color=\"red\"><b>" + noOfAccessingUsers + "</b> " + userAccessText + "</font></html>");
			} else if (noOfAccessingUsers == 1) {
				userAccessLabel.setText("<html><b>" + noOfAccessingUsers + "</b> " + singleUserAccessText + "</html>");
			} else {
				userAccessLabel.setText("<html><b>" + noOfAccessingUsers + "</b> " + userAccessText + "</html>");
			}

		} else {
			userAccessLabel.setVisible(false);
		}
	}

	/**
	 * Displays the result of a test action or a configurator action.
	 */
	protected void displayResult(ActionResult result) {
		ImageIcon icon = null;
		if (Result.SUCCESS.equals(result.getResult())) {
			icon = SUCCESS_ICON;
			testLabel.setIcon(icon);
			// SwingTools.showMessageDialog(ConfigurableDialog.this, "configuration.test.success",
			// result.getMessage());
		} else if (Result.FAILURE.equals(result.getResult())) {
			icon = FAILURE_ICON;
			testLabel.setIcon(icon);
			SwingTools.showVerySimpleErrorMessage(ConfigurableDialog.this, "configuration.test.fail", result.getMessage());
		}

		testLabel.setToolTipText(result.getMessage());

		updateButtonState(false);
	}

	/**
	 * Displays that an action is currently running.
	 */
	protected void displayActionIsRunning() {

		testLabel.setIcon(WORKING_ICON);
	}

	/**
	 * Displays an error dialog indicating that saving has failed.
	 */
	protected void displaySaveErrorDialog() {
		SwingTools.showVerySimpleErrorMessage(ConfigurableDialog.this, "configurable_dialog.save_configurable");
	}

	/**
	 * Displays an error dialog indicating that saving has failed due to the server which is not
	 * up-to-date.
	 */
	protected void displaySaveUploadErrorDialogServerNotUpToDate(String serverName) {
		SwingTools.showVerySimpleErrorMessage(ConfigurableDialog.this,
				"configurable_controller_upload_error_server_not_up_to_date", serverName);
	}

	/**
	 * Displays an error dialog indicating that saving for a specific typeId has failed.
	 */
	protected void displaySaveUploadErrorDialog(String typeId, String repositoryName, String repositoryURL) {
		SwingTools.showVerySimpleErrorMessage(ConfigurableDialog.this, "configurable_controller_upload_error_server", typeId,
				repositoryName, repositoryURL);
	}

	/**
	 * Displays an error dialog indicating that the connection to the server has failed.
	 */
	protected void displayConnectionErrorDialog(String repositoryName, String repositoryURL) {
		SwingTools.showVerySimpleErrorMessage(ConfigurableDialog.this, "configurable_controller_connection_failed",
				repositoryName, repositoryURL);
	}

	/**
	 * Collapses a remote task pane, if it exists
	 */
	protected void collapseRemoteTaskPane(String repositoryName) {
		if (remoteTaskPanes.containsKey(repositoryName)) {
			remoteTaskPanes.get(repositoryName).setCollapsed(true);
		}
	}

	/**
	 * Tries to select the specified {@link Configurable} identified via its name and typeId.
	 */
	public void selectConfigurable(String configurableName, String typeId) {

		// set a preferred typeId for new configurables
		// dialog is started from the parameter panel of a specific operator which needs a
		// preferred
		// configuration type
		preferredTypeId = typeId;

		// go through all existing configurables in the models, try to find and select it
		boolean done = false;

		if (processLocation != null) {
			localTaskPane.setCollapsed(true);
			for (String source : remoteConfigListModels.keySet()) {
				if (processLocation.getName().equals(source)) {
					remoteTaskPanes.get(source).setCollapsed(false);
					// check if you find a configurable with the given name and preferred typeid
					for (int i = 0; i < remoteConfigListModels.get(source).size(); i++) {
						if (remoteConfigListModels.get(source).get(i).getName().equals(configurableName)) {
							if (remoteConfigListModels.get(source).get(i).getTypeId().equals(preferredTypeId)) {
								remoteConfigLists.get(source).setSelectedIndex(i);
								done = true;
								break;
							}
						}
					}
					// if this was not possible
					if (!done) {
						// check if you find any configurable with the preferred typeid
						for (int i = 0; i < remoteConfigListModels.get(source).size(); i++) {
							if (remoteConfigListModels.get(source).get(i).getTypeId().equals(preferredTypeId)) {
								remoteConfigLists.get(source).setSelectedIndex(i);
								done = true;
								break;
							}
						}
						// if this was also not possible
						if (!done) {
							// select the first element if there is one
							if (remoteControllers.get(source).getModel().getConfigurables().size() > 0) {
								remoteConfigLists.get(source).setSelectedIndex(0);
							}
						}
					}
				} else {
					remoteTaskPanes.get(source).setCollapsed(true);
				}
			}
		} else {
			localTaskPane.setCollapsed(false);
			for (int i = 0; i < localConfigListModel.size(); i++) {
				// check if you find a configurable with the given name and preferred typeid
				if (localConfigListModel.get(i).getName().equals(configurableName)) {
					if (localConfigListModel.get(i).getTypeId().equals(preferredTypeId)) {
						localConfigList.setSelectedIndex(i);
						done = true;
						break;
					}
				}
			}
			// if this was also not possible
			if (!done) {
				for (int i = 0; i < localConfigListModel.size(); i++) {
					// check if you find any configurable with the preferred typeid
					if (localConfigListModel.get(i).getTypeId().equals(preferredTypeId)) {
						localConfigList.setSelectedIndex(i);
						done = true;
						break;
					}
				}
				// if this was also not possible
				if (!done) {
					// select the first element if there is one
					if (localController.getModel().getConfigurables().size() > 0) {
						localConfigList.setSelectedIndex(0);
					}
				}
			}
			for (String source : remoteConfigListModels.keySet()) {
				remoteTaskPanes.get(source).setCollapsed(true);
			}
		}
	}

	private Configurable getSelectedValue() {

		if (!localConfigList.isSelectionEmpty()) {
			return localConfigList.getSelectedValue();
		} else {
			for (JList<Configurable> list : remoteConfigLists.values()) {
				if (!list.isSelectionEmpty()) {
					return list.getSelectedValue();
				}
			}
		}
		return null;
	}

	private ConfigurableModelEventListener getConfigurableModelEventListener() {
		return modelEventListener;
	}

	/**
	 * @param source
	 *            the source of the remote repository
	 * @return the list of configurables
	 */
	private List<Configurable> getRemoteConfigurables(final String source) {
		return remoteControllers.get(source).getModel().getConfigurables();
	}

	/**
	 * Reloads configurables for the given source and task pane.
	 *
	 * @param source
	 * @param remoteTaskPane
	 */
	private void updateConfigurables(final String source, final JXTaskPane remoteTaskPane) {

		if (remoteConfigListModels.get(source).isEmpty()) {
			boolean notFetching = configurablesFetching.get(source) == null
					|| !configurablesFetching.get(source).booleanValue();

			if (notFetching) {

				ProgressThread pt = new ProgressThread("refresh_configurables") {

					@Override
					public void run() {

						configurablesFetching.put(source, true);
						getProgressListener().setTotal(100);
						getProgressListener().setCompleted(10);
						for (ConfigurableController controller : remoteControllers.values()) {
							RemoteRepository repository = controller.getModel().getSource();
							if (repository.getName().equals(source)) {
								addingConfigurablesFirstTime.put(source, true);
								remoteInfoLabelListModels.get(source).removeAllElements();
								remoteInfoLabelListModels.get(source)
										.addElement(ConfigurableInfoLabelType.LOADING.toString());
								remoteTaskPane.repaint();

								// try to connect
								try {
									repository.setPasswortInputCanceled(false);
									getProgressListener().setCompleted(30);
									remoteControllers.get(source).getModel().resetConnection();
									getProgressListener().setCompleted(80);

								} catch (RepositoryException e1) {

									// timeout or connection failed
									remoteInfoLabelListModels.get(source).removeAllElements();
									remoteInfoLabelListModels.get(source)
											.addElement(ConfigurableInfoLabelType.FAILED.toString());
									addingConfigurablesFirstTime.remove(source);
									configurablesFetching.remove(source);
								}
								remoteControllers.get(source).getModel().isEditingPossible();

								// update UI
								remoteTaskPane.repaint();

								break;
							}
						}
						getProgressListener().setCompleted(100);
						getProgressListener().complete();
						configurablesFetching.remove(source);
						remoteTaskPane.repaint();
					}
				};
				pt.start();
			}
		}
	}

	/**
	 * Refreshes {@link Configurable}s for the given source
	 *
	 * @param source
	 *            Name of the {@link RemoteRepository}, from where the {@link Configurable}s are
	 *            loaded
	 */
	public void refreshConfigurables(String source) {
		// reset the task panes, connections and reload them
		// make sure they are added together
		addingConfigurablesFirstTime.put(source, true);
		lastSelected = getSelectedValue();
		remoteTaskPanes.get(source).setCollapsed(false);
		// reset the old configurables
		remoteControllers.get(source).getModel().resetConfigurables();
		// reload the configurables (contains resetConnection)
		updateConfigurables(source, remoteTaskPanes.get(source));
	}

	/**
	 * Updates the underlying {@link ConfigurableModel} with the currently displayed parameter data
	 */
	public void updateModel(){
		if (configParamPanel != null && previousConfigurable != null) {
			if (previousConfigurable.getSource() == null) {
				localController.saveConfigurable(previousConfigurable, configParamPanel.getParameters());
			} else {
				remoteControllers.get(previousConfigurable.getSource().getName()).saveConfigurable(previousConfigurable,
						configParamPanel.getParameters());
			}
		}
	}
}
