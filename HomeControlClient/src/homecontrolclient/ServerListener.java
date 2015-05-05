package homecontrolclient;

import java.util.EventListener;

public interface ServerListener extends EventListener 
{
	public void dataChanged(ServerEvent ue);
}

