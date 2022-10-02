package su.plo.lib.server.command;

import com.mojang.brigadier.CommandDispatcher;
import lombok.RequiredArgsConstructor;
import net.minecraft.commands.CommandSourceStack;
import org.jetbrains.annotations.NotNull;
import su.plo.lib.server.MinecraftServerLib;

@RequiredArgsConstructor
public final class ModCommandManager extends MinecraftCommandManager {

    private final MinecraftServerLib minecraftServer;

    private boolean registered;

    @Override
    public synchronized void register(@NotNull String name, @NotNull MinecraftCommand command, String... aliases) {
        if (registered) throw new IllegalStateException("register after commands registration is not supported");

        super.register(name, command, aliases);
    }

    @Override
    public synchronized boolean unregister(@NotNull String name) {
        throw new IllegalStateException("unregister is not supported");
    }

    @Override
    public synchronized void clear() {
        super.clear();
        this.registered = false;
    }

    public synchronized void registerCommands(@NotNull CommandDispatcher<CommandSourceStack> dispatcher) {
        commandByName.forEach((name, command) -> {
            ModCommand modCommand = new ModCommand(minecraftServer, command);
            modCommand.register(dispatcher, name);
        });
        this.registered = true;
    }
}
