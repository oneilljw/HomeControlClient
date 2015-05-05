package homecontrolclient;

import java.util.EventObject;

public class ServerEvent extends EventObject 
{
	/**
	 * An event that represents changes received from the server by polling
	 */
	private static final long serialVersionUID = 1L;
	private String event_type;
	private String json;
	
	public ServerEvent(Object source, String event_type, String json) 
	{
		super(source);
		this.event_type = event_type;
		this.json = json;
	}
	
	String getType() { return event_type; }
	String getJson() { return json; }
}
