import java.io.*;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;


public class KeyPairInit {
	static KeyPair keyPairs;
	
	public KeyPair instancekeyPair(AmazonEC2 ec2)
	{
		String key=null; 
		try
		
		{ 
	    CreateKeyPairRequest kpReq = new CreateKeyPairRequest();
	    kpReq.setKeyName("ColoudWorkingSystemKey");
	    CreateKeyPairResult kpres = ec2.createKeyPair(kpReq);
	    
	    System.out.println("Cloud Working System keypair has been created");
	    keyPairs = kpres.getKeyPair();
	    key=keyPairs.getKeyMaterial();
		}
		   catch (AmazonServiceException ase) {
			    System.out.println("Caught Exception: " + ase.getMessage());
			    System.out.println("Reponse Status Code: " + ase.getStatusCode());
			    System.out.println("Error Code: " + ase.getErrorCode());
			    System.out.println("Request ID: " + ase.getRequestId());
		   }
	    
	    // store the key to a pem file so that later we can use it to ssh login
	    try{
	    	  FileWriter fstream = new FileWriter("ColoudWorkingSystemKey.pem");
	    	  BufferedWriter out = new BufferedWriter(fstream);
	    	  out.write(key);
	    	  out.close();
	    	  System.out.println("ColoudWorkingSystemKey.pem has been created");
	    	  }catch (Exception e){
	    	  System.err.println("Error: " + e.getMessage());
	    	  }
		
	    return keyPairs;
	}
	
//	public static void main(String[] args) throws Exception {
//
//		AWSCredentials credentials = new PropertiesCredentials(
//				KeyPairInit.class.getResourceAsStream("AwsCredentials.properties"));
//	System.out.println("#1 Create Amazon Client object");
//	AmazonEC2 ec2 = new AmazonEC2Client(credentials);
//	KeyPairInit sgi = new KeyPairInit();
//		sgi.instancekeyPair(ec2);
//		System.out.println("ok");
//	}
}
