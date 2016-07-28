package solver;

import java.io.IOException;
import java.util.List;

import callbacks.BranchHandler;
import callbacks.NodeHandler;
import dataTypes.SubtreeMetaData; 
import dataTypes.NodeAttachment;
import dataTypes.Solution;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.cplex.IloCplex; 
import static constantsAndParams.Parameters.*;
import static constantsAndParams.Constants.*;

public class Solver {
    
    //this is the CPLEX object we are attached to  
    private IloCplex cplex   ;
    private SubtreeMetaData metaData;
    
    //this is the branch handler for the CPLEX object
    private BranchHandler branchHandler;
    //and the node handler
    private NodeHandler nodeHandler ;
         
    public Solver (IloCplex cplex , SubtreeMetaData metaData ) throws Exception{
            
        this.cplex=cplex;
        this.  metaData=  metaData;
        
        branchHandler = new BranchHandler(      metaData   );
        nodeHandler = new  NodeHandler (    metaData) ;
        
        this.cplex.use(branchHandler);
        this.cplex.use(nodeHandler);   
        
        setSolverParams();  
    
    }
    
    public void setSolverParams() throws IloException {
        //depth first?
        if ( DEPTH_FIRST_SEARCH) cplex.setParam(IloCplex.Param.MIP.Strategy.NodeSelect, ZERO); 
        
        //MIP gap
        if ( RELATIVE_MIP_GAP>ZERO) cplex.setParam( IloCplex.Param.MIP.Tolerances.MIPGap, RELATIVE_MIP_GAP);

        //others
    }
    
    public boolean isEntireTreeDiscardable() {
        return this.metaData.isEntireTreeDiscardable();
    }
    
    public IloCplex.Status solve(double timeSliceInSeconds,     double bestKnownGlobalOptimum   ) 
            throws IloException, IOException{
        
        //can we supply MIP  start along with bestKnownGlobalOptimum ?
                
        branchHandler.refresh(bestKnownGlobalOptimum);  
       
        cplex.setParam(IloCplex.Param.TimeLimit, timeSliceInSeconds); 
        cplex.solve();
        
        return cplex.getStatus();
    }
    
}
