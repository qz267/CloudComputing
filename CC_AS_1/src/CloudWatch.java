import java.util.*;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.cloudwatch.*;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;

/**
 * This class implements cloud watch methods.
 * 
 * @Modified by zhengqin
 * 
 */
public class CloudWatch {
	
	AWSCredentials credentials;
	public CloudWatch(AWSCredentials credentials)
	{ 
		this.credentials=credentials;
	}
	
	public Double monitor(String instanceId,String instanceName)
	{
		Double aveOfCPU=null; 
		try
		{AmazonCloudWatchClient cloudWatch = new AmazonCloudWatchClient(credentials) ;
		
		//Set request message
		GetMetricStatisticsRequest statRequest = new GetMetricStatisticsRequest();
		statRequest.setNamespace("AWS/EC2"); 
		statRequest.setPeriod(60); 
		ArrayList<String> stats = new ArrayList<String>();

		//Set average and sum are the monitor values
		stats.add("Average"); 
		stats.add("Sum");
		statRequest.setStatistics(stats);
		
		//Set CPU utilization to be the monitor value.
		statRequest.setMetricName("CPUUtilization"); 
		
		//Set monitor scan frequency.
		GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		calendar.add(GregorianCalendar.SECOND, -1 * calendar.get(GregorianCalendar.SECOND)); //1 sec ago
		Date endTime = calendar.getTime();
		calendar.add(GregorianCalendar.MINUTE, -10); //10 mins ago
		Date startTime = calendar.getTime();
		statRequest.setStartTime(startTime);
		statRequest.setEndTime(endTime);
		
		//Set the instance ID
		ArrayList<Dimension> dimensions = new ArrayList<Dimension>();
		dimensions.add(new Dimension().withName("instanceId").withValue(instanceId));
		statRequest.setDimensions(dimensions);
		GetMetricStatisticsResult statResult = cloudWatch.getMetricStatistics(statRequest);
		
		//Output monitor information
		System.out.println(statResult.toString());
		List<Datapoint> dataList = statResult.getDatapoints();
		if(dataList.size()>0)
		{for (Datapoint data : dataList){
			System.out.println(instanceName+" Average CPU utlilization for last 5 minutes is: "+data.getAverage());
			aveOfCPU = data.getAverage();
			}
		}
		
	} catch (AmazonServiceException ase) {
	    System.out.println("Caught Exception: " + ase.getMessage());
	    System.out.println("Reponse Status Code: " + ase.getStatusCode());
	    System.out.println("Error Code: " + ase.getErrorCode());
	    System.out.println("Request ID: " + ase.getRequestId());
	}
        return aveOfCPU;
	}

}
