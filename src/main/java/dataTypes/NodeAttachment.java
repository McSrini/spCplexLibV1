package dataTypes;

import java.io.Serializable;
import java.util.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import static constantsAndParams.Constants.*;
 

/**
 * 
 * @author srini
 *
 * This object is attached to every tree node, and can be used to reconstruct migrated leaf nodes.
 * 
 * It contains branching variable bounds, and other useful information.
 * 
 */
public class NodeAttachment implements Serializable{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    //every time there is a branching on a variable, we update on of these lists with the
    //new bound corresponding to the branching condition
    //
    //the key is the Variable name
    //
    //Note that this list may also have bounds on non-branching variables 
    //
    private Map< String, Double > upperBounds  = new HashMap< String, Double >();
    private Map< String, Double > lowerBounds = new HashMap< String, Double >();
    
    public NodeAttachmentMetadata metadata;
    
    //constructors    
    public NodeAttachment () {
      
        metadata = new NodeAttachmentMetadata();
    }
    
    public NodeAttachment ( boolean easy,  Map< String, Double > upperBounds, 
            Map< String, Double > lowerBounds,  int distanceFromOriginalRoot, int distanceFromSubtreeRoot) {
         
        this.upperBounds = new HashMap< String, Double >();
        this.lowerBounds = new HashMap< String, Double >();
        for (Entry <String, Double> entry : upperBounds.entrySet()){
            this.upperBounds.put(entry.getKey(), entry.getValue()  );
        }
        for (Entry <String, Double> entry : lowerBounds.entrySet()){
            this.lowerBounds.put(entry.getKey(), entry.getValue()  );
        }
        
        metadata = new NodeAttachmentMetadata();
        metadata.isEasy = easy;
        metadata.distanceFromOriginalRoot=distanceFromOriginalRoot;
        metadata.distanceFromSubtreeRoot=distanceFromSubtreeRoot;
    }
    
    public String toString() {
        String result = metadata.distanceFromOriginalRoot + NEWLINE;
        result += metadata.distanceFromSubtreeRoot+ NEWLINE;
        for (Entry entry : upperBounds.entrySet()) {
            result += entry.getKey()+BLANKSPACE + entry.getValue()+ NEWLINE;
                    
        }
        for (Entry entry : lowerBounds.entrySet()) {
            result += entry.getKey()+BLANKSPACE + entry.getValue()+ NEWLINE;
                    
        }
        return result;
    }
    
    public int getDepthFromOriginalRoot(  ){
        return metadata.distanceFromOriginalRoot  ;
    }
    
    public void setDepthFromSubtreeRoot(int depth ){
        this.metadata.distanceFromSubtreeRoot = depth;
    }
    
    public int getDepthFromSubtreeRoot(){
        return metadata.distanceFromSubtreeRoot  ;
    }
    
    public double getTimeFor_LP_Relaxation() {
        return  this.metadata.endTimeFor_LP_Relaxation_millisec-this.metadata.startTimeFor_LP_Relaxation_millisec;
    }

    public double getStartTimeFor_LP_Relaxation() {        
        return this.metadata.startTimeFor_LP_Relaxation_millisec;
    }
    public double getEndTimeFor_LP_Relaxation() {        
        return this.metadata.endTimeFor_LP_Relaxation_millisec;
    }
    
    public void setStartTimeFor_LP_Relaxation(double time) {
        metadata.startTimeFor_LP_Relaxation_millisec = time;
    }

    public void setEndTimeFor_LP_Relaxation(double time) {
        metadata.endTimeFor_LP_Relaxation_millisec = time;
    }
    
    public void setEasy(){
        metadata.isEasy = true;
    }
    
    public boolean isEasy(){
        return metadata.isEasy  ;
    }

    public Map< String, Double >   getUpperBounds   () {
        return  upperBounds ;
    }

    public Map< String, Double >   getLowerBounds   () {
        return  lowerBounds ;
    }

    
    
}
