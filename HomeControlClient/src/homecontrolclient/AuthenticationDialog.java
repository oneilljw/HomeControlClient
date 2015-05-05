package homecontrolclient;

import java.awt.Cursor;

import javax.swing.JFrame;
import javax.swing.JPasswordField;

import com.google.gson.Gson;

public class AuthenticationDialog extends ConnectDialog
{
	/*************************************************************************************
	 * This class implements the authentication dialog for the ONC application. The 
	 * constructor attempts create a connection to the ONC Server. If the ONC Server 
	 * connection fails, the constructor substitutes a hard coded user list. The dialog
	 * subclasses the ONCConnectDialog class.
	 * 
	 * A modal login dialog is constructed that has a user id text field and a password
	 * text field using JPasswordField. If the user successfully enters a user id and password
	 * that is found on the server or the hard coded user list, the login is successful. 
	 * If the user is unable to complete the login, the dialog loops until that occurs or the 
	 * dialog is canceled. Upon cancel, the application exits.
	 * 
	 * The dialog has a debug_mode flag. When set, it fills the user id and password
	 * fields and doesn't try to read the external user file, using the hard coded user
	 * information instead to construct the users array list class member. 
	 * 
	 * The dialog listens for parent move events and relocates the dialog accordingly
	 * 
	 * The users.dat file contains a record for each user. Each record contains five fields:
	 * 	Field 0 - user id
	 * 	Field 1 - password
	 * 	Field 2 - user permission level: SUPERUSER, ADMIN, or GENERALUSER
	 * 	Field 3 - User First Name
	 * 	Field 4 - User Last Name
	 ************************************************************************************/
	private static final long serialVersionUID = 1L;
	
	private int count;	//Login attempts
	private JPasswordField passwdPF;
	public AuthenticationDialog(final JFrame parent, boolean debug_mode)
	{
		super(parent);
		this.setTitle("Our Neighbor's Child Login");

		//Initialize the class variables
       	count = 0;
       		
		//Layout GUI
       	lblMssg1.setText("<html><b><i>Welcome to Home Control</i></b><br></html>");
		lblMssg2.setText("<html>Working Offline, Please Login</html>");
		lblTF1.setText("User Name:");
		lblTF2.setText("Password:  ");
		passwdPF = new JPasswordField(12);
		p4.add(passwdPF);
		
		//To make login quicker for debug, pre-fill user id and password
		if(debug_mode)
		{
			tf1.setText("john");
			passwdPF.setText("erin1992");
		}
		
		btnAction.setText("Login");
		
		//Determine if connected the server or working off-line
        if(serverIF != null && serverIF.isConnected())
        	connectedToServer();
        else
        	notConnectedToServer();
	}
	
	void connectedToServer()
	{
		//Display login message from server
    	lblMssg2.setText("<html>"+ serverIF.getLoginMssg().substring(5) +"</html>");
    	this.setTitle("Connected to Home Control Server");
	}
	
	
	/******************************************************************************
	 * ONCConnectDialog base class calls this when the "Login" button is pressed
	 * When connected to ONC Server, send LOGIN command to server, wait for response
	 *****************************************************************************/
	void onServerAttempt()
	{
		String response = "";	
		Login loginReq = new Login(tf1.getText(), new String(passwdPF.getPassword()), "1.0");
		
		if(serverIF != null && serverIF.isConnected())
		{
			//change the cursor
			try 
			{
				Gson gson = new Gson();
				
	            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	            
	            //create the login request, including the user id and password json
//				response = serverIF.sendRequest("LOGIN_REQUEST{userid:\"" + uid + "\",password:\"" + pw + "\"}");
				response = serverIF.sendRequest("LOGIN_REQUEST" + gson.toJson(loginReq, Login.class));
				
				if (response.startsWith("INVALID"))	//Login unsuccessful, print retry message
				{
					String reason = response.substring(7);
					
					if(reason.startsWith("Downlevel"))
						lblMssg2.setText("<html><font color=red><b>" + reason + "</b></font></html>");
					else if(reason.startsWith("Inactive"))
						lblMssg2.setText("<html><font color=red><b>" + reason + ", please contact Exec Dir</b></font></html>");
					else
						lblMssg2.setText("<html><font color=red><b>" + reason + ", please try agian</b></font></html>");
					
//					lblMssg2.setText("<html><font color=red><b>" + reason + remedy + "</b></font></html>");
					btnAction.setText("Login Try " + Integer.toString(++count));
					passwdPF.requestFocus();
					passwdPF.selectAll();
				}
	            else if(response.startsWith("VALID"))	//Login successful, now get Userslist
	            {             	
	            	//Create the user object
//	            	Gson gson = new Gson();
	    			this.dispose();
	            }
	        } 
			finally 
			{
	            this.setCursor(Cursor.getDefaultCursor());
	        }	
		}
	}

	@Override
	void notConnectedToServer() {
		// TODO Auto-generated method stub
		
	}

	@Override
	void onLocalAttempt() {
		// TODO Auto-generated method stub
		
	}
}
