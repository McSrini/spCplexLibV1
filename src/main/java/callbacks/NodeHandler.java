package callbacks;
 
import dataTypes.NodeAttachment;
import dataTypes.SubtreeMetaData;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import static constantsAndParams.Constants.*;
import static constantsAndParams.Parameters.*;
import ilog.cplex.IloCplex.NodeId;
import java.io.IOException;
import org.apache.log4j.*; 

/**
 * 
 * @author srini
 * 
 * records solution start time for this node
 *
 */
public class NodeHandler extends IloCplex.NodeCallback{
    
    private static Logger logger=Logger.getLogger(NodeHandler.class);
    
    //meta data of the subtree which we are monitoring
    private SubtreeMetaData metaData;
    
    public NodeHandler (SubtreeMetaData metaData) throws IOException {
        this.  metaData= metaData;
        
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%d{ISO8601} [%t] %-5p %c %x - %m%n");     
        logger.addAppender(new RollingFileAppender(layout,LOG_FOLDER+this.getClass().getSimpleName()+PARTITION_ID+LOG_FILE_EXTENSION));
    }
 
    protected void main() throws IloException {
        
        if (ZERO<getNremainingNodes64()) {
            
            //pick a remaining node,if any, that is in the pending list
            int selectedNodeIndex = -ONE;
            NodeId selectedNodeID = null;
            for (int index = ZERO; index < getNremainingNodes64(); index ++){
                selectedNodeID=getNodeId(index);
                if (this.metaData.getLeafNodesPendingSolution().containsKey(selectedNodeID.toString())){
                    //select this node 
                    selectedNodeIndex= index ;
                    break;
                }
            }
            
            if (selectedNodeIndex>=ZERO) {
                
                //we have work to do
                selectNode(selectedNodeIndex);
                
                //get the node data for the node chosen for solving 
                NodeAttachment nodeData = (NodeAttachment) getNodeData(selectedNodeIndex );
                
                //Mark the solution start time. 
                //Solution time may end up being an overestimate, since it could include spark iteration restart time.
                /*if (nodeData.getStartTimeFor_LP_Relaxation()<=ZERO)*/ nodeData.setStartTimeFor_LP_Relaxation(System.currentTimeMillis());
                setNodeData(selectedNodeIndex,nodeData);
                
                //remove this node from the list of unsolved nodes
                metaData.removeUnsolvedLeafNode( selectedNodeID.toString());
                //mark it as being solved
                this.metaData.setNodeBeingSolved( selectedNodeID, nodeData);
                
            } else {
                //no kids left to solve, we are done solving this subtree
                logger.info("All kids migrated for tree" + metaData.getGUID());
                this.metaData.setAllKidsMigratedAway();
            }            
            
        } else{
            //no kids left to solve, we are done solving this subtree
            //
            //I am not sure why the node callback will look for a node to solve, if there are no kids pending solution
            //
            logger.info("No remaining nodes for tree" + metaData.getGUID());
            this.metaData.setAllKidsMigratedAway();            
        }        
    }//end main method

}
