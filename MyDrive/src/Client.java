

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.commons.io.FileUtils;
import org.apache.commons.vfs2.FileChangeEvent;
import org.apache.commons.vfs2.FileListener;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.impl.DefaultFileMonitor;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;

public class Client {

	private JFrame frame;
	private JTextField textFieldUserName;
	private JTextField textFieldPassword;

	private Timer updatesCheckingTimer;
	private DefaultFileMonitor fileMonitor;
	
	private Map<String,FileInfo> versionTable;
	private String userName;
	private String passWord;
	private Lock bookKeepingLock;
	
	private static final String LOCAL_BOOKKEEPING_FILE = "version.ser";
	private static final String USER_CREDENTIAL_FILE = "user.inf";
	private static final String homeDir = "C:\\MyDrive\\";
	private static final String serverURL = "http://mydriveenv-tdhahbcv6d.elasticbeanstalk.com/mydrive";

	private static final String USER_NAME = "username";
	private static final String PASSWORD = "password";
	private static final String COMMAND = "command";
	private static final String FILES = "files";
	private static final String FILE_NAME = "filename";

	private static final String CMD_USER_REGISTER = "0";
	private static final String CMD_USER_SIGNIN = "1";
	private static final String CMD_GET_VERSION = "2";
	private static final String CMD_GET_URL = "3";
	private static final String CMD_UPLOAD_FILE = "4";
	private static final String CMD_DELETE_FILE = "5";
	private static final String CMD_USER_DEREGISTER = "6";

	private static final String SUCCESS_EC = "0";
	private static final String FAILURE_EC = "1";
	
	private static final long POLLING_DELAY = 1000;
	private static final long POLLING_PERIOD = 300000;

	/**
	 * Launch the application
	 * @param args
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Client window = new Client();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 * @throws NumberFormatException 
	 */
	public Client() throws NumberFormatException, IOException, ClassNotFoundException {
		bookKeepingLock = new ReentrantLock();
		versionTable = new HashMap<String,FileInfo>();
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 * @throws NumberFormatException 
	 */
	private void initialize() throws IOException, ClassNotFoundException {
		//initialize UI
		initializeUI();

		//read local user name and password
		String res = readUserCredentials();
		if(res.equals(SUCCESS_EC)){
			//user has signed in before and not signed out

			//read local bookkeeping table
			populateBookKeepingMap();

			//traverse MyDrive folder
			//the folder should exists
			Set<String> keys = new HashSet<String>();
			keys.addAll(versionTable.keySet());
			traverseMyDrive(new File(homeDir),keys);
			for(String key : keys){
				//those are deleted when the program is not running or
				//file monitor doesn't catch
				versionTable.remove(key);
				writeBookKeepingMap();
				//send request to delete file on server
				//deleteFile(key);				
			}

			//register monitor for tracking local file changes
			registerFileMonitor();

			//set timer to polling updates from server periodically
			updatesCheckingTimer = new Timer(true);
			updatesCheckingTimer.schedule(new UpdatesCheckingTask(), POLLING_DELAY, POLLING_PERIOD);
		}
	}

	private void initializeUI(){
		frame = new JFrame();
		frame.setBounds(100, 100, 450, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JPanel panel = new JPanel();
		frame.getContentPane().add(panel, BorderLayout.CENTER);
		panel.setLayout(null);

		textFieldUserName = new JTextField();
		textFieldUserName.setBounds(161, 45, 89, 23);
		panel.add(textFieldUserName);
		textFieldUserName.setColumns(10);

		textFieldPassword = new JTextField();
		textFieldPassword.setBounds(161, 76, 89, 23);
		panel.add(textFieldPassword);
		textFieldPassword.setColumns(10);

		JLabel lblUsername = new JLabel("Username");
		lblUsername.setBounds(85, 48, 66, 23);
		panel.add(lblUsername);

		JLabel lblPassword = new JLabel("Password");
		lblPassword.setBounds(85, 79, 66, 23);
		panel.add(lblPassword);

		JButton btnSignIn = new JButton("Sign In");
		btnSignIn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String username = textFieldUserName.getText();
				String pwd = textFieldPassword.getText();
				signinUser(username,pwd);
			}
		});
		btnSignIn.setBounds(161, 121, 89, 23);
		panel.add(btnSignIn);

		JButton btnRegister = new JButton("Register");
		btnRegister.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String username = textFieldUserName.getText();
				String pwd = textFieldPassword.getText();
				createUser(username, pwd);			
			}
		});
		btnRegister.setBounds(161, 155, 89, 23);
		panel.add(btnRegister);

		JButton btnSync = new JButton("Synchronize");
		btnSync.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//somehow the file monitor could lost some events
				//so traverse before check with server
				Set<String> keys = new HashSet<String>();
				keys.addAll(versionTable.keySet());
				traverseMyDrive(new File(homeDir),keys);
				for(String key : keys){
					//those are deleted when the program is not running or
					//file monitor doesn't catch
					versionTable.remove(key);
					writeBookKeepingMap();
					//send request to delete file on server
					//deleteFile(key);				
				}
				checkAndUpdate();
			}
		});
		btnSync.setBounds(161, 189, 89, 23);
		panel.add(btnSync);

		JButton btnUnregister = new JButton("Unregister");
		btnUnregister.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String username = textFieldUserName.getText();
				String pwd = textFieldPassword.getText();
				deleteUser(username,pwd);
			}
		});
		btnUnregister.setBounds(161, 223, 89, 23);
		panel.add(btnUnregister);
	}

	private void registerFileMonitor() throws FileSystemException{
		FileSystemManager fsManager = VFS.getManager();
		FileObject listendir = fsManager.resolveFile(homeDir);

		fileMonitor = new DefaultFileMonitor(new CustomerFileListener());
		fileMonitor.setRecursive(true);
		fileMonitor.addFile(listendir);
		fileMonitor.start();
	}

	public class CustomerFileListener implements FileListener{

		@Override
		public void fileChanged(FileChangeEvent arg0) throws Exception {
			bookKeepingLock.lock();
			try{
				String path = arg0.getFile().getName().getPath();
				System.out.println("file changed: " + path);

				path = "C:" + path.replace('/','\\');
				File file = new File(path);
				//do nothing when a file folder changed
				if(file.isFile()){
					String s3Key = path.substring(homeDir.length()).replace('\\', '/');
					FileInfo fileInfo = versionTable.get(s3Key);
					if(fileInfo != null){
						if(fileInfo.isModifiedByUser){
							//file updated by user, not by our program
							fileInfo.isModified = true;
							fileInfo.localLastModifiedTime = file.lastModified();
							//upload file now or wait until later for the timer?
							String res = uploadFile(path);

							//further process response
							String[] responses = res.trim().split(",");
							if(responses[0].trim().equals(SUCCESS_EC)){
								fileInfo.version = responses[1];
								fileInfo.isModified = false;
							} else {
								System.err.println(res);
							}
							writeBookKeepingMap();
						}
					} else {
						//this should not happen
						//maybe just treat it as new file 
						//fileCreated(arg0);
					}
				}
			} finally {
				bookKeepingLock.unlock();
			}
		}

		@Override
		public void fileCreated(FileChangeEvent arg0) throws Exception {
			bookKeepingLock.lock();
			try {
				String path = arg0.getFile().getName().getPath();
				System.out.println("file added: " + path);

				path = "C:" + path.replace('/','\\');
				File file = new File(path);
				//do nothing when a file folder added
				if(file.isFile()){
					String s3Key = path.substring(homeDir.length()).replace('\\','/');
					FileInfo fileInfo = versionTable.get(s3Key);
					if(fileInfo == null){
						//added by user
						fileInfo = new FileInfo();
						fileInfo.absolutePath = path;
						fileInfo.isModified = true;
						fileInfo.isModifiedByUser = true;
						fileInfo.localLastModifiedTime = file.lastModified();
						fileInfo.version = "";
						versionTable.put(s3Key, fileInfo);
						//upload file now or wait until later for the timer?
						String res = uploadFile(path);

						//further process response
						String[] responses = res.trim().split(",");
						if(responses[0].trim().equals(SUCCESS_EC)){
							fileInfo.version = responses[1];
							fileInfo.isModified = false;
						} else {
							System.err.println(res);
						}
						writeBookKeepingMap();
					}
				}
			} finally {
				bookKeepingLock.unlock();
			}
		}

		@Override
		public void fileDeleted(FileChangeEvent arg0) throws Exception {
			bookKeepingLock.lock();
			try {
				String path = arg0.getFile().getName().getPath();
				System.out.println("file removed: " + path);

				FileType fileType = arg0.getFile().getName().getType();
				//do nothing when a file folder removed
				if(fileType == FileType.FILE){
					path = "C:" + path.replace('/','\\');
					String s3Key = path.substring(homeDir.length()).replace('\\','/');
					FileInfo fileInfo = versionTable.get(s3Key);
					if(fileInfo != null){
						versionTable.remove(s3Key);
						writeBookKeepingMap();
						//send request to delete file on server
						deleteFile(s3Key);
					}
				}
			} finally {
				bookKeepingLock.unlock();
			}			
		}

	}

	static class FileInfo implements Serializable{
		public String absolutePath;
		public String version;
		public Boolean isModified;
		public Boolean isModifiedByUser;
		public Long localLastModifiedTime;
		
		public FileInfo(){
			absolutePath = "";
			version = "";
			isModified = false;
			isModifiedByUser = true;
			localLastModifiedTime = 0L;
		}
	}

	class UpdatesCheckingTask extends TimerTask {
		public void run(){
			//somehow the file monitor could lost some events
			//so traverse before check with server
			Set<String> keys = new HashSet<String>();
			keys.addAll(versionTable.keySet());
			traverseMyDrive(new File(homeDir),keys);
			for(String key : keys){
				//those are deleted when the program is not running or
				//file monitor doesn't catch
				versionTable.remove(key);
				writeBookKeepingMap();
				//send request to delete file on server
				//deleteFile(key);				
			}
			
			//polling updates from server
			System.out.println("Polling updates from server...");
			checkAndUpdate();
			System.out.println("Synchronization done!");
		}
	}

	private void createUser(String userName, String passWord){		
		try {
			MultipartEntity multiPartEntity = new MultipartEntity () ;
			multiPartEntity.addPart(USER_NAME, new StringBody(userName));
			multiPartEntity.addPart(PASSWORD, new StringBody(passWord));
			multiPartEntity.addPart(COMMAND, new StringBody(CMD_USER_REGISTER));

			HttpPost postRequest = new HttpPost (serverURL);
			postRequest.setEntity(multiPartEntity);

			String res =  executeRequest (postRequest);
			System.out.print(res);

			//further process response
			String[] responses = res.trim().split(",");
			if(responses[0].trim().equals(SUCCESS_EC)){
				//automatically sign in
				signinUser(userName, passWord);
			} else {
				System.err.print(res);
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	private void signinUser(String userName, String passWord){
		try {
			if(!this.userName.equals(userName) || 
					!this.passWord.equals(passWord)){
				//this user has not signed in yet
				//send request to sign in
				MultipartEntity multiPartEntity = new MultipartEntity () ;
				multiPartEntity.addPart(USER_NAME, new StringBody(userName));
				multiPartEntity.addPart(PASSWORD, new StringBody(passWord));
				multiPartEntity.addPart(COMMAND, new StringBody(CMD_USER_SIGNIN));

				HttpPost postRequest = new HttpPost (serverURL);
				postRequest.setEntity(multiPartEntity);

				String res = executeRequest(postRequest);
				System.out.print(res);

				//further process response
				String[] responses = res.trim().split(",");
				if(responses[0].trim().equals(SUCCESS_EC)){
					//save user name and password locally
					this.userName = userName;
					this.passWord = passWord;
					saveUserCredentials();

					//create a new folder for the new signed in user
					File myDrive = new File(homeDir);
					if(myDrive.exists()){
						FileUtils.deleteDirectory(myDrive);
					}
					myDrive.mkdir();

					//clear history resource
					if(fileMonitor != null){
						fileMonitor.stop();
						fileMonitor = null;
					}

					if(updatesCheckingTimer != null){
						updatesCheckingTimer.cancel();
						updatesCheckingTimer = null;
					}
				} else {
					System.err.print(res);
				}
			}

			//register monitor for tracking local file changes
			if(fileMonitor == null){
				registerFileMonitor();
			}

			//set timer to polling updates from server periodically
			if(updatesCheckingTimer == null){
				updatesCheckingTimer = new Timer(true);
				updatesCheckingTimer.schedule(new UpdatesCheckingTask(), POLLING_DELAY, POLLING_PERIOD);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void deleteUser(String userName, String passWord){
		try {
			MultipartEntity multiPartEntity = new MultipartEntity () ;
			multiPartEntity.addPart(USER_NAME, new StringBody(userName));
			multiPartEntity.addPart(PASSWORD, new StringBody(passWord));
			multiPartEntity.addPart(COMMAND, new StringBody(CMD_USER_DEREGISTER));

			HttpPost postRequest = new HttpPost (serverURL);
			postRequest.setEntity(multiPartEntity);

			String res = executeRequest(postRequest);
			System.out.println(res);

			//further process response
			String[] responses = res.trim().split(",");
			if(responses[0].trim().equals(SUCCESS_EC)){
				//do nothing
				//service for this user will not work
				//the local files still exists
			} else {
				System.err.println(res);
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	private String uploadFile(String absPath){
		String res = SUCCESS_EC;
		try {
			MultipartEntity multiPartEntity = new MultipartEntity ();
			multiPartEntity.addPart(USER_NAME, new StringBody(userName));
			multiPartEntity.addPart(PASSWORD, new StringBody(passWord));
			multiPartEntity.addPart(COMMAND, new StringBody(CMD_UPLOAD_FILE));

			String relPath = absPath.substring(homeDir.length());
			//replace "\" to "/"
			multiPartEntity.addPart(FILE_NAME , new StringBody(relPath.replace('\\','/')));

			File file = new File(absPath);
			FileBody fileBody = new FileBody(file, "application/octect-stream") ;
			multiPartEntity.addPart("attachment", fileBody) ;

			HttpPost postRequest = new HttpPost (serverURL);
			postRequest.setEntity(multiPartEntity);

			res =  executeRequest(postRequest);
			System.out.print(res);
			
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			res = FAILURE_EC;
		}		
		return res;
	}

	private String downLoadFile(String filePath, String s3URL){
		String ec = SUCCESS_EC;
		HttpClient httpClient = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(s3URL);
		try{
			HttpResponse response = httpClient.execute(httpGet);
			if(response != null){
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					int pos = filePath.lastIndexOf('\\');
					File dir = new File(filePath.substring(0, pos + 1));
					if(!dir.exists()){
						dir.mkdirs();
					}
					FileOutputStream fos = new FileOutputStream(filePath);
					entity.writeTo(fos);
					fos.close();
				}
			}
		} catch(Exception e){
			e.printStackTrace();
			ec = FAILURE_EC;
		}
		return ec;
	}

	private void deleteFile(String fileKey){
		MultipartEntity multiPartEntity = new MultipartEntity () ;
		try {
			multiPartEntity.addPart(USER_NAME, new StringBody(userName));
			multiPartEntity.addPart(PASSWORD, new StringBody(passWord));
			multiPartEntity.addPart(COMMAND, new StringBody(CMD_DELETE_FILE));
			multiPartEntity.addPart(FILES, new StringBody(fileKey));

			HttpPost postRequest = new HttpPost (serverURL);
			postRequest.setEntity(multiPartEntity);

			String res =  executeRequest (postRequest);
			System.out.print(res);

			//further process response
			String[] responses = res.trim().split(",");
			if(!responses[0].trim().equals(SUCCESS_EC)){
				System.err.println(res);
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	private static String executeRequest(HttpRequestBase requestBase){        
		String responseString = "" ;
		InputStream responseStream = null ;
		HttpClient client = new DefaultHttpClient () ;

		try{
			HttpResponse response = client.execute(requestBase) ;
			if (response != null){
				HttpEntity responseEntity = response.getEntity() ;
				if (responseEntity != null){
					responseStream = responseEntity.getContent() ;
					if (responseStream != null){
						BufferedReader br = new BufferedReader (new InputStreamReader (responseStream)) ;
						String responseLine = br.readLine() ;
						String tempResponseString = "" ;
						while (responseLine != null){
							tempResponseString = tempResponseString + responseLine + System.getProperty("line.separator") ;
							responseLine = br.readLine() ;
						}
						br.close() ;
						if (tempResponseString.length() > 0){
							responseString = tempResponseString ;
						}
					}
				}
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			if (responseStream != null){
				try {
					responseStream.close() ;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		client.getConnectionManager().shutdown() ;
		return responseString ;
	}


	private void checkAndUpdate(){
		bookKeepingLock.lock();
		try{
			//get updated info from server
			Map<String,String> remoteVersionMap = getFileVersions("");
			Map<String,String> urlMap = getFileURLs("");

			Set<String> fileSet = new HashSet<String>();
			fileSet.addAll(versionTable.keySet());
			fileSet.addAll(remoteVersionMap.keySet());

			for(String fn : fileSet){
				if(versionTable.containsKey(fn) && remoteVersionMap.containsKey(fn)){
					//file exists both locally and remotely
					FileInfo fileInfo = versionTable.get(fn);
					if(fileInfo.version.equals(remoteVersionMap.get(fn))){
						if(fileInfo.isModified){
							//file is updated locally, and needs to be updated to server
							String res = uploadFile(fileInfo.absolutePath);
							String[] responses = res.trim().split(",");
							if(responses[0].trim().equals(SUCCESS_EC)){
								fileInfo.version = responses[1];
								fileInfo.isModified = false;
								writeBookKeepingMap();
							} else {
								System.err.println(res);
							}
						}
					} else {
						if(fileInfo.isModified){
							//file is updated both remotely and locally. 
							//Conflicts needs to be solved manually
							//we could provide alerts to user, asking him/her to solve it, and do the synchronization.
							System.err.println("Conflicts: updated both remotely and locally for file:" + fn);
						} else {
							//file is updated remotely, and needs to be download to client
							String res = downLoadFile(fileInfo.absolutePath, urlMap.get(fn));
							if(res.equals(SUCCESS_EC)){
								fileInfo.version = remoteVersionMap.get(fn);
								fileInfo.isModifiedByUser = false;
								fileInfo.localLastModifiedTime = new File(fileInfo.absolutePath).lastModified();
								writeBookKeepingMap();
							}   					   
						}
					}
				} else if(versionTable.containsKey(fn) && !remoteVersionMap.containsKey(fn)){
					//file exists only locally
					FileInfo fileInfo = versionTable.get(fn);
					if(fileInfo.isModified){
						//this is a new file, and needs to be uploaded to server
						String res = uploadFile(fileInfo.absolutePath);
						String[] responses = res.trim().split(",");
						if(responses[0].trim().equals(SUCCESS_EC)){
							fileInfo.version = responses[1];
							fileInfo.isModified = false;
							writeBookKeepingMap();
						} else {
							System.err.println(res);
						}
					} else {
						//this file has been deleted from sever, and needs to be deleted on local machine too.
						File file = new File(fileInfo.absolutePath);
						file.delete();
						versionTable.remove(fn);
						writeBookKeepingMap();
					}
				} else if(!versionTable.containsKey(fn) && remoteVersionMap.containsKey(fn)){
					//file exists only remotely
					//just download the file from the server
					String absPath = homeDir + fn.replace('/', '\\');
					String res = downLoadFile(absPath, urlMap.get(fn));
					if(res.equals(SUCCESS_EC)){
						FileInfo fileInfo = new FileInfo();
						fileInfo.absolutePath = absPath;
						fileInfo.isModified = false;
						fileInfo.isModifiedByUser = false;
						fileInfo.localLastModifiedTime = new File(absPath).lastModified();
						fileInfo.version = remoteVersionMap.get(fn);
						versionTable.put(fn, fileInfo);
						writeBookKeepingMap();
					}
				}
			}
		} finally {
			bookKeepingLock.unlock();
		}
	}

	/*
	 * Populates the local bookkeeping table
	 */
	private void populateBookKeepingMap() throws IOException, ClassNotFoundException {
		File file = new File(LOCAL_BOOKKEEPING_FILE);
		if(file.exists())
		{
			FileInputStream fileIn = new FileInputStream(file);
			ObjectInputStream oin = new ObjectInputStream(fileIn);
			versionTable = (Map<String,FileInfo>)oin.readObject();
			oin.close();
		} else {
			versionTable = new HashMap<String,FileInfo>();
		}
	}

	/*
	 * Writes the HashMap to a log
	 */
	private void writeBookKeepingMap(){
		try{
			FileOutputStream fileOut = new FileOutputStream(LOCAL_BOOKKEEPING_FILE);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(versionTable);
			out.close();
		} catch(Exception e){
			e.printStackTrace();
		}
	}

	private String readUserCredentials() throws IOException, ClassNotFoundException{
		// The initial traversal
		File file = new File(USER_CREDENTIAL_FILE);
		if(file.exists()) {
			FileInputStream fileIn = new FileInputStream(file);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			Map<String,String> userInfo = (Map<String,String>)in.readObject();
			in.close();
			userName = userInfo.get(USER_NAME);
			passWord = userInfo.get(PASSWORD);
			return SUCCESS_EC;
		} else {
			userName = "";
			passWord = "";
			return FAILURE_EC;
		}
	}

	private void saveUserCredentials(){
		try{
			FileOutputStream fileOut = new FileOutputStream(USER_CREDENTIAL_FILE);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			Map<String,String> userInfo = new HashMap<String,String>();
			userInfo.put(USER_NAME, userName);
			userInfo.put(PASSWORD, passWord);
			out.writeObject(userInfo);
			out.close();
		} catch(Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * traverse the local MyDrive folder
	 * check if anything changed when the program is not running
	 *
	 * @param myDrive
	 * @param keys: VersionTable keys, it is used to check if any
	 * file is deleted when the program is not run
	 */
	private void traverseMyDrive(File myDrive, Set<String> keys) {
		bookKeepingLock.lock();
		try{			
			// Traverses all files in the folder
			for( File entry : myDrive.listFiles()){
				if(entry.isFile()){
					String absPath = entry.getAbsolutePath();
					String s3Key = absPath.substring(homeDir.length()).replace('\\', '/');			

					if(!versionTable.containsKey(s3Key)){
						//new file added when our program is not running
						FileInfo fi = new FileInfo();
						fi.absolutePath = absPath;
						fi.isModified = true;
						fi.isModifiedByUser = true;
						fi.version = "";
						fi.localLastModifiedTime =entry.lastModified();
						versionTable.put(s3Key,fi);
						writeBookKeepingMap();
					} else {
						FileInfo fi = versionTable.get(s3Key);
						if(fi.localLastModifiedTime != entry.lastModified()){
							//file modified when our program is not running
							fi.localLastModifiedTime = entry.lastModified();
							fi.isModified = true;
							fi.isModifiedByUser = true;
							writeBookKeepingMap();
						}
						keys.remove(s3Key);
					}
				} else {
					// Recursive call to traverse
					traverseMyDrive(entry, keys);
				}
			}
		} finally {
			bookKeepingLock.unlock();
		}
		
	}

	/**
	 * get s3 versions for files
	 * @param files: s3Keys separated by comma. Use empty or null string for all files
	 */
	private Map<String,String> getFileVersions(String files){
		Map<String,String> versionMap = new HashMap<String,String>();
		try {
			MultipartEntity multiPartEntity = new MultipartEntity () ;
			multiPartEntity.addPart(USER_NAME, new StringBody(userName));
			multiPartEntity.addPart(PASSWORD, new StringBody(passWord));
			multiPartEntity.addPart(COMMAND, new StringBody(CMD_GET_VERSION));
			multiPartEntity.addPart(FILES, new StringBody(files));

			HttpPost postRequest = new HttpPost (serverURL);
			postRequest.setEntity(multiPartEntity);

			String res =  executeRequest(postRequest);
			System.out.println(res);

			//further process response
			String[] responses = res.trim().split(",");
			if(responses[0].trim().equals(SUCCESS_EC)){
				for(int i=1;i<responses.length;i++){
					String[] pair = responses[i].split("::");
					versionMap.put(pair[0], pair[1]);
				}
			} else {
				System.err.println(res);
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return versionMap;
	}

	/**
	 * get s3 url for files
	 * @param files: s3Keys separated by comma. Use empty or null string for all files
	 */
	private Map<String,String> getFileURLs(String files){
		Map<String,String> urlMap = new HashMap<String,String>();
		try {
			MultipartEntity multiPartEntity = new MultipartEntity() ;
			multiPartEntity.addPart(USER_NAME, new StringBody(userName));
			multiPartEntity.addPart(PASSWORD, new StringBody(passWord));
			multiPartEntity.addPart(COMMAND, new StringBody(CMD_GET_URL));
			multiPartEntity.addPart(FILES, new StringBody(files));

			HttpPost postRequest = new HttpPost (serverURL);
			postRequest.setEntity(multiPartEntity);

			String res =  executeRequest(postRequest);
			System.out.println(res);

			//further process response
			String[] responses = res.trim().split(",");
			if(responses[0].trim().equals(SUCCESS_EC)){
				for(int i=1;i<responses.length;i++){
					String[] pair = responses[i].split("::");
					urlMap.put(pair[0], pair[1]);
				}
			} else {
				System.err.println(res);
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return urlMap;
	}

}
