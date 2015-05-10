package homecontrolclient;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class HomeControl 
{
	private static final String HCC_VERSION = "1.0";
	private static final String HCC_COPYRIGHT = "\u00A92015 John W. O'Neill";	
	private static final String APPNAME = "Home Control";
	private static final boolean DEBUG_MODE = false;
	
	//GUI Objects
	private JFrame hccFrame;
	private JPanel hccContentPane, hccSplashPanel;
	
	//Server Connection
	private ServerIF serverIF;	
//	private static String defaultServerAddress = "72.209.192.152";	//Cox based server
	private static String defaultServerAddress = "192.168.1.102";	//local server
//	private static String defaultServerAddress = "localhost";
	private static final int PORT = 8902;
	
	//Check if we are on Mac OS X.  This is crucial to loading and using the OSXAdapter class.
    private static boolean MAC_OS_X = (System.getProperty("os.name").toLowerCase().startsWith("mac os x"));
    
    
    public HomeControl()
    {
    	//If running under MAC OSX, use the system menu bar and set the application title appropriately and
    	//set up our application to respond to the Mac OS X application menu
        if (MAC_OS_X) 
        {          	
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", APPNAME);
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();}
			catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(); }
			catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(); }
			catch (UnsupportedLookAndFeelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(); }
			
            // Generic registration with the Mac OS X application, attempts to register with the Apple EAWT
            // See OSXAdapter.java to see how this is done without directly referencing any Apple APIs
            try
            {
                // Generate and register the OSXAdapter, passing it a hash of all the methods we wish to
                // use as delegates for various com.apple.eawt.ApplicationListener methods
                OSXAdapter.setQuitHandler(this, getClass().getDeclaredMethod("quit", (Class[])null));
                OSXAdapter.setAboutHandler(this,getClass().getDeclaredMethod("about", (Class[])null));
                OSXAdapter.setPreferencesHandler(this, getClass().getDeclaredMethod("preferences", (Class[])null));
 //             OSXAdapter.setFileHandler(this, getClass().getDeclaredMethod("loadImageFile", new Class[] { String.class }));
            } 
            catch (Exception e)
            {
                System.err.println("Error while loading the OSXAdapter:");
                e.printStackTrace();
            }
        }
       
        //Create and show mainframe with splash panel
        createMainFrame();
        
        //Setup networking with the ONC Server
        serverIF = null;
        int retrycount = 0;
        
        //get the last server address saved in the ipaddressfile.txt located in the same folder as the
        //client application .jar or .app
        String serverIPAddress = defaultServerAddress;
        //connect to server
        while(retrycount < 3)
        {
        	try 
        	{
        		serverIF = new ServerIF(serverIPAddress, PORT);
        		break;
        	} 
        	catch (SocketTimeoutException e2) 
        	{
        		System.out.println("SocketTimeoutException");
//        		serverIPAddress = getServerIPAddress(serverIPAddress);
        		if(serverIPAddress == null)
        			break;
        		else
        			retrycount++;
        	} 
        	catch (UnknownHostException e2) 
        	{
//        		serverIPAddress = getServerIPAddress(serverIPAddress);
        		if(serverIPAddress == null)
        			break;
        		else
        			retrycount++;
        	} 
        	catch (IOException e2) 
        	{
 //       		serverIPAddress = getServerIPAddress(serverIPAddress);
        		if(serverIPAddress == null)
        			break;
        		else
        			retrycount++;
        	}
		}
        
        //if the server if is not connected, notify and exit
        if(serverIF == null)
        {
        	JOptionPane.showMessageDialog(hccFrame, "Server connection not established, please contact " +
        			"the IT director", "Home Control Server Connecton Error", JOptionPane.ERROR_MESSAGE);
        	System.exit(0);
        }
        
        //Get and authenticate user and privileges with Authentication dialog. Can't get past this
        //modal dialog unless a valid user id and password is authenticated by the server. 
        AuthenticationDialog authDlg = new AuthenticationDialog(hccFrame, DEBUG_MODE);	
		authDlg.setVisible(true);
		
		//if we get here, the server has authenticated this client's userID and password
		//create the garage door status dialog and show it
		GarageDoorStatusDialog gdsDlg = new GarageDoorStatusDialog(hccFrame);
		Point gdsLoc = hccFrame.getLocation();
		gdsLoc.x += 400;
		gdsLoc.y += 300;
		gdsDlg.setLocation(gdsLoc);
		
		gdsDlg.setVisible(true);
		
		serverIF.setEnabledServerPolling(true);
    }
	
	  // General quit handler; fed to the OSXAdapter as the method to call when a system quit event occurs
    // A quit event is triggered by Cmd-Q, selecting Quit from the application or Dock menu, or logging out
    public boolean quit()
    {	
    	if(serverIF != null && serverIF.isConnected())
    	{
			serverIF.sendRequest("LOGOUT");
    		serverIF.close();
    	}
    	
    	return true;
    }
    
    // General info dialog; fed to the OSXAdapter as the method to call when 
    // "About OSXAdapter" is selected from the application menu   
    public void about()
    {
    	//User has chosen to view the About ONC dialog
		String versionMsg = String.format("Home Control Client Version %s\n%s", 
											HCC_VERSION,HCC_COPYRIGHT);
//		JOptionPane.showMessageDialog(oncFrame, versionMsg, "About the ONC App", 
//										JOptionPane.INFORMATION_MESSAGE,oncGVs.getImageIcon(0));
    }

    // General preferences dialog; fed to the OSXAdapter as the method to call when
    // "Preferences..." is selected from the application menu
    public void preferences()
    {
//    	prefsDlg.setLocation((int)oncFrame.getLocation().getX() + 22, (int)oncFrame.getLocation().getY() + 22);
//    	prefsDlg.display();
//      prefsDlg.setVisible(true);   	
    }
    
    void exit(String command)
    {
    	if(serverIF != null && serverIF.isConnected())
    	{
			serverIF.sendRequest("LOGOUT");
    		serverIF.close();
    	}
    	
    	System.exit(0);
    }
    
    private void createMainFrame()
    {
    	hccFrame = new JFrame(APPNAME);
		hccFrame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we)
			 {
				exit("QUIT");			  
			 }});
        hccFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);	//On close, user is prompted to confirm
        hccFrame.setMinimumSize(new Dimension(832, 550));
        hccFrame.setLocationByPlatform(true);
        
        //Create a content panel for the frame and add components to it.
        hccContentPane = new JPanel();
        hccContentPane.setLayout(new BoxLayout(hccContentPane, BoxLayout.PAGE_AXIS));
          
        // set up a splash screen panel
      	hccSplashPanel = new JPanel();        
      	JLabel lblONCicon = new JLabel(createImageIcon("HomeControl.png", "Full Screen Logo"));
      	hccSplashPanel.add(lblONCicon);	 
        hccContentPane.add(hccSplashPanel);
        
        hccFrame.setContentPane(hccContentPane); 
        hccFrame.setVisible(true);
    }
    
    /** Returns an ImageIcon, or null if the path was invalid. */
   	ImageIcon createImageIcon(String path, String description)
   	{
   		java.net.URL imgURL = getClass().getResource(path);
   		if (imgURL != null) { return new ImageIcon(imgURL, description); } 
   		else { System.err.println("Couldn't find file: " + path); return null; }
   	}
    
    public static void main(String args[])
	 {
		 SwingUtilities.invokeLater(new Runnable() {
	            public void run() { new HomeControl(); }
	     });
	 }	    
}//End of Class
