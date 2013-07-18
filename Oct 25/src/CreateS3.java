import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;


public class CreateS3 {
    static AmazonS3Client s3;

	public void createS3(AWSCredentials credentials)
	{
        s3  = new AmazonS3Client(credentials);
        
        //create bucket
        String bucketName = "qz-test-1-bucket";
        s3.createBucket(bucketName);
        
        //set key
        String key = "qz-as1.txt";
        
        //set value
        File file=null;
        try
        {
        file = File.createTempFile("temp", ".txt");
        file.deleteOnExit();
        Writer writer = new OutputStreamWriter(new FileOutputStream(file));
        writer.write("Uers1's file.\r\nYes!");
        writer.close();
        }
        catch (Exception e){
	    	  System.err.println("Error: " + e.getMessage());
	    	  }
        
        //put object - bucket, key, value(file)
        s3.putObject(new PutObjectRequest(bucketName, key, file));
        
        //get object
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
/*	public static void main(String[] args) throws Exception {
		CreateS3 s3=new CreateS3();
		AWSCredentials credentials = new PropertiesCredentials(
				Assignment1.class.getResourceAsStream("AwsCredentials.properties"));
		s3.createS3(credentials);
		
		
	}*/
	

}
