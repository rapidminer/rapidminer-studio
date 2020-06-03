/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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
package com.rapidminer.repository.versioned.gui;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.lang.StringUtils;

import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceActionAdapter;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.FixedWidthLabel;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.gui.RepositoryConfigurationPanel;
import com.rapidminer.repository.local.LocalRepository;
import com.rapidminer.repository.versioned.FilesystemRepositoryAdapter;
import com.rapidminer.repository.versioned.FilesystemRepositoryFactory;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.container.Pair;
import com.rapidminer.tools.encryption.EncryptionProvider;


/**
 * This configuration panel is used for filesystem repositories.
 *
 * @author Andreas Timm
 * @since 9.7
 */
public class FilesystemRepositoryConfigurationPanel extends JPanel implements RepositoryConfigurationPanel {

    private static final long serialVersionUID = 1L;
    private static final String NEW_LOCAL_REPOSITORY = "New Local Repository";
    private static final ImageIcon SUCCESS_ICON = SwingTools.createIcon("16/ok.png");
    private static final ImageIcon ERROR_ICON = SwingTools.createIcon("16/error.png");
    private static final ImageIcon UNKNOWN_ICON = SwingTools.createIcon("16/question.png");
    private static final ImageIcon LOADING_ICON = SwingTools.createIcon("16/loading.gif");

    private final JTextField pathField = new JTextField(30);
    private final JTextField aliasField = new JTextField(NEW_LOCAL_REPOSITORY, 30);
    private final JCheckBox standardLocation = new JCheckBox(new ResourceActionAdapter("repositorydialog.use_standard_location"));
    private final boolean isInEditMode;
    private final AtomicBoolean isChecking = new AtomicBoolean(false);
    private final AtomicBoolean isSuccess = new AtomicBoolean(false);

    private FixedWidthLabel errorLabel = new FixedWidthLabel(200, "");
    private JButton chooseFileButton;

    /** The optional finish button will be disabled when invalid information is entered. */
    private JButton finishButton;


    public FilesystemRepositoryConfigurationPanel(boolean isNew) {
        this.isInEditMode = !isNew;

        standardLocation.setSelected(isNew);
        errorLabel.setForeground(Color.red);

        GridBagLayout gbl = new GridBagLayout();
        setLayout(gbl);
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.weighty = 0;
        c.weightx = .5;
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;

        // ALIAS
        c.gridwidth = 1;
        JLabel label = new ResourceLabel("repositorydialog.alias");
        label.setLabelFor(aliasField);
        gbl.setConstraints(label, c);
        add(label);

        c.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(aliasField, c);
        add(aliasField);

        // Path
        c.gridwidth = 1;
        c.gridheight = 2;
        label = new ResourceLabel("repositorydialog.root_directory");
        label.setLabelFor(pathField);
        gbl.setConstraints(label, c);
        add(label);

        if (isNew) {
            c.gridheight = 1;
            c.gridwidth = GridBagConstraints.REMAINDER;
            add(standardLocation, c);
        }
        c.gridwidth = GridBagConstraints.RELATIVE;
        gbl.setConstraints(pathField, c);
        add(pathField);

        c.gridwidth = GridBagConstraints.REMAINDER;
        chooseFileButton = new JButton(new ResourceAction(true, "choose_file") {

            private static final long serialVersionUID = 1L;

            @Override
            public void loggedActionPerformed(ActionEvent e) {
                Path path = SwingTools.chooseFile(FilesystemRepositoryConfigurationPanel.this, null, true, true, (String) null, null).toPath();
                pathField.setText(path.toString());
            }
        });
        add(chooseFileButton, c);

        c.gridwidth = 1;
        JLabel placeholder = new JLabel();
        gbl.setConstraints(placeholder, c);
        add(placeholder);
        c.gridwidth = GridBagConstraints.REMAINDER;
        add(errorLabel, c);

        JPanel dummy = new JPanel();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        gbl.setConstraints(dummy, c);
        add(dummy);

        aliasField.selectAll();
        pathField.selectAll();

        aliasField.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void removeUpdate(DocumentEvent e) {
                aliasUpdated();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                aliasUpdated();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                aliasUpdated();
            }
        });
        pathField.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void removeUpdate(DocumentEvent e) {
                dumbSettingsCheck();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                dumbSettingsCheck();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                dumbSettingsCheck();
            }
        });
        standardLocation.addActionListener(e -> {
            enableFileField();
            aliasUpdated();
        });
        enableFileField();
        aliasUpdated();
    }

    @Override
    public void makeRepository() throws RepositoryException {
        // do a last simple validation
        Pair<String, Object[]> error = checkSettings();
        if (error != null) {
            isSuccess.set(false);
            isChecking.set(false);
            updateErrorLabel(I18N.getGUIMessage("gui.dialog.repositorydialog.error." + error.getFirst(), error.getSecond()));
            throw new RepositoryException("Field validation failed!");
        }

        // try to create storage path
        Path path = Paths.get(getPath());
        if (!Files.exists(path)) {
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                throw new RepositoryException("Cannot create repository folder", e);
            }
        }
        String alias = getAlias();

        // no error so far, now try to actually create the repository
        FilesystemRepositoryFactory.createRepository(alias, path, EncryptionProvider.DEFAULT_CONTEXT);
    }

    @Override
    public void configureUIElementsFrom(Repository repository) {
        isChecking.set(true);
        pathField.setText(((FilesystemRepositoryAdapter) repository).getRoot().toString());
        aliasField.setText(repository.getName());
        isChecking.set(false);

        dumbSettingsCheck();
    }

    @Override
    public boolean configure(Repository repository) {
        Path path = Paths.get(getPath());
        String alias = getAlias();
        try {
            // in case only the alias has changed the file stays the same
            if (((FilesystemRepositoryAdapter) repository).getRoot().equals((path))) {
                path = null;
            }
            if (repository.getName().equals(alias)) {
                alias = null;
            }
            FilesystemRepositoryFactory.checkConfiguration(path, alias);
            if (alias != null) {
                repository.rename(alias);
            }
        } catch (RepositoryException e) {
            SwingTools.showSimpleErrorMessage("cannot_create_repository", e);
            return false;
        }
        return true;
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

    @Override
    public void setOkButton(JButton okButton) {
        this.finishButton = okButton;
    }

    @Override
    public List<AbstractButton> getAdditionalButtons() {
        return new LinkedList<>();
    }

    private String getAlias() {
        return StringUtils.trimToEmpty(aliasField.getText());
    }

    private String getPath() {
        return StringUtils.trimToEmpty(pathField.getText());
    }

    /**
     * Sets the folder based on the alias.
     */
    private void aliasUpdated() {
        if (standardLocation.isSelected() && !isInEditMode) {
            pathField.setText(LocalRepository.getDefaultRepositoryFolder(getAlias()).toString());
        }
        dumbSettingsCheck();
    }

    /** Enables or disable the file field iff {@link #standardLocation} is deselected. */
    private void enableFileField() {
        boolean enabled = !standardLocation.isSelected() && !isInEditMode;
        pathField.setEditable(enabled);
        chooseFileButton.setEnabled(enabled);
        dumbSettingsCheck();
    }

    private void dumbSettingsCheck() {
        if (isChecking.compareAndSet(false, true)) {
            isSuccess.set(false);
            Pair<String, Object[]> error = checkSettings();
            if (error == null) {
                isSuccess.set(true);
                isChecking.set(false);
                updateErrorLabel("");
                if (finishButton != null) {
                    finishButton.setEnabled(true);
                }
            } else {
                isSuccess.set(false);
                isChecking.set(false);
                updateErrorLabel(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.repositorydialog.error." + error.getFirst(), error.getSecond()));
                if (finishButton != null) {
                    finishButton.setEnabled(false);
                }
            }
            isChecking.set(false);
        }
    }

    private void updateErrorLabel(String text) {
        if (StringUtils.stripToNull(text) == null) {
            if (isChecking.get()) {
                errorLabel.setIcon(LOADING_ICON);
            } else {
                if (isSuccess.get()) {
                    errorLabel.setIcon(SUCCESS_ICON);
                } else {
                    errorLabel.setIcon(UNKNOWN_ICON);
                }
            }
            errorLabel.setText("");
        } else {
            if (isChecking.get()) {
                errorLabel.setIcon(ERROR_ICON);
            } else {
                if (isSuccess.get()) {
                    errorLabel.setIcon(SUCCESS_ICON);
                } else {
                    errorLabel.setIcon(ERROR_ICON);
                }
            }
            errorLabel.setText(text);
        }
    }

    /** Checks the current settings and returns an I18N key and parameters if settings are incorrect. */
    private Pair<String, Object[]> checkSettings() {
        if (getAlias().isEmpty()) {
            return new Pair<>("alias_cannot_be_empty", new Object[0]);
        }
        if (!RepositoryLocation.isNameValid(getAlias())) {
            return new Pair<>("alias_invalid", new Object[]{ RepositoryLocation.getIllegalCharacterInName(getAlias()) });
        }
        if (getPath().isEmpty()) {
            return new Pair<>("folder_cannot_be_empty", new Object[0]);
        }
        Path path = Paths.get(getPath());
        if (Files.exists(path) && !Files.isDirectory(path)) {
            return new Pair<>("root_is_not_a_directory", new Object[0]);
        }
        if (Files.exists(path) && !Files.isWritable(path)) {
            return new Pair<>("root_is_not_writable", new Object[0]);
        }

        while (!Files.exists(path)) {
            path = path.getParent();
            if (path == null) {
                return new Pair<>("cannot_determine_root", new Object[0]);
            }
        }
        if (!Files.isWritable(path)) {
            return new Pair<>("cannot_create_root_folder", new Object[0]);
        }
        return null;
    }

}
