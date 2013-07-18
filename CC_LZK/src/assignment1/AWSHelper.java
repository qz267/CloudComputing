package assignment1;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AllocateAddressResult;
import com.amazonaws.services.ec2.model.AssociateAddressRequest;
import com.amazonaws.services.ec2.model.AttachVolumeRequest;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateImageRequest;
import com.amazonaws.services.ec2.model.CreateImageResult;
import com.amazonaws.services.ec2.model.CreateKeyPairRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairResult;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateVolumeRequest;
import com.amazonaws.services.ec2.model.CreateVolumeResult;
import com.amazonaws.services.ec2.model.DeleteKeyPairRequest;
import com.amazonaws.services.ec2.model.DeleteSnapshotRequest;
import com.amazonaws.services.ec2.model.DeregisterImageRequest;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeKeyPairsResult;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.DetachVolumeRequest;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.KeyPair;
import com.amazonaws.services.ec2.model.KeyPairInfo;
import com.amazonaws.services.ec2.model.Placement;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.Volume;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * Helper singleton class for AWS operations
 * 
 * @author edwardlzk
 * 
 */
public class AWSHelper {

  public static AWSHelper instance = new AWSHelper();

  AmazonEC2 ec2;
  AmazonCloudWatchClient cloudWatch;
  AmazonAutoScaling autoScaling;
  String defaultAmi = "ami-aecd60c7";
  String groupName = "TestGroup";
  HashMap<String, String> keypair = new HashMap<String, String>();
  HashMap<String, String> volumes = new HashMap<String, String>();
  String keyPairLoc = "/home/edwardlzk/keys/";
  String AmiName = "TestAMI";
  String pythonLoop = "/home/edwardlzk/Dropbox/workspace1/DummyLoop/src/dummyloop.py";
  String rfile = "~/";
  int[] validPort = { 22, 80, 443 };// We allow ssh, http and https
  int waitTime = 180;// Waiting seconds for the instance creation.
  int checkInterval = 15;// seconds
  HashMap<String, String> savedIP = new HashMap<String, String>();
  HashMap<String, String> savedKey = new HashMap<String, String>();
  HashMap<String, String> savedVolume = new HashMap<String, String>();
  String availabilityZone = "us-east-1b";

  public static AWSHelper getInstance() {
    return instance;
  }

  private AWSHelper() {
    try {
      AWSCredentials credentials = new PropertiesCredentials(
          AWSHelper.class.getResourceAsStream("/AwsCredentials.properties"));
      ec2 = new AmazonEC2Client(credentials);
      cloudWatch = new AmazonCloudWatchClient(credentials);
    } catch (IOException e) {
      e.printStackTrace();
    }
  };

  /**
   * Initiate an instance and associate it with a new elastic IP
   * 
   * @return Instance
   * @throws Exception
   */
  public Instance initiateInstance() {

    createSecurityGroup(groupName, validPort);

    String uniqueKeyName = generateUniqueKeyName();

    createKeyPair(uniqueKeyName, keyPairLoc, true);

    String createdInstanceId = createSingleInstanceFromImage(defaultAmi,
        uniqueKeyName);

    

    getDNSInfo(createdInstanceId);

    // Accosiate EIP
    createEIPAssociate(createdInstanceId);
    
    //Create and assciate with a volume
    createAndAttachVolume(getInstance(createdInstanceId));

    return getInstance(createdInstanceId);
  }

  /**
   * Upload file through SSH, like scp command
   * 
   * @param instance
   *          the instance you want to upload to
   * @param keyLoc
   *          the Key file to this instance, <code>null</code> for default
   * @param localLoc
   *          the local file location
   * @param remoteLoc
   *          the remote file location
   */
  public void uploadFile(Instance instance, String keyLoc, String localLoc,
      String remoteLoc) throws JSchException, IOException, InterruptedException {

    int waitTime = this.waitTime;
    int checkInterval = this.checkInterval;

    waitForRunning(instance);

    System.out.println("uploading " + localLoc + " to instance:"
        + instance.getInstanceId());

    FileInputStream fis = null;
    String lfile = localLoc;
    String user = "ec2-user";
    String host = instance.getPublicIpAddress();
    if (host == null || host.equals("")) {
      // Elastic IP not binded.
      host = instance.getPublicDnsName();
    }
    String rfile = remoteLoc;

    if (keyLoc == null) {
      String key = keypair.get(instance.getInstanceId());
      keyLoc = keyPairLoc + key + ".pem";
    }
    JSch jsch = new JSch();
    jsch.addIdentity(keyLoc);
    Session session = jsch.getSession(user, host, 22);

    session.setConfig("StrictHostKeyChecking", "no");

    boolean loop = true;
    while (loop && waitTime > 0) {
      try {
        System.out.println("Try to log in through ssh");
        session.connect(checkInterval * 1000);
        System.out.println("Log in secessfully! Continue uploading");
        loop = false;
      } catch (JSchException e) {
        System.out.println("Log in failed, retry after " + checkInterval
            + " seconds");
//         e.printStackTrace();waitTime -= checkInterval;
        Thread.sleep(checkInterval * 1000);
        waitTime -= checkInterval;
      }
    }

    if (loop) {
      System.out.println("Failed");
      System.exit(0);
    }

    boolean ptimestamp = true;

    // exec 'scp -t rfile' remotely
    String command = "scp " + (ptimestamp ? "-p" : "") + " -t " + rfile;
    Channel channel = session.openChannel("exec");
    ((ChannelExec) channel).setCommand(command);

    // get I/O streams for remote scp
    OutputStream out = channel.getOutputStream();
    InputStream in = channel.getInputStream();

    channel.connect();

    if (checkAck(in) != 0) {
      System.exit(0);
    }
    File _lfile = new File(lfile);

    if (ptimestamp) {
      command = "T " + (_lfile.lastModified() / 1000) + " 0";
      // The access time should be sent here,
      // but it is not accessible with JavaAPI ;-<
      command += (" " + (_lfile.lastModified() / 1000) + " 0\n");
      out.write(command.getBytes());
      out.flush();
      if (checkAck(in) != 0) {
        System.exit(0);
      }
    }

    // send "C0644 filesize filename", where filename should not include '/'
    long filesize = _lfile.length();
    command = "C0644 " + filesize + " ";
    if (lfile.lastIndexOf('/') > 0) {
      command += lfile.substring(lfile.lastIndexOf('/') + 1);
    } else {
      command += lfile;
    }
    command += "\n";
    out.write(command.getBytes());
    out.flush();
    if (checkAck(in) != 0) {
      System.exit(0);
    }

    // send a content of lfile
    fis = new FileInputStream(lfile);
    byte[] buf = new byte[1024];
    while (true) {
      int len = fis.read(buf, 0, buf.length);
      if (len <= 0)
        break;
      out.write(buf, 0, len); // out.flush();
    }
    fis.close();
    fis = null;
    // send '\0'
    buf[0] = 0;
    out.write(buf, 0, 1);
    out.flush();
    if (checkAck(in) != 0) {
      System.exit(0);
    }
    out.close();

    channel.disconnect();
    session.disconnect();

    System.out.println("uploaded");

  }

  public void createAndAttachVolume(Instance instance){
    System.out.println("Creating and Attaching EBS to Instance:"+instance.getInstanceId());
    String zone = getZoneFromInstance(instance);
    String createdVolumeId = createVolume(zone);
    attachVolume(instance, createdVolumeId);
  }
  
  public String detachExternalVolume(Instance instance){
    
    String volumeId = volumes.get(instance.getInstanceId());
    if(volumeId != null){
      System.out.println("Detaching "+volumeId+" from Instance:"+instance.getInstanceId());
      DetachVolumeRequest dvr = new DetachVolumeRequest();
      dvr.setVolumeId(volumeId);
      dvr.setInstanceId(instance.getInstanceId());
      ec2.detachVolume(dvr);
      volumes.remove(instance.getInstanceId());
    }
    return volumeId;
  }
  
  
  public void attachVolume(Instance instance, String volumeId){
    System.out.println("attaching "+volumeId+" to Instance:"+instance.getInstanceId());
    waitForRunning(instance);
    AttachVolumeRequest avr = new AttachVolumeRequest();
    avr.setVolumeId(volumeId);
    avr.setInstanceId(instance.getInstanceId());
    avr.setDevice("/dev/sdf");
    ec2.attachVolume(avr);
    volumes.put(instance.getInstanceId(), volumeId);
  }
  
  /**
   * Execute command on a remote instance through SSH
   * 
   * @param instance
   *          the instance you want to reach
   * @param keyLoc
   *          the Key file to this instance, <code>null</code> for default
   * @param command
   *          command you need to execute
   */
  public void execCommand(Instance instance, String keyLoc, String command)
      throws JSchException, IOException, InterruptedException {
    int waitTime = this.waitTime;
    int checkInterval = this.checkInterval;

    JSch jsch = new JSch();
    if (keyLoc == null) {
      String key = keypair.get(instance.getInstanceId());
      keyLoc = keyPairLoc + key + ".pem";
    }
    jsch.addIdentity(keyLoc);
    String user = "ec2-user";
    String host = instance.getPublicIpAddress();
    if (host == null || host.equals("")) {
      // Elastic IP not binded.
      host = instance.getPublicDnsName();
    }
    Session session = jsch.getSession(user, host, 22);

    session.setConfig("StrictHostKeyChecking", "no");

    boolean loop = true;
    while (loop && waitTime > 0) {
      try {
        System.out.println("Try to log in through ssh");
        session.connect(checkInterval * 1000);
        System.out.println("Log in secessfully! Continue");
        loop = false;
      } catch (JSchException e) {
        System.out.println("Log in failed, retry after " + checkInterval
            + " seconds");
        // e.printStackTrace();
        Thread.sleep(checkInterval * 1000);
        waitTime -= checkInterval;
      }
    }

    if (loop) {
      System.out.println("Failed");
      System.exit(0);
    }
    Channel channel = session.openChannel("exec");
    System.out.println("Executing the following command in remote instance:"
        + instance.getInstanceId());
    System.out.println(command);
    ((ChannelExec) channel).setCommand(command);
    channel.setInputStream(System.in);
    ((ChannelExec) channel).setErrStream(System.err);
    InputStream in = channel.getInputStream();
    channel.connect();
    byte[] tmp = new byte[1024];
    while (true) {
      while (in.available() > 0) {
        int i = in.read(tmp, 0, 1024);
        if (i < 0)
          break;
        System.out.print(new String(tmp, 0, i));
      }
      if (channel.isClosed()) {
        System.out.println("exit-status: " + channel.getExitStatus());
        break;
      }
      try {
        Thread.sleep(1000);
      } catch (Exception ee) {
      }
    }
    channel.disconnect();
    session.disconnect();
  }

  /**
   * Back up the running instance to an AMI, returns the AMI ID for future use.
   * 
   * @param instanceId
   *          the instance Id which needs to be backed up.
   * @return AMI ID created
   */
  public String createImage(Instance instance) throws InterruptedException {
    int waitTime = this.waitTime;
    int checkInterval = this.checkInterval;
    if (instance != null) {

      waitForRunning(instance);

      System.out.println("Creating image for instance "
          + instance.getInstanceId());
      CreateImageRequest createImageRequest = new CreateImageRequest(
          instance.getInstanceId(), AmiName + instance.getInstanceId());
      CreateImageResult createImageResult = ec2.createImage(createImageRequest);
      String AmiId = createImageResult.getImageId();
      // Wait for AMI ID
      while (AmiId.equals("") && waitTime > 0) {
        System.out.println("Wait for the AMI");
        Thread.sleep(checkInterval*1000);
        waitTime -= checkInterval;
        DescribeImagesRequest describeImagesRequest = new DescribeImagesRequest();
        describeImagesRequest.withOwners(Arrays.<String> asList("self"));
        DescribeImagesResult describeImagesResult = ec2
            .describeImages(describeImagesRequest);
        List<Image> images = describeImagesResult.getImages();
        for (Image i : images) {
          if (i.getName().equals(AmiName + instance.getInstanceId())) {
            AmiId = i.getImageId();
            break;
          }
        }
        
      }
      if (AmiId.equals("")) {
        System.err.println("Try a bigger wait time");
        return null;
      }

      
      
      return AmiId;
    } else {
      return null;
    }
  }

  private String createVolume(String zone) {
    System.out.println("Creating Volume for Zone:"+zone);
    
    CreateVolumeRequest cvr = new CreateVolumeRequest();
    cvr.setAvailabilityZone(zone);
    cvr.setSize(10); // size = 10 gigabytes
    CreateVolumeResult volumeResult = ec2.createVolume(cvr);
    String createdVolumeId = volumeResult.getVolume().getVolumeId();

    return createdVolumeId;
  }
  
  

  /**
   * Terminate an instance, and create AMI and snapshot before terminating
   * 
   * @param instance
   *          the instance you need to terminate
   */
  public void terminateInstance(Instance instance) {
    //Detach the volume
    String volumeId = detachExternalVolume(instance);
    
    
    String amiId = null;
    
    //Snapshot the instance
    try {
      amiId = createImage(instance);
    } catch (Exception e1) {
      //AMI exist, may because of the elastic termination
    }
    
    try {
      waitAMIReady(amiId);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    System.out.printf("Terminating instance: %s\n", instance.getInstanceId());
    TerminateInstancesRequest tir = new TerminateInstancesRequest(
        Arrays.asList(instance.getInstanceId()));
    ec2.terminateInstances(tir);
    
    if(volumeId !=null){
      savedVolume.put(amiId, volumeId);
    }
    savedIP.put(amiId, instance.getPublicIpAddress());
    savedKey.put(amiId, keypair.get(instance.getInstanceId()));
    
    keypair.remove(instance.getInstanceId());
  }

  /**
   * Terminate all running instances
   */
  public void saveTerminateAll() {
    
    DescribeInstancesResult describeInstancesResult = ec2.describeInstances();
    List<Reservation> reservations = describeInstancesResult.getReservations();
    Set<Instance> instances = new HashSet<Instance>();
    // add all instances to a Set.
    for (Reservation reservation : reservations) {
      instances.addAll(reservation.getInstances());
    }
    for (Instance ins : instances) {

      if (!ins.getState().getName().equals("running")) {
        continue;
      }

      terminateInstance(ins);

    }
  }

  /**
   * resume all the images that terminated before with the same IP and key file
   */
  public void resumeSavedImage() {
    Set<String> images = savedIP.keySet();
    for (String i : images) {

      // Reassociate Keys
      String key = savedKey.get(i);

      System.out.println(i);
      System.out.println("Resuming " + i);
      String createdInstanceId = null, ip = null;
      createdInstanceId = createSingleInstanceFromImage(i, key);
      ip = savedIP.get(i);
      if (ip == null || ip.equals("")) {
        System.out.println("Image " + i + " does not have an IP record");
        continue;
      }
      getDNSInfo(createdInstanceId);
      associateIp(createdInstanceId, ip);

      waitForRunning(getInstance(createdInstanceId));
      
      String volumeId = savedVolume.get(i);
      if(volumeId != null){
        attachVolume(getInstance(createdInstanceId), volumeId);
      }
      destroyAMI(i);
      
      
    }
    System.out
        .println("All the stored instances have been resumed, a new day begin!!");
    savedIP = new HashMap<String, String>();
    savedKey = new HashMap<String, String>();
    savedVolume = new HashMap<String, String>();
  }
  

  /**
   * Create an instance from a given AMI, make sure the group and key pair are
   * created.
   * 
   * @param imageId
   *          ID of the AMI
   * @return created Instance ID
   * @throws InterruptedException
   */
  public String createSingleInstanceFromImage(String imageId, String keyName) {

    try {
      waitAMIReady(imageId);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    int minInstanceCount = 1; // create 1 instance
    int maxInstanceCount = 1;
    RunInstancesRequest rir = new RunInstancesRequest();
    Placement p = new Placement();
    p.setAvailabilityZone(availabilityZone);
    rir.withImageId(imageId).withInstanceType("t1.micro")
        .withMinCount(minInstanceCount).withMaxCount(maxInstanceCount)
        .withSecurityGroups(groupName).withKeyName(keyName).setPlacement(p);
    RunInstancesResult result = ec2.runInstances(rir);

    // get instanceId from the result
    List<Instance> resultInstance = result.getReservation().getInstances();
    String createdInstanceId = null;
    for (Instance ins : resultInstance) {
      createdInstanceId = ins.getInstanceId();
      System.out.println("New instance has been created: "
          + ins.getInstanceId());
    }
    keypair.put(createdInstanceId, keyName);
    return createdInstanceId;
  }

  /**
   * Add a new thread to the system which is to execute a dummy loop on the
   * instance. Make sure you uploaded the file before.
 * @throws InterruptedException 
 * @throws IOException 
 * @throws JSchException 
   */
  public void addLoad(Instance instance) throws JSchException, IOException, InterruptedException {

//    try {
      uploadFile(instance, null, pythonLoop, rfile);
//    } catch (JSchException | IOException | InterruptedException e) {
//      e.printStackTrace();
//    }
    Pattern pattern = Pattern.compile(".*/(\\w*\\.py)$");
    Matcher matcher = pattern.matcher(pythonLoop);
    matcher.find();
    String pythonFileName = matcher.group(1);

    AddLoadRunnable addLoad = new AddLoadRunnable(instance, rfile
        + pythonFileName);
    Thread addLoadThread = new Thread(addLoad);
    addLoadThread.start();
  }

  /**
   * Cloud watch for a bunch of instances, get back average CPU usage in a given
   * time window
   * 
   * @param instances
   *          an array of instances that need to be monitored
   * @param timeLength
   *          the desired time window, in minutes
   * @return a <code>Hashtable</code> that maps instance identifier to CPU
   *         usage.
   */
  public double getCpuUsable(Instance instance, int timeLength) {

    double result = -1;

    if (instance == null) {
      System.out.println("No instance");
      return -1;
    }
    // create request message
    GetMetricStatisticsRequest statRequest = new GetMetricStatisticsRequest();

    // set up request message
    statRequest.setNamespace("AWS/EC2"); // namespace
    statRequest.setPeriod(60); // period of data
    ArrayList<String> stats = new ArrayList<String>();

    // Use one of these strings: Average, Maximum, Minimum, SampleCount, Sum
    stats.add("Average");
    stats.add("Sum");
    statRequest.setStatistics(stats);

    // Use one of these strings: CPUUtilization, NetworkIn, NetworkOut,
    // DiskReadBytes, DiskWriteBytes, DiskReadOperations
    statRequest.setMetricName("CPUUtilization");

    // set time
    GregorianCalendar calendar = new GregorianCalendar(
        TimeZone.getTimeZone("UTC"));
    calendar.add(GregorianCalendar.SECOND,
        -1 * calendar.get(GregorianCalendar.SECOND)); // 1 second ago
    Date endTime = calendar.getTime();
    calendar.add(GregorianCalendar.MINUTE, -timeLength); // timeLength minutes
                                                         // ago
    Date startTime = calendar.getTime();
    statRequest.setStartTime(startTime);
    statRequest.setEndTime(endTime);

    // specify an instance
    ArrayList<Dimension> dimensions = new ArrayList<Dimension>();
    dimensions.add(new Dimension().withName("InstanceId").withValue(
        instance.getInstanceId()));

    statRequest.setDimensions(dimensions);

    // get statistics
    GetMetricStatisticsResult statResult = cloudWatch
        .getMetricStatistics(statRequest);

    List<Datapoint> dataList = statResult.getDatapoints();
    Double averageCPU = null;
    for (int i = 0; i < dataList.size(); i++) {
      Datapoint data = dataList.get(i);
      averageCPU = data.getAverage();
      result = averageCPU;
      // System.out.println("Average CPU utlilization of "
      // + instance.getInstanceId() + " for last " + timeLength
      // + " minutes: " + averageCPU);
    }

    return result;
  }

  /**
   * Destroy the AMI and its snapshot.
   * 
   * @param amiId
   *          the AMI needs to be destroyed.
   */
  private void destroyAMI(String amiId) {
    System.out.println("====Destroying the AMI:" + amiId
        + " and connected Snapshot====");
    DescribeImagesRequest describeImagesRequest = new DescribeImagesRequest();
    describeImagesRequest.withImageIds(amiId);
    DescribeImagesResult describeImagesResult = ec2
        .describeImages(describeImagesRequest);
    if (describeImagesResult.getImages().size() == 0) {
      System.out.println("AMI ID:" + amiId + " not found");
      return;
    }
    Image image = describeImagesResult.getImages().get(0);
    if (!image.getState().equals("available")) {
      try {
        waitAMIReady(amiId);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    String snapshotId = image.getBlockDeviceMappings().get(0).getEbs()
        .getSnapshotId();
    // Deregister the AMI image
    DeregisterImageRequest deregisterImageRequest = new DeregisterImageRequest(
        amiId);
    ec2.deregisterImage(deregisterImageRequest);

    // Delete Snapshot
    DeleteSnapshotRequest deleteSnapshotRequest = new DeleteSnapshotRequest(
        snapshotId);
    ec2.deleteSnapshot(deleteSnapshotRequest);

    System.out.println("AMI:" + amiId + " and Snapshot:" + snapshotId
        + " have been deleted");
  }

  /**
   * AMI needs some time to turn to available status, this function blocks the
   * thread until the requested AMI is ready
   * 
   * @param amiId
   *          requested AMI
   * @throws InterruptedException 
   */
  private void waitAMIReady(String amiId) throws InterruptedException {
    int waitTime = this.waitTime;
    int checkInterval = this.checkInterval;
    // Check if the AMI is pending
    DescribeImagesRequest describeImagesRequest = new DescribeImagesRequest();
    describeImagesRequest.withImageIds(amiId);
    DescribeImagesResult describeImagesResult = null;
    while(waitTime > 0){
    try{
      
      describeImagesResult = ec2
        .describeImages(describeImagesRequest);
      break;
    }
    catch(Exception e){
      Thread.sleep(checkInterval * 1000);
      waitTime -= checkInterval;
    }
    }
    Image image = describeImagesResult.getImages().get(0);
    while (!image.getState().equals("available") && waitTime > 0) {
      System.out.println("Wait for AMI");
      try {
        Thread.sleep(checkInterval * 1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      waitTime -= checkInterval;
      image = ec2.describeImages(describeImagesRequest).getImages().get(0);
    }
    if (!image.getState().equals("available")) {
      System.err.println("Try a bigger wait time");
      return;
    }
  }

  /**
   * Internal function for scp
   */
  private int checkAck(InputStream in) throws IOException {
    int b = in.read();
    // b may be 0 for success,
    // 1 for error,
    // 2 for fatal error,
    // -1
    if (b == 0)
      return b;
    if (b == -1)
      return b;

    if (b == 1 || b == 2) {
      StringBuffer sb = new StringBuffer();
      int c;
      do {
        c = in.read();
        sb.append((char) c);
      } while (c != '\n');
      if (b == 1) { // error
        System.out.print(sb.toString());
      }
      if (b == 2) { // fatal error
        System.out.print(sb.toString());
      }
    }
    return b;
  }

  /**
   * create security group for ec2, with some ports opened. If the security
   * group exists, do not try to create.
   * 
   * @param groupName
   *          Name of the security group
   * @param ports
   *          Ports that to be opened
   */
  private void createSecurityGroup(String groupName, int[] ports) {
    DescribeSecurityGroupsResult dsgr = ec2.describeSecurityGroups();
    // If the system does not contain a security group with same name, create
    // the group
    List<SecurityGroup> currentGroups = dsgr.getSecurityGroups();
    boolean groupFound = false;
    for (SecurityGroup group : currentGroups) {
      if (group.getGroupName().equals(groupName)) {
        groupFound = true;
      }
    }
    if (!groupFound) {
      System.out.printf(
          "Security group %s does not exist, new security group created.\n",
          groupName);
      CreateSecurityGroupRequest createSecurityGroupRequest = new CreateSecurityGroupRequest()
          .withGroupName(groupName)
          .withDescription("A security group for test");
      ec2.createSecurityGroup(createSecurityGroupRequest);

      // Set rules for security group

      IpPermission[] permissions = new IpPermission[ports.length];
      for (int i = 0; i < ports.length; i++) {
        int port = ports[i];
        IpPermission ipPermission = new IpPermission();
        ipPermission.withIpRanges("0.0.0.0/0").withIpProtocol("tcp")
            .withFromPort(port).withToPort(port);
        permissions[i] = ipPermission;
      }
      AuthorizeSecurityGroupIngressRequest authorizeSecurityGroupIngressRequest = new AuthorizeSecurityGroupIngressRequest();
      authorizeSecurityGroupIngressRequest.withGroupName(groupName)
          .withIpPermissions(permissions);
      ec2.authorizeSecurityGroupIngress(authorizeSecurityGroupIngressRequest);
    } else {
      System.out
          .println("Security group with same name exsited, proceed without creating a new one.");
    }
  }

  /**
   * Create key pair with given keyName and store the key to the given location.
   * Can choose if overwrite the key when the key name is not unique.
   * 
   * @param keyName
   *          name of the key pair
   * @param Loc
   *          location of storing key
   * @param overwrite
   *          whether or not overwrite
   */
  private void createKeyPair(String keyName, String Loc, boolean overwrite) {
    // get existing key pairs
    boolean keyFound = false;
    DescribeKeyPairsResult dkr = ec2.describeKeyPairs();
    List<KeyPairInfo> keys = dkr.getKeyPairs();
    for (KeyPairInfo keyInfo : keys) {
      if (keyInfo.getKeyName().equals(keyName)) {
        keyFound = true;
      }
    }
    // if key is not found, create a new one and stored into the location.
    if (keyFound && !overwrite) {
      System.out
          .println("Key pair with same name exsited, proceed without creating a new one.");
      return;
    }

    if (keyFound && overwrite) {
      DeleteKeyPairRequest deleteKeyPairRequest = new DeleteKeyPairRequest(
          keyName);
      ec2.deleteKeyPair(deleteKeyPairRequest);
    }

    System.out.printf(
        "Key name %s does not exist, new key pair created and stored in %s.\n",
        keyName, keyPairLoc);
    CreateKeyPairRequest createKeyPairRequest = new CreateKeyPairRequest();
    createKeyPairRequest.withKeyName(keyName);
    CreateKeyPairResult createKeyPairResult = ec2
        .createKeyPair(createKeyPairRequest);
    KeyPair keyPair = createKeyPairResult.getKeyPair();
    String privateKey = keyPair.getKeyMaterial();
    // Write the key to disk
    try {
      FileWriter fstream = new FileWriter(keyPairLoc + keyName + ".pem");
      BufferedWriter out = new BufferedWriter(fstream);
      out.write(privateKey);
      // Close the output stream
      out.close();
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
  }

  /**
   * Allocate elastic IP and associate it with an instance.
   * 
   * @param instanceId
   *          the instance Id to be associated.
   * @return Elastic IP
   */
  public String createEIPAssociate(String instanceId) {
    // allocate
    AllocateAddressResult elasticResult = ec2.allocateAddress();
    String elasticIp = elasticResult.getPublicIp();
    System.out.println("New elastic IP: " + elasticIp);
    associateIp(instanceId, elasticIp);
    return elasticIp;
  }

  /**
   * Associate existing EIP to an instance
   * 
   * @param instanceId
   *          instance ID
   * @param elasticIp
   *          Elastic IP address
   */
  public void associateIp(String instanceId, String elasticIp) {
    System.out.println("Associating " + elasticIp + " to Instance:"
        + instanceId);
    AssociateAddressRequest aar = new AssociateAddressRequest();
    aar.setInstanceId(instanceId);
    aar.setPublicIp(elasticIp);
    ec2.associateAddress(aar);
  }

  /**
   * Get the public DNS information of an instance, wait until the network layer
   * is initiated.
   * 
   * @param instanceId
   *          ID of instance
   * @return public DNS
   * @throws InterruptedException
   */
  private String getDNSInfo(String instanceId) {
    int waitTime = this.waitTime;
    int checkInterval = this.checkInterval;

    System.out.println("Wait the Instance to be created");
    DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
    describeInstancesRequest.withInstanceIds(instanceId);
    DescribeInstancesResult describeRunningInstancesResult;
    List<Reservation> runningReservations;
    String publicDNS = "";
    while (publicDNS.equals("") && waitTime > 0) {
      describeRunningInstancesResult = ec2
          .describeInstances(describeInstancesRequest);
      runningReservations = describeRunningInstancesResult.getReservations();
      if (runningReservations.size() == 0
          || runningReservations.get(0).getInstances().size() == 0) {
        // not even initiated yet
        System.out.println("Waiting for initiating");
        try {
          Thread.sleep(checkInterval * 1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        waitTime -= checkInterval;
        continue;
      }
      Instance runningInstance = runningReservations.get(0).getInstances()
          .get(0);
      publicDNS = runningInstance.getPublicDnsName();
      if (publicDNS.equals("")) {
        // not given a public DNS
        System.out.println("Waiting for DNS");
        try {
          Thread.sleep(checkInterval * 1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        waitTime -= checkInterval;
        continue;
      }
    }
    if (publicDNS.equals("")) {
      // Maybe due to timeout
      System.out.println("Can not get a public DNS, try a bigger wait time");
    } else {
      System.out.println("The Public DNS is " + publicDNS);
      System.out.println("Run the following command to access the server:");

      String key = keypair.get(instanceId);
      System.out.printf("ssh -i %s%s.pem ec2-user@%s\n", keyPairLoc, key,
          publicDNS);
    }
    return publicDNS;
  }

  /**
   * This function blocks the thread until the instance is on running status
   * 
   * @param instance
   *          requested instance
   */
  private void waitForRunning(Instance instance) {
    int waitTime = this.waitTime;
    int checkInterval = this.checkInterval;
    if (!instance.getState().getName().equals("running") && waitTime > 0) {
      System.out.println("Waiting the instance:" + instance.getInstanceId()
          + " get to running state");
      try {
        Thread.sleep(checkInterval * 1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      waitTime -= checkInterval;
      instance = getInstance(instance.getInstanceId());
    }
    if (!instance.getState().getName().equals("running")) {
      System.out.println("The instance:" + instance.getInstanceId()
          + " is not able to get to running state");
    }
  }

  /**
   * Generate a unique <code>String</code> that can be used as key name.
   * 
   * @return
   */
  private String generateUniqueKeyName() {
    return UUID.randomUUID() + "";
  }

  /**
   * Get <code>Instance</code> object with ID.
   * 
   * @param instanceId
   *          instance Id
   * @return corresponding Id, or <code>null</code> for not existing instance.
   */
  public Instance getInstance(String instanceId) {
    DescribeInstancesResult describeInstancesResult = ec2.describeInstances();
    List<Reservation> reservations = describeInstancesResult.getReservations();
    Set<Instance> instances = new HashSet<Instance>();
    // add all instances to a Set.
    for (Reservation reservation : reservations) {
      instances.addAll(reservation.getInstances());
    }
    Instance instance = null;
    for (Instance ins : instances) {

      // instance id
      String currentInstanceId = ins.getInstanceId().trim();

      if (currentInstanceId.equals(instanceId.trim())) {
        instance = ins;
        break;
      }

    }
    return instance;
  }

  private String getZoneFromInstance(Instance instance){
    String volumeId = instance.getBlockDeviceMappings().get(0).getEbs().getVolumeId();
    DescribeVolumesRequest describeVolumesRequest = new DescribeVolumesRequest().withVolumeIds(volumeId);
    DescribeVolumesResult describeVolumesResult = ec2.describeVolumes(describeVolumesRequest);
    Volume volume = describeVolumesResult.getVolumes().get(0);
    return volume.getAvailabilityZone();
    
  }
  
  
  /**
   * Get currently available zones
   * 
   * @return zones
   */
  public DescribeAvailabilityZonesResult describeAvailableZones() {
    return ec2.describeAvailabilityZones();
  }

}
