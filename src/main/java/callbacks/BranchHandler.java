package callbacks;
 
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import utilities.UtilityLibrary;
import static constantsAndParams.Constants.*;
import static constantsAndParams.Parameters.*; 
import dataTypes.NodeAttachment;
import dataTypes.SubtreeMetaData;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.BranchDirection;
import ilog.cplex.IloCplex.NodeId;
import java.io.IOException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 * 
 * @author srini
 * 
 * 1) accumulates branching conditions and any other variable bounds , into the kids
 * 2) discards nodes, or entire subtree,  which are inferior to already known incumbent
 * 3) implements distributed MIP gap by using the bestKnownGlobalOptimum
 *
 */
public class BranchHandler extends IloCplex.BranchCallback{
    
    private static Logger logger=Logger.getLogger(BranchHandler.class);
      
    static   {
        
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%d{ISO8601} [%t] %-5p %c %x - %m%n");     
        try {
            logger.addAppender(new RollingFileAppender(layout,LOG_FOLDER+BranchHandler.class.getSimpleName()+PARTITION_ID+LOG_FILE_EXTENSION));
        } catch (IOException ex) {
             
        }
    }
        
    //meta data of the subtree which we are monitoring
    private SubtreeMetaData metaData;
    
    //best known optimum is used to prune nodes
    private double bestKnownGlobalOptimum;
    
    public BranchHandler (SubtreeMetaData metaData) {
        this.  metaData= metaData;       
    }
     
    public void refresh( double bestKnownGlobalOptimum) {
        this.bestKnownGlobalOptimum=bestKnownGlobalOptimum;
      
    } 
    
    /**
     * discard inferior nodes, or entire trees
     * Otherwise branch the 2 kids and accumulate variable bound information
     *   
     */
    protected void main() throws IloException {

        if ( getNbranches()> ZERO ){  
          
            //tree is branching
            //logger.debug(this.metaData.getGUID() + " Tree is branching and it has this many pending nodes " +this.metaData.getLeafNodesPendingSolution().size() ) ;
            
            //first check if entire tree can be discarded
            if (canTreeBeDiscarded() || (canNodeBeDiscarded()&&isSubtreeRoot())   ){
                
                //no point solving this tree any longer 
                 
                metaData.setEntireTreeDiscardable();
                abort();
                
            } else  /*check if this node can be discarded*/ if (canNodeBeDiscarded()) {               
                // this node and its kids are useless
                 
                prune();  
            } else {
                //we must create the 2 kids
                               
                //get the node attachment for this node, any child nodes will accumulate the branching conditions
                NodeAttachment nodeData = (NodeAttachment) getNodeData();
                if (nodeData==null ) { //it will be null for subtree root
                    NodeAttachment subTreeRoot = metaData.getRootNodeAttachment();
                    nodeData=new NodeAttachment (   subTreeRoot.isEasy(),  
                            subTreeRoot.getUpperBounds(), 
                            subTreeRoot.getLowerBounds(),  
                            subTreeRoot.getDepthFromOriginalRoot(), 
                            ZERO);         
                }
                //update the node attachment with end time                
                if (nodeData.getEndTimeFor_LP_Relaxation()<=ZERO)  {
                    nodeData.setEndTimeFor_LP_Relaxation(System.currentTimeMillis());
                    setNodeData(nodeData);
                }
                
                //get the branches about to be created
                IloNumVar[][] vars = new IloNumVar[TWO][] ;
                double[ ][] bounds = new double[TWO ][];
                BranchDirection[ ][]  dirs = new  BranchDirection[ TWO][];
                getBranches(  vars, bounds, dirs);

                //get bound tightenings
                Map< IloNumVar,Double > upperBoundTightenings = findIntegerBounds(true);
                Map< IloNumVar,Double > lowerBoundTightenings = findIntegerBounds(false);
                
                //now allow  both kids to spawn
                for (int childNum = ZERO ;childNum<getNbranches();  childNum++) {    
                    //apply the bound changes specific to this child
                    NodeAttachment thisChild  = UtilityLibrary.createChildNode( nodeData,
                            dirs[childNum], bounds[childNum], vars[childNum]  , isChildEasy() ); 

                    //apply bound tightenings

                    for (Entry<IloNumVar, Double> entry : upperBoundTightenings.entrySet()){
                        UtilityLibrary. mergeBound(thisChild, entry.getKey().getName(), entry.getValue()  , true);
                    }
                    for (Entry<IloNumVar, Double> entry : lowerBoundTightenings.entrySet()){
                        UtilityLibrary. mergeBound(thisChild, entry.getKey().getName(), entry.getValue()  , false);
                    }

                    //   create the  kid,  and attach node data  to the kid
                    NodeId nodeID = makeBranch(childNum,thisChild );
                    //make a note of the new child in the meta data
                    this.metaData.addUnsolvedLeafNodes(nodeID.toString(), thisChild);

                }//end for 2 kids
                
                //check if number of unsolved kids has grown too large                  
                if (this.metaData.getLeafNodesPendingSolution().size()> MAX_UNSOLVED_CHILD_NODES_PER_SUB_TREE) abort() ;
                
            } //and if else
        }//end getNbranches()> ZERO
    } //end main
                   
    private boolean canNodeBeDiscarded () throws IloException {
        boolean result = false;

        //get LP relax value
        double nodeObjValue = getObjValue();
        
        result = isMaximization  ? 
                    (nodeObjValue < getCutoff()) || (nodeObjValue <= bestKnownGlobalOptimum )  : 
                    (nodeObjValue > getCutoff()) || (nodeObjValue >= bestKnownGlobalOptimum );

        /*if (result) logger.debug(  " Discard node   " + bestKnownGlobalOptimum + " " +getCutoff() +
                " "+ nodeObjValue);*/
        return result;
    }
    
    //can this ILOCLPEX object  be discarded ?
    private boolean canTreeBeDiscarded(  ) throws IloException{     
        
        //check if objective and incumbent values are accurate even if some kids are 
        //not going to be solved here, because they have been migrated.
        //should be okay.
        
        double metric =  getBestObjValue() -bestKnownGlobalOptimum ;
        metric = metric /(EPSILON +bestKnownGlobalOptimum);
        
        //|bestnode-bestinteger|/(1e-10+|bestinteger|) 
        boolean mipHaltCondition =  RELATIVE_MIP_GAP >= Math.abs(metric)  ;
        
        //also halt if we cannot do better than bestKnownGlobalOptimum
        boolean inferiorityHaltConditionMax = isMaximization && 
                                              (bestKnownGlobalOptimum>=getIncumbentObjValue()) && 
                                              (bestKnownGlobalOptimum>=getBestObjValue());
        boolean inferiorityHaltConditionMin = !isMaximization && 
                                               (bestKnownGlobalOptimum<=getIncumbentObjValue()) && 
                                               (bestKnownGlobalOptimum<=getBestObjValue());
         
        /*if (inferiorityHaltConditionMax) logger.debug(  " Discard tree inferiorityHaltConditionMax " + bestKnownGlobalOptimum + " " +getIncumbentObjValue() +
                " "+ getBestObjValue());
        if (inferiorityHaltConditionMin) logger.debug(  " Discard tree inferiorityHaltConditionMin " + bestKnownGlobalOptimum + " " +getIncumbentObjValue() +
                " "+ getBestObjValue());*/
        return  mipHaltCondition || inferiorityHaltConditionMin|| inferiorityHaltConditionMax;       
      
    }
    
    private boolean isSubtreeRoot () throws IloException {
        
        boolean isRoot = true;
        
        if (getNodeData()!=null  ) {
            NodeAttachment thisNodeData =(NodeAttachment) getNodeData();
            if (thisNodeData.getDepthFromSubtreeRoot()>ZERO) {
                
                isRoot = false;
                
            }
        }    
        
        return isRoot;
        
    }
    
    private boolean isChildEasy(){
        //fill up later
        return false;
    }
    
    private Map< IloNumVar,Double > findIntegerBounds (boolean isUpperBound) throws IloException {
        Map< IloNumVar,Double > results = new  HashMap< IloNumVar,Double >();
        IloNumVar[] modelIntVars = this.metaData.getIntvars();
        
        double[] values  = isUpperBound ? getUBs(modelIntVars ): getLBs(modelIntVars);

        for (int index = ZERO ; index <modelIntVars.length; index ++ ){

            results.put( modelIntVars[index] , values[index]) ;
        }
        return results;
    }
    
}
