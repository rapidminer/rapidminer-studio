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

import com.rapidminer.Process;
import com.rapidminer.RepositoryProcessLocation;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.gui.renderer.Renderer;
import com.rapidminer.gui.renderer.RendererService;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.io.AbstractReader;
import com.rapidminer.operator.nio.CSVExampleSource;
import com.rapidminer.operator.nio.model.CSVResultSet;
import com.rapidminer.operator.preprocessing.sampling.StratifiedSamplingOperator;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.report.Renderable;
import com.rapidminer.report.Reportable;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.local.LocalRepository;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.XMLException;
import com.rapidminer.tools.container.Pair;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;

import javax.imageio.ImageIO;


/**
 * Used to control the state of a {@link Template}. All settings are stored in the
 * {@link TemplateState} which is accessible via {@link #getModel()}.
 * 
 * @author Simon Fischer
 * 
 */
public class TemplateController {

	public static final String RESULT_RENDERER_NAME_TEXT = "Text";

	public static final int MAX_RECOMMENDED_DATA_SIZE = 500;

	private TemplateState state = new TemplateState();

	private Process currentProcess;

	private final Object processLock = new Object();

	public static final String RESULT_RENDERER_NAME_TABLE = "Table";

	public static final String RESULT_PROPERTY_TITLE = "title";

	public static final String RESULT_PROPERTY_DESCRIPTION = "description";

	public static final String RESULTS_PROPERTY_RENDERER = "renderer_name";

	public static final String RESULT_PROPERTY_INDEX = "result_index";

	public static final String RESULT_PROPERTY_TOOLTIP = "tooltip";

	public static final String RESULT_PROPERTY_LINKED_OPERATOR = "linked_operator";

	public TemplateState getModel() {
		return state;
	}

	public void setTemplate(Template template) {
		state.setTemplate(template);
		setInput(null);
		state.setResults(null);
		state.clearMacros();
		if (template != null) {
			ActionStatisticsCollector.getInstance().log(ActionStatisticsCollector.TYPE_TEMPLATE, template.getName(),
					"selected");
		}
	}

	/**
	 * Assigns the input data set for the {@link Template}. This method should be called whenever
	 * the user specifies an input data set.
	 */
	public void setInput(ExampleSet exampleSet) {
		if (this.state.getInputData() != exampleSet) {
			if (exampleSet != null) {
				exampleSet.recalculateAllAttributeStatistics(); // needed for distribution preview
																// plot
			}
			this.state.setInputData(exampleSet);
			guessRoles();
		}
	}

	/**
	 * Assigns a role for a {@link RoleRequirement} specified by the {@link Template}. This method
	 * should be called as soon as the user makes an assignment.
	 */
	public void assignRole(RoleRequirement req, String attributeName, String positiveClass) {
		if (attributeName == null) {
			assignRole(req, null);
		} else {
			if (positiveClass == null) {
				positiveClass = guessPositiveClass(req, attributeName);
			}
			assignRole(req, new RoleAssignment(attributeName, positiveClass));
		}
	}

	private String guessPositiveClass(RoleRequirement req, String attributeName) {
		if (!Ontology.ATTRIBUTE_VALUE_TYPE.isA(req.getValueType(), Ontology.NOMINAL)) {
			return null;
		}
		ExampleSet exampleSet = getModel().getInputData();
		Attribute att = exampleSet.getAttributes().get(attributeName);
		if (att == null) {
			return null;
		}
		int leastIndex = (int) exampleSet.getStatistics(att, Statistics.LEAST);
		String leastValue = att.getMapping().mapIndex(leastIndex);
		return leastValue;
	}

	public void assignRole(RoleRequirement req, RoleAssignment assignment) {
		state.assignRole(req.getRoleName(), assignment);
	}

	/**
	 * Tries to import the file. Returns an ExampleSet if successful or null if not. Does not set
	 * {@link TemplateState#getInputData()}. DONE: Run in background
	 */
	public ExampleSet importFile(File file) {
		URI fileURI = file.toURI();
		URL fileURL;
		try {
			fileURL = fileURI.toURL();
		} catch (MalformedURLException e) {
			throw new RuntimeException("Failed to create URI for file " + file, e);
		}
		boolean exists = Files.exists(Paths.get(fileURI));
		if (exists && AbstractReader.canMakeReaderFor(fileURL)) {
			AbstractReader reader = null;
			try {
				reader = AbstractReader.createReader(fileURI);
				if (reader instanceof CSVExampleSource) {
					CSVExampleSource csvExampleSource = (CSVExampleSource) reader;
					String guessedColumnSeperator = CSVResultSet.guessColumnSeperator(csvExampleSource
							.getParameterAsString(CSVExampleSource.PARAMETER_CSV_FILE));
					csvExampleSource.setParameter(CSVExampleSource.PARAMETER_COLUMN_SEPARATORS, guessedColumnSeperator);
				}
			} catch (OperatorCreationException | UndefinedParameterError e) {
				LogService.getRoot().log(
						Level.WARNING,
						I18N.getMessage(LogService.getRoot().getResourceBundle(),
								"com.rapidminer.template.TemplateController.reader_creation_failed", fileURL.getFile(), e),
						e);
				return null;
			}
			IOObject result;
			try {
				result = reader.read();
				if (state.getTemplate() != null) {
					ActionStatisticsCollector.getInstance().log(ActionStatisticsCollector.TYPE_TEMPLATE,
							state.getTemplate().getName(), "import_successful");
				}
			} catch (OperatorException e) {
				LogService.getRoot().log(Level.WARNING, "Failed to read " + file + ": " + e, e);
				if (state.getTemplate() != null) {
					ActionStatisticsCollector.getInstance().log(ActionStatisticsCollector.TYPE_TEMPLATE,
							state.getTemplate().getName(), "import_unsuccessful");
				}
				return null;
			}
			if (result instanceof ExampleSet) {
				return (ExampleSet) result;
			}
		}
		return null;
	}

	/**
	 * Runs the process using the made settings. Note that this method can block and should
	 * therefore called from outside the EDT.
	 * 
	 * @throws OperatorCreationException
	 */
	public void run() throws OperatorException, OperatorCreationException {
		if (state.getTemplate() != null) {
			ActionStatisticsCollector.getInstance().log(ActionStatisticsCollector.TYPE_TEMPLATE,
					state.getTemplate().getName(), "run");
		}
		ExampleSet input = prepareInput();
		Process process = this.state.getTemplate().makeProcess();
		try {
			synchronized (processLock) {
				this.currentProcess = process;
			}
			for (RoleRequirement req : state.getTemplate().getRoleRequirements()) {
				RoleAssignment roleAssignment = state.getRoleAssignment(req.getRoleName());
				if ((roleAssignment != null) && (roleAssignment.getPositiveClass() != null)) {
					process.getContext().addMacro(new Pair<>(req.getRoleName(), roleAssignment.getAttributeName()));
					process.getContext().addMacro(
							new Pair<>(req.getRoleName() + "_positive_class", roleAssignment.getPositiveClass()));
					Attribute att = input.getAttributes().get(roleAssignment.getAttributeName());
					String negativeClass = null;
					for (String other : att.getMapping().getValues()) {
						if (!roleAssignment.getPositiveClass().equals(other)) {
							negativeClass = other;
							break;
						}
					}
					if (negativeClass != null) {
						process.getContext().addMacro(new Pair<>(req.getRoleName() + "_negative_class", negativeClass));
					}
				}
			}
			IOContainer results = process.run(new IOContainer(input));
			state.clearMacros();
			Iterator<String> i = process.getMacroHandler().getDefinedMacroNames();
			while (i.hasNext()) {
				String key = i.next();
				state.setMacro(key, process.getMacroHandler().getMacro(key));
			}
			state.setResults(results.getIOObjects());
			if (state.getTemplate() != null) {
				ActionStatisticsCollector.getInstance().log(ActionStatisticsCollector.TYPE_TEMPLATE,
						state.getTemplate().getName(), "run_success");
			}
		} finally {
			currentProcess = null;
		}
	}

	/**
	 * Assigns roles to {@link #state.inputData} according to the {@link #state.roleAssignments}.
	 * 
	 * @throws OperatorCreationException
	 * @throws OperatorException
	 */
	private ExampleSet prepareInput() throws OperatorCreationException, OperatorException {
		ExampleSet input = getInputWithAssignedRoles();
		if (state.isDownsamplingEnabled() && input.size() > TemplateController.MAX_RECOMMENDED_DATA_SIZE) {
			StratifiedSamplingOperator samplingOperator = OperatorService.createOperator(StratifiedSamplingOperator.class);
			samplingOperator.setParameter(StratifiedSamplingOperator.PARAMETER_SAMPLE,
					StratifiedSamplingOperator.SAMPLE_MODES[StratifiedSamplingOperator.SAMPLE_ABSOLUTE]);
			samplingOperator.setParameter(StratifiedSamplingOperator.PARAMETER_SAMPLE_SIZE,
					String.valueOf(TemplateController.MAX_RECOMMENDED_DATA_SIZE));
			samplingOperator.setParameter(RandomGenerator.PARAMETER_USE_LOCAL_RANDOM_SEED, String.valueOf(true));
			input = samplingOperator.apply(input);
		}
		return input;
	}

	/**
	 * @return Same as the {@link TemplateState#getInputData()}, but with the role assignments
	 *         already applied.
	 */
	public ExampleSet getInputWithAssignedRoles() {
		if (state.getInputData() == null) {
			return null;
		}
		ExampleSet input = (ExampleSet) state.getInputData().clone();
		for (RoleRequirement req : state.getTemplate().getRoleRequirements()) {
			RoleAssignment roleAssignment = state.getRoleAssignment(req.getRoleName());
			if (roleAssignment != null) {
				String assignedAttribute = roleAssignment.getAttributeName(); // cannot be null
				Attribute att = input.getAttributes().get(assignedAttribute);
				Attribute oldSpecial = input.getAttributes().getSpecial(req.getRoleName());
				if (att != oldSpecial) {
					input.getAttributes().setSpecialAttribute(att, req.getRoleName());
					if (oldSpecial != null) {
						input.getAttributes().addRegular(oldSpecial);
					}
				}
			}
		}
		return input;
	}

	/** Guesses attributes for all {@link Template#getRoleRequirements()}. */
	private void guessRoles() {
		if (state.getInputData() == null) {
			return;
		}
		state.clearRoleAssignments();
		for (RoleRequirement req : state.getTemplate().getRoleRequirements()) {
			RoleAssignment guessedAtt = guessRole(req, state.getInputData());
			if (guessedAtt != null) {
				assignRole(req, guessedAtt);
			}
		}
	}

	/**
	 * Currently only checks whether the {@link RoleRequirement} role name is found in the
	 * {@link ExampleSet} either as attribute name or role name. If that does not work, takes the
	 * attribute with the largest skew (ratio between most and least frequent nominal value.
	 */
	private RoleAssignment guessRole(RoleRequirement req, ExampleSet exampleSet) {
		Iterator<Attribute> allAttributes = exampleSet.getAttributes().allAttributes();
		if (req.getRoleName() != null) {
			Iterator<AttributeRole> i = exampleSet.getAttributes().allAttributeRoles();
			while (i.hasNext()) {
				AttributeRole role = i.next();
				if (req.getRoleName().equalsIgnoreCase(role.getSpecialName()) && isCompatible(role.getAttribute(), req)
						&& !isAttributeAssigned(role.getAttribute())) {
					// do not assign role twice
					if (!isAttributeAssigned(role.getAttribute())) {
						return makeRoleAssignment(req, role.getAttribute());
					}
				}
			}
			Iterator<Attribute> j = allAttributes;
			double minSkew = Double.MAX_VALUE;
			Attribute skewedAtt = null;

			String rarestValue = null;
			while (j.hasNext()) {
				Attribute att = j.next();
				// do not assign role twice or use incompatible type
				if (isAttributeAssigned(att) || !isCompatible(att, req)) {
					continue;
				}
				if (att.getName().equalsIgnoreCase(req.getRoleName())) {
					return makeRoleAssignment(req, att);
				} else {
					// set binominal attribute with largest skew as label
					if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(att.getValueType(), Ontology.NOMINAL)) {

						int modeIndex = (int) exampleSet.getStatistics(att, Statistics.MODE);
						String modeValue = att.getMapping().mapIndex(modeIndex);
						double mode = exampleSet.getStatistics(att, Statistics.COUNT, modeValue);

						int leastIndex = (int) exampleSet.getStatistics(att, Statistics.LEAST);
						String leastValue = att.getMapping().mapIndex(leastIndex);
						double least = exampleSet.getStatistics(att, Statistics.COUNT, leastValue);

						double skewness = least / mode;

						if (skewness < minSkew) {
							minSkew = skewness;
							skewedAtt = att;
							rarestValue = att.getMapping().mapIndex(leastIndex);
						}
					}
				}
			}

			if (skewedAtt != null) {
				if (!isAttributeAssigned(skewedAtt)) {
					return new RoleAssignment(skewedAtt.getName(), rarestValue);
				}
			}
			return null;
		}
		return null;
	}

	/** Uses first value as positive class. */
	public RoleAssignment makeRoleAssignment(RoleRequirement requirement, Attribute att) {
		String positiveClass = null;
		if (att.isNominal() && att.getMapping().size() > 0) {
			for (String value : att.getMapping().getValues()) {
				if ("positive".equals(value.toLowerCase()) || "true".equals(value.toLowerCase())
						|| "yes".equals(value.toLowerCase())) {
					positiveClass = value;
					break;
				}
			}
			if (positiveClass == null) {
				double least = state.getInputData().getStatistics(att, Statistics.LEAST);
				positiveClass = att.getMapping().mapIndex((int) least);
			}
		}
		return new RoleAssignment(att.getName(), positiveClass);
	}

	/**
	 * Returns true if that attribute from {@link #state.inputData} is already assigned a role by
	 * {@link #state.roleAssignments}.
	 */
	private boolean isAttributeAssigned(Attribute att) {
		for (RoleRequirement req : state.getTemplate().getRoleRequirements()) {
			RoleAssignment roleAssignment = state.getRoleAssignment(req.getRoleName());
			if (roleAssignment != null) {
				if (att.getName().equals(roleAssignment.getAttributeName())) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Utility method to replace macros in a string.
	 * 
	 * @return
	 */
	public static String expandMacros(String line, TemplateState state) {
		int startIndex = line.indexOf("%{");
		StringBuffer result = new StringBuffer();
		while (startIndex >= 0) {
			result.append(line.substring(0, startIndex));
			int endIndex = line.indexOf("}", startIndex + 2);
			String key = line.substring(startIndex + 2, endIndex);
			String macroValue = state.getMacro(key);
			if (macroValue != null) {
				result.append(macroValue);
			} else {
				result.append("%{" + key + "}");
			}
			line = line.substring(endIndex + 1);
			startIndex = line.indexOf("%{");
		}
		result.append(line);
		return result.toString();
	}

	public boolean isRoleAssignmentComplete() {
		ExampleSet input = state.getInputData();
		if (input == null) {
			return false;
		}
		for (RoleRequirement requirement : state.getTemplate().getRoleRequirements()) {
			RoleAssignment assignment = state.getRoleAssignment(requirement.getRoleName());
			if (assignment == null) {
				return false;
			}
			if (input.getAttributes().get(assignment.getAttributeName()) == null) {
				return false;
			}
		}
		return true;
	}

	@SuppressWarnings("resource")
	// Needed for copyStreamSynchronously which leads to incorrect unclosed stream warning
	public void exportHTML(File target) throws IOException, RepositoryException, OperatorCreationException,
			OperatorException {
		String dirPrefix = target.getName() + "-files";
		// Folder to place files in
		File filesFolder = new File(target.getParentFile(), dirPrefix);
		filesFolder.mkdir();
		PrintWriter htmlOut = new PrintWriter(new FileWriter(target));
		try {
			htmlOut.printf(
					"<!DOCTYPE html>\n"
							+ "<html><head><title>%s</title>"
							+ "<meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\">"
							+ "<link href='http://fonts.googleapis.com/css?family=Open+Sans:300italic,400italic,600italic,700italic,800italic,400,300,600,700,800' rel='stylesheet' type='text/css'>"
							+ "<link href=\"%s/template.css\" rel=\"stylesheet\" type=\"text/css\" />" +
							// DONT REMOVE COMMENTED CODE UNTIL WE KNOW WHETHER WE WILL USE JQUERY
							// IN THE HTML EXPORT
							// "<script src=\"jquery-1.7.2.min.js\" type=\"text/javascript\"></script>"
							// +
							// "<link href=\"tmp/jquery/css/ui-lightness/jquery-ui-1.10.3.custom.css\" rel=\"stylesheet\">"+
							// "<script src=\"tmp/jquery/js/jquery-1.9.1.js\"></script>"+
							// "<script src=\"tmp/jquery/js/jquery-ui-1.10.3.custom.js\"></script>"+
							"<script src=\"%s/template.js\" type=\"text/javascript\"></script>" +

							"</head><body>" + "<div>" + "<h1>%s</h1>", state.getTemplate().getTitle(), dirPrefix, dirPrefix,
					state.getTemplate().getTitle());

			// htmlOut.printf(
			// "<div id=\"help-text-wrapper\">" +
			// "<div class=\"help-text\" id=\"template-description-general\">%s</div>" +
			// "<div class=\"help-text\" id=\"template-description-results\">%s</div>" +
			// "</div>",
			// relativURLs(state.getTemplate().getHelpText(Step.TEMPLATE)),
			// relativURLs(state.getTemplate().getHelpText(Step.RESULTS))
			// );

			htmlOut.printf("<div id=\"main-wrapper\"><div id=\"tabs\"><ul>"
					+ "<li><a href=\"#input-data\">Input Data</a></li>"
					+ "<li><a href=\"#results\">Analytical Results</a></li>" + "</ul>");
			// <span class=\"help-button\" id=\"input-help\">?</span>
			htmlOut.printf("<div id=\"input-data\" class=\"section input-data\"><h2>Input Data</h2>");
			writeAsHTML(prepareInput(), htmlOut);
			htmlOut.printf("</div>");

			// <span class=\"help-button\" id=\"results-help\">?</span>
			htmlOut.printf("<div id=\"results\" class=\"section analytical-results\"><h2>Analytical Results</h2>");
			IOObject[] results = state.getResults();
			int resultIndex = 0;
			for (Properties resultPlotterSettings : state.getTemplate().getResultPlotterSettings()) {
				htmlOut.print("<div class=\"result-wrapper\">");
				resultIndex++;
				String indexString = resultPlotterSettings.getProperty(TemplateController.RESULT_PROPERTY_INDEX);
				int index;
				try {
					index = Integer.parseInt(indexString);
				} catch (NumberFormatException e) {
					continue;
				}
				if (index > results.length) {
					continue;
				}
				final IOObject ioo = results[index - 1];
				String reportableType = RendererService.getName(ioo.getClass());
				String rendererName = resultPlotterSettings.getProperty(TemplateController.RESULTS_PROPERTY_RENDERER);

				if (resultPlotterSettings.containsKey(RESULT_PROPERTY_TITLE)) {
					htmlOut.printf("<h3 class=\"result-title\">%s</h3>",
							Tools.escapeHTML(resultPlotterSettings.getProperty(RESULT_PROPERTY_TITLE)));
				}
				if (RESULT_RENDERER_NAME_TEXT.equals(rendererName)) {
					htmlOut.printf("<div class=\"result-text\">%s</div>",
							expandMacros(resultPlotterSettings.getProperty("text"), state));
				} else if (RESULT_RENDERER_NAME_TABLE.equals(rendererName)) {
					ExampleSet eSet = (ExampleSet) ioo;
					writeAsHTML(eSet, htmlOut);
				} else {
					List<Renderer> renderers = RendererService.getRenderers(reportableType);
					Renderer selectedRenderer = null;
					for (Renderer renderer : renderers) {
						if (renderer.getName().equals(rendererName)) {
							selectedRenderer = renderer;
							break;
						}
					}
					if (selectedRenderer == null) {
						LogService.getRoot().log(Level.WARNING, "Runknown renderer: " + rendererName);
						continue;
					}

					// set parameters of renderer
					for (Entry<Object, Object> prop : resultPlotterSettings.entrySet()) {
						selectedRenderer.getParameters().setParameter(prop.getKey().toString(), prop.getValue().toString());
					}

					// write report
					int width = 600;
					int height = 400;
					Reportable reportable = selectedRenderer.createReportable(ioo, new IOContainer(), width, height);
					if (reportable instanceof Renderable) {
						Renderable renderable = (Renderable) reportable;
						renderable.prepareRendering();
						BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
						renderable.render(img.getGraphics(), width, height);
						renderable.finishRendering();
						String imageName = target.getName() + "-result-" + resultIndex + ".png";
						FileOutputStream imgOut = new FileOutputStream(new File(filesFolder, imageName));
						try {
							ImageIO.write(img, "png", imgOut);
						} finally {
							imgOut.close();
						}
						htmlOut.printf("<div class=\"chart-wrapper\"><img class=\"result-chart\" src=\"%s\"/></div>",
								dirPrefix + "/" + imageName);
					}
				}

				if (resultPlotterSettings.containsKey(RESULT_PROPERTY_DESCRIPTION)) {
					htmlOut.printf("<div class=\"result-caption\">%s</div>",
							Tools.escapeHTML(resultPlotterSettings.getProperty(RESULT_PROPERTY_DESCRIPTION)));
				}
				htmlOut.print("</div>");
			}
			htmlOut.printf("</div></div></div>"); // </div>"); // end analytical results, tabs,
													// main-wrapper

			htmlOut.printf("</div></body></html>");
			// Copy template.css to output location
			Tools.copyStreamSynchronously(Tools.getResourceInputStream("template/template.css"), new FileOutputStream(
					new File(filesFolder, "template.css")), true);
			Tools.copyStreamSynchronously(Tools.getResourceInputStream("template/template.js"), new FileOutputStream(
					new File(filesFolder, "template.js")), true);
			for (String resource : state.getTemplate().getResourceNames()) {
				Tools.copyStreamSynchronously(state.getTemplate().getResource(resource), new FileOutputStream(new File(
						filesFolder, resource)), true);
			}
		} finally {
			htmlOut.close();
		}
	}

	private void writeAsHTML(ExampleSet eSet, PrintWriter htmlOut) {
		htmlOut.print("<table class=\"result-table\"><thead><tr>");
		Iterator<AttributeRole> i = eSet.getAttributes().allAttributeRoles();
		while (i.hasNext()) {
			AttributeRole role = i.next();
			htmlOut.printf("<th class=\"%s\">%s</th>",
					role.getSpecialName() == null ? "regular" : "special role-" + role.getSpecialName(), role.getAttribute()
							.getName());
		}
		htmlOut.print("</tr></thead><tbody>");
		for (Example example : eSet) {
			htmlOut.print("<tr>");
			i = eSet.getAttributes().allAttributeRoles();
			while (i.hasNext()) {
				AttributeRole role = i.next();
				htmlOut.printf("<td class=\"%s\">%s</td>", role.getSpecialName() == null ? "regular" : "special role-"
						+ role.getSpecialName(), Tools.escapeHTML(example.getValueAsString(role.getAttribute())));
			}
			htmlOut.print("</tr>");
		}
		htmlOut.print("</tbody></table>");
	}

	private final DateFormat REPOSITORY_LOCATION_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hhmm");

	/**
	 * Saves the template as a project in the "Templates" repository. If that does not exist, adds
	 * it.
	 * 
	 * @return The saved process, with repository locations correctly assigned.
	 */
	public Process save(ProgressListener listener) throws RepositoryException, MalformedRepositoryLocationException,
			IOException, XMLException {
		RepositoryManager repoManager = RepositoryManager.getInstance(null);
		boolean found = false;
		for (Repository repo : repoManager.getRepositories()) {
			if ("Template".equals(repo.getName())) {
				found = true;
				break;
			}
		}
		if (!found) {
			repoManager.addRepository(new LocalRepository("Template"));
		}

		RepositoryLocation folderLoc = new RepositoryLocation(String.format("//Template/%s/%s", state.getTemplate()
				.getTitle(), REPOSITORY_LOCATION_DATE_FORMAT.format(state.getCreationTimestamp())));
		folderLoc.createFoldersRecursively();

		Process process = state.getTemplate().makeProcess();
		String inputName = String.format("input-%s", REPOSITORY_LOCATION_DATE_FORMAT.format(state.getDataTimestamp()));
		process.getContext().setInputRepositoryLocation(0, inputName);

		RepositoryLocation procRepoLoc = new RepositoryLocation(folderLoc, "process");
		RepositoryProcessLocation procLoc = new RepositoryProcessLocation(procRepoLoc);
		process.setProcessLocation(procLoc);
		process.save();

		RepositoryLocation inputLoc = new RepositoryLocation(folderLoc, inputName);
		repoManager.store(getInputWithAssignedRoles(), inputLoc, null, listener);

		return process;
	}

	/** Stops the last process started by {@link #run()} */
	public void stop() {
		synchronized (processLock) {
			if (currentProcess != null) {
				currentProcess.stop();
			}
		}
	}

	/**
	 * @return the compatible, alphabetically sorted, attribute names in the input data for this
	 *         role requirement.
	 */
	public SortedSet<String> getCompatibleAttributes(RoleRequirement requirement) {
		ExampleSet input = getModel().getInputData();
		if (input == null) {
			return new TreeSet<>();
		}
		SortedSet<String> sortedNames = new TreeSet<>();
		Iterator<Attribute> i = input.getAttributes().allAttributes();
		while (i.hasNext()) {
			Attribute att = i.next();
			if (isCompatible(att, requirement)) {
				sortedNames.add(att.getName());
			}
		}
		return sortedNames;
	}

	/**
	 * True iff the attribute's value type matches the one defined in the RoleRequirement. Differs
	 * by simply calling {@link Ontology#isA(int, int)} in that an attribute counts as binominal if
	 * it has a nominal mapping of size 2, even if not marked explicitly as binominal (but only
	 * nominal).
	 */
	public static boolean isCompatible(Attribute att, RoleRequirement req) {
		if (req.getValueType() == Ontology.BINOMINAL) {
			return att.isNominal() && att.getMapping().size() <= 2;
		} else {
			return Ontology.ATTRIBUTE_VALUE_TYPE.isA(att.getValueType(), req.getValueType());
		}
	}
}
