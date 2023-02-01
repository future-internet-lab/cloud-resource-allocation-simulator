package fil.ml;

import ai.djl.*;
import ai.djl.inference.*;
import ai.djl.ndarray.*;
import ai.djl.ndarray.types.*;
import ai.djl.translate.*;
import java.util.*;
import java.io.IOException;
import java.nio.file.*;
import  ai.djl.ndarray.*;

public class LSTM extends Inference{
	final static String PATH = System.getProperty("user.dir") + "\\MLModels";
	/* variables for general model */
	private Path modelDir;
	private Model model; 
	private Translator<NDList, Float> translator;
	private Predictor<NDList, Float> predictor;
	private LinkedList<LinkedList<Float>> state; 
	private NDArray preState1;
	private NDArray preState2;
	private NDManager manager;
	private String type;

	/* variables for specific model */
	private int seqLength;
	private int numLayer;
	private int batchSize;
	private int hidSize;
	
	public LSTM(double tw, String type, int seqLen, int numLayer, int batchSize, int hidSize) {
		/* load specific variable */
		this.type = type;
		this.seqLength = seqLen;
		this.numLayer = numLayer;
		this.batchSize = batchSize;
		this.hidSize = hidSize;
		this.state = new LinkedList<>();
		this.manager = NDManager.newBaseManager();
		for(int i = 0; i < this.seqLength; i++) {
			if(type == "den") {
				this.state.add(new LinkedList<Float>(Collections.nCopies(1, 0.0f)));
			}else {
				this.state.add(new LinkedList<Float>(Collections.nCopies(2, 0.0f)));
			}
		}
		this.preState1 = this.manager.zeros(new Shape(this.numLayer, this.batchSize, this.hidSize));
		this.preState2 = this.manager.zeros(new Shape(this.numLayer, this.batchSize, this.hidSize));
		/* load general model from local file */
		this.modelDir = Paths.get(PATH + "\\" + tw + "\\" + type + "-LSTM.zip");
		this.model = Model.newInstance("BadManagement");
		try {
			this.model.load(this.modelDir);
		} catch (MalformedModelException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(this.model);
		/* change the below method for overriding input/output data */
		this.translator = this.overRideMethod();
		this.predictor = this.model.newPredictor(this.translator);
		this.manager = NDManager.newBaseManager();
		
	}
	
	public Translator<NDList, Float> overRideMethod() {
		NoBatchifyTranslator<NDList, Float> translator = new NoBatchifyTranslator <NDList, Float>() {
		    @Override
		    public NDList processInput(TranslatorContext ctx, NDList input) {
//		        NDManager manager = ctx.getNDManager();
//		        NDArray array = manager.create(new float[] {input});
		        return new NDList (input);
		    }
		    
		    @Override
		    public Float processOutput(TranslatorContext ctx, NDList list) {
		        NDArray temp_arr = list.get(0);
		        return temp_arr.getFloat();
		    }
		    
//		    @Override
//		    public Batchifier getBatchifier() {
//		        // The Batchifier describes how to combine a batch together
//		        // Stacking, the most common batchifier, takes N [X1, X2, ...] arrays to a single [N, X1, X2, ...] array
//		        return null;
//		    }
		};
		
		return translator;
	}
	
	public float[][] listToArray(LinkedList<LinkedList<Float>> input) {
		float[][] output = new float[input.size()][];

		for (int i = 0; i < input.size(); i++) {
			LinkedList<Float> row = input.get(i);

		    // Perform equivalent `toArray` operation
		    float[] copy = new float[row.size()];
		    for (int j = 0; j < row.size(); j++) {
		        // Manually loop and set individually
		        copy[j] = row.get(j);
		    }
		    output[i] = copy;
		}
		return output;
	}
	
	public NDList genData(Map<String, Double> input_map) {
		int feaNum = 0;
		if (this.type == "den") {
			double input = input_map.get("# VNF Density current");
			System.out.println("Input for LSTM: " + input);
			this.state.removeFirst();
			this.state.addLast(new LinkedList<Float>(Arrays.asList((float) input)));
			feaNum = 1;
		}else {
			double input1 = input_map.get("Time ");
			double input2 = input_map.get("# VNF Receive current");
			System.out.println("Input for LSTM: " + input1 + " " + input2);
			this.state.removeFirst();
			this.state.addLast(new LinkedList<Float>(Arrays.asList((float)input1, (float)input2)));
			feaNum = 2;
		}
		float[][] input_array = this.listToArray(this.state);
		NDArray input_ND = this.manager.create(input_array);
		input_ND = input_ND.reshape(this.batchSize, this.seqLength, feaNum);
		NDList inputs = new NDList(input_ND, this.preState1, this.preState2);
		return inputs;
	}
	
	@Override
	public double predict(Map<String, Double> input_map) {
		Float output = 0.0f;
//		LinkedList<Double> input_l = new LinkedList<Double>(input_map.values());
//		this.state.removeFirst();
//		this.state.addLast(input_l);
//		double[][] input_array = this.listToArray(this.state);
//		NDArray input_ND = this.manager.create(input_array);
		NDList inputs = this.genData(input_map);
		this.predictor = model.newPredictor(this.translator);
		try {
			output = this.predictor.predict(inputs);
			System.out.println("LSTM model " + this.type + " predicted ouput is : " + output);
		} catch (TranslateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return (double) output;

	}
	
	
	/*
	 * this main function is written to just test loading 
	 * LSTM torch_script made by Nghia.
	 */
	
	public static void main(String[] args) {
		

		String nameModel05 = "model_script_21.zip"; 
		String path = "C:\\Users\\kienkk\\OneDrive - Hanoi University of Science and Technology\\Simulators-n-tools\\low-complexity\\20211204-sync-simulator\\lowcomplexity\\MLModels\\LSTM\\model\\torch_script\\" + nameModel05;
		
		// load model from local file
		Path modelDir = Paths.get(path);
		Model model = Model.newInstance("BadManagement");
		try {
			model.load(modelDir);
		} catch (MalformedModelException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
//		System.out.println(model);
		
		Translator<NDList, Float> translator = new Translator<NDList, Float>(){
		    @Override
		    public NDList processInput(TranslatorContext ctx, NDList input) {
//		        NDManager manager = ctx.getNDManager();
//		        NDArray array = manager.create(new float[] {input});
		        return new NDList (input);
		    }
		    
		    @Override
		    public Float processOutput(TranslatorContext ctx, NDList list) {
		        NDArray temp_arr = list.get(0);
		        return temp_arr.getFloat();
		    }
		    
		    @Override
		    public Batchifier getBatchifier() {
		        // The Batchifier describes how to combine a batch together
		        // Stacking, the most common batchifier, takes N [X1, X2, ...] arrays to a single [N, X1, X2, ...] array
		        return Batchifier.STACK;
		    }
		};
		/*
		 * Now we try to emulate an inputs and feed it into the model to check the
		 * output result.
		 * The input of 4 points:
		 * 0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	0.0	4.0
		 * 2.0	0.328	24.6	0.08	0.008	0.6	2.4	2.4	4.0	1.0	1.0	0.0	4.0	0.0	14.0
		 * 4.0	1.1480000000000001	24.6	0.28	0.027999999999999994	0.5999999999999999	8.399999999999999	8.399999999999999	14.0	1.0	1.0	0.0	14.0	0.0	73.0
		 * 6.0	5.985999999999993	26.026086956521706	1.46	0.1460000000000002	0.6347826086956531	14.600000000000001	10.949999999999998	69.0	3.0	4.0	0.0	73.0	880.0	1473.0
		 */
		
		float [][] input = {

				{2.0f,	0.328f,	24.6f,	0.08f,	0.008f,	0.6f,	2.4f,	2.4f,	4.0f,	1.0f,	1.0f},
				{4.0f,	1.1480000000000001f,	24.6f,	0.28f,	0.027999999999999994f,	0.5999999999999999f,	8.399999999999999f,	8.399999999999999f,	14.0f,	1.0f,	1.0f},
				{6.0f,	5.985999999999993f,	26.026086956521706f,	1.46f,	0.1460000000000002f,	0.6347826086956531f,	14.600000000000001f,	10.949999999999998f,	69.0f,	3.0f,	4.0f},
				{8.0f,	79.74599999999988f,	79.74599999999988f,	52.598f,	53.505633333333435f,	53.505633333333435f,	947.836315789471f,	608.0083333333342f,	300.0f,	19.0f,	30.0f}
		};
		//		
//		int T = 10;
//		int tau = 4;
//		NDArray features = manager.zeros(new Shape(T - tau, tau));
//		
//		NDArray time = manager.arange(1f, T+1);
//		NDArray x = time.mul(0.01).sin().add(
//		    manager.randomNormal(0f, 0.2f, new Shape(T), DataType.FLOAT32));
//		
//		for (int i = 0; i < tau; i++) {
//		    features.set(new NDIndex(":, {}", i), x.get(new NDIndex("{}:{}", i, T - tau + i)));
//		}
//		System.out.println(features);
		
//		float [][] ex = {{1, 2, 3}, {4, 5, 6}};

//		NDarray ex1_array = Shape(1, 2, 3);
//		for(int i = 0; i < ex1.length; i ++) {
//			
//		}
		
		NDManager manager = NDManager.newBaseManager();
		NDArray ex_array = manager.create(input);
		NDArray prev_state_1 = manager.zeros(new Shape(4, 100));
		NDArray prev_state_2 = manager.zeros(new Shape(4, 100));
//		System.out.println(prev_state_1);
//		NDList prev_state = new NDList(prev_state_1, prev_state_2);

//		prev_state.setName("prev_state");
//		ex_array = ex_array.reshape(1, 4, 11);
		System.out.println(ex_array);
		ex_array.setName("x");
		NDList inputs = new NDList(ex_array, prev_state_1, prev_state_2);
		
		Predictor<NDList, Float> predictor = model.newPredictor(translator);
		try {
			Float output = predictor.predict(inputs);
			System.out.println("Ouput: " + output);
		} catch (TranslateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		NDArray ex_array = manager.full(new Shape(1, 2, 3), ex);

//		NDArray labels = x.get(new NDIndex("" + tau + ":")).reshape(new Shape(-1,1));
		
//		System.out.println(labels);
	// end class
	}

//	public LinkedList<LinkedList<Double>> getState() {
//		return state;
//	}
//
//	public void setState(LinkedList<LinkedList<Double>> state) {
//		this.state = state;
//	}

	public int getSeqLength() {
		return seqLength;
	}

	public void setSeqLength(int seqLength) {
		this.seqLength = seqLength;
	}

	public int getNumLayer() {
		return numLayer;
	}

	public void setNumLayer(int numLayer) {
		this.numLayer = numLayer;
	}

	public int getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	public int getHidSize() {
		return hidSize;
	}

	public void setHidSize(int hidSize) {
		this.hidSize = hidSize;
	}
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

}
