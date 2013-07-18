package assignment1;

import com.amazonaws.services.ec2.model.Instance;

public class Control {

  //TODO
  //2. data volumn attach & detach
  /**
   * @param args
   */
  public static void main(String[] args) {
    AWSHelper awsHelper = AWSHelper.getInstance();
    
    try {
      
      //Initialize the two instances
      Instance instance1 = awsHelper.initiateInstance();
      Instance instance2 = awsHelper.initiateInstance();
      
      //Add Volumns to them
//      instance1.getBlockDeviceMappings().get(0).getEbs().getVolumeId();
      
//      Instance instance = awsHelper.getInstance("i-34b0cb49");
      awsHelper.addLoad(instance1);
      
      MonitorWrapperThread monitorWrapperThread = new MonitorWrapperThread(60,5, 4);
      monitorWrapperThread.start();
      
      monitorWrapperThread.addMoniterInstance(instance1);
      monitorWrapperThread.addMoniterInstance(instance2);
      
      
      Thread.sleep(8*60*1000);// One day work
      System.out.println("====Time's up, save and terminate all running instances, Good Night====");
      monitorWrapperThread.threadStop();
      awsHelper.saveTerminateAll();
      
      Thread.sleep(3*60*1000);//At night
      
      System.out.println("Back to work");
      awsHelper.resumeSavedImage();
      
    } catch (Exception e) {
      e.printStackTrace();
    }

  }
  
  
  

}



