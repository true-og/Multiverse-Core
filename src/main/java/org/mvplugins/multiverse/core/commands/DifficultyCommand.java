package org.mvplugins.multiverse.core.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Flags;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import jakarta.inject.Inject;
import org.bukkit.Difficulty;
import org.jetbrains.annotations.NotNull;
import org.jvnet.hk2.annotations.Service;

import org.mvplugins.multiverse.core.commandtools.MVCommandIssuer;
import org.mvplugins.multiverse.core.commandtools.MVCommandManager;
import org.mvplugins.multiverse.core.commandtools.MultiverseCommand;
import org.mvplugins.multiverse.core.world.LoadedMultiverseWorld;

@Service
@CommandAlias("mv")
class DifficultyCommand extends MultiverseCommand {

    @Inject
    DifficultyCommand(@NotNull MVCommandManager commandManager) {
        super(commandManager);
    }

    @Subcommand("difficulty")
    @CommandPermission("multiverse.core.difficulty")
    @CommandCompletion("@difficulty @mvworlds:scope=both")
    @Syntax("<difficulty> [world]")
    void onDifficultyCommand(
            MVCommandIssuer issuer,

            @Syntax("<difficulty>")
            @Description("")
            Difficulty difficulty,

            @Flags("resolve=issuerAware")
            @Syntax("[world]")
            @Description("")
            // TODO: Change to MultiverseWorld
            LoadedMultiverseWorld world) {
        world.setDifficulty(difficulty)
                .onSuccess(w -> issuer.sendInfo("Difficulty set to " + difficulty + " for world " + world.getName()))
                .onFailure(failure -> issuer.sendError(failure.getMessage()));
    }
}
