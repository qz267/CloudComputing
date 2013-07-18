import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.AllocateAddressResult;
import com.amazonaws.services.ec2.model.AssociateAddressRequest;
import com.amazonaws.services.ec2.model.DisassociateAddressRequest;

/**
 * This class implements ELB methods.
 * 
 * @Modified by zhengqin
 * 
 */
public class ELB {
	AmazonEC2 ec2;
	public ELB(AmazonEC2 ec2)
	{
		this.ec2=ec2;
	}
	public String allocateIp()
	{
		//allocate
		String elasticIp=null;  
		try
		   {	
			AllocateAddressResult elasticResult = ec2.allocateAddress();
			elasticIp = elasticResult.getPublicIp();
			System.out.println("New elastic IP: "+elasticIp);
			
		   }
		   catch (AmazonServiceException ase) {
			    System.out.println("Caught Exception: " + ase.getMessage());
			    System.out.println("Reponse Status Code: " + ase.getStatusCode());
			    System.out.println("Error Code: " + ase.getErrorCode());
			    System.out.println("Request ID: " + ase.getRequestId());
		   }
		   return elasticIp;
	}
	
	public void associateIp(String instanceId,String elasticIp)
	{		
		//associate
		try	
		{AssociateAddressRequest aar = new AssociateAddressRequest();
			aar.setInstanceId(instanceId);
			aar.setPublicIp(elasticIp);
			ec2.associateAddress(aar);
			System.out.println("Associate IP address: "+elasticIp+" to Instance: "+instanceId);
		}
		   catch (AmazonServiceException ase) {
			    System.out.println("Caught Exception: " + ase.getMessage());
			    System.out.println("Reponse Status Code: " + ase.getStatusCode());
			    System.out.println("Error Code: " + ase.getErrorCode());
			    System.out.println("Request ID: " + ase.getRequestId());
		   }
	}
	public void disassociate(String elasticIp)
	{
		//disassociate
		try
		{	DisassociateAddressRequest dar = new DisassociateAddressRequest();
			dar.setPublicIp(elasticIp);
			ec2.disassociateAddress(dar);
			System.out.println("Disassociate Ip: "+elasticIp);
		}
		   catch (AmazonServiceException ase) {
			    System.out.println("Caught Exception: " + ase.getMessage());
			    System.out.println("Reponse Status Code: " + ase.getStatusCode());
			    System.out.println("Error Code: " + ase.getErrorCode());
			    System.out.println("Request ID: " + ase.getRequestId());
		   }
	}

}
