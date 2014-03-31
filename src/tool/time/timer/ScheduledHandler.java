package tool.time.timer;

import common.World;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class ScheduledHandler implements Runnable {
    //world scheduler
    ScheduledExecutorService scheduler = World.data.getScheduler();
    //future task
    private ScheduledFuture<?> scheduled;

    //simple schedule
    public ScheduledHandler(int time, TimeUnit unit) {
        this.scheduled = scheduler.schedule(this, time, unit);
    }

    //schedule with fixed delay
    public ScheduledHandler(long first, long second, TimeUnit unit) {
        this.scheduled = scheduler.scheduleWithFixedDelay(this, first, second, unit);
    }

    public void cancel(boolean b) {
        this.scheduled.cancel(b);
    }

    public abstract void run();

    private static void exemple() {
        //simple schedule
        new ScheduledHandler(5, TimeUnit.SECONDS) {
            public void run() {
                //you can cancel your task into
                cancel(true);
            }
        };

        //with fixed delay
        new ScheduledHandler(5, 5, TimeUnit.SECONDS) {
            public void run() {
                //you can cancel your task into
                cancel(true);
            }
        };
    }
}
