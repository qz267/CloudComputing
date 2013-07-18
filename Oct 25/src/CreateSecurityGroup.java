import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.amazonaws.services.ec2.model.IpPermission;


class CreateSecurityGroup {
	public CreateSecurityGroupResult createSecurityGroup(AmazonEC2 ec2)
	{
		CreateSecurityGroupResult seqRes=null;
        //Create security group
        try
		{CreateSecurityGroupRequest seqGr=new CreateSecurityGroupRequest("programSecurityGroup","programmable created security group");
        seqRes=ec2.createSecurityGroup(seqGr);
        //create and initialize an ipPermission
        IpPermission ipPermission = new IpPermission();
        ipPermission.withIpRanges("69.141.91.249/32")
                    .withIpProtocol("tcp")
                    .withFromPort(22)
                    .withToPort(22);
        AuthorizeSecurityGroupIngressRequest authorizeSecurityGroupIngressRequest =new AuthorizeSecurityGroupIngressRequest();
        authorizeSecurityGroupIngressRequest.withGroupName("programSecurityGroup").withIpPermissions(ipPermission);
        ec2.authorizeSecurityGroupIngress(authorizeSecurityGroupIngressRequest);
		}
		   catch (AmazonServiceException ase) {
			    System.out.println("Caught Exception: " + ase.getMessage());
			    System.out.println("Reponse Status Code: " + ase.getStatusCode());
			    System.out.println("Error Code: " + ase.getErrorCode());
			    System.out.println("Request ID: " + ase.getRequestId());
		   }
        return seqRes;
	}
//	public static void main(String[] args) throws Exception {
//
//		AWSCredentials credentials = new PropertiesCredentials(
//				CreateSecurityGroup.class.getResourceAsStream("AwsCredentials.properties"));
//	System.out.println("#1 Create Amazon Client object");
//	AmazonEC2 ec2 = new AmazonEC2Client(credentials);
//	CreateSecurityGroup sgi = new CreateSecurityGroup();
//		sgi.createSecurityGroup(ec2);
//		System.out.println("ok");
//	}

}
