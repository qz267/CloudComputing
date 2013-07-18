import java.util.List;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.CreateImageRequest;
import com.amazonaws.services.ec2.model.CreateImageResult;
import com.amazonaws.services.ec2.model.DeregisterImageRequest;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.Image;

/**
 * This class implements AMI methods.
 * 
 * @Modified by zhengqin
 * 
 */
public class AMI {

    /*
     * Important: Be sure to fill in your AWS access credentials in the
     *            AwsCredentials.properties file before you try to run this
     *            sample.
     * http://aws.amazon.com/security-credentials
     */

    AmazonEC2      ec2;
    public AMI(AmazonEC2 ec2)
    {
    	this.ec2=ec2;
    }
    
 public String createAMI(String instanceId,String amiName) throws Exception {

	 String createdImageId=null;
        // we assume that we've already created an instance. Use the id of the instance.
         try{
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

 			//If the image can lunch
 			while(true)
 			{
 				describeImage=ec2.describeImages(describeImagesRequest);
 				imgs=describeImage.getImages();
 				for(Image img:imgs)
 				{
 					if(img.getImageId().equals(createdImageId))
 					{
 						while(img.getState().equals("available"))
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
                System.out.println("Caught Exception: " + ase.getMessage());
                System.out.println("Reponse Status Code: " + ase.getStatusCode());
                System.out.println("Error Code: " + ase.getErrorCode());
                System.out.println("Request ID: " + ase.getRequestId());

        }
       finally
       {
    	   return createdImageId;
    	   }

        
    }
 	public void deregisterImage(String imageId)
 	{
 			DeregisterImageRequest deregisterImageRequest=new DeregisterImageRequest();
			deregisterImageRequest.setImageId(imageId);
 			ec2.deregisterImage(deregisterImageRequest);
 	}
}
