
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.KeyPair;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;

/**
 * This class implements two methods.
 * InstanceRelease method used to release a instance.
 * Display method used to display the simulate of a elastic balance process.
 * 
 * @author zhengqin
 * 
 */
public class Runner {

    /*
     * Important: Be sure to fill in your AWS access credentials in the
     *            AwsCredentials.properties file before you try to run this
     *            sample.
     * http://aws.amazon.com/security-credentials
     */

	/**
	 * @param args
	 */
	static AmazonEC2 ec2;
	static AWSCredentials credentials;
	static CloudWatch cw;
	static String AMIa=null, za=null, AMIb=null, zb=null;
	static int tempa = 50;
	static int tempb=2*tempa;
	static String va,vb;

	public static String instanceRelease(String instanceId,String volumeId,String amiName,String ami) throws Exception
	{
		String amiA = null;
		String amiB = ami;
		VolumeInit volume=new VolumeInit(ec2);
		volume.detachVolume(instanceId, volumeId);
		AMI amis=new AMI(ec2);
		amiA =amis.createAMI(instanceId, amiName);
		while(amiA==null)
		{
			Thread.sleep(60*1000);
			amiA = amis.createAMI(instanceId, amiName);
		}
			
        System.out.println("Release Instance: "+instanceId);
        List<String> instanceIds = new LinkedList<String>();
        instanceIds.add(instanceId);
        try
        {TerminateInstancesRequest tir = new TerminateInstancesRequest(instanceIds);
        ec2.terminateInstances(tir);
        if(amiB!=null)
        {
        	System.out.println("Stop working...");
        	amis.deregisterImage(amiB);
        }
        }
        catch (AmazonServiceException ase) {
	  	    System.out.println("Caught Exception: " + ase.getMessage());
	  	    System.out.println("Reponse Status Code: " + ase.getStatusCode());
	  	    System.out.println("Error Code: " + ase.getErrorCode());
	  	    System.out.println("Request ID: " + ase.getRequestId());
	  	}
        return amiA;
	}
	
	
	public static boolean display(long startTime,String Ida, String Idb) throws Exception
	{
		int counter=0;
		Double CPUa=null,CPUb=null;
		boolean tempfa=false,tempfb=false,display=false;

		while(true)
		{
			Date now =new Date();
			//Setup day time
			if((now.getTime()-startTime)>=3*60*1000)
			{
				System.out.println("Prepare to stop all instances...");
				if(!tempfa)
				{
						tempa++;
						AMIa=instanceRelease(Ida,va,String.valueOf(tempa),AMIa);
						tempfa = true;
				}
				if(!tempfb)
				{	
						tempb++;
						AMIb=instanceRelease(Idb,vb,String.valueOf(tempb),AMIb);
						tempfb = true;
					
				}
				if(tempfa && tempfb)
					{
						display=true;
						break;
					}
			}
			else 
			{
				counter++;
				if(counter>=5)
				{
					counter=0;
					if(!tempfa)
					{
						CPUa=cw.monitor(Ida,"Instance_A");
						if((CPUa!=null) && CPUa < 5)//Set the alarm bar of CPU usage.
						{
							tempa++;
							System.out.println("Instance_A's CPU average usage below 5%, system is going to RELEASE the instance...");
							AMIa=instanceRelease(Ida,va,String.valueOf(tempa),AMIa);
							tempfa = true;
						}
					}
					if(!tempfb)
					{
						CPUb=cw.monitor(Idb,"Instance_B");
						if((CPUb!=null) && CPUb<5)
						{
							tempb++;
							System.out.println("Instance_B's CPU average usage below 5%, system is going to RELEASE the instance...");
							AMIb=instanceRelease(Idb,vb,String.valueOf(tempb),AMIb);
							tempfb = true;
						}
					}
				}
				if(tempfa && tempfb)
						{
							display = true;
							break;
						}
					
				}
			System.out.println("Working now and wait for Cpu data...");
			Thread.sleep(10*1000);

			}
		return display;
		}
	
	
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
			credentials = new PropertiesCredentials(
					Runner.class.getResourceAsStream("AwsCredentials.properties"));
		ec2 = new AmazonEC2Client(credentials);

		//We have two working instances a and b;
		//Setup S3, Volume, ELB, KeyPair, Security Group,
		S3 s3=new S3();
		VolumeInit volume=new VolumeInit(ec2);
		ELB elb=new ELB(ec2);
		KeyPair keyPair=null;
		CreateSecurityGroupResult seqRes=null;
		InstanceInit tempIn =new InstanceInit();
		
		//Setup instance A
		Instance ia=tempIn.instanceinit(ec2,"ami-4c59e625", keyPair, seqRes,za);
		za=ia.getPlacement().getAvailabilityZone();
		String Ida=ia.getInstanceId();
		va=volume.createVolume(za);
		volume.attachVolume(Ida, va);
		String ipa=elb.allocateIp();
		elb.associateIp(Ida, ipa);
		
		//Setup instance B
		Instance ib=tempIn.instanceinit(ec2,"ami-4c59e625", keyPair, seqRes,zb);
		zb=ib.getPlacement().getAvailabilityZone();
		String Idb=ib.getInstanceId();
		vb=volume.createVolume(zb);
		volume.attachVolume(Idb, vb);
		String ipb=elb.allocateIp();
		elb.associateIp(Idb, ipb);
		System.out.println("Instance A is in zone "+ ia.getPlacement().getAvailabilityZone()+ "\n" 
		+ "Instance B is in zone "+ib.getPlacement().getAvailabilityZone());
		s3.S3Init(credentials);
		
		//Simulate a business day
		System.out.println("This is a simulate of a business day...");
		Thread.sleep(5*60*1000);// 5 minutes a day.
		Date startTime=new Date();
		cw =new CloudWatch(credentials);
		while(true)
		{	
			System.out.println("Start of a business day!");
			if(display(startTime.getTime(),Ida,Idb))
			{
				System.out.println("Low loading, sleep the instance for 10 seconds...");
				Thread.sleep(10*1000);// 10 seconds
				System.out.println("Wake up slept instance...");
				
				ia=tempIn.instanceinit(ec2,AMIa, keyPair, seqRes,za);
				Ida=ia.getInstanceId();
				elb.associateIp(Ida, ipa);
				volume.attachVolume(Ida, va);
				
				ib=tempIn.instanceinit(ec2,AMIb, keyPair, seqRes,zb);
				Idb=ib.getInstanceId();
				elb.associateIp(Idb, ipb);
				volume.attachVolume(Idb, vb);
				
				startTime=new Date();
				System.out.println("Working for 1 minutes...");
				Thread.sleep(1*60*1000);
			}
			else{
				System.out.println("Error");
				break;
			}
			
		}
		
	}
		
}
