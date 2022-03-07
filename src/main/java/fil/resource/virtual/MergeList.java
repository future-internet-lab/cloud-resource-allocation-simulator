package fil.resource.virtual;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Java program to merge multiple List into a single List using JDK and open source
 * Apache Commons library.
 *
 * @author Javin Paul
 */
public class MergeList {


    public static void main(String args[]) {
      
       ArrayList<Integer> hundreads = new ArrayList<>();
       hundreads.add(0);
       hundreads.add(1);
       hundreads.add(2);
       ArrayList<Integer> thousands = new ArrayList<>();
       thousands.add(100);
       thousands.add(200);
       thousands.add(300);
        // merging two list using core Java
       ArrayList<Integer> merged = new ArrayList<>();
       merged.addAll(hundreads);
        merged.addAll(thousands);
      
        System.out.println("List 1 : " + hundreads);
        System.out.println("List 2 : " + thousands);
        System.out.println("Merged List : " + merged);
        
        hundreads.remove(0);
        System.out.println("List 1 : " + hundreads);
        System.out.println("Merged List : " + merged);
        // another way to merge two list in Java
        // using ListUtils from Apache commons Collection
      
    }
  
}


