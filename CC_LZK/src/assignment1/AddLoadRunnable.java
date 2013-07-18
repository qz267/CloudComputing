package assignment1;

import java.io.IOException;

import com.amazonaws.services.ec2.model.Instance;
import com.jcraft.jsch.JSchException;

public class AddLoadRunnable implements Runnable{
  
  private Instance targetObject;
  private AWSHelper awsHelper;
  private String identifier;
  
  public AddLoadRunnable(Instance instance, String rFile){
    this.targetObject = instance;
    awsHelper = AWSHelper.getInstance();
    identifier = "[Add Load Thread: Instance "+instance.getInstanceId()+"]";
    System.out.println(identifier+"New thread for running infinite loop created");
  }
  
  @Override
  public void run(){
    try {
      awsHelper.execCommand(targetObject, null,"python ~/dummyloop.py");
    } catch (JSchException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  
}
  

