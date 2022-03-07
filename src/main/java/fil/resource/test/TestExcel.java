package fil.resource.test;

import java.io.FileInputStream;  
import java.io.FileNotFoundException;  
import java.io.IOException;
import java.util.LinkedList;

import org.apache.poi.ss.usermodel.Cell;  
import org.apache.poi.ss.usermodel.*;  
import org.apache.poi.ss.usermodel.Sheet;  
import org.apache.poi.ss.usermodel.Workbook;  
import org.apache.poi.xssf.usermodel.XSSFWorkbook;  

public class TestExcel{  
	
	public static void main(String[] args)   {  
		TestExcel rc = new TestExcel();   //object of the class  
		//reading the value of 2nd row and 2nd column
		String path = "C:\\Users\\kienkk\\Downloads\\2018-weekdays-quarter (2)\\test-TW.xlsx";
		LinkedList<Double> list = rc.excelToList(path); 
		System.out.println("list size: " + list.size());
		LinkedList<LinkedList<Double>> days = rc.separateDays(list, 2);
		System.out.println("Number of day: " + days.size());
		System.out.println("Size of day 0 is: " + days.get(0).size());
		for(double a : days.get(0)) {
			System.out.println("Values in day 0 is: " + a);

		}

//		for(Double a : list) {
//			System.out.println(a);
//		}
//		System.out.println("Number of day: " + list.size()/100);
	}
	
	// create a list of days based on TW
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
			System.out.println("days size: " + days.size());
		}
		return days;
	}
	
//method defined for reading a cell  
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
}  
