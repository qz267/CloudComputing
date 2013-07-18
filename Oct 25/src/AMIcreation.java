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
 * 
 * 
 */
import java.util.List;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.CreateImageRequest;
import com.amazonaws.services.ec2.model.CreateImageResult;
import com.amazonaws.services.ec2.model.DeregisterImageRequest;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.Image;



public class AMIcreation {

    /*
     * Important: Be sure to fill in your AWS access credentials in the
     *            AwsCredentials.properties file before you try to run this
     *            sample.
     * http://aws.amazon.com/security-credentials
     */

    AmazonEC2      ec2;
    public AMIcreation(AmazonEC2 ec2)
    {
    	this.ec2=ec2;
    }
    
 public String createAMI(String instanceId,String amiName) throws Exception {

	 String createdImageId=null;
         // we assume that we've already created an instance. Use the id of the instance.
        // String instanceId = "i-cd6d01b0"; //put your own instance id to test this code.
         try{
       
        	 /***********************************
              *   #2 Create an AMI from an instance
              *********************************/
        	CreateImageRequest cir = new CreateImageRequest();
 			cir.setInstanceId(instanceId);
 			cir.setName(amiName);
 			CreateImageResult createImageResult = ec2.createImage(cir);
 			createdImageId = createImageResult.getImageId();
        	 
 			System.out.println("Sent creating AMI request. AMI id="+createdImageId);
 			DescribeImagesRequest describeImagesRequest=new DescribeImagesRequest();
 			describeImagesRequest.withImageIds(createdImageId);
 			DescribeImagesResult describeImage;
 			List<Image> imgs;
 			boolean flag=false;
 			while(true)
 			{
 				//Test wether the image is available for lunching
 				describeImage=ec2.describeImages(describeImagesRequest);
 				imgs=describeImage.getImages();
 				for(Image img:imgs)
 				{
 					if(img.getImageId().equals(createdImageId))
 					{
 						if(img.getState().equals("available"))
 							{
 								flag=true;
 								break;
 							}
 					}
 				}
 				if(flag)
 					break;
 				else
 					Thread.sleep(20*1000);
 				
 			}
 			
 			
 			
 			
 			

 			
        } catch (AmazonServiceException ase) {
        		createdImageId=null;
        		System.out.println("Caution: The requested AMI doesn't exist anymore, we will try to create one again");
                System.out.println("Caught Exception: " + ase.getMessage());
                System.out.println("Reponse Status Code: " + ase.getStatusCode());
                System.out.println("Error Code: " + ase.getErrorCode());
                System.out.println("Request ID: " + ase.getRequestId());
        		System.out.println("Caution: The requested AMI doesn't exist anymore, we will try to create one again");

        }
       finally{ return createdImageId;}

        
    }
 	public void deregisterImage(String imageId)
 	{
 			DeregisterImageRequest deregisterImageRequest=new DeregisterImageRequest();
			deregisterImageRequest.setImageId(imageId);
 			ec2.deregisterImage(deregisterImageRequest);
 	}
}