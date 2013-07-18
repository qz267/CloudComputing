import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;


import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.amazonaws.services.ec2.model.KeyPair;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.Instance;


public class Assignment1 {

	/**
	 * @param args
	 */
	static AmazonEC2 ec2;
	static AWSCredentials credentials;
	static CreateCloudWatch cwatch;
	static String oneAMI=null;
	static String twoAMI=null;
	static int namecounter1=100;
	static int namecounter2=200;
	static String vol1;
	static String vol2;
	static String zone1=null;
	static String zone2=null;


	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
			credentials = new PropertiesCredentials(
			Assignment1.class.getResourceAsStream("AwsCredentials.properties"));
		/*********************************************
		 *  	#1 Create Amazon Client object
		 *********************************************/
		System.out.println("#1 Create Amazon Client object");
		ec2 = new AmazonEC2Client(credentials);

		//create Instance one and two
		KeyPair kpairs=null;
		//    KeyPair kpairs=new CreateKeyPair().createKeyPair(ec2);
		CreateSecurityGroupResult seqRes=null;
	//	CreateSecurityGroupResult seqRes=new CreateSecurityGroup().createSecurityGroup(ec2);
		CreateInstance one=new CreateInstance();
		Instance insone=one.createInstance(ec2,"ami-aecd60c7", kpairs, seqRes,zone1);
		Instance instwo=one.createInstance(ec2,"ami-aecd60c7", kpairs, seqRes,zone2);
		zone1=insone.getPlacement().getAvailabilityZone();
		System.out.println(zone1);
		zone2=instwo.getPlacement().getAvailabilityZone();
		System.out.println(zone2);
		String oneId=insone.getInstanceId();
		String twoId=instwo.getInstanceId();
		
		//Create ElasticIp address for the instances
		CreateElasticIp eip=new CreateElasticIp(ec2);
		String ip1=eip.allocateIp();
		String ip2=eip.allocateIp();
		// attach the ip address to the instances;
		eip.associateIp(oneId, ip1);
		eip.associateIp(twoId, ip2);
		
		//create EBS
		CreateVolume volclass=new CreateVolume(ec2);
		vol1=volclass.createVolume(zone1);
		vol2=volclass.createVolume(zone2);
		//attach EBS to instance
		volclass.attachVolume(oneId, vol1);
		volclass.attachVolume(twoId, vol2);
		
		//create S3 storage for user1
		CreateS3 s3=new CreateS3();
		s3.createS3(credentials);
		System.out.println("running 8 minute...");
		Thread.sleep(8*60*1000);
		Date startTime=new Date();
		cwatch=new CreateCloudWatch(credentials);
		while(true)
		{	
			System.out.println("Start daily job now...");
			if(monitor(startTime.getTime(),oneId,twoId))
			{
				System.out.println("Sleep half a minute...");
				Thread.sleep(30*1000);
				System.out.println("Wake up now...");
				insone=one.createInstance(ec2,oneAMI, kpairs, seqRes,zone1);
				oneId=insone.getInstanceId();
				instwo=one.createInstance(ec2,twoAMI, kpairs, seqRes,zone2);
				twoId=instwo.getInstanceId();
				eip.associateIp(oneId, ip1);
				eip.associateIp(twoId, ip2);
				volclass.attachVolume(oneId, vol1);
				volclass.attachVolume(twoId, vol2);
				startTime=new Date();
				System.out.println("running 8 minutes for the instance warm up...");
				Thread.sleep(8*30*1000);
			}
			else
				{
					System.out.println("Something goes wrong...");
					break;
				}
			
			
			
		}
		
		//ec2.shutdown();
        //s3.shutdown();
		//test();
	}
		public static boolean monitor(long startTime,String oneId, String twoId) throws Exception
		{
			int counter=0;
			boolean flag=false;
			Double cpu1=null;
			Double cpu2=null;
			boolean terminateFlag1=false;
			boolean terminateFlag2=false;

			while(true)
			{
				Date currentTime=new Date();
				if((currentTime.getTime()-startTime)>=16*60*1000)
				{
					System.out.println("5pm now, prepare to deleteInstance...");


					if(!terminateFlag1)
					{
					//	cpu1=cwatch.cloudWatch(oneId,"User1");
					//	if(cpu1!=null &&cpu1<20)
					//	{
							namecounter1++;
							oneAMI=deleteInstance(oneId,vol1,String.valueOf(namecounter1),oneAMI);
							terminateFlag1=true;
					//	}
					}
					if(!terminateFlag2)
					{	
						//cpu2=cwatch.cloudWatch(twoId,"User2");
						//if(cpu2!=null && cpu2<20)
						//{					
							namecounter2++;
							twoAMI=deleteInstance(twoId,vol2,String.valueOf(namecounter2),twoAMI);
							terminateFlag2=true;
						//}
						
					}
					if(terminateFlag1&&terminateFlag2)
						{
							flag=true;
							break;
						}
					
				}
				else 
				{
					counter++;
					if(counter>=5)
					{
						counter=0;
						if(!terminateFlag1)
						{
							cpu1=cwatch.cloudWatch(oneId,"User1");
							if((cpu1!=null)&&cpu1<5)
							{
								namecounter1++;
								System.out.println("User1 CPU average utilization below 5%, prepare to delete the instance now...");
								oneAMI=deleteInstance(oneId,vol1,String.valueOf(namecounter1),oneAMI);
								terminateFlag1=true;
							}
						}
						if(!terminateFlag2)
						{
							cpu2=cwatch.cloudWatch(twoId,"User2");
							if((cpu2!=null)&&cpu2<5)
							{
								namecounter2++;
								System.out.println("User2 CPU average utilization below 5%, prepare to delete the instance now...");
								twoAMI=deleteInstance(twoId,vol2,String.valueOf(namecounter2),twoAMI);
								terminateFlag2=true;
							}
						}
					}
					if(terminateFlag1&&terminateFlag2)
							{
								flag=true;
								break;
							}
						
					}
				System.out.println("Working now and wait for Cpu data...");
				 Thread.sleep(1000*60);

				}
			return flag;
			}
			
		

	public static String deleteInstance(String instanceId,String createdVolumeId,String amiName,String AMI) throws Exception
	{
		String createdAMI=null;
		String oldAMI=AMI;
		CreateVolume vols=new CreateVolume(ec2);
		vols.detachVolume(instanceId, createdVolumeId);
		AMIcreation amis=new AMIcreation(ec2);
		createdAMI=amis.createAMI(instanceId, amiName);
		while(createdAMI==null)
		{
			Thread.sleep(1000*60);
			createdAMI=amis.createAMI(instanceId, amiName);
			
		}
			
        System.out.println("Terminate Instance: "+instanceId);
        List<String> instanceIds = new LinkedList<String>();
        instanceIds.add(instanceId);
        try
        {TerminateInstancesRequest tir = new TerminateInstancesRequest(instanceIds);
        ec2.terminateInstances(tir);
        if(oldAMI!=null)
        {
        	System.out.println("deregister oldAMI now...");
        	amis.deregisterImage(oldAMI);
        }
        }
        catch (AmazonServiceException ase) {
	  	    System.out.println("Caught Exception: " + ase.getMessage());
	  	    System.out.println("Reponse Status Code: " + ase.getStatusCode());
	  	    System.out.println("Error Code: " + ase.getErrorCode());
	  	    System.out.println("Request ID: " + ase.getRequestId());
	  	}
        return createdAMI;
		
	}
	
	public static void test() throws Exception
	{
	    KeyPair kpairs=new CreateKeyPair().createKeyPair(ec2);
		CreateSecurityGroupResult seqRes=new CreateSecurityGroup().createSecurityGroup(ec2);
		CreateInstance one=new CreateInstance();
		Instance insone=one.createInstance(ec2,"ami-aecd60c7", kpairs, seqRes,zone1);
		zone1=insone.getPlacement().getAvailabilityZone();
		System.out.println(zone1);
		String oneId=insone.getInstanceId();

		
		//Create ElasticIp address for the instances
		CreateElasticIp eip=new CreateElasticIp(ec2);
		String ip1=eip.allocateIp();

		// attach the ip address to the instances;
		eip.associateIp(oneId, ip1);
		
		//create EBS
		CreateVolume volclass=new CreateVolume(ec2);
		String vol1=volclass.createVolume(zone1);
		//attach EBS to instance
		volclass.attachVolume(oneId, vol1);

		System.out.println("Machine running 1 minutes...");
		Thread.sleep(1*60*1000);
		String AMI=null;
		while(true)
		{
			namecounter1++;
			System.out.println("5pm now, delete Instance...");
			AMI = deleteInstance(oneId,vol1,String.valueOf(namecounter1),AMI);
			System.out.println("Sleeping now...");
			Thread.sleep(60*1000);
			System.out.println("wake up now");
			insone=one.createInstance(ec2,AMI, kpairs, seqRes,zone1);
			oneId=insone.getInstanceId();
			eip.associateIp(oneId, ip1);
			volclass.attachVolume(oneId, vol1);
			System.out.println("Machine Running 1 minutes...");
			Thread.sleep(60*1000);
		}
	}


}
