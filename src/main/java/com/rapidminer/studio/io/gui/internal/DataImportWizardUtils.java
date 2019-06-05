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
package com.rapidminer.studio.io.gui.internal;

import java.awt.Color;
import java.util.Locale;
import javax.swing.Icon;

import com.rapidminer.core.io.data.ColumnMetaData.ColumnType;
import com.rapidminer.core.io.data.source.DataSourceFactory;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.actions.OpenAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;


/**
 * Utility class for the {@link DataImportWizard}. It contains for example methods to translate
 * {@link DataSourceFactory} to UI messages.
 *
 * @author Nils Woehler
 * @since 7.0.0
 *
 */
public final class DataImportWizardUtils {

	/** font color for the preview lettering */
	private static final Color BACKGROUND_PREVIEW_GRAY = Color.LIGHT_GRAY;
	private static final Color BACKGROUND_PREVIEW_GRAY_WITH_ALPHA = new Color(BACKGROUND_PREVIEW_GRAY.getRed(),
			BACKGROUND_PREVIEW_GRAY.getGreen(), BACKGROUND_PREVIEW_GRAY.getBlue(), 50);

	private DataImportWizardUtils() throws IllegalAccessException {
		throw new IllegalAccessException("Utility class");
	}

	/**
	 * @return the color for the preview font displayed by various data preview tables.
	 */
	public static Color getPreviewFontColor() {
		return BACKGROUND_PREVIEW_GRAY_WITH_ALPHA;
	}

	/**
	 * Looks up the description for a data source factory.
	 *
	 * @param factory
	 *            the factory to lookup the description for
	 * @return the I18Nized description if the lookup was successful. Otherwise the message key is
	 *         returned.
	 */
	public static String getFactoryDescription(final DataSourceFactory<?> factory) {
		return I18N.getGUIMessage("gui.io.dataimport.source." + factory.getI18NKey() + ".description");
	}

	/**
	 * Looks up the label for a data source factory.
	 *
	 * @param factory
	 *            the factory to lookup the label for
	 * @return the I18Nized label if the lookup was successful. Otherwise the message key is
	 *         returned.
	 */
	public static String getFactoryLabel(final DataSourceFactory<?> factory) {
		return I18N.getGUIMessage("gui.io.dataimport.source." + factory.getI18NKey() + ".label");
	}

	/**
	 * Looks up the icon for a data source factory.
	 *
	 * @param factory
	 *            the factory to lookup the icon for
	 * @return the icon if the lookup was successful. Otherwise {@code null} is returned.
	 */
	public static Icon getFactoryIcon(final DataSourceFactory<?> factory) {
		return SwingTools
				.createIcon("24/" + I18N.getGUIMessage("gui.io.dataimport.source." + factory.getI18NKey() + ".icon"));
	}

	/**
	 * Logs an event for the "new_import" type with the {@link ActionStatisticsCollector}. Event
	 * type and value can be passed as arguments, e.g. ("datasource_selected",
	 * "local_file_datasource").
	 *
	 * @param type
	 *            the event type
	 * @param value
	 *            the value for the event type
	 */
	public static void logStats(DataWizardEventType type, String value) {
		ActionStatisticsCollector.getInstance().log(ActionStatisticsCollector.TYPE_NEW_IMPORT,
				type.toString().toLowerCase(Locale.ENGLISH), value);
	}

	/**
	 * Converts the enum entry to the associated value type string.
	 *
	 * @param type
	 *            the column type to convert
	 * @return the ontology name for the type
	 */
	public static String getNameForColumnType(ColumnType type) {
		switch (type) {
			case REAL:
				return Ontology.VALUE_TYPE_NAMES[Ontology.REAL];
			case INTEGER:
				return Ontology.VALUE_TYPE_NAMES[Ontology.INTEGER];
			case BINARY:
				return Ontology.VALUE_TYPE_NAMES[Ontology.BINOMINAL];
			case DATE:
				return Ontology.VALUE_TYPE_NAMES[Ontology.DATE];
			case DATETIME:
				return Ontology.VALUE_TYPE_NAMES[Ontology.DATE_TIME];
			case TIME:
				return Ontology.VALUE_TYPE_NAMES[Ontology.TIME];
			case CATEGORICAL:
			default:
				return Ontology.VALUE_TYPE_NAMES[Ontology.POLYNOMINAL];

		}
	}


	/**
	 * If the data import wizard should go to the results view after finishing the import, this callback supports it.
	 *
	 * @return a callback for the {@link DataImportWizard} to end up in the results highlighting the entryLocation
	 * @since 9.0.0
	 */
	public static DataImportWizardCallback showInResultsCallback() {
		return ((wizard, entryLocation) -> {
			// Select repository entry
			if (RapidMinerGUI.getMainFrame() != null) {
				RapidMinerGUI.getMainFrame().getRepositoryBrowser()
						.expandToRepositoryLocation(entryLocation);
				// Switch to result
				try {
					Entry entry = entryLocation.locateEntry();
					if (entry instanceof IOObjectEntry) {
						OpenAction.showAsResult((IOObjectEntry) entry);
					}
				} catch (RepositoryException e) {
					SwingTools.showSimpleErrorMessage(wizard.getDialog(), "cannot_open_imported_data", e);
				}
			}
		});
	}
}
