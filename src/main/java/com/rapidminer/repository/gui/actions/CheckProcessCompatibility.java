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
package com.rapidminer.repository.gui.actions;

import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.gui.RepositoryTree;
import com.rapidminer.repository.internal.remote.RemoteEntry;
import com.rapidminer.repository.internal.remote.RemoteRepository;
import com.rapidminer.repository.internal.remote.exception.NotYetSupportedServiceException;
import com.rapidminer.tools.I18N;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.util.List;


/**
 * This action checks if the Server could run this Process
 *
 * @author Andreas Timm
 * @since 9.5.0
 */
public class CheckProcessCompatibility extends AbstractRepositoryAction<RemoteEntry> {

    private static final long serialVersionUID = 1L;

    public CheckProcessCompatibility(RepositoryTree tree) {
        super(tree, RemoteEntry.class, false, "repository_check_process_compatibility");
    }

    @Override
    public void actionPerformed(RemoteEntry e) {
        if (!(e instanceof ProcessEntry)) {
            return;
        }
        ProgressThread pt = new ProgressThread("process_compatibility") {
            @Override
            public void run() {
                String location = e.getPath();
                try {
                    final Repository repository = e.getLocation().getRepository();
                    if (repository instanceof RemoteRepository) {
                        RemoteRepository remoteRepository = (RemoteRepository) repository;
                        final List<String> response = remoteRepository.getClient().checkProcessCompatibility(location);
                        if (response.isEmpty()) {
                            SwingUtilities.invokeLater(() -> SwingTools.showMessageDialog("process_compatible", location));
                        } else {
                            String compatibility = "<ul><li>" + String.join("</li><li>", response) + "</li></ul>";
                            SwingUtilities.invokeLater(() -> SwingTools.showLongMessage("process_incompatible", I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.process_incompatible.long_message", location, compatibility)));
                        }
                    }
                } catch (NotYetSupportedServiceException nye) {
                    SwingUtilities.invokeLater(() -> SwingTools.showMessageDialog("unsupported_service", nye.getMessage()));
                } catch (RepositoryException | IOException e1) {
                    SwingUtilities.invokeLater(() -> SwingTools.showMessageDialog("process_compatible_check_failed", e1.getMessage()));
                }
            }
        };
        pt.start();
    }

}