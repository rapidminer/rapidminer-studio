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
package com.rapidminer.studio.io.data.internal.file.binary;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.io.IOException;

import javax.swing.JLabel;
import javax.swing.JTextArea;

import org.apache.tika.Tika;

import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.repository.gui.RepositoryLocationChooser;


/**
 * {@link RepositoryLocationChooser} that allows to specify an additional media or MIME type.
 *
 * @author Michael Knopf
 */
class BinaryImportDestinationChooser extends RepositoryLocationChooser {

	private static final long serialVersionUID = 1L;

	private JTextArea mediaType;

	/**
	 * Creates a new {@link RepositoryLocationChooser} that allows to specify a media or MIME type
	 * for the given data source.
	 *
	 * @param source
	 *            the data source
	 * @param initialDestination
	 *            the initial location (optional)
	 */
	public BinaryImportDestinationChooser(BinaryDataSource source, String initialDestination) {
		super(null, null, initialDestination, true, false, true, true, Colors.WHITE);

		// Use generic mime type as default and try to guess more specific.
		String type = "application/octet-stream";
		Tika tika = new Tika();
		try {
			type = tika.detect(source.getLocation());
		} catch (IOException e) {
			// ignore
		}

		JLabel mediaTypelabel = new ResourceLabel("repository_chooser.mime_type");
		mediaType = new JTextArea(type);

		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(ButtonDialog.GAP, 0, 0, ButtonDialog.GAP);
		c.gridwidth = GridBagConstraints.RELATIVE;
		add(mediaTypelabel, c);

		c.insets = new Insets(ButtonDialog.GAP, 0, 0, 0);
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.HORIZONTAL;
		add(mediaType, c);
	}

	public String getMediaType() {
		return mediaType.getText().trim();
	}

}
