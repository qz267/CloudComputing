


/*
 * Copyright 2010 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * 
 *
 * Modified by Sambit Sahu
 * 
 */
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.aspectj.weaver.ast.Expr;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairResult;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeKeyPairsResult;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.KeyPair;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;


public class AwsSample {

    /*
     * Important: Be sure to fill in your AWS access credentials in the
     *            AwsCredentials.properties file before you try to run this
     *            sample.
     * http://aws.amazon.com/security-credentials
     */

    static AmazonEC2      ec2;

    public static void main(String[] args) throws Exception {


    	 AWSCredentials credentials = new PropertiesCredentials(
    			 AwsSample.class.getResourceAsStream("AwsCredentials.properties"));

         /*********************************************
          * 
          *  #1 Create Amazon Client object
          *  
          *********************************************/
    	 System.out.println("#1 Create Amazon Client object");
         ec2 = new AmazonEC2Client(credentials);
        

        
    	
    	// Retrieves the credentials from an AWSCredentials.properties file.
    	
//    	AWSCredentials credentials = null;
//    	
//    	try {
//			credentials = new PropertiesCredentials(
//			           	AwsSample.class.getResourceAsStream("AwsCredentials.properties"));
//		} catch (IOException e1) {
//			System.out.println("Credentials were not properly entered into AwsCredentials.properties.");
//			System.out.println(e1.getMessage());
//			System.exit(-1);
//		}
//    	
//		// Create the AmazonEC2Client object so we can call various APIs.
//		AmazonEC2 ec2 = new AmazonEC2Client(credentials);

         
       
         
	    try {
	    	
	    	  /*********************************************
	          * 
	          *  # Create a new security gorup.
	          *  
	          *********************************************/
	    	
	        	CreateSecurityGroupRequest securityGroupRequest = new CreateSecurityGroupRequest("GettingStartedGroup", "Getting Started Security Group");
	        	ec2.createSecurityGroup(securityGroupRequest); 		
	    	} catch (AmazonServiceException ase) {
	    		// Likely this means that the group is already created, so ignore.
	    		System.out.println(ase.getMessage());
	    	}

	    	String ipAddr = "0.0.0.0/0";
	    	
	    	// Get the IP of the current host, so that we can limit the Security Group
	    	// by default to the ip range associated with your subnet.
	    	try {
	    	    InetAddress addr = InetAddress.getLocalHost();

	    	    // Get IP Address
	    	    ipAddr = addr.getHostAddress()+"/10";
	    	} catch (UnknownHostException e) {
	    	}

	    	//System.exit(-1);
	    	// Create a range that you would like to populate.
	    	ArrayList<String> ipRanges = new ArrayList<String>();
	    	ipRanges.add(ipAddr);
	    	
	    	// Open up port 29 for TCP traffic to the associated IP from above (e.g. ssh traffic).
	    	ArrayList<IpPermission> ipPermissions = new ArrayList<IpPermission> ();
	    	IpPermission ipPermission = new IpPermission();
	    	ipPermission.setIpProtocol("tcp");
	    	ipPermission.setFromPort(new Integer(28));
	    	ipPermission.setToPort(new Integer(28));
	    	ipPermission.setIpRanges(ipRanges);
	    	ipPermissions.add(ipPermission);
	    	
	    	try {
		    	// Authorize the ports to the used.
		    	AuthorizeSecurityGroupIngressRequest ingressRequest = new AuthorizeSecurityGroupIngressRequest("GettingStartedGroup",ipPermissions);
		    	ec2.authorizeSecurityGroupIngress(ingressRequest);
	    	} catch (AmazonServiceException ase) {
	    		// Ignore because this likely means the zone has already been authorized.
	    		System.out.println(ase.getMessage());
	    	}
	    	
	    	
	    try{
	    	/*********************************************
        	 * 
             *  Create a key pair.
             *  
             *********************************************/
	    	
	    	String keyName = "Test";
	    	CreateKeyPairRequest createKeyPairRequest = new CreateKeyPairRequest();
	    	createKeyPairRequest.withKeyName(keyName);
	    	CreateKeyPairResult createKeyPairResult = ec2.createKeyPair(createKeyPairRequest);
	    	KeyPair keyPair = new KeyPair();
	    	keyPair = createKeyPairResult.getKeyPair();
	    	String privateKey = keyPair.getKeyMaterial();
	    	
	    }catch(Exception e){
	    	System.out.println("Can not create new key pair.");
	    }
    	
       
        try {
        	
        	/*********************************************
        	 * 
             *  #2 Describe Availability Zones.
             *  
             *********************************************/
        	System.out.println("#2 Describe Availability Zones.");
            DescribeAvailabilityZonesResult availabilityZonesResult = ec2.describeAvailabilityZones();
            System.out.println("You have access to " + availabilityZonesResult.getAvailabilityZones().size() +
                    " Availability Zones.");

            /*********************************************
             * 
             *  #3 Describe Available Images
             *  
             *********************************************/
            System.out.println("#3 Describe Available Images");
            DescribeImagesResult dir = ec2.describeImages();
            List<Image> images = dir.getImages();
            System.out.println("You have " + images.size() + " Amazon images");
            
            
            /*********************************************
             *                 
             *  #4 Describe Key Pair
             *                 
             *********************************************/
            System.out.println("#9 Describe Key Pair");
            DescribeKeyPairsResult dkr = ec2.describeKeyPairs();
            System.out.println(dkr.toString());
            
            /*********************************************
             * 
             *  #5 Describe Current Instances
             *  
             *********************************************/
            System.out.println("#4 Describe Current Instances");
            DescribeInstancesResult describeInstancesRequest = ec2.describeInstances();
            List<Reservation> reservations = describeInstancesRequest.getReservations();
            Set<Instance> instances = new HashSet<Instance>();
            // add all instances to a Set.
            for (Reservation reservation : reservations) {
            	instances.addAll(reservation.getInstances());
            }
            
            System.out.println("You have " + instances.size() + " Amazon EC2 instance(s).");
            for (Instance ins : instances){
            	
            	// instance id
            	String instanceId = ins.getInstanceId();
            	
            	// instance state
            	InstanceState is = ins.getState();
            	System.out.println(instanceId+" "+is.getName());
           // 	System.out.println("Key for the instance  "+keyName);
            }
            
            /*********************************************
             * 
             *  #6 Create an Instance
             *  
             *********************************************/
            System.out.println("#5 Create an Instance");
//            String imageId = "ami-76f0061f"; //Basic 32-bit Amazon Linux AMI
            String imageId = "ami-74f0061d"; //Basic 64-bit Amazon Linux AMI
            int minInstanceCount = 1; // create 1 instance
            int maxInstanceCount = 1;
            RunInstancesRequest rir = new RunInstancesRequest(imageId, minInstanceCount, maxInstanceCount);
            RunInstancesResult result = ec2.runInstances(rir);
            
            //get instanceId from the result
            List<Instance> resultInstance = result.getReservation().getInstances();
            String createdInstanceId = null;
            for (Instance ins : resultInstance){
            	createdInstanceId = ins.getInstanceId();
            	System.out.println("New instance has been created: "+ins.getInstanceId());
      //        System.out.println("getKeyName() is: "+ ins.setKeyName(myEC2key));
            }
            
            
            /*********************************************
             * 
             *  #7 Create a 'tag' for the new instance.
             *  
             *********************************************/
            System.out.println("#6 Create a 'tag' for the new instance.");
            List<String> resources = new LinkedList<String>();
            List<Tag> tags = new LinkedList<Tag>();
            Tag nameTag = new Tag("Name", "MyFirstAwsJavaProject");
//            Tag nameTag = new Tag("Name", "MyFirstInstance");
            
            resources.add(createdInstanceId);
            tags.add(nameTag);
            
            CreateTagsRequest ctr = new CreateTagsRequest(resources, tags);
            ec2.createTags(ctr);
            
            
                        
            /*********************************************
             * 
             *  #8 Stop/Start an Instance
             *  
             *********************************************/
            System.out.println("#7 Stop the Instance");
            List<String> instanceIds = new LinkedList<String>();
            instanceIds.add(createdInstanceId);
            
            //stop
            StopInstancesRequest stopIR = new StopInstancesRequest(instanceIds);
            //ec2.stopInstances(stopIR);
            
            //start
            StartInstancesRequest startIR = new StartInstancesRequest(instanceIds);
            //ec2.startInstances(startIR);
            
            
            /*********************************************
             * 
             *  #9 Terminate an Instance
             *  
             *********************************************/
            System.out.println("#8 Terminate the Instance");
            TerminateInstancesRequest tir = new TerminateInstancesRequest(instanceIds);
            //ec2.terminateInstances(tir);
            
                        
            /*********************************************
             *  
             *  #10 shutdown client object
             *  
             *********************************************/
            ec2.shutdown();
            
            
            
        } catch (AmazonServiceException ase) {
                System.out.println("Caught Exception: " + ase.getMessage());
                System.out.println("Reponse Status Code: " + ase.getStatusCode());
                System.out.println("Error Code: " + ase.getErrorCode());
                System.out.println("Request ID: " + ase.getRequestId());
        }

        
    }
}

