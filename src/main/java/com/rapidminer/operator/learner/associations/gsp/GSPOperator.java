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
package com.rapidminer.operator.learner.associations.gsp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.example.set.SortedExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.error.AttributeNotFoundError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.AttributeParameterPrecondition;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Ontology;


/**
 * This operator searches sequential patterns in a set of transactions. Each transaction must be
 * encoded as an single example and must contain one attribute for the time and for the customer.
 * This pair of attribute is used for generate one sequence per customer containing each single
 * transaction ordered by the time of each transaction. The algorithm then searches sequential
 * patterns in the form of: If a customer bought a and c in one transaction, he bought b in the
 * next: <a, c> then <b>. The minimal support describes how many customer must support such a
 * pattern for regarding it as frequent. Infrequent patterns will be dropped. A customer supports
 * such a pattern, if there are some parts of his sequence including the pattern. The above pattern
 * would be supported by a customer with this transactions: <s, g> then <a, s, c> then <b> then <f,
 * h>.
 *
 * The parameters min_gap, max_gap and window_size determine how transaction are handled. For
 * example, if the above customer forgot to by c, and had to return 5 minutes later to buy c, then
 * his transactions would look like that: <s, g> then <a, s> then <c> then <b> then <f, h> This
 * would not support the pattern <a, c> then <b>. To avoid this problem, the window size determines,
 * how long a subsequent transaction is treated as the same transaction. If the window size is
 * larger than 5 minutes, the <c> would be treated as being part of the second transaction and hence
 * this customer would support the above pattern.
 *
 * The max_gap parameter causes a customers sequence not to support a pattern, if the transactions
 * containing this pattern are to widely separated in time. The min_gap parameter does the same if
 * they are to near.
 *
 * @author Sebastian Land
 */
public class GSPOperator extends Operator {

	public static final String TIME_ROLE = "time";
	public static final String CUSTOMER_ROLE = "customer";

	public static final String PARAMETER_CUSTOMER_ATTRIBUTE = "customer_id";
	public static final String PARAMETER_TIME_ATTRIBUTE = "time_attribute";
	public static final String PARAMETER_WINDOW_SIZE = "window_size";
	public static final String PARAMETER_MAX_GAP = "max_gap";
	public static final String PARAMETER_MIN_GAP = "min_gap";
	public static final String PARAMETER_POSITIVE_VALUE = "positive_value";
	public static final String PARAMETER_MIN_SUPPORT = "min_support";

	private static final OperatorVersion VERSION_MADE_POSITIVE_CLASS_MANDATORY = new OperatorVersion(5, 2, 0);

	private InputPort exampleSetInput = getInputPorts().createPort("example set");
	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
	private OutputPort patternOutput = getOutputPorts().createPort("patterns");

	public GSPOperator(OperatorDescription description) {
		super(description);

		exampleSetInput.addPrecondition(new SimplePrecondition(exampleSetInput, new ExampleSetMetaData(), true) {

			@Override
			public void makeAdditionalChecks(MetaData metaData) {
				if (metaData instanceof ExampleSetMetaData) {
					ExampleSetMetaData emd = (ExampleSetMetaData) metaData;

					String customerAttribute = "";
					String timeAttribute = "";
					try {
						customerAttribute = getParameterAsString(PARAMETER_CUSTOMER_ATTRIBUTE);
						timeAttribute = getParameterAsString(PARAMETER_TIME_ATTRIBUTE);
					} catch (UndefinedParameterError e) {
					}

					// checking allowed types
					for (AttributeMetaData amd : emd.getAllAttributes()) {
						if (amd.isSpecial()) {
							continue;
						}
						// check if name is in ignore list
						if (amd.getName().equals(customerAttribute) || amd.getName().equals(timeAttribute)) {
							continue;
						}

						// otherwise do check
						if (!Ontology.ATTRIBUTE_VALUE_TYPE.isA(amd.getValueType(), Ontology.NOMINAL)) {
							createError(Severity.ERROR, "regular_type_mismatch",
									new Object[] { Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(Ontology.BINOMINAL) });
							break;
						}
					}
				}
			}
		});
		exampleSetInput
				.addPrecondition(new AttributeParameterPrecondition(exampleSetInput, this, PARAMETER_CUSTOMER_ATTRIBUTE));
		exampleSetInput.addPrecondition(
				new AttributeParameterPrecondition(exampleSetInput, this, PARAMETER_TIME_ATTRIBUTE, Ontology.NUMERICAL));

		getTransformer().addPassThroughRule(exampleSetInput, exampleSetOutput);
		getTransformer().addGenerationRule(patternOutput, GSPSet.class);

	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
		Attributes attributes = exampleSet.getAttributes();

		String timeAttributeName = getParameterAsString(PARAMETER_TIME_ATTRIBUTE);
		String customerAttributeName = getParameterAsString(PARAMETER_CUSTOMER_ATTRIBUTE);
		if (timeAttributeName.equals("")) {
			throw new UndefinedParameterError(PARAMETER_TIME_ATTRIBUTE, this);
		}
		if (customerAttributeName.equals("")) {
			throw new UndefinedParameterError(PARAMETER_CUSTOMER_ATTRIBUTE, this);
		}

		double minSupport = getParameterAsDouble(PARAMETER_MIN_SUPPORT);
		double maxGap = getParameterAsDouble(PARAMETER_MAX_GAP);
		double minGap = getParameterAsDouble(PARAMETER_MIN_GAP);
		double windowSize = getParameterAsDouble(PARAMETER_WINDOW_SIZE);

		Attribute timeAttribute = attributes.get(timeAttributeName);
		Attribute customerAttribute = attributes.get(customerAttributeName);
		if (timeAttribute == null) {
			throw new AttributeNotFoundError(this, PARAMETER_TIME_ATTRIBUTE, timeAttributeName);
		}
		if (customerAttribute == null) {
			throw new AttributeNotFoundError(this, PARAMETER_CUSTOMER_ATTRIBUTE, customerAttributeName);
		}

		if (!timeAttribute.isNumerical()) {
			throw new UserError(this, 144, timeAttribute.getName(), "GSP");
		}

		// setting both attributes special
		attributes.setSpecialAttribute(timeAttribute, TIME_ROLE);
		attributes.setSpecialAttribute(customerAttribute, CUSTOMER_ROLE);

		// now check that only binominal attributes are present and fetch positive index
		Tools.onlyNominalAttributes(exampleSet, "GSP");
		double positiveIndices[] = new double[attributes.size()];
		Arrays.fill(positiveIndices, 1);
		if (isParameterSet(PARAMETER_POSITIVE_VALUE)) {
			int attributeIndex = 0;
			String positiveValue = getParameterAsString(PARAMETER_POSITIVE_VALUE);
			for (Attribute attribute : attributes) {
				positiveIndices[attributeIndex] = attribute.getMapping().getIndex(positiveValue);
				attributeIndex++;
			}
		}

		// now build items from attributes
		Item[] items = new Item[attributes.size()];
		int i = 0;
		for (Attribute attribute : attributes) {
			items[i] = new Item(attribute.getName(), i);
			i++;
		}

		// building sequences
		ArrayList<DataSequence> dataSequences = buildSequences(exampleSet, attributes, timeAttribute, customerAttribute,
				positiveIndices, items);
		double numberOfSequences = dataSequences.size();

		if (numberOfSequences * minSupport < 5) {
			// LogService.getGlobal().log("Found only " + numberOfSequences +
			// " sequences. Together with the small minimal support, this could result in very many
			// patterns and a long calculation time.",
			// LogService.WARNING);
			LogService.getRoot().log(Level.WARNING,
					"com.rapidminer.operator.learner.associations.gsp.GSPOperator.number_of_sequences_founded",
					numberOfSequences);
		}

		// find frequent items: Items are frequent if occur in enough sequences
		int minFrequency = Math.max(1, (int) Math.floor(numberOfSequences * minSupport));
		LinkedHashSet<Item> frequentItems = findFrequentItems(dataSequences, items, minFrequency);

		// remove infrequent items from sequences
		Iterator<DataSequence> sequenceIterator = dataSequences.iterator();
		while (sequenceIterator.hasNext()) {
			DataSequence sequence = sequenceIterator.next();
			Iterator<Transaction> transactionIterator = sequence.iterator();
			while (transactionIterator.hasNext()) {
				Transaction transaction = transactionIterator.next();
				Iterator<Item> itemIterator = transaction.iterator();
				while (itemIterator.hasNext()) {
					Item item = itemIterator.next();
					if (!frequentItems.contains(item)) {
						itemIterator.remove();
					}
				}
				if (transaction.isEmpty()) {
					transactionIterator.remove();
				}
			}
			if (sequence.isEmpty()) {
				sequenceIterator.remove();
			}
		}

		// build first seed set
		HashSet<Sequence> seeds = buildSeeds(frequentItems);

		// now iteratively build candidates and filter them to seeds
		GSPSet model = new GSPSet();
		int round = 0;
		while (seeds.size() > 0) {
			checkForStop();
			ArrayList<Sequence> candidates = generateCandidates(seeds, round == 0);

			if (candidates.size() == 0) {
				break;
			}
			checkForStop();

			// if new candidates filter them from ones with to small support
			int[] supportCounter = countSupportingCustomer(candidates, dataSequences, windowSize, maxGap, minGap,
					minSupport);

			Iterator<Sequence> iterator = candidates.iterator();
			for (i = 0; i < supportCounter.length; i++) {
				Sequence currentSequence = iterator.next();
				double support = supportCounter[i] / numberOfSequences;
				if (support != 0.0d && support >= minSupport) {
					model.addSequence(currentSequence, support);
				} else {
					iterator.remove();
				}
			}
			// LogService.getGlobal().log("Filtered Candidates. Remaining: " + candidates.size(),
			// LogService.INIT);
			LogService.getRoot().log(Level.INFO,
					"com.rapidminer.operator.learner.associations.gsp.GSPOperator.filtered_candidates", candidates.size());

			// using filtered candidates as seeds
			seeds.clear();
			seeds.addAll(candidates);

			round++;
		}

		exampleSetOutput.deliver(exampleSet);
		patternOutput.deliver(model);
	}

	private int[] countSupportingCustomer(ArrayList<Sequence> candidates, ArrayList<DataSequence> dataSequences,
			double windowSize, double maxGap, double minGap, double minSupport) {
		// LogService.getGlobal().log("Building Hashtree for counting candidates of length " +
		// candidates.get(0).getNumberOfItems(), LogService.INIT);
		LogService.getRoot().log(Level.INFO,
				"com.rapidminer.operator.learner.associations.gsp.GSPOperator.building_hashtree_for_counting_candidates",
				candidates.get(0).getNumberOfItems());

		// build hashtree: root becomes immediately inner node, since candidates will probably
		// exceed limit
		HashTreeNode root = new HashTreeRootNode();
		int i = 0;
		for (Sequence candidate : candidates) {
			root.addSequence(candidate, i, 0, null, candidates);
			i++;
		}

		// LogService.getGlobal().log("Counting supporting sequences for candidates of length " +
		// candidates.get(0).getNumberOfItems(),
		// LogService.INIT);
		LogService.getRoot().log(Level.INFO,
				"com.rapidminer.operator.learner.associations.gsp.GSPOperator.counting_supporting_sequences_for_candidates",
				candidates.get(0).getNumberOfItems());
		// now run through all data sequences and counting occurrences of candidate sequences
		int[] counter = new int[candidates.size()];
		boolean[] occurs = new boolean[candidates.size()];
		CountingInformations countingInformations = new CountingInformations(occurs, candidates, windowSize, maxGap, minGap);
		for (DataSequence dataSequence : dataSequences) {
			// calling tree to let it count the dataSequence
			root.countCoveredCandidates(dataSequence, 0, countingInformations);

			for (i = 0; i < occurs.length; i++) {
				counter[i] += occurs[i] ? 1 : 0;
				occurs[i] = false;
			}
		}

		return counter;
	}

	private static ArrayList<Sequence> generateCandidates(HashSet<Sequence> seeds, boolean isFirstRound) {
		// LogService.getGlobal().log("Generating Candidates of length " +
		// seeds.iterator().next().getNumberOfItems(), LogService.INIT);
		LogService.getRoot().log(Level.INFO,
				"com.rapidminer.operator.learner.associations.gsp.GSPOperator.generating_candidates",
				seeds.iterator().next().getNumberOfItems());
		ArrayList<Sequence> candidates = new ArrayList<>();
		int pruneCheckCounter = 0;
		// generate set of candidates
		for (Sequence sequence1 : seeds) {
			for (Sequence sequence2 : seeds) {
				if (sequence1.equals(0, sequence2, sequence2.getNumberOfItems() - 1)) {
					if (isFirstRound || sequence2.getLastTransaction().size() == 1) {
						Sequence candidate = Sequence.appendTransaction(sequence1, sequence2.getLastTransaction());

						pruneCheckCounter++;
						if (pruneCheckCounter % 10000 == 0) {
							LogService.getRoot().info(
									"....................................................................................................");
						}

						if (!isPruned(seeds, candidate)) {
							candidates.add(candidate);
						}
					}
					if (isFirstRound || sequence2.getLastTransaction().size() > 1) {
						Sequence candidate = Sequence.appendItem(sequence1, sequence2.getLastTransaction().getLastItem());

						pruneCheckCounter++;
						if (pruneCheckCounter % 10000 == 0) {
							LogService.getRoot().info(
									"....................................................................................................");
						}

						if (!isPruned(seeds, candidate)) {
							candidates.add(candidate);
						}
					}
				}
			}
		}

		// LogService.getGlobal().log("Generated " + candidates.size() + " candidates",
		// LogService.INIT);
		LogService.getRoot().log(Level.INFO,
				"com.rapidminer.operator.learner.associations.gsp.GSPOperator.generating_candidates", candidates.size());
		return candidates;
	}

	private static boolean isPruned(HashSet<Sequence> seeds, Sequence candidate) {
		if (candidate.getNumberOfItems() < seeds.iterator().next().getNumberOfItems() + 1) {
			return true;
		}
		boolean contained = true;
		// removing from first transaction
		for (int i = 0; i < candidate.get(0).size(); i++) {
			if (!isFrequent(Sequence.removeItem(candidate, 0, i), seeds)) {
				return true;
			}
		}
		if (contained) {
			// removing from last transaction
			int lastIndex = candidate.size() - 1;
			for (int i = 0; i < candidate.get(lastIndex).size(); i++) {
				if (!isFrequent(Sequence.removeItem(candidate, lastIndex, i), seeds)) {
					return true;
				}
			}
		}
		if (contained) {
			// removing from center, hence skip first and last
			for (int transactionIndex = 1; transactionIndex < candidate.size() - 1; transactionIndex++) {
				int transactionSize = candidate.get(transactionIndex).size();
				if (transactionSize > 1) {
					for (int i = 0; i < transactionSize; i++) {
						if (!isFrequent(Sequence.removeItem(candidate, transactionIndex, i), seeds)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	// test if the candidate is frequent by checking if contained in seeds
	private static boolean isFrequent(Sequence testCandidate, HashSet<Sequence> seeds) {
		return seeds.contains(testCandidate);
	}

	private HashSet<Sequence> buildSeeds(LinkedHashSet<Item> frequentItems) {
		HashSet<Sequence> seeds = new HashSet<>(frequentItems.size());
		for (Item item : frequentItems) {
			Transaction transaction = new Transaction(Double.NaN);
			transaction.add(item);
			Sequence sequence = new Sequence();
			sequence.add(transaction);
			seeds.add(sequence);
		}
		return seeds;
	}

	private LinkedHashSet<Item> findFrequentItems(ArrayList<DataSequence> sequences, Item[] items, int minFrequency) {
		int[] itemCounters = new int[items.length];
		for (Sequence sequence : sequences) {
			boolean[] itemCounted = new boolean[items.length];
			for (Transaction transaction : sequence) {
				for (Item item : transaction) {
					int index = item.getIndex();
					if (!itemCounted[index]) {
						itemCounters[index]++;
						itemCounted[index] = true;
					}
				}
			}
		}

		LinkedHashSet<Item> frequentItems = new LinkedHashSet<>();
		for (int i = 0; i < items.length; i++) {
			if (itemCounters[i] >= minFrequency) {
				frequentItems.add(items[i]);
			}
		}
		return frequentItems;
	}

	private ArrayList<DataSequence> buildSequences(ExampleSet exampleSet, Attributes attributes, Attribute timeAttribute,
			Attribute customerAttribute, double[] positiveIndices, Item[] items) {
		ArrayList<DataSequence> sequences = new ArrayList<>();
		// now sort exampleSet according to customer attribute and time attribute
		SortedExampleSet sortedSet = new SortedExampleSet(exampleSet, timeAttribute, SortedExampleSet.INCREASING);
		sortedSet = new SortedExampleSet(sortedSet, customerAttribute, SortedExampleSet.INCREASING);

		// now build sequences from exampleset: Each Customer is one sequence, each transaction one
		// item set
		double lastCustomerId = Double.NEGATIVE_INFINITY;
		DataSequence currentSequence = null;
		for (Example example : sortedSet) {
			double customerId = example.getValue(customerAttribute);
			if (lastCustomerId != customerId) {
				// if completely filled: Build access structure
				if (currentSequence != null) {
					currentSequence.buildAccessStructure();
				}
				// then create new sequence
				currentSequence = new DataSequence(items.length);
				sequences.add(currentSequence); // add reference already: Will be filled later
				lastCustomerId = customerId;
			}

			Transaction currentSet = new Transaction(example.getValue(timeAttribute));
			int attributeIndex = 0;
			for (Attribute attribute : attributes) {
				if (example.getValue(attribute) == positiveIndices[attributeIndex]) {
					currentSet.add(items[attributeIndex]);
				}
				attributeIndex++;
			}
			if (currentSet.size() > 0) {
				currentSequence.add(currentSet);
			}
		}
		// building structure for last sequence.
		currentSequence.buildAccessStructure();
		return sequences;
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return new OperatorVersion[] { VERSION_MADE_POSITIVE_CLASS_MANDATORY };
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType type = new ParameterTypeAttribute(PARAMETER_CUSTOMER_ATTRIBUTE,
				"This attribute will be used to identify the customer of a transaction.", exampleSetInput, false);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeAttribute(PARAMETER_TIME_ATTRIBUTE,
				"This numerical attribute specifies the time of a transaction.", exampleSetInput, false, Ontology.NUMERICAL);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_MIN_SUPPORT, "This specifies the minimal support of a pattern", 0, 1,
				false);
		type.setDefaultValue(0.9);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_WINDOW_SIZE, "This specifies the window size", 0, Double.POSITIVE_INFINITY,
				false);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_MAX_GAP, "This specifies the maximal gap", 0, Double.POSITIVE_INFINITY,
				false);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_MIN_GAP, "This specifies the minimal gap", 0, Double.POSITIVE_INFINITY,
				false);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeString(PARAMETER_POSITIVE_VALUE,
				"This parameter determines, which value of the binominal attributes is treated as positive. Attributes with that value are considered as part of a transaction. If left blank, the example set determines, which is value is used.",
				getCompatibilityLevel().isAtMost(VERSION_MADE_POSITIVE_CLASS_MANDATORY));
		types.add(type);

		return types;
	}
}
