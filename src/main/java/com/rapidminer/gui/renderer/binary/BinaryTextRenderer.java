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
package com.rapidminer.gui.renderer.binary;

import java.awt.BorderLayout;
import java.awt.Component;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.rapidminer.gui.renderer.AbstractRenderer;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.nio.file.BinaryEntryFileObject;
import com.rapidminer.report.Reportable;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.Tools;


/**
 * Renderer to display text file contents.
 *
 * @author Marco Boeck
 * @since 9.7
 */
public class BinaryTextRenderer extends AbstractRenderer {

	private static class TextComponent extends JPanel implements Reportable {

		private TextComponent() {
			super(new BorderLayout());
		}
	}


	@Override
	public String getName() {
		return I18N.getGUIMessage("gui.cards.result_view.content.title");
	}

	@Override
	public Component getVisualizationComponent(Object renderable, IOContainer ioContainer) {
		return createPanel((BinaryEntryFileObject) renderable);
	}

	@Override
	public Reportable createReportable(Object renderable, IOContainer ioContainer, int desiredWidth, int desiredHeight) {
		return createPanel((BinaryEntryFileObject) renderable);
	}

	private TextComponent createPanel(BinaryEntryFileObject binaryEntryFileObject) {
		TextComponent panel = new TextComponent();
		try {
			try (InputStream in = binaryEntryFileObject.openStream()) {
				JTextArea textArea = new JTextArea(Tools.readFirstNLines(new InputStreamReader(in, StandardCharsets.UTF_8), 10_000L, true));
				textArea.setEditable(false);
				textArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
				JScrollPane sp = new ExtendedJScrollPane(textArea);
				sp.setBorder(null);
				panel.add(sp, BorderLayout.CENTER);
			}
		} catch (OperatorException | IOException e1) {
			panel.add(new JLabel(e1.getMessage()), BorderLayout.CENTER);
		}

		return panel;
	}
}
