package homecontrolclient;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

import com.google.gson.Gson;


public class GarageDoorStatusDialog extends JDialog implements ActionListener, ServerListener
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final int DOOR_CLOSING_TIME = 1000 * 10 * 2;
	private JLabel lblLeftDoorOpen, lblRightDoorOpen;
	private JButton btnLeftDoor, btnRightDoor;
	private ServerIF serverIF;
	GarageDoor garageDoorStatus;
	private Timer statusTimer;
	private boolean bStatusValid;
	
	public GarageDoorStatusDialog(JFrame owner)
	{
		super(owner, "Garage Door Status");
		//Create the polling timer
    	statusTimer = new Timer(DOOR_CLOSING_TIME, this);
    	bStatusValid = true;
		
		serverIF = ServerIF.getInstance();
		if(serverIF != null)
		{
			serverIF.addServerListener(this);
			getGarageDoorStatus();
		}
		
		Container contentPane = this.getContentPane();
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
		
		JPanel leftDoorPanel = new JPanel();
		String lDoorStatus = garageDoorStatus.isLeftDoorOpen() ? "Open" : "Closed";
		lblLeftDoorOpen = new JLabel("Left Door: " + lDoorStatus);
		lDoorStatus = garageDoorStatus.isLeftDoorOpen() ? "Close" : "Open";
		btnLeftDoor = new JButton(lDoorStatus + " Left Door");
		btnLeftDoor.addActionListener(this);
		leftDoorPanel.add(lblLeftDoorOpen);
		leftDoorPanel.add(btnLeftDoor);

		JPanel rightDoorPanel = new JPanel();
		String rDoorStatus = garageDoorStatus.isRightDoorOpen() ? "Open" : "Closed";
		lblRightDoorOpen = new JLabel("Right Door: " + rDoorStatus);
		rDoorStatus = garageDoorStatus.isRightDoorOpen() ? "Close" : "Open";
		btnRightDoor = new JButton(rDoorStatus + " Right Door");
		btnRightDoor.addActionListener(this);
		rightDoorPanel.add(lblRightDoorOpen);
		rightDoorPanel.add(btnRightDoor);
		
		contentPane.add(leftDoorPanel);
		contentPane.add(rightDoorPanel);
		
		this.setPreferredSize(new Dimension(290, 100));
		pack();	
	}
	
	void getGarageDoorStatus()
	{
		Gson gson = new Gson();
		
		String response = serverIF.sendRequest("GET<garage_door_status>");
		if(response.startsWith("STATUS_GARAGE_DOOR"))
		{
			garageDoorStatus = gson.fromJson(response.substring(18), GarageDoor.class);
		}
	}
	
	void processDoorStatusUpdate(String doorJson)
	{	
		if(bStatusValid)
		{	
			Gson gson = new Gson();
			GarageDoor doorStatus = gson.fromJson(doorJson, GarageDoor.class);
			
//			System.out.println(String.format("processDoorStatusUpdate: Left Door: %b, Right door: %b",
//					doorStatus.isLeftDoorOpen(), doorStatus.isRightDoorOpen()));
		 
			String lDoorStatus = doorStatus.isLeftDoorOpen() ? "Open" : "Closed";
			lblLeftDoorOpen.setText("Left Door: " + lDoorStatus);
	
			lDoorStatus = doorStatus.isLeftDoorOpen() ? "Close" : "Open";
			btnLeftDoor.setText(lDoorStatus + " Left Door");
		
			garageDoorStatus.setLeftDoorOpen(doorStatus.isLeftDoorOpen());
		
			String rDoorStatus = doorStatus.isRightDoorOpen() ? "Open" : "Closed";
			lblRightDoorOpen.setText("Right Door: " + rDoorStatus);
	
			rDoorStatus = doorStatus.isRightDoorOpen() ? "Close" : "Open";
			btnRightDoor.setText(rDoorStatus + " Right Door");
		
			garageDoorStatus.setRightDoorOpen(doorStatus.isRightDoorOpen());
		
			btnLeftDoor.setEnabled(true);
			btnRightDoor.setEnabled(true);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == btnLeftDoor)
		{
			//set door status and send it to server
			garageDoorStatus.setLeftDoorOpen(!garageDoorStatus.isLeftDoorOpen());
			
			Gson gson = new Gson();
			String response = serverIF.sendRequest("POST<garage_door_status" + gson.toJson(garageDoorStatus));
			bStatusValid = false;
			statusTimer.start();
			
			if(response != null && response.equals("UPDATED_GARAGE_DOOR"))
			{
				lblLeftDoorOpen.setText("Left Door: ????");
				btnLeftDoor.setText("Door Moving");
				btnLeftDoor.setEnabled(false);
			}
			
		}
		else if(e.getSource() == btnRightDoor)
		{
			//set door status and send it to server
			garageDoorStatus.setRightDoorOpen(!garageDoorStatus.isRightDoorOpen());
			
			Gson gson = new Gson();
			String response = serverIF.sendRequest("POST<garage_door_status" + gson.toJson(garageDoorStatus));
			bStatusValid = false;
			statusTimer.start();
			
			if(response != null && response.equals("UPDATED_GARAGE_DOOR"))
			{
				lblRightDoorOpen.setText("Right Door: ????");
				btnRightDoor.setText("Door Moving");
				btnRightDoor.setEnabled(false);
			}
		}
		else if(e.getSource() == statusTimer)
		{
			statusTimer.stop();
			bStatusValid = true;
		}
	}

	@Override
	public void dataChanged(ServerEvent se) 
	{
		if(se.getType().equals("STATUS_GARAGE_DOOR"))
		{
			processDoorStatusUpdate(se.getJson());
		}
	}
}
