package fil.ml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.dmg.pmml.FieldName;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.EvaluatorUtil;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.LoadingModelEvaluatorBuilder;
import org.xml.sax.SAXException;

public class Others extends Inference{
	final static String PATH = System.getProperty("user.dir");
	private Evaluator evaluator;
	private String output;
	
	public Others(double tw, String VNFtype, String simulType, String output) throws IOException, SAXException, JAXBException  {
		// example name: den-24h.jpmml
		this.evaluator = new LoadingModelEvaluatorBuilder().load(new File(PATH + "\\MLModels" + "\\" + tw + "\\" + VNFtype + "-" + simulType + ".pmml")).build();
		this.output = output;
	}

	@Override
	@SuppressWarnings("unchecked")
	public double predict(Map<String, Double> feedData) {
		
//		Evaluator evaluator1 = new LoadingModelEvaluatorBuilder().load(new File(type)).build();
		List<? extends InputField> inputFields = this.evaluator.getInputFields();
//		System.out.println("Input fields: " + inputFields);
		
		
		Map<FieldName, FieldValue> arguments = new LinkedHashMap<FieldName, FieldValue>();
		// Mapping the record field-by-field from data source schema to PMML schema
		
		for(InputField inputField : inputFields){ 
			FieldName inputName = inputField.getName();
			FieldValue inputValue;
			// Transforming an arbitrary user-supplied value to a known-good PMML value
			for(Map.Entry<String, Double> entry : feedData.entrySet()) {
//				System.out.println("inputName: " + inputName.getValue());
//				System.out.println("entry: " + entry.getKey());
				if(entry.getKey().equals(inputName.getValue())) {
					inputValue = inputField.prepare(entry.getValue());
					arguments.put(inputName, inputValue);
				}
			}
//			FieldValue inputValue = inputField.prepare(feedData.get(i));
		}
//		System.out.println(arguments);
		Map<FieldName, ?> results = evaluator.evaluate(arguments);
		Map<String, Double> resultRecord = (Map<String, Double>) EvaluatorUtil.decodeAll(results);
		System.out.println(resultRecord);
		double result = resultRecord.get(this.output);
//		Collection<Double> result = resultRecord.values();
//		System.out.println("result: " + result);
		return result;
	}
	
	public static void main(String[] args) throws IOException, SAXException, JAXBException {
		Others a = new Others(1.0, "den", "24h", "# VNF Density future");
		Map<String, Double> feedData = new HashMap<>();
//		4	33.58	44.77333333	9.018	2.100233333	2.838153153	212.4233333	85.68875	225	3	8	22
		
		feedData.put("CPU edge average (2)", 44.77); //CPU edge (2)
		feedData.put("CPU server average", 9.018); //CPU server
		feedData.put("# links (2)", 8.0);  //#link (2)
		feedData.put("BW edge (1)", 2.1); //BW edge (1)
		feedData.put("# edge device", 225.0); //#edge device
		feedData.put("BW edge (1).1", 2.84); //BW edge (2)
		feedData.put("BW server (1)", 212.42); //BW server (1)
		feedData.put("CPU edge average (1)", 33.58); //CPU edge (1)
		feedData.put("# VNF Density current", 22.0); //#VNF current
		feedData.put("Time ", 4.0); //time
		feedData.put("BW server (2)", 85.688); //BW server (2)
		feedData.put("# links (1)", 3.0); //#link (1)
		String output = "# VNF Density future";
		double outputValue = a.predict(feedData);
		System.out.println("Output: " + outputValue);
		double test = 0.0;
		if (test % 2.0 == 0.0)
			System.out.println("abc");
	}

	public Evaluator getEvaluator() {
		return evaluator;
	}

	public void setEvaluator(Evaluator evaluator) {
		this.evaluator = evaluator;
	}

	public String getOutput() {
		return output;
	}

	public void setOutput(String output) {
		this.output = output;
	}

}
