package assignment1;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.amazonaws.services.ec2.model.Instance;

/**
 * Thread for monitering instances, including elastic support
 * 
 * @author edwardlzk
 * 
 */
public class MonitorWrapperThread extends Thread {

  private AWSHelper awsHelper;
  private int upScale;
  private int downScale;
  private int upscaleNum = 1;
  private int frequency = 60;// seconds
  private boolean keepMonitoring = true;
  private int maxInstances;
  private List<Instance> monitered = new CopyOnWriteArrayList<Instance>();

  /**
   * Constructor for the monitering thread
   * 
   * @param upScale
   *          threshold for increasing instances
   * @param downScale
   *          threshold for removing instances
   * @param maxInstances
   *          max instance number in this system.
   */
  public MonitorWrapperThread(int upScale, int downScale, int maxInstances) {
    this.upScale = upScale;
    this.downScale = downScale;
    this.maxInstances = maxInstances;
    awsHelper = AWSHelper.getInstance();

  }

  /**
   * Add instance to the monitered list
   * 
   * @param instance
   */
  public void addMoniterInstance(Instance instance) {
    monitered.add(instance);
    System.out.println("[Moniter Thread]Instance " + instance.getInstanceId()
        + " is added to the monitered list, will be checked in next batch");
  }

  /**
   * Remove the instance from monitered list
   * 
   * @param instance
   */
  public void removeMoniterInstance(Instance instance) {
    monitered.remove(instance);
    System.out.println("[Moniter Thread]Instance " + instance.getInstanceId()
        + " has been removed from the list");
  }

  @Override
  public void run() {
    while (keepMonitoring) {
      try {
        System.out.println("[Moniter Thread]Checking CPU usage now");
        if (monitered.size() == 0) {
          System.out
              .println("[Moniter Thread]No registered instances, next batch will run in "
                  + frequency + " seconds");
        }
        Hashtable<String, Double> result = new Hashtable<String, Double>();
        for (Instance i : monitered) {
          double currentResult = awsHelper.getCpuUsable(i, 6);
          if (currentResult < 0) {
            System.out.println("[Moniter Thread]" + i.getInstanceId()
                + ":unavailable");
          } else {
            System.out.println("[Moniter Thread]" + i.getInstanceId() + ":"
                + currentResult + "%");
            result.put(i.getInstanceId(), currentResult);
          }
        }

        if (result.size() == 0) {
          System.out
              .println("[Moniter Thread]Reports are currently unavailable");
          Thread.sleep(frequency * 1000);
          continue;
        }
        Enumeration<String> enumKey = result.keys();
        while (enumKey.hasMoreElements()) {
          String key = enumKey.nextElement();
          double value = result.get(key);
          System.out.println("[Moniter Thread] Instance " + key + ":" + value
              + "%");
          elasticScale(key, value);
        }

        Thread.sleep(frequency * 1000);

      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

  }

  public void threadStop() {
    keepMonitoring = false;
    monitered = new ArrayList<Instance>();
  }

  private void elasticScale(String instanceId, double cpuUsage) {
    if (cpuUsage > upScale) {
      if (monitered.size() < maxInstances) {
        // Add more instance
        System.out.println("[Moniter Thread] Instance " + instanceId
            + " too busy, add " + upscaleNum + " more instances");
        Instance instance = awsHelper.initiateInstance();
        addMoniterInstance(instance);
      } else {
        System.out.println("[Moniter Thread]The system has " + monitered.size()
            + " running instances, not able to add more.");
      }
      return;
    }
    if (cpuUsage < downScale) {
      // Terminate that instance
      System.out.println("[Moniter Thread] Instance " + instanceId
          + " should be terminated");
      Instance instance = awsHelper.getInstance(instanceId);
      awsHelper.terminateInstance(instance);
      removeMoniterInstance(instance);
    }

  }

}
