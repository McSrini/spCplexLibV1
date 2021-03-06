package constantsAndParams;

import java.io.Serializable;

public class Constants implements Serializable{
    
    public static final int ZERO = 0;
    public static final int ONE = 1;
    public static final int TWO = 2;
     public static final int FIVE = 5;
    public static final int SIX = 6;
     public static final int TEN = 10;
    public static final int SIXTEEN = 16;
    public static final int MINUS_ONE = -1;
    public static final int THOUSAND = 1000;
    public static final long PLUS_INFINITY = (long) Math.pow(TEN, TWO*SIXTEEN);
    public static final long MINUS_INFINITY = -1*PLUS_INFINITY;
    public static final double EPSILON = 0.0000000001;
    
    public  static final  boolean isMaximization = false;
    
    public static final String BLANKSPACE = " ";
    public static final String NEWLINE = "\n";
    public static final String LOG_FILE_EXTENSION = ".log";
    public static final String ORIGINAL_PROBLEM_TREE_GUID = "ORIGINAL_PROBLEM_TREE_GUID";

}

 