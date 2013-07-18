import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.amazonaws.services.ec2.model.IpPermission;

/**
 * This class implements security group initial methods.
 * 
 * @Modified by zhengqin
 * 
 */
public class SecurityGroupInit {
	public CreateSecurityGroupResult createSecurityGroup(AmazonEC2 ec2)
	{
		CreateSecurityGroupResult cSGRs=null;
		
        try
		{CreateSecurityGroupRequest cSGRe=new CreateSecurityGroupRequest("cwsSecurityGroup","ColoudWorkingSystemSecurity Group");
		cSGRs=ec2.createSecurityGroup(cSGRe);
        
        IpPermission ipPermission = new IpPermission();
        ipPermission.withIpRanges("23.152.18.249/32")//We allow ssh connect.
                    .withIpProtocol("tcp")
                    .withFromPort(22)
                    .withToPort(22);
        AuthorizeSecurityGroupIngressRequest authorizeSecurityGroupIngressRequest =new AuthorizeSecurityGroupIngressRequest();
        authorizeSecurityGroupIngressRequest.withGroupName("cwsSecurityGroup").withIpPermissions(ipPermission);
        ec2.authorizeSecurityGroupIngress(authorizeSecurityGroupIngressRequest);
		}
		   catch (AmazonServiceException ase) {
			    System.out.println("Caught Exception: " + ase.getMessage());
			    System.out.println("Reponse Status Code: " + ase.getStatusCode());
			    System.out.println("Error Code: " + ase.getErrorCode());
			    System.out.println("Request ID: " + ase.getRequestId());
		   }
        return cSGRs;
	}
	
//	public static void main(String[] args) throws Exception {
//
//		AWSCredentials credentials = new PropertiesCredentials(
//				SecurityGroupInit.class.getResourceAsStream("AwsCredentials.properties"));
//	System.out.println("#1 Create Amazon Client object");
//	AmazonEC2 ec2 = new AmazonEC2Client(credentials);
//	SecurityGroupInit sgi = new SecurityGroupInit();
//		sgi.createSecurityGroup(ec2);
//		System.out.println("ok");
//	}
}
