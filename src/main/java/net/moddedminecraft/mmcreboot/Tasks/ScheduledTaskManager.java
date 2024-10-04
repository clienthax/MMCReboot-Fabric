package net.moddedminecraft.mmcreboot.Tasks;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

// TODO add support for 'realtime' tasks that don't rely on ticks, or account for skipped ticks
public class ScheduledTaskManager {

    private final Map<Runnable, Long> tasks = new ConcurrentHashMap<>();
    private final Map<Runnable, Long> initialDelays = new ConcurrentHashMap<>();
    private final List<Runnable> tasksToRemove = new CopyOnWriteArrayList<>();
    private boolean clearAllRequested = false;

    // Add a task to be run after delay and then periodically
    public void scheduleRepeatingTask(Runnable task, double delay, double period, TimeUnit unit) {
        long delayInTicks = convertToTicks(delay, unit); // Convert delay to ticks
        long periodInTicks = convertToTicks(period, unit); // Convert period to ticks
        tasks.put(task, periodInTicks);
        initialDelays.put(task, delayInTicks);
    }

    // Add a task to be run only once after a delay
    public void scheduleSingleTask(Runnable task, double delay, TimeUnit unit) {
        long delayInTicks = convertToTicks(delay, unit); // Convert delay to ticks
        tasks.put(task, 0L);  // period 0 means run once
        initialDelays.put(task, delayInTicks);
    }

    // Called every tick to check and run tasks
    public void tick() {
        if (clearAllRequested) {
            tasks.clear();
            initialDelays.clear();
            clearAllRequested = false;
            return; // Skip the rest of the tick processing if tasks are being cleared
        }

        tasks.forEach((task, period) -> {
            long delay = initialDelays.get(task);
            if (delay <= 0) {
                task.run();  // Execute the task
                if (period > 0) {
                    initialDelays.put(task, period);  // Reset delay to period for the next execution
                } else {
                    tasksToRemove.add(task);  // Mark task for removal if it doesn't repeat
                }
            } else {
                initialDelays.put(task, delay - 1);  // Decrease the delay
            }
        });

        // Remove tasks that have completed and should not repeat
        for (Runnable task : tasksToRemove) {
            tasks.remove(task);
            initialDelays.remove(task);
        }
        tasksToRemove.clear();  // Clear the list of tasks to remove
    }

    public void clearAllTasks() {
        clearAllRequested = true;
    }

    // TimeUnit.toSeconds() is completely useless as it doesn't handle decimal values correctly
    public long convertToTicks(double delay, TimeUnit unit) {
        double delayInSeconds = switch (unit) {
            case HOURS -> delay * 3600;
            case MINUTES -> delay * 60;
            case SECONDS -> delay;
            case MILLISECONDS -> delay / 1000;
            case MICROSECONDS -> delay / 1_000_000;
            case NANOSECONDS -> delay / 1_000_000_000;
            default -> throw new IllegalArgumentException("Unsupported TimeUnit: " + unit);
        };

        return (long) (delayInSeconds * 20); // Convert seconds to ticks
    }

    public void removeTasks(List<Runnable> rebootTimerTasks) {
        tasksToRemove.addAll(rebootTimerTasks);
    }
}
