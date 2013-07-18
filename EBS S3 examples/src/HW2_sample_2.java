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
 * Modified by Sambit Sahu
 * Modified by Kyung-Hwa Kim (kk2515@columbia.edu)
 * Modified by Qin Zheng
 * 
 * 
 */
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateImageRequest;
import com.amazonaws.services.ec2.model.CreateImageResult;



public class HW2_sample_2 {

    /*
     * Important: Be sure to fill in your AWS access credentials in the
     *            AwsCredentials.properties file before you try to run this
     *            sample.
     * http://aws.amazon.com/security-credentials
     */

    static AmazonEC2      ec2;

    public static void main(String[] args) throws Exception {


    	 AWSCredentials credentials = new PropertiesCredentials(
    			 HW2_sample_2.class.getResourceAsStream("AwsCredentials.properties"));

         /*********************************************
          *  #1 Create Amazon Client object
          *********************************************/
         ec2 = new AmazonEC2Client(credentials);

         
         // we assume that we've already created an instance. Use the id of the instance.
         String instanceId = "i-9ef1afe3"; //put your own instance id to test this code.
         try{
       
        	 /***********************************
              *   #2 Create an AMI from an instance
              *********************************/
        	CreateImageRequest cir = new CreateImageRequest();
 			cir.setInstanceId(instanceId);
 			cir.setName("qz_test_ami_3");
 			CreateImageResult createImageResult = ec2.createImage(cir);
 			String createdImageId = createImageResult.getImageId();
 			System.out.println("Sent creating AMI request. AMI id="+createdImageId);
 			
 			ec2.shutdown();

        } catch (AmazonServiceException ase) {
                System.out.println("Caught Exception: " + ase.getMessage());
                System.out.println("Reponse Status Code: " + ase.getStatusCode());
                System.out.println("Error Code: " + ase.getErrorCode());
                System.out.println("Request ID: " + ase.getRequestId());
        }

        
    }
}