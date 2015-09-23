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
package com.rapidminer.template;

import com.rapidminer.repository.RepositoryException;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.io.GlobFilenameFilter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;


/**
 * Singleton entry point for retrieving {@link Template}s.
 * 
 * @author Simon Fischer
 * 
 */
public class TemplateManager {

	private static final TemplateManager INSTANCE = new TemplateManager();

	private Map<String, Template> templatesByName = new LinkedHashMap<>();

	private TemplateManager() {
		try {
			register(new Template("marketing"));
			register(new Template("maintenance"));
			register(new Template("churn"));
			register(new Template("sentiment"));
		} catch (IOException | RepositoryException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.template.TemplateManager.failed_to_load_template", e);
		}
		// Load templates supplied by from folder to make debugging easier, without compiling RM
		File tempDir;
		try {
			tempDir = new File(FileSystemService.getRapidMinerHome(), "templates");
			if (tempDir.exists() && tempDir.isDirectory()) {
				for (File file : tempDir.listFiles(new GlobFilenameFilter("*.zip"))) {
					try {
						register(new Template(file));
					} catch (IOException | RepositoryException e) {
						LogService.getRoot().log(Level.WARNING,
								"com.rapidminer.template.TemplateManager.failed_to_load_template", new Object[] { file, e });
					}
				}
			}
		} catch (IOException e1) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.template.TemplateManager.cannot_find_rm_home", e1);
		}

	}

	public static TemplateManager getInstance() {
		return INSTANCE;
	}

	private void register(Template template) {
		templatesByName.put(template.getName(), template);
	}

	public List<Template> getAllTemplates() {
		return new ArrayList<>(templatesByName.values());
	}

	public Template getTemplateByName(String name) {
		return templatesByName.get(name);
	}
}
