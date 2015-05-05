package homecontrolclient;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import javax.swing.Timer;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class ServerIF
{
	/***
	 * Implements the server interface for the ONC client. The serverIF is a singleton that is
	 * instantiated once in each ONC Client instance. The class maintains a reference that is 
	 * accessed by the getInstance() method. 
	 */
	
	private static final int SOCKET_CREATION_TIMEOUT = 1000 * 3;	//Timeout delay if server doesn't respond, in milliseconds
	private static final int SOCKET_TRANSMISSION_TIMEOUT = 0;	//Timeout delay if server doesn't respond, in milliseconds
	private static final int NETWORK_TIME_LIMIT = 1000 * 10;	//Timeout delay if server doesn't respond, in milliseconds
	private static final int SERVER_LOG_TIME_INTERVAL = 1000 * 60 * 5;	//time interval between writing server logs
	private static final int NORMAL_POLLING_RATE = 1000 * 10;	//frequency of server polling, in milliseconds
//	private static final int ACTIVE_POLLING_RATE = 100;	//frequency of server polling, in milliseconds
	private static final int SERVER_LOG_LINE_LENGTH = 96;	//maximum number of characters in log line stored
	
	//Reference to the singleton object
	private static ServerIF instance;
	
	//Server Connection
    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
//  private PrintWriter out;
    private boolean bConnected;
    private String loginMssg;
    private Timer timer;
    private long timeCommandSent;
    private long timeLastLogWritten;
    private int nServerErrorsDetected;
    private ArrayList<String> serverLog;
    private boolean bLogFileA; 	//determines which log file (A or B) to write
    private boolean bDatabaseLoaded;	//true when the user has loaded local data base from the server
    
    //List of registered listeners for Sever data changed events
    private ArrayList<ServerListener> listeners;
    
    ServerIF(String serverAddress, int port) throws UnknownHostException, IOException, SocketTimeoutException
    {    	
    	bConnected = false;
    	timeCommandSent = 0;
    	timeLastLogWritten = System.currentTimeMillis();
    	nServerErrorsDetected = 0;
    	bLogFileA = true;
    	bDatabaseLoaded = false;
    	
    	socket = new Socket();
    	
    	timeCommandSent = System.currentTimeMillis();
//    	time = new Date(timeCommandSent);
    	
//    	System.out.println("ServerIF Connecting to Server at: " + timeCommandSent);
    	
    	socket.connect(new InetSocketAddress(serverAddress, port), SOCKET_CREATION_TIMEOUT);
    	socket.setSoTimeout ( SOCKET_TRANSMISSION_TIMEOUT );

        if(socket != null)
        {
        	try 
        	{
        		out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()),1); 
        	} 
        	catch (IOException e1)
        	{
        		// TODO Auto-generated catch block
        		e1.printStackTrace();
        	}
        
        	try 
        	{
        		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        		loginMssg = in.readLine();
 
        		if (loginMssg.startsWith("LOGIN"))
        			bConnected = true;
			
        	} 
        	catch (IOException e1) 
        	{
        		// TODO Auto-generated catch block
        		e1.printStackTrace();
        	}
        
        	//Create the polling timer
        	timer = new Timer(NORMAL_POLLING_RATE, new TimerListener());
        }
        
        serverLog = new ArrayList<String>();
      
        instance = this;
    }
    
    public synchronized static ServerIF getInstance() { return instance; }
    
    void setEnabledServerPolling(boolean tf)
    {
    	if(tf)
    		timer.start();
    	else
    		timer.stop();
    }
    
    synchronized String sendRequest(String request)
    {
    	timeCommandSent = System.currentTimeMillis();
    	
    	try {
			out.write(request);
			out.newLine();
	    	out.flush();
	    	
		} 
    	catch (IOException e1)
    	{
//    		GlobalVariables gvs = GlobalVariables.getInstance();
    		
	    	String mssg = String.format("Error sending command<br>%s<br> to ONC Server,<br>netwok connection may be lost." +
	    			"<br>Server errors detected: %d", request, nServerErrorsDetected); 			   		
	    	
//	    	ONCPopupMessage clientIDPU = new ONCPopupMessage(gvs.getImageIcon(0));
//			clientIDPU.setLocationRelativeTo(GlobalVariables.getFrame());
//			clientIDPU.show("ONC Server I/F Exception", mssg);
	    	
			e1.printStackTrace();
		}
    	
    	String response = null;
    		
    	try { 
    		response = in.readLine(); 
    	}	//Blocks until response received or timeout occurs
		catch (IOException e)
		{ 
		}
    	
    	//if the network response time is very slow, notify the user
    	long elapsedTime = System.currentTimeMillis() - timeCommandSent;
//    	System.out.println("Elapsed Time: " + elapsedTime);
    	
		if(elapsedTime > NETWORK_TIME_LIMIT && bDatabaseLoaded)	//Don't show pop-up until local data loaded
		{
//	    	GlobalVariables gvs = GlobalVariables.getInstance();
	    	String mssg = String.format("Server I/F is slow,<br>last transaction took %d seconds", 
	    									elapsedTime/1000);
	    	
//	    	ONCPopupMessage clientIDPU = new ONCPopupMessage(gvs.getImageIcon(0));
//			clientIDPU.setLocationRelativeTo(GlobalVariables.getFrame());
//			clientIDPU.show("ONC Server I/F Notification", mssg);
		}
		
    	if(response == null)
		{
//			GlobalVariables gvs = GlobalVariables.getInstance();
	    	String mssg = "Server did not respond,<br>netwok connection may be lost"; 			   		
	    	
//	    	ONCPopupMessage clientIDPU = new ONCPopupMessage(gvs.getImageIcon(0));
//			clientIDPU.setLocationRelativeTo(GlobalVariables.getFrame());
//			clientIDPU.show("ONC Server I/F Exception", mssg);
			
	    	response ="ERROR_SERVER_DID_NOT_RESPOND";
		}
    	
       	return response;
    }
    
    String getLoginMssg() { return loginMssg; }
    
    void close()
    {
    	timer.stop();

    	try {
			socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    ArrayList<String> getServerLog() { return serverLog; }
    
    boolean isConnected() { return bConnected; }
    
    void processChanges(String qContentJson)
    {
    	String[] serverResponses = {"UPDATED_GARAGE_DOOR"};
    	
    	Gson gson = new Gson();
    	Type listtype = new TypeToken<ArrayList<String>>(){}.getType();
    	ArrayList<String> changeList = gson.fromJson(qContentJson, listtype);
    	
    	//loop thru list of changes, processing each one
    	for(String change: changeList)
    	{
    		//all other change processing requires local data bases to be loaded from the server first
    		//otherwise we run the risk of updating data without a local copy present
    		int index = 0;
    		while(index < serverResponses.length && !change.startsWith(serverResponses[index]))
    			index++;
    	
    		if(index < serverResponses.length)
    		{
    			fireDataChanged(serverResponses[index], change.substring(serverResponses[index].length()));
    		}
    	}
    }
    
    void processGarageDoorStatus(String doorStatusJson)
    {
    	String[] doorStatusResponses = {"STATUS_GARAGE_DOOR"};
    	
    	if(doorStatusJson.startsWith(doorStatusResponses[0]))
    	{
    		fireDataChanged(doorStatusResponses[0], doorStatusJson.substring(doorStatusResponses[0].length()));
    	}
    }
    
    /** Register a listener for server DataChange events */
    synchronized public void addServerListener(ServerListener l)
    {
    	if (listeners == null)
    		listeners = new ArrayList<ServerListener>();
    	listeners.add(l);
    }  

    /** Remove a listener for server DataChange */
    synchronized public void removeServerListener(ServerListener l)
    {
    	if (listeners == null)
    		listeners = new ArrayList<ServerListener>();
    	listeners.remove(l);
    }
    
    /** Fire a Data ChangedEvent to all registered listeners */
    protected void fireDataChanged(String event_type, String json)
    {
    	// if we have no listeners, do nothing...
    	if (listeners != null && !listeners.isEmpty())
    	{
    		// create the event object to send
    		ServerEvent event = new ServerEvent(this, event_type, json);

    		// make a copy of the listener list in case anyone adds/removes listeners
    		ArrayList<ServerListener> targets;
    		synchronized (this) { targets = (ArrayList<ServerListener>) listeners.clone(); }

    		// walk through the cloned listener list and call the dataChanged method in each
    		for(ServerListener l:targets)
    			l.dataChanged(event);
    	}
    }
    
    private class TimerListener implements ActionListener
    {
		@Override
		public void actionPerformed(ActionEvent e) 
		{
			
			if(e.getSource() == timer)
			{
				//pause the timer so this EDT thread can complete. It should happen quickly, 
				//but not necessarily. Don't stack up timer events
				timer.stop();     	
        	
				String response = sendRequest("GET<garage_door_status>");
        	
				if(response == null || response.startsWith("ERROR"))
				{
					//Server communication error occurred, deal with heart beat issues here
				}
				else
				{
					processGarageDoorStatus(response);
				}
				
				timer.start();
			}
        }
    }
}
