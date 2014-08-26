package quickml.supervised.classifier;

import quickml.data.PredictionMap;

import java.io.Serializable;
import java.util.Map;

/**
 * Created by alexanderhawk on 8/17/14.
 */
//where do we want Classifier as a generic type...in downsampling PMB.
public abstract class AbstractClassifier implements Classifier {
    private static final long serialVersionUID = -5052476771686106526L;
    public double getProbability(Map<String, Serializable> attributes, Serializable classification) {
        return predict(attributes).get(classification);
    }
    public abstract PredictionMap predict(Map<String, Serializable> attributes);

    public Serializable getClassificationByMaxProb(Map<String, Serializable> attributes) {
        PredictionMap predictions = predict(attributes);
        Serializable mostProbableClass = null;
        double probabilityOfMostProbableClass = 0;
        for (Serializable key : predictions.keySet()) {
            if (predictions.get(key).doubleValue() > probabilityOfMostProbableClass) {
                mostProbableClass = key;
                probabilityOfMostProbableClass = predictions.get(key).doubleValue();
            }
        }
        return mostProbableClass;
    }
}