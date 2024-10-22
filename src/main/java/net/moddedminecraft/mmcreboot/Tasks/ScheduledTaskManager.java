package net.moddedminecraft.mmcreboot.Tasks;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public class ScheduledTaskManager {

    private final Map<Runnable, Long> tasks = new ConcurrentHashMap<>(); // Maps tasks to their period (in ms)
    private final Map<Runnable, Long> nextExecutionTimes = new ConcurrentHashMap<>(); // Maps tasks to the next execution timestamp
    private final List<Runnable> tasksToRemove = new CopyOnWriteArrayList<>();
    private boolean clearAllRequested = false;

    // Schedule a repeating task based on real time
    public void scheduleRepeatingTask(Runnable task, double delay, double period, TimeUnit unit) {
        long delayInMillis = convertToMillis(delay, unit);
        long periodInMillis = convertToMillis(period, unit);
        long nextExecutionTime = System.currentTimeMillis() + delayInMillis;

        tasks.put(task, periodInMillis);
        nextExecutionTimes.put(task, nextExecutionTime);
    }

    // Schedule a single execution task based on real time
    public void scheduleSingleTask(Runnable task, double delay, TimeUnit unit) {
        long delayInMillis = convertToMillis(delay, unit);
        long nextExecutionTime = System.currentTimeMillis() + delayInMillis;

        tasks.put(task, 0L); // 0 period means it runs only once
        nextExecutionTimes.put(task, nextExecutionTime);
    }

    // Called periodically (e.g., once per tick) to check and run tasks
    public void tick() {
        if (clearAllRequested) {
            tasks.clear();
            nextExecutionTimes.clear();
            clearAllRequested = false;
            return;
        }

        long currentTime = System.currentTimeMillis();

        tasks.forEach((task, period) -> {
            long nextExecutionTime = nextExecutionTimes.get(task);
            if (currentTime >= nextExecutionTime) {
                task.run();  // Execute the task

                if (period > 0) {
                    nextExecutionTimes.put(task, currentTime + period);  // Schedule the next execution
                } else {
                    tasksToRemove.add(task);  // Mark non-repeating tasks for removal
                }
            }
        });

        // Remove completed tasks
        for (Runnable task : tasksToRemove) {
            tasks.remove(task);
            nextExecutionTimes.remove(task);
        }
        tasksToRemove.clear();
    }

    public void clearAllTasks() {
        clearAllRequested = true;
    }

    // Convert various time units to milliseconds
    public long convertToMillis(double time, TimeUnit unit) {
        return switch (unit) {
            case HOURS -> (long) (time * 3600000);
            case MINUTES -> (long) (time * 60000);
            case SECONDS -> (long) (time * 1000);
            case MILLISECONDS -> (long) time;
            case MICROSECONDS -> (long) (time / 1000);
            case NANOSECONDS -> (long) (time / 1_000_000);
            default -> throw new IllegalArgumentException("Unsupported TimeUnit: " + unit);
        };
    }

    public void removeTasks(List<Runnable> rebootTimerTasks) {
        tasksToRemove.addAll(rebootTimerTasks);
    }
}
