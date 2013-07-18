import java.io.BufferedWriter;
import java.io.FileWriter;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateKeyPairRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairResult;
import com.amazonaws.services.ec2.model.KeyPair;


class CreateKeyPair {
	static KeyPair kpairs;
	public KeyPair createKeyPair(AmazonEC2 ec2)
	{
		String pem=null; 
		try
		
		{ 
	    CreateKeyPairRequest kpReq = new CreateKeyPairRequest();
	    kpReq.setKeyName("programKey");
	    CreateKeyPairResult kpres = ec2.createKeyPair(kpReq);
	    System.out.println("The keypair <programKey> has been generated");
	    kpairs = kpres.getKeyPair();
	    pem=kpairs.getKeyMaterial();
		}
		   catch (AmazonServiceException ase) {
			    System.out.println("Caught Exception: " + ase.getMessage());
			    System.out.println("Reponse Status Code: " + ase.getStatusCode());
			    System.out.println("Error Code: " + ase.getErrorCode());
			    System.out.println("Request ID: " + ase.getRequestId());
		   }
	    
	    // store the key to a pem file so that later we can use it to ssh login
	    try{
	    	  FileWriter fstream = new FileWriter("programKey.pem");
	    	  BufferedWriter out = new BufferedWriter(fstream);
	    	  out.write(pem);
	    	  out.close();
	    	  System.out.println("The <programKey.pem> file has been generated");
	    	  }catch (Exception e){
	    	  System.err.println("Error: " + e.getMessage());
	    	  }
		
	    return kpairs;
	}
	
//	public static void main(String[] args) throws Exception {
//
//		AWSCredentials credentials = new PropertiesCredentials(
//				CreateKeyPair.class.getResourceAsStream("AwsCredentials.properties"));
//	System.out.println("#1 Create Amazon Client object");
//	AmazonEC2 ec2 = new AmazonEC2Client(credentials);
//	CreateKeyPair sgi = new CreateKeyPair();
//		sgi.createKeyPair(ec2);
//		System.out.println("ok");
//	}

}
