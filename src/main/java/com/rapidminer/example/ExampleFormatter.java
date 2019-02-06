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
package com.rapidminer.example;

import java.util.LinkedList;
import java.util.List;

import com.rapidminer.tools.Tools;


/**
 * Formats an example as specified by the format string. The dollar sign '$' is an escape character.
 * Squared brackets '[' and ']' have a special meaning. The following escape sequences are
 * interpreted:
 * <dl>
 * <dt>$a:</dt>
 * <dd>All attributes separated by the default separator</dd>
 * <dt>$a[separator]:</dt>
 * <dd>All attributes separated by separator</dd>
 * <dt>$s[separator][indexSeparator]:</dt>
 * <dd>Sparse format. For all non 0 attributes the following strings are concatenated: the column
 * index, the value of indexSeparator, the attribute value. Attributes are separated by separator.</dd>
 * <dt>$v[name]:</dt>
 * <dd>The value of the attribute with the given name (both regular and special attributes)</dd>
 * <dt>$k[index]:</dt>
 * <dd>The value of the attribute with the given index in the example set</dd>
 * <dt>$l:</dt>
 * <dd>The label</dd>
 * <dt>$p:</dt>
 * <dd>The predicted label</dd>
 * <dt>$d:</dt>
 * <dd>All prediction confidences for all classes in the form conf(class)=value</dd>
 * <dt>$d[class]:</dt>
 * <dd>The prediction confidence for the defined class as a simple number</dd>
 * <dt>$i:</dt>
 * <dd>The id</dd>
 * <dt>$w:</dt>
 * <dd>The weight</dd>
 * <dt>$c:</dt>
 * <dd>The cluster</dd>
 * <dt>$b:</dt>
 * <dd>The batch</dd>
 * <dt>$n:</dt>
 * <dd>The newline character</dd>
 * <dt>$t:</dt>
 * <dd>The tabulator character</dd>
 * <dt>$$:</dt>
 * <dd>The dollar sign</dd>
 * <dt>$[:</dt>
 * <dd>The '[' character</dd>
 * <dt>$]:</dt>
 * <dd>The ']' character</dd>
 * </dl>
 * 
 * @author Simon Fischer, Ingo Mierswa Exp $
 */
public class ExampleFormatter {

	/** Represents one piece of formatting. */
	public static interface FormatCommand {

		public String format(Example example);
	}

	/**
	 * Implements some simple format commands like 'a' for all attributes or 'l' for the label.
	 */
	public static class SimpleCommand implements FormatCommand {

		private char command;

		private String[] arguments;

		private int fractionDigits = -1;

		private boolean quoteNominal;

		private SimpleCommand(ExampleSet exampleSet, char command, String[] arguments, int fractionDigits,
				boolean quoteNominal) throws FormatterException {
			this.command = command;
			this.fractionDigits = fractionDigits;
			this.quoteNominal = quoteNominal;
			switch (command) {
				case 'a':
					if (arguments.length == 0) {
						arguments = new String[] { Example.SEPARATOR };
					}
					break;
				case 's':
					if (arguments.length == 0) {
						arguments = new String[] { Example.SEPARATOR, Example.SPARSE_SEPARATOR };
					}
					if (arguments.length == 1) {
						arguments = new String[] { arguments[0], Example.SPARSE_SEPARATOR };
					}
					if (arguments.length == 2) {
						arguments = new String[] { arguments[0], arguments[1] };
					}
					if (arguments.length > 2) {
						throw new FormatterException(
								"For command 's' only up to two arguments (separator and sparse separator) are allowed.");
					}
					break;
				case 'l':
					if (exampleSet.getAttributes().getLabel() == null) {
						throw new FormatterException("Example set does not provide 'label' attribute, $l will not work.");
					}
					break;
				case 'p':
					if (exampleSet.getAttributes().getPredictedLabel() == null) {
						throw new FormatterException(
								"Example set does not provide 'predicted label' attribute, $p will not work.");
					}
					break;
				case 'i':
					if (exampleSet.getAttributes().getId() == null) {
						throw new FormatterException("Example set does not provide 'id' attribute, $i will not work.");
					}
					break;
				case 'w':
					if (exampleSet.getAttributes().getWeight() == null) {
						throw new FormatterException("Example set does not provide 'weight' attribute, $w will not work.");
					}
					break;
				case 'c':
					if (exampleSet.getAttributes().getCluster() == null) {
						throw new FormatterException("Example set does not provide 'cluster' attribute, $c will not work.");
					}
					break;
				case 'b':
					if (exampleSet.getAttributes().getSpecial(Attributes.BATCH_NAME) == null) {
						throw new FormatterException("Example set does not provide 'batch' attribute, $b will not work.");
					}
					break;
				case 'd':
					if (exampleSet.getAttributes().getPredictedLabel() == null) {
						throw new FormatterException(
								"Example set does not provide 'confidence' attributes, $d will not work.");
					}
					break;
				default:
					throw new FormatterException("Unknown command: '" + command + "'");
			}
			this.arguments = arguments;
		}

		@Override
		public String format(Example example) {
			Attributes attributes = example.getAttributes();
			Attribute chosenAttribute = null;
			switch (command) {
				case 'a': {
					StringBuffer str = new StringBuffer();
					boolean first = true;
					for (Attribute attribute : attributes) {
						if (!first) {
							str.append(arguments[0]);
						}
						str.append(example.getValueAsString(attribute, fractionDigits, quoteNominal));
						first = false;
					}
					return str.toString();
				}
				case 's':
					return example.getAttributesAsSparseString(arguments[0], arguments[1], fractionDigits, quoteNominal);
				case 'l':
					chosenAttribute = attributes.getLabel();
					break;
				case 'p':
					chosenAttribute = attributes.getPredictedLabel();
					break;
				case 'i':
					chosenAttribute = attributes.getId();
					break;
				case 'w':
					chosenAttribute = attributes.getWeight();
					break;
				case 'c':
					chosenAttribute = attributes.getCluster();
					break;
				case 'b':
					chosenAttribute = attributes.getSpecial(Attributes.BATCH_NAME);
					break;
				case 'd': {
					if (arguments.length != 0) {
						return Tools.formatNumber(example.getConfidence(arguments[0]), fractionDigits);
					}
					StringBuffer str = new StringBuffer();
					boolean first = true;
					for (String value : attributes.getPredictedLabel().getMapping().getValues()) {
						if (first) {
							first = false;
						} else {
							str.append(Example.SEPARATOR);
						}
						str.append(
								"conf(" + value + ")=" + Tools.formatNumber(example.getConfidence(value), fractionDigits));
					}
					return str.toString();
				}
				default:
					return command + "";
			}
			return example.getValueAsString(chosenAttribute, fractionDigits, quoteNominal);
		}
	}

	/** Returns the value of an argument which must be an attribute's name. */
	public static class ValueCommand implements FormatCommand {

		private Attribute attribute;

		private int fractionDigits = -1;

		private boolean quoteWhitespace = false;

		public ValueCommand(char command, String[] arguments, ExampleSet exampleSet, int fractionDigits,
				boolean quoteWhitespace) throws FormatterException {
			this.fractionDigits = fractionDigits;
			this.quoteWhitespace = quoteWhitespace;
			if (arguments.length < 1) {
				throw new FormatterException("Command 'v' needs argument!");
			}
			switch (command) {
				case 'v':
					attribute = exampleSet.getAttributes().get(arguments[0]);
					if (attribute == null) {
						throw new FormatterException("Unknown attribute: '" + arguments[0] + "'!");
					}
					break;
				case 'k':
					int column = -1;
					try {
						column = Integer.parseInt(arguments[0]);
					} catch (NumberFormatException e) {
						throw new FormatterException("Argument for 'k' must be an integer!");
					}
					if ((column < 0) || (column >= exampleSet.getAttributes().size())) {
						throw new FormatterException("Illegal column: '" + arguments[0] + "'!");
					}

					int counter = 0;
					for (Attribute attribute : exampleSet.getAttributes()) {
						if (counter >= column) {
							this.attribute = attribute;
							break;
						}
						counter++;
					}
					if (attribute == null) {
						throw new FormatterException("Attribute #" + column + " not found.");
					}
					break;
				default:
					throw new FormatterException("Illegal command for ValueCommand: '" + command + "'");
			}
		}

		@Override
		public String format(Example example) {
			return example.getValueAsString(attribute, fractionDigits, quoteWhitespace);
		}
	}

	/** Returns simply the given text. */
	public static class TextCommand implements FormatCommand {

		private String text;

		private TextCommand(String text) {
			this.text = text;
		}

		@Override
		public String format(Example example) {
			return text;
		}
	}

	/** The commands used subsequently to format the example. */
	private FormatCommand[] formatCommands;

	/**
	 * Constructs a new ExampleFormatter that executes the given array of formatting commands. The
	 * preferred way of creating an instance of ExampleFormatter is to
	 * {@link ExampleFormatter#compile(String, ExampleSet, int, boolean)} a format string.
	 */
	public ExampleFormatter(FormatCommand[] formatCommands) {
		this.formatCommands = formatCommands;
	}

	/**
	 * Factory method that compiles a format string and creates an instance of ExampleFormatter.
	 */
	public static ExampleFormatter compile(String formatString, ExampleSet exampleSet, int fractionDigits,
			boolean quoteWhitespace) throws FormatterException {
		List<FormatCommand> commandList = new LinkedList<FormatCommand>();
		compile(formatString, exampleSet, commandList, fractionDigits, quoteWhitespace);
		FormatCommand[] commands = new FormatCommand[commandList.size()];
		commandList.toArray(commands);
		return new ExampleFormatter(commands);
	}

	/** Adds all commands to the <code>commandList</code>. */
	private static void compile(String formatString, ExampleSet exampleSet, List<FormatCommand> commandList,
			int fractionDigits, boolean quoteWhitespace) throws FormatterException {
		int start = 0;
		while (true) {
			int tagStart = formatString.indexOf("$", start);
			if (tagStart == -1) {
				commandList.add(new TextCommand(formatString.substring(start)));
				break;
			}

			if (tagStart == formatString.length() - 1) {
				throw new FormatterException("Format string ends in '$'.");
			}

			commandList.add(new TextCommand(formatString.substring(start, tagStart)));

			char command = formatString.charAt(tagStart + 1);
			if ((command == '$') || (command == '[') || (command == ']')) {
				commandList.add(new TextCommand("" + command));
				start = tagStart + 2;
				continue;
			} else if (command == 'n') {
				commandList.add(new TextCommand(Tools.getLineSeparator()));
				start = tagStart + 2;
				continue;
			} else if (command == 't') {
				commandList.add(new TextCommand("\t"));
				start = tagStart + 2;
				continue;
			}

			start = tagStart + 2;
			List<String> argumentList = new LinkedList<String>();
			while ((start < formatString.length()) && (formatString.charAt(start) == '[')) {
				int end = formatString.indexOf(']', start);
				if (end == -1) {
					throw new FormatterException("Unclosed '['!");
				}
				argumentList.add(formatString.substring(start + 1, end));
				start = end + 1;
			}

			String[] arguments = new String[argumentList.size()];
			argumentList.toArray(arguments);
			switch (command) {
				case 'v':
				case 'k':
					commandList.add(new ValueCommand(command, arguments, exampleSet, fractionDigits, quoteWhitespace));
					break;
				default:
					commandList.add(new SimpleCommand(exampleSet, command, arguments, fractionDigits, quoteWhitespace));
					break;
			}
		}
	}

	/** Formats a single example. */
	public String format(Example example) {
		StringBuffer str = new StringBuffer();
		for (int i = 0; i < formatCommands.length; i++) {
			str.append(formatCommands[i].format(example));
		}
		return str.toString();
	}
}
