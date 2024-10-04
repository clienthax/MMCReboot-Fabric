package net.moddedminecraft.mmcreboot.mixin;

import net.minecraft.server.MinecraftServer;
import net.moddedminecraft.mmcreboot.Tasks.ScheduledTaskManager;
import net.moddedminecraft.mmcreboot.Tasks.ServerTickMixinAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ConcurrentModificationException;

@Mixin(MinecraftServer.class)
public class ServerTickMixin implements ServerTickMixinAccessor {

    private final ScheduledTaskManager taskManager = new ScheduledTaskManager();

    @Inject(method = "tick", at = @At("TAIL"))
    public void onServerTick(CallbackInfo ci) {
        taskManager.tick();  // Run the task manager on each tick
    }

    public ScheduledTaskManager getTaskManager() {
        return taskManager;
    }
}
