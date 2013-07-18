import java.io.*;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

/**
 * This class implements S3 methods.
 * 
 * @Modified by zhengqin
 * 
 */
public class S3 {
	
	 static AmazonS3Client s3;

		public void S3Init(AWSCredentials credentials)
		{
	        s3  = new AmazonS3Client(credentials);
	        String bucketName = "cloudworkingsystem";
	        s3.createBucket(bucketName);
	        String key = "WorkingInstance.txt";
	        File file=null;
	        try
	        {
	        file = File.createTempFile("temp", ".txt");
	        file.deleteOnExit();
	        Writer writer = new OutputStreamWriter(new FileOutputStream(file));
	        writer.write("Working instances list is writen to S3.");
	        writer.close();
	        }
	        catch (Exception e){
		    	  System.err.println("Error: " + e.getMessage());
		    	  }
	        
//	        Put objects into bucket by (key, value(file))
	        s3.putObject(new PutObjectRequest(bucketName, key, file));
//	        Get objects from bucket by (key, value(file))
	        S3Object object = s3.getObject(new GetObjectRequest(bucketName, key));
	        try
	        {
	        BufferedReader reader = new BufferedReader(
	        		new InputStreamReader(object.getObjectContent()));
	        String data = null;
	        while ((data = reader.readLine()) != null) {
	            System.out.println(data);
	        }
	        }
	        catch (Exception e){
		    	  System.err.println("Error: " + e.getMessage());
		    	  }
		}
}
