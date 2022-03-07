package fil.resource.virtual;

import java.util.LinkedList;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import fil.resource.substrate.Rpi;


public class GenRequest {
	final static double LIVETIME = 2.0;
//	final static double TIMEWINDOW = 1.0;
	final static int COF = 3; //6 for full 24h option //3 for not-full option
	final static int PINUMBER = 300;
	final static int HOURS = 24;
	private LinkedList<Double> listRaw = new LinkedList<>();
//	private LinkedList<LinkedList<Event>> listEvent;
	private LinkedList<Event> listEvent;
	private LinkedList<HashMap<Rpi, Double>> listEventCR;
//	private PoissonDistribution poisson;
	private int [] LUT_24 = {32,22,20,23,37,89,211,335,337,282,263,269,
			276,287,313,350,375,304,269,151,110,80,30,20}; //full 24h
	private Double [] LUT_IL = {0.025,0.05,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0}; // not-full
	private String type;
	private int numOfWD;
	
	public GenRequest() {
//		this.listTotalRequest = new LinkedList<>();
//		this.poisson = new PoissonDistribution();
		this.setType("il");
		this.setNumOfWD(0);
		this.setListEvent(new LinkedList<>());
		this.setListEventCR(new LinkedList<>());
		this.setListEventTW(new LinkedList<>());
	}
	
	public GenRequest(String path) {
		this.setListRaw(excelToList(path));
		this.setListEvent(new LinkedList<>());
		this.setListEventCR(new LinkedList<>());
		this.setListEventTW(new LinkedList<>());
	}
	
	public void generate(String type) {
		
		this.type = type;

		if(this.type == "il") {
			this.numOfWD = LUT_IL.length;
		} else if(this.type == "24h") {
			this.numOfWD = LUT_24.length;
		}else {
			System.out.println("Wrong input");
			System.exit(1);
		}
		int sfcID = 0;
		Random generator = new Random();		
		for(int WD = 0; WD < this.numOfWD; WD ++) {
	
			HashMap<Rpi, Double> deviceEachTW = new HashMap<>(); // store request in 1 TW
//			int lambda = LUT[WD]*COF;
			double lambda = 0.0;
			double timeArrival = WD;

			if(type == "il") {
				lambda = LUT_IL[WD]*2100;
			} else if(type == "24h") {
				lambda = LUT_24[WD]*COF;
			}else {
				System.out.println("Wrong input");
				System.exit(1);
			}

			while(timeArrival < (WD + 1)) {
				double interArrival = (StdRandom.exp(lambda));
				timeArrival += interArrival; // arrival time of the next request
				timeArrival = (double)Math.round(timeArrival * 100000d) / 100000d;
				double endTime = timeArrival + StdRandom.exp(1.0/LIVETIME*1.0);
				endTime = (double)Math.round(endTime * 100000d) / 100000d;
				int piID = generator.nextInt(PINUMBER);
				Rpi pi = new Rpi(piID);
				deviceEachTW.put(pi, endTime);
				SFC sfc = new SFC(sfcID, piID, endTime);
				sfcID ++;
				Event join = new Event("join", sfc, timeArrival, piID);
				Event leave = new Event("leave", sfc, endTime, piID);
				this.listEvent.add(join);
				this.listEvent.add(leave);
			}
			this.listEventCR.add(deviceEachTW);
		}
		// sort event in time order following an input Time Window
		this.listEvent = sortEvent(this.listEvent);
	}
	
	public void generate(int day, double tw) {
		// divide raw data follows defined tw
		LinkedList<LinkedList<Double>> days = separateDays(this.getListRaw(), tw);
		LinkedList<Double> aDay = days.get(day);
		System.out.println("Number of simulation day: " + days.size());
		Random generator = new Random();
		int sfcID = 0;
		double timeArrival = 0.0;
		this.setNumOfWD(HOURS); // temporary no better choice
		for(int t = 0; t < aDay.size(); t ++) {
//			int lambda = LUT[WD]*COF;
			double lambda = aDay.get(t);
			if(lambda == 0)
				lambda = 1;
			lambda *= COF;
			while(timeArrival < tw * (t + 1)) { // store request based on TW
				double interArrival = (StdRandom.exp(lambda));
				timeArrival += interArrival; // arrival time of the next request
				timeArrival = (double)Math.round(timeArrival * 100000d) / 100000d;
				double endTime = timeArrival + StdRandom.exp(1.0/LIVETIME*1.0);
				endTime = (double)Math.round(endTime * 100000d) / 100000d;
				int piID = generator.nextInt(PINUMBER);
				Rpi pi = new Rpi(piID);
				SFC sfc = new SFC(sfcID, piID, endTime);
				sfcID ++;
				Event join = new Event("join", sfc, timeArrival, piID);
				Event leave = new Event("leave", sfc, endTime, piID);
				this.listEvent.add(join);
				this.listEvent.add(leave);		
			}
		}
		// sort event in time order following an input Time Window
		this.listEvent = sortEvent(this.listEvent);
	}

	
	public LinkedList<LinkedList<Double>> generateTW(double tw) {
		LinkedList<LinkedList<Double>> days = separateDays(this.getListRaw(), tw);
		System.out.println("Number of day: " + days.size());
		return days;
	}
	
	public LinkedList<Double> excelToList(String filePath)  {
		LinkedList<Double> listData = new LinkedList<>();
		int cellNum = 2;			// change it to change the column of data
		Workbook wb = null;           //initialize Workbook null  
		try  {  
			//reading data from a file in the form of bytes  
			FileInputStream fis = new FileInputStream(filePath);  
			//constructs an XSSFWorkbook object, by buffering the whole stream into the memory  
			wb = new XSSFWorkbook(fis);  
		}  
		catch(FileNotFoundException e)  {  
			e.printStackTrace();  
		}  
		catch(IOException e1)  {  
			e1.printStackTrace();  
		}  
		Sheet sheet=wb.getSheetAt(0);   //getting the XSSFSheet object at given index
		int numRowMax = sheet.getPhysicalNumberOfRows();
		for(int i = 0; i < numRowMax; i++) {
			Row row = sheet.getRow(i); //returns the logical row
			Cell cell = row.getCell(cellNum); //getting the cell representing the given column
			if (cell != null) {
				double value = cell.getNumericCellValue();    //getting cell value
				listData.add(value);
			}
		}
		return listData;               //returns the cell value  
	}  
	
	public LinkedList<LinkedList<Double>> separateDays(LinkedList<Double> list, double tw){
		LinkedList<LinkedList<Double>> days = new LinkedList<>();
		double period = tw/0.25;
		for(int i = 0; i < list.size(); i += 96) {
			double sum = 0.0;
			LinkedList<Double> day = new LinkedList<>();
			for( int j = i; j < i + 96; j ++) {
				sum += list.get(j);
				if((j + 1) % period == 0) {
					day.add(sum);
					sum = 0.0;
				}
			}
			days.add(day);
//			System.out.println("days size: " + days.size());
		}
		return days;
	}
	
	public LinkedList<LinkedList<Event>> sortInTW(double tw){
		LinkedList<LinkedList<Event>> result = new LinkedList<>();
		for(Double T = 0.0; T < this.numOfWD; T += tw) {
			LinkedList<Event> listEvent_temp = new LinkedList<>();
			for(Event event : this.listEvent) {
				if(event.getTime() > T && event.getTime() <= (T + tw))
					listEvent_temp.add(event);
			}
			Collections.sort(listEvent_temp);
			result.add(listEvent_temp);
		}
		return result;
	}
	
	public LinkedList<Event> sortEvent(LinkedList<Event> listEvent){
		//order = true: low to high, false means high to low
		Collections.sort(listEvent, new Comparator<Event>() {
			@Override
	        public int compare(Event event1, Event event2) { 
	            // for comparison
				if(event1.getTime() > event2.getTime()) {
					return -1;
				}
				else if(event1.getTime() > event2.getTime()) {
					return 1;
				}
				return 0;
	        }
		});
		return listEvent;
	}

	
	public static void main(String[] args) {
		String path = "C:\\Users\\kienkk\\Downloads\\2018-weekdays-quarter (2)\\test-TW.xlsx";
		GenRequest sample = new GenRequest(path);
//		sample.generator(0.5);
		sample.generate(1, 0.25);
		LinkedList<LinkedList<Event>> list = sample.sortInTW(0.25);
		System.out.println("Size of list: " + list.size());
		for(LinkedList<Event> hour : list) {
			System.out.println("Size of hour: " + hour.size());
		}
	}

	public LinkedList<Event> getListEvent() {
		return listEvent;
	}
	public void resetListEvent() {
		for(Event event: listEvent) {
			event.getSfc().reset();
		}
	}

	public void setListEvent(LinkedList<Event> listEvent) {
		this.listEvent = listEvent;
	}

	public LinkedList<Event> getListEventTW() {
		return listEvent;
	}

	public void setListEventTW(LinkedList<Event> listEventTW) {
		this.listEvent = listEventTW;
	}

	public LinkedList<HashMap<Rpi, Double>> getListEventCR() {
		return listEventCR;
	}

	public void setListEventCR(LinkedList<HashMap<Rpi, Double>> listEventCR) {
		this.listEventCR = listEventCR;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getNumOfWD() {
		return numOfWD;
	}

	public void setNumOfWD(int numOfWD) {
		this.numOfWD = numOfWD;
	}

	public LinkedList<Double> getListRaw() {
		return listRaw;
	}

	public void setListRaw(LinkedList<Double> listRaw) {
		this.listRaw = listRaw;
	}
	
//	public LinkedList<HashMap<Rpi, Double>> getListTotalRequest() {
//		return listTotalRequest;
//	}
//
//	public void setListTotalRequest(LinkedList<HashMap<Rpi, Double>> listTotalRequest) {
//		this.listTotalRequest = listTotalRequest;
//	}
	
}
