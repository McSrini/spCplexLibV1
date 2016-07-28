package drivers;

import callbacks.NodeHandler;
import static constantsAndParams.Constants.SAV_FILENAME;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import dataTypes.ActiveSubtree;
import dataTypes.NodeAttachment;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import static constantsAndParams.Constants.*;
import static constantsAndParams.Parameters.*;
import dataTypes.Solution;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 * 
 * @author srini
 * 
 * solve trees for 10 seconds each , until they exceed 10 thousand nodes - at which point distribute all but 10000 nodes away
 * 
 * remove any inferior or completed subtrees and update incumbent solution
 * 
 * print incumbent when no trees left
 * 
 */
public class SimpleDriver {
    
     private static Logger logger=Logger.getLogger(SimpleDriver.class);
    
    private static int treesLeft ( List<ActiveSubtree> activeSubtreeList) throws Exception {
        int TREES_LEFT=  ZERO;
        for (int index = ZERO ; index < activeSubtreeList.size(); index ++){
            ActiveSubtree tree = activeSubtreeList.get(index);
            if (tree.isEntireSubtreeDiscardable()) continue ;
            if (tree.isSolvedToCompletion()) continue ;
            
            TREES_LEFT++;
        }
         
        logger.info("Number of trees left  "+ TREES_LEFT);
        return TREES_LEFT;
    }

    public static void main(String[] args) throws Exception {
        
        
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%d{ISO8601} [%t] %-5p %c %x - %m%n");     
        logger.addAppender(new RollingFileAppender(layout,LOG_FOLDER+SimpleDriver.class.getSimpleName()+PARTITION_ID+ LOG_FILE_EXTENSION));
                
        double bestKnownIncumbentValue = PLUS_INFINITY;
        Solution bestKnownSolution = new Solution();
        int bestKnownIncumbentIndex = -ONE;
        
        List <NodeAttachment> farmedOutNodes = new ArrayList <NodeAttachment>(); 
       
        int TIME_SLICE = 5 ; //seconds
        int MAX_TREE_SIZE_AFTER_TRIMMING = 10000 ;  
        
        int ITER_NUMBER = 0;
         
        logger.info("Started at  "+ LocalDateTime.now());
       
        try {
            
            List<ActiveSubtree> activeSubtreeList = new ArrayList<ActiveSubtree>();
            activeSubtreeList.add(new ActiveSubtree( new NodeAttachment()));
           
            while (ZERO < treesLeft (  activeSubtreeList)) {
                
                ITER_NUMBER++;
                 
                //solve active trees for time slice
                for (int index = ZERO ; index < activeSubtreeList.size(); index ++){
                    ActiveSubtree tree = activeSubtreeList.get(index);
                    if (tree.isEntireSubtreeDiscardable()) continue ;
                    if (tree.isSolvedToCompletion()) continue ;
                     
                    tree.solve( TIME_SLICE, bestKnownIncumbentValue);
                }
                
                //update the incumbent, ignoring discardable trees
                for (int index = ZERO ; index < activeSubtreeList.size(); index ++){
                    
                    ActiveSubtree tree = activeSubtreeList.get(index);
                    if (tree.isEntireSubtreeDiscardable()) continue ;
                     
                    if (tree.getSolution().getObjectiveValue() < bestKnownIncumbentValue){
                        bestKnownIncumbentValue =tree.getSolution().getObjectiveValue() ;
                        bestKnownIncumbentIndex =index;
                        bestKnownSolution =  tree.getSolution();
                    }
                    
                    //if the tree has more than 10000 nodes, farm out nodes
                    if (MAX_TREE_SIZE_AFTER_TRIMMING< tree.getPendingChildNodeCount()) {
                        farmedOutNodes =tree.farmOutNodes(MAX_TREE_SIZE_AFTER_TRIMMING);
                    }
                     
                }
                
                //convert farmed out nodes into subtrees and add into tree list
                for (int index = ZERO ; index < farmedOutNodes.size(); index ++){
                    NodeAttachment node = farmedOutNodes.get(index);
                    activeSubtreeList.add(new ActiveSubtree(node));
                }
                farmedOutNodes = new ArrayList <NodeAttachment>(); 
                
            }   
            
        }catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        logger.info("Best solution is "+bestKnownSolution.toString());
        logger.info("Number of iterations is "+ ITER_NUMBER);
        logger.info("Completed at  "+ LocalDateTime.now());
        
    }//main
}//class
                
  

/**
 * 
 * 
 
0029 1.0
0028 0.0
0027 1.0
0026 1.0
0025 1.0
0024 0.0
0023 1.0
0022 1.0
0021 1.0
0020 0.0
0019 0.0
0018 1.0
0017 0.0
0016 1.0
0015 1.0
0014 0.0
0045 1.0
0013 0.0
0044 0.0
0012 0.0
0043 1.0
0011 1.0
0042 1.0
0010 1.0
0041 1.0
0040 1.0
0009 1.0
0008 1.0
0039 1.0
0007 -0.0
0038 0.0
0006 1.0
0037 1.0
0005 1.0
0036 0.0
0004 1.0
0003 1.0
0035 1.0
0002 1.0
0034 1.0
0001 1.0
0033 0.0
0032 0.0
0031 0.0
0030 1.0
 
 * 
 */

