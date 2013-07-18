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

public class CreateInstance
{
	public Instance createInstance(AmazonEC2 ec2, String imageId, KeyPair kpairs,CreateSecurityGroupResult seqRes,String zone) throws Exception
	{
		Instance createdInstance=null;
		String newInId=null;
	    try   
	    {	
	    	//String imageId="ami-aecd60c7"; //Amazon Linux AMI x86_64 
	        int minInstanceCount = 1; // create 1 instance
	        int maxInstanceCount = 1;
	        RunInstancesRequest rir = new RunInstancesRequest(imageId, minInstanceCount, maxInstanceCount);
	        rir.setInstanceType("t1.micro");
	        //Set keyPair for the user
	      //  rir.setKeyName(kpairs.getKeyName());
	        rir.setKeyName("programKey");	  
	        //Set SecurityGroup
	       // rir.withSecurityGroupIds(seqRes.getGroupId());
	        rir.withSecurityGroups("programSecurityGroup");
	        //Set zone
	        if(zone!=null)
	        {
	        	Placement placement=new Placement(zone);
	        	rir.setPlacement(placement);
	        }

	        RunInstancesResult result = ec2.runInstances(rir);
	    
	        //get instanceId from the result
	        newInId=result.getReservation().getInstances().get(0).getInstanceId();// get the new instance ID that I just created
	        //polling the new instance to check its state, if it's in running state, fetch its dns information
	        DescribeInstancesResult describeInstancesRequest;
	        List<Reservation> reservations;
	        Set<Instance> instances;
	        String newInstanceState=null;
	        Instance one=null;
	        while(true)
            {
            	describeInstancesRequest = ec2.describeInstances();            	
            	reservations = describeInstancesRequest.getReservations();
                instances = new HashSet<Instance>();
                // add all instances to a Set.
                for (Reservation reservation : reservations) {
                	instances.addAll(reservation.getInstances());
                }
                for (Instance ins2 : instances){
                	
                	if (ins2.getInstanceId().equals(newInId))
                    	{
                    		newInstanceState=ins2.getState().getName();
                    		createdInstance=ins2;
                    		break;
                    	}
                }
            	System.out.println("The new created instance state is: "+newInstanceState);
            		if(newInstanceState!=null && newInstanceState.equals("running"))
            		{
            			break;	
            		}
            	else
            	{	try
            		{	Thread.sleep(5000);
            		
            		}
            		catch(Exception e){
            			System.out.println("Error: " + e.getMessage());
            		}
            	}
            }
	           System.out.println("The new instance dnsName is: "+createdInstance.getPublicDnsName());
	           System.out.println("The new instance public ip address is: "+createdInstance.getPublicIpAddress());
	           System.out.println("The new instance keyName is: "+ createdInstance.getKeyName());
	    }
		   catch (AmazonServiceException ase) {
			    System.out.println("Caught Exception: " + ase.getMessage());
			    System.out.println("Reponse Status Code: " + ase.getStatusCode());
			    System.out.println("Error Code: " + ase.getErrorCode());
			    System.out.println("Request ID: " + ase.getRequestId());
		   }
	        return createdInstance;

	    }
	}