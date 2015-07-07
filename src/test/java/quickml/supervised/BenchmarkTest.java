package quickml.supervised;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.Before;
import org.junit.Test;
import quickml.data.AttributesMap;
import quickml.data.ClassifierInstance;
import quickml.data.InstanceWithAttributesMap;
import quickml.scorers.Scorer;
import quickml.supervised.classifier.Classifier;
import quickml.supervised.crossValidation.ClassifierLossChecker;
import quickml.supervised.crossValidation.CrossValidator;
import quickml.supervised.crossValidation.data.FoldedData;
import quickml.supervised.crossValidation.lossfunctions.ClassifierLogCVLossFunction;
import quickml.supervised.ensembles.randomForest.randomDecisionForest.RandomDecisionForest;
import quickml.supervised.ensembles.randomForest.randomDecisionForest.RandomDecisionForestBuilder;
import quickml.supervised.tree.attributeIgnoringStrategies.IgnoreAttributesWithConstantProbability;
import quickml.supervised.tree.constants.ForestOptions;
import quickml.supervised.tree.decisionTree.DecisionTree;
import quickml.supervised.tree.decisionTree.DecisionTreeBuilder;
import quickml.supervised.tree.decisionTree.scorers.GiniImpurityScorer;
import quickml.supervised.tree.decisionTree.scorers.MSEScorer;
import quickml.supervised.tree.decisionTree.scorers.SplitDiffScorer;
import quickml.supervised.tree.decisionTree.valueCounters.ClassificationCounter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.zip.GZIPInputStream;

import static com.google.common.collect.Lists.newArrayList;

public class BenchmarkTest {

    private ClassifierLossChecker<ClassifierInstance> classifierLossChecker;
    private ArrayList<Scorer<ClassificationCounter>> scorers;
    private DecisionTreeBuilder<ClassifierInstance> treeBuilder;
    private RandomDecisionForestBuilder randomDecisionForestBuilder;

    @Before
    public void setUp() throws Exception {
        classifierLossChecker = new ClassifierLossChecker<>(new ClassifierLogCVLossFunction(0.000001));
        scorers = newArrayList(
                new SplitDiffScorer(),
                new MSEScorer(MSEScorer.CrossValidationCorrection.FALSE),
                new MSEScorer(MSEScorer.CrossValidationCorrection.TRUE));
        treeBuilder = createTreeBuilder();
        randomDecisionForestBuilder = createRandomForestBuilder();
    }

    @Test
    public void testDiaInstances() throws Exception {
        testWithInstances("dia", loadDiabetesDataset());
    }

    @Test
    public void testMoboInstances() throws Exception {
        testWithInstances("mobo", loadMoboDataset());
    }

    @Test
    public void performanceTest() throws Exception {
        Random random = new Random();
        List<ClassifierInstance> instances = loadDiabetesDataset();
        for (int i =1; i<60000; i++) {
            instances.add(instances.size(), instances.get(random.nextInt(instances.size()-1)));
        }
        double time0 = System.currentTimeMillis();
        DecisionTreeBuilder<ClassifierInstance> treeBuilder = new DecisionTreeBuilder<ClassifierInstance>().scorer(new GiniImpurityScorer())
                .numSamplesPerNumericBin(20)
                .numNumericBins(5)
                .attributeIgnoringStrategy(new IgnoreAttributesWithConstantProbability(0.0))
                .maxDepth(16)
                .minLeafInstances(5);

        treeBuilder.buildPredictiveModel(instances);

        double time1 = System.currentTimeMillis();
        System.out.println("run time in seconds on numeric data set: " + (time1-time0)/1000);

    }


    private void testWithInstances(String dsName, final List<ClassifierInstance> instances) {
        FoldedData<ClassifierInstance> data = new FoldedData<>(instances, 4, 4);

        for (final Scorer scorer : scorers) {
            Map<String, Serializable> cfg = Maps.newHashMap();
            cfg.put(ForestOptions.SCORER.name(), scorer);
            CrossValidator<AttributesMap, Classifier, ClassifierInstance> validator = new CrossValidator<AttributesMap, Classifier, ClassifierInstance>(treeBuilder, classifierLossChecker, data);
            System.out.println(dsName + ", single-oldTree, " + scorer + ", " + validator.getLossForModel(cfg));
            validator = new CrossValidator<AttributesMap, Classifier, ClassifierInstance>(randomDecisionForestBuilder, classifierLossChecker, data);
            System.out.println(dsName + ", random-forest, " + scorer + ", " + validator.getLossForModel(cfg));
        }
    }

    private List<ClassifierInstance> loadDiabetesDataset() throws IOException {
        final BufferedReader br = new BufferedReader(new InputStreamReader((new GZIPInputStream(BenchmarkTest.class.getResourceAsStream("diabetesDataset.txt.gz")))));
        final List<ClassifierInstance> instances = Lists.newLinkedList();


        String line = br.readLine();
        while (line != null) {
            String[] splitLine = line.split("\\s");
            AttributesMap attributes = AttributesMap.newHashMap();
            for (int x = 0; x < 8; x++) {
                attributes.put("attr" + x, Double.parseDouble(splitLine[x]));
            }
            instances.add(new ClassifierInstance(attributes, splitLine[8]));
            line = br.readLine();
        }

        return instances;
    }


    private List<ClassifierInstance> loadMoboDataset() throws IOException {
        final BufferedReader br = new BufferedReader(new InputStreamReader((new GZIPInputStream(BenchmarkTest.class.getResourceAsStream("mobo1.json.gz")))));

        final List<ClassifierInstance> instances = Lists.newLinkedList();

        String line = br.readLine();
        while (line != null) {
            final JSONObject jo = (JSONObject) JSONValue.parse(line);
            AttributesMap a = AttributesMap.newHashMap();
            a.putAll((JSONObject) jo.get("attributes"));
            String binaryClassification = jo.get("output").equals("none") ? "none" : "notNone";
            instances.add(new ClassifierInstance(a, binaryClassification));
            line = br.readLine();
        }
        return instances;
    }

    private DecisionTreeBuilder<ClassifierInstance> createTreeBuilder() {
        return new DecisionTreeBuilder<>().attributeIgnoringStrategy(new IgnoreAttributesWithConstantProbability(0.7))
                .maxDepth(12).minAttributeValueOccurences(8).minLeafInstances(10);
    }

    private RandomDecisionForestBuilder createRandomForestBuilder() {
        return new RandomDecisionForestBuilder(createTreeBuilder()).numTrees(5);
    }

}

