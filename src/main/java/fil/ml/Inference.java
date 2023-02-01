package fil.ml;

import java.util.Map;

	
public abstract class Inference {
	
	public abstract double predict(Map<String, Double> input);
	
}
