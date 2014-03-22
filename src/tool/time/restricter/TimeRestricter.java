package tool.time.restricter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import objects.Personnage;


public class TimeRestricter {
	private RestrictLevel level;
	private int countAuthorized;
	private long interval, blockTime;
	private boolean activeErrorMessage;
	
	private Map<Integer, ClientInstance> players = new ConcurrentHashMap<>();
	
	public TimeRestricter(RestrictLevel level, int countAuthorizes, long interval, TimeUnit iUnit, long blockTime, TimeUnit bUnit) {
		this.level = level;
		this.countAuthorized = countAuthorizes;
		this.interval = TimeUnit.MILLISECONDS.convert(interval, iUnit);
		this.blockTime = TimeUnit.MILLISECONDS.convert(blockTime,bUnit);
	}
	
	public void activeErrorMessage() {
		this.activeErrorMessage = true;
	}
	
	public boolean authorizes(Personnage player) {
		ClientInstance instance = find(player);
		
		if (instance.isBlocked()) {
			return blocked(player, instance);
		} else {
			instance.addHit();
			
			if (instance.getLastHit() + this.interval >= System.currentTimeMillis()) {
				if (instance.getHits() <= this.countAuthorized)
					return true;
				else { 
					instance.block();
					return blocked(player, instance);
				}
			} else {
				instance.updateLastHit();
				instance.resetHits();
			}
			return true;
		}
	}
	
	private boolean blocked(Personnage player, ClientInstance instance) {
		if(activeErrorMessage) {
			long time = TimeUnit.MINUTES.convert(instance.getRemainingTime(), TimeUnit.MILLISECONDS) + 1;
			player.sendText("Vous devez encore attendre <b>"+time+"</b> minutes.");
		}
		return false;
	}
	
	private ClientInstance find(Personnage player) {
		ClientInstance result = players.get(player.get_GUID());
		switch(this.level) {
			case ACCOUNT:
				if(result == null) {
					for(ClientInstance instance: players.values()) {
						if(instance.getAccount() == player.get_compte().get_GUID()) {
							result = instance;
							break;
						}
					}
				}
				break;
			case IP:
				if(result == null) {
					for(ClientInstance instance: players.values()) {
						if(instance.getIp() == player.get_compte().get_curIP()) {
							result = instance;
							break;
						}
					}
				}
				break;
			case PLAYER:
				break;
		}
		if (result != null) return result;
		
		result = new ClientInstance(player, this);
		players.put(player.get_GUID(), result);
		return result;
	}
	
	public long getBlockTime() {
		return this.blockTime;
	}
}

class ClientInstance {
	private int player, account, hits;
	private String ip;
	private long lastHit;
	private long blockTime;
	private TimeRestricter restricter;
	
	public ClientInstance(Personnage player, TimeRestricter restricter) {
		this.player = player.get_GUID();
		this.account = player.get_compte().get_GUID();
		this.ip = player.get_compte().get_curIP();
		this.hits = 0;
		this.restricter = restricter;
	}
	
	public void addHit() {
		this.hits++;
	}
	
	public void resetHits() {
		this.hits = 0;
	}
	
	public void updateLastHit() {
		this.lastHit = System.currentTimeMillis();
	}

	public int getPlayer() {
		return this.player;
	}

	public long getLastHit() {
		return lastHit;
	}

	public int getHits() {
		return hits;
	}

	public long getBlockTime() {
		return blockTime;
	}

	public void setBlockTime(long blockTime) {
		this.blockTime = blockTime;
	}
	
	public int getAccount() {
		return this.account;
	}
	
	public String getIp() {
		return this.ip;
	}
	
	public void block() {
		resetHits();
		updateLastHit();
		this.blockTime = System.currentTimeMillis();
	}
	
	public boolean isBlocked() {
		long future = this.blockTime + this.restricter.getBlockTime();
		return System.currentTimeMillis() < future;
	}
	
	public long getRemainingTime() {
		long future = this.blockTime + this.restricter.getBlockTime();
		return future - System.currentTimeMillis();
	}
}