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
package com.rapidminer.gui.actions.export;

import com.rapidminer.gui.tools.components.ButtonBarCardPanel;
import com.rapidminer.operator.IOObject;
import com.rapidminer.tools.I18N;

import javax.swing.JPanel;


/**
 * An abstract {@link JPanel} which displays an {@link IOObject} in a {@link ButtonBarCardPanel} and
 * can be printed/exported.
 * 
 * @author Nils Woehler
 * 
 */
public abstract class AbstractPrintableIOObjectPanel extends JPanel implements PrintableComponent {

	private static final long serialVersionUID = 1L;

	private final String source;
	private final String i18nKey;

	public AbstractPrintableIOObjectPanel(IOObject ioObject, String i18NKey) {
		i18nKey = i18NKey;
		source = ioObject.getSource();
	}

	@Override
	public String getExportName() {
		return I18N.getMessage(I18N.getGUIBundle(), "gui.cards.result_view." + i18nKey + ".title");
	}

	@Override
	public String getIdentifier() {
		return source;
	}

	@Override
	public String getExportIconName() {
		return I18N.getGUIMessage("gui.cards.result_view." + i18nKey + ".icon");
	}

}
