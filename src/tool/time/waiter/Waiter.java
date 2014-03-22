package tool.time.waiter;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

import common.World;

public class Waiter implements Runnable {
	private Deque<Instance> waitingList = new ConcurrentLinkedDeque<>();
	
	private void scheduleNextTask() {
		final Long time = waitingList.getFirst().getTime();
		
		World.data.getScheduler().schedule(this, time, TimeUnit.MILLISECONDS);
	}
	
	public void run() {
		Runnable toExecute = waitingList.pop().getRunnable();
		toExecute.run();
		
		if(!waitingList.isEmpty())
			scheduleNextTask();
	}
	
	public void addNext(Runnable run, long put, TimeUnit unit) {
		long time = TimeUnit.MILLISECONDS.convert(put, unit);
		this.waitingList.addLast((new Instance(run, Long.valueOf(time))));
		
		if(this.waitingList.size() == 1)
			scheduleNextTask();
	}
	
	public void addNext(Runnable run, long time) {
		addNext(run, time, TimeUnit.MILLISECONDS);
	}
}

class Instance {
	private Runnable runnable;
	private Long time;
	
	public Instance(Runnable runnable, Long time) {
		this.runnable = runnable;
		this.time = time;
	}

	public Runnable getRunnable() {
		return runnable;
	}

	public Long getTime() {
		return time;
	}
}
