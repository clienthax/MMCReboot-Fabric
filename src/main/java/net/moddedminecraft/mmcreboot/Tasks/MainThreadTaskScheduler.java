package net.moddedminecraft.mmcreboot.Tasks;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

public class MainThreadTaskScheduler {
    private static final Queue<Task> scheduledTasks = new ArrayDeque<>();

    // Inner class to represent a task with a delay
    private static class Task {
        private final Runnable runnable;
        private int ticksRemaining;

        public Task(Runnable runnable, int delayInTicks) {
            this.runnable = runnable;
            this.ticksRemaining = delayInTicks;
        }

        public boolean tick() {
            if (--ticksRemaining <= 0) {
                runnable.run();
                return true; // Task is done
            }
            return false;
        }
    }

    // Schedule a task to run after a delay (in seconds)
    public static void scheduleTask(Runnable runnable, long delay, TimeUnit unit) {
        int delayInTicks = (int) (unit.toSeconds(delay) * 20); // Convert delay to ticks
        scheduledTasks.add(new Task(runnable, delayInTicks));
    }

    public static void init() {
        // Register a server tick event to run scheduled tasks
        ServerTickEvents.END_SERVER_TICK.register(server -> scheduledTasks.removeIf(Task::tick));
    }
}