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
package com.rapidminer.studio.io.data.internal.file.binary;

import com.rapidminer.gui.look.Colors;
import com.rapidminer.repository.RepositoryTools;
import com.rapidminer.repository.gui.RepositoryLocationChooser;


/**
 * {@link RepositoryLocationChooser} that allows to specify an additional media or MIME type.
 *
 * @author Michael Knopf
 */
class BinaryImportDestinationChooser extends RepositoryLocationChooser {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new {@link RepositoryLocationChooser} that allows to specify a media or MIME type
     * for the given data source.
     *
     * @param initialDestination the initial location (optional)
     */
    public BinaryImportDestinationChooser(String initialDestination) {
        super(null, null, initialDestination, true, false, true, true, Colors.WHITE, RepositoryTools.ONLY_REPOSITORIES_WITH_BINARY_ENTRY_SUPPORT);
    }
}
