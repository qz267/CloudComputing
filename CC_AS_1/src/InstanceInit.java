import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.KeyPair;
import com.amazonaws.services.ec2.model.Placement;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;



/**
 * This class implements instance initial methods.
 * 
 * @author zhengqin
 * 
 */
public class InstanceInit {
	public Instance instanceinit(AmazonEC2 ec2, String imageId, KeyPair keyPair,CreateSecurityGroupResult seqRes,String zone) throws Exception
	{
		Instance instanceInit=null;
		String newInId=null;
	    try   
	    {	
	        int minInstanceCount = 1;
	        int maxInstanceCount = 1;
	        RunInstancesRequest runIRe = new RunInstancesRequest(imageId, minInstanceCount, maxInstanceCount);
	        runIRe.setInstanceType("t1.micro");
	        runIRe.setKeyName("ColoudWorkingSystemKey");	  
	        runIRe.withSecurityGroups("cwsSecurityGroup");
	        
	        //Initial instance's local zone if not.
	        if(zone!=null)
	        {
	        	Placement placement=new Placement(zone);
	        	runIRe.setPlacement(placement);
	        }
	        
	        RunInstancesResult runIRs = ec2.runInstances(runIRe);
	        newInId=runIRs.getReservation().getInstances().get(0).getInstanceId();
	        DescribeInstancesResult describeInstancesRequest;
	        List<Reservation> reservations;
	        Set<Instance> instances;
	        String newInstance=null;
	        
	        while(true)
            {
            	describeInstancesRequest = ec2.describeInstances();            	
            	reservations = describeInstancesRequest.getReservations();
                instances = new HashSet<Instance>();
                for (Reservation reservation : reservations) {
                	instances.addAll(reservation.getInstances());
                }
                for (Instance tempInstance : instances){
                	
                	if (tempInstance.getInstanceId().equals(newInId))
                    	{
                		instanceInit = tempInstance;
                    	newInstance =tempInstance.getState().getName();
                    	break;
                    	}
                }
            	System.out.println("New instance is "+newInstance+" now, please wait...");
            		if(newInstance!=null && newInstance.equals("running"))
            		{
            			break;	
            		}
            	else
            	{	try
            		{	
            			Thread.sleep(5000);
            		}
            		catch(Exception e){
            			System.out.println("Error: " + e.getMessage());
            		}
            	}
            }
	           System.out.println("New instance's DNSName is: "+instanceInit.getPublicDnsName());
	           System.out.println("New instance's IP address is: "+instanceInit.getPublicIpAddress());
	    }
		   catch (AmazonServiceException ase) {
			    System.out.println("Caught Exception: " + ase.getMessage());
			    System.out.println("Reponse Status Code: " + ase.getStatusCode());
			    System.out.println("Error Code: " + ase.getErrorCode());
			    System.out.println("Request ID: " + ase.getRequestId());
		   }
	        return instanceInit;
	    }
}
