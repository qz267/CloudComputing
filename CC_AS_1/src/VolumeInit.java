import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.AttachVolumeRequest;
import com.amazonaws.services.ec2.model.CreateVolumeRequest;
import com.amazonaws.services.ec2.model.CreateVolumeResult;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.DetachVolumeRequest;
import com.amazonaws.services.ec2.model.Volume;

/**
 * This class implements Volume methods.
 * 
 * @Modified by zhengqin
 * 
 */
public class VolumeInit {
	AmazonEC2 ec2;
	public VolumeInit(AmazonEC2 ec2)
	{
		this.ec2=ec2;
	}
	public String createVolume(String zone) throws Exception
	{
		String createdVolumeId =null;  
		try{
	        //create a volume
			CreateVolumeRequest cvr = new CreateVolumeRequest();
	        cvr.setAvailabilityZone(zone);
	        cvr.setSize(10); //size = 10 gigabytes
        	CreateVolumeResult volumeResult = ec2.createVolume(cvr);
        	Volume createdVol=volumeResult.getVolume();
        	DescribeVolumesResult describeVol;
        	createdVolumeId = createdVol.getVolumeId();
        	Volume one=null;
        	while(true)
        	{
        		describeVol=ec2.describeVolumes();
        		for(Volume vol:describeVol.getVolumes())
        		{
        			if(vol.getVolumeId().equals(createdVolumeId))
        				{
        					one=vol;
        					break;
        				}
        		}
        		if(one!=null && one.getState().equals("available"))
        			break;
        		else
        			Thread.sleep(20*1000);
        	}
        
        	System.out.println("Created volume: "+createdVolumeId);
         	
	      }
	      catch (AmazonServiceException ase) {
              System.out.println("Caught Exception: " + ase.getMessage());
              System.out.println("Reponse Status Code: " + ase.getStatusCode());
              System.out.println("Error Code: " + ase.getErrorCode());
              System.out.println("Request ID: " + ase.getRequestId());
      }
	      return createdVolumeId;
	}
	public void attachVolume(String instanceId,String createdVolumeId)
	{
	      try   	
	      {	
	        	AttachVolumeRequest avr = new AttachVolumeRequest();
	        	avr.setVolumeId(createdVolumeId);
	        	avr.setInstanceId(instanceId);
	        	avr.setDevice("/dev/sdf");
	        	ec2.attachVolume(avr);
	        	System.out.println("Attach volume: "+createdVolumeId+" to instance: "+instanceId);
	      }
	      catch (AmazonServiceException ase) {
	  	    System.out.println("Caught Exception: " + ase.getMessage());
	  	    System.out.println("Reponse Status Code: " + ase.getStatusCode());
	  	    System.out.println("Error Code: " + ase.getErrorCode());
	  	    System.out.println("Request ID: " + ase.getRequestId());
	  	}
	}
	public void detachVolume(String instanceId,String createdVolumeId)
	{
		try
		{
	        	DetachVolumeRequest dvr = new DetachVolumeRequest();
	        	dvr.setVolumeId(createdVolumeId);
	        	dvr.setInstanceId(instanceId);
	        	ec2.detachVolume(dvr);
	        	System.out.println("Detach volume: "+createdVolumeId+" for instance: "+instanceId);
		}
	      catch (AmazonServiceException ase) {
		  	    System.out.println("Caught Exception: " + ase.getMessage());
		  	    System.out.println("Reponse Status Code: " + ase.getStatusCode());
		  	    System.out.println("Error Code: " + ase.getErrorCode());
		  	    System.out.println("Request ID: " + ase.getRequestId());
		  	}
		
	}

}
