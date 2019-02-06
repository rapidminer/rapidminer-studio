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
package com.rapidminer.template;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.rapidminer.repository.RepositoryException;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.io.GlobFilenameFilter;


/**
 * Singleton entry point for registering and retrieving {@link Template}s.
 *
 * @author Simon Fischer, Gisa Schaefer
 */
public enum TemplateManager {

	INSTANCE;

	/**
	 * the folder inside .RapidMiner containing the templates
	 */
	private static final String FOLDER_TEMPLATES = "templates";

	private Map<String, Template> templatesByName = new LinkedHashMap<>();

	private TemplateManager() {
		// blank process template
		register(PreparedTemplates.BLANK_PROCESS_TEMPLATE);
		// turbo prep
		register(PreparedTemplates.TURBO_PREP_TEMPLATE);
		// auto model
		register(PreparedTemplates.AUTO_MODEL_TEMPLATE);
		// load templates from bundled resources
		registerTemplate("telco_churn_modeling");
		registerTemplate("direct_marketing");
		registerTemplate("credit_risk_modeling");
		registerTemplate("market_basket_analysis");
		registerTemplate("predictive_maintenance");
		registerTemplate("price_risk_clustering");
		registerTemplate("lift_chart");
		registerTemplate("operationalization");
		registerTemplate("anomaly_detection");
		registerTemplate("geographic_distances");

		// Load templates from .RapidMiner folder to allow sharing
		File tempDir;
		tempDir = new File(FileSystemService.getUserRapidMinerDir(), FOLDER_TEMPLATES);
		if (tempDir.exists() && tempDir.isDirectory()) {
			for (File file : tempDir.listFiles(new GlobFilenameFilter("*.template"))) {
				try {
					register(new Template(Paths.get(file.toURI())));
				} catch (IOException | RepositoryException e) {
					LogService.getRoot().log(Level.WARNING,
							"com.rapidminer.template.TemplateManager.failed_to_load_templatefile", new Object[]{file, e});
				}
			}
		}
	}

	/**
	 * Registers a template that should be loaded from the resources at
	 * template/[templateName].template. Extensions can add templates to the new process tab of the
	 * startup dialog via this method within the {@code initGui(MainFrame)} method. </br>
	 * </br>
	 * Given a file called {@code marketing.template} is present at
	 * {@code src/main/resources/com.rapidminer.resources.template/} the following snippet will
	 * register the template:
	 *
	 * <pre>
	 * {@code public static void initGui(MainFrame mainframe) {
	 * 	TemplateManager.INSTANCE.registerTemplate("marketing");
	 * }
	 * </pre>
	 *
	 * @param templateName
	 * 		the unique name of the template
	 * @see Template Description of the .template file contents
	 */
	public void registerTemplate(String templateName) {
		if (templatesByName.containsKey(templateName)) {
			LogService.getRoot().log(Level.INFO, "com.rapidminer.template.TemplateManager.template_already_registered", templateName);
			return;
		}
		try {
			register(new Template(templateName));
		} catch (IOException | RepositoryException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.template.TemplateManager.failed_to_load_template", e);
		}
	}

	private void register(Template template) {
		templatesByName.put(template.getName(), template);
	}

	/**
	 * @return all registered {@link Template}s
	 */
	public List<Template> getAllTemplates() {
		return new ArrayList<>(templatesByName.values());
	}

	public List<Template> getBlankProcessTemplates() {
		return Arrays.asList(PreparedTemplates.BLANK_PROCESS_TEMPLATE, PreparedTemplates.TURBO_PREP_TEMPLATE, PreparedTemplates.AUTO_MODEL_TEMPLATE);
	}
}
