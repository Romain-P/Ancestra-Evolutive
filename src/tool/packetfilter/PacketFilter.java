package tool.packetfilter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PacketFilter {
	
	private int maxConnections, restrictedTime;
	private Map<String, IpInstance> ipInstances = new HashMap<>();
	private boolean safe;
	
	public PacketFilter(int maxConnections, int time, TimeUnit unit) {
		this.maxConnections = maxConnections;
		this.restrictedTime = (int)TimeUnit.MILLISECONDS.convert(time, unit);
	}
	
	public synchronized boolean safeCheck(String ip) {
		return unSafeCheck(ip);
	}
	
	public boolean unSafeCheck(String ip) {
		IpInstance ipInstance = find(ip);
		
		if (ipInstance.isBanned()) {
			return false;
		} else {
			ipInstance.addConnection();
			
			if (ipInstance.getLastConnection() + this.restrictedTime >= System.currentTimeMillis()) {
				if (ipInstance.getConnections() < this.maxConnections)
					return true;
				else {
					ipInstance.ban();
					return false;
				}
			} else {
				ipInstance.updateLastConnection();
				ipInstance.resetConnections();
			}
			return true;
		}
	}
	
	public boolean authorizes(String ip) {
		return safe ? safeCheck(ip) : unSafeCheck(ip);
	}
	
	public PacketFilter activeSafeMode() {
		this.safe = true;
		return this;
	}
	
	 private IpInstance find(String ip) {
	    ip = clearIp(ip);
	    
        IpInstance result = ipInstances.get(ip);
        if (result != null) return result;
	    
        result = new IpInstance(ip);
        ipInstances.put(ip, result);
        return result;
	 }
	
	private String clearIp(String ip) {
		return ip.contains(":")?ip.split(":")[0]:ip;
	}
}
