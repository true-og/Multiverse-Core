package org.mvplugins.multiverse.core.commands;

import static org.mvplugins.multiverse.core.utils.file.FileUtils.getBukkitConfig;
import static org.mvplugins.multiverse.core.utils.file.FileUtils.getServerProperties;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jvnet.hk2.annotations.Service;
import org.mvplugins.multiverse.core.MultiverseCore;
import org.mvplugins.multiverse.core.commandtools.MVCommandManager;
import org.mvplugins.multiverse.core.commandtools.flags.CommandFlag;
import org.mvplugins.multiverse.core.commandtools.flags.CommandValueFlag;
import org.mvplugins.multiverse.core.commandtools.flags.ParsedCommandFlags;
import org.mvplugins.multiverse.core.event.MVVersionEvent;
import org.mvplugins.multiverse.core.utils.MVCorei18n;
import org.mvplugins.multiverse.core.utils.webpaste.PasteFailedException;
import org.mvplugins.multiverse.core.utils.webpaste.PasteService;
import org.mvplugins.multiverse.core.utils.webpaste.PasteServiceFactory;
import org.mvplugins.multiverse.core.utils.webpaste.PasteServiceType;
import org.mvplugins.multiverse.core.world.WorldManager;

import com.dumptruckman.minecraft.util.Logging;

import co.aikar.commands.CommandIssuer;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import jakarta.inject.Inject;

@Service
@CommandAlias("mv")
class DumpsCommand extends CoreCommand {

    private final MultiverseCore plugin;
    private final WorldManager worldManager;

    private final CommandValueFlag<LogsTypeOption> LOGS_FLAG = flag(CommandValueFlag
            .enumBuilder("--logs", LogsTypeOption.class)
            .addAlias("-l")
            .build());

    private final CommandValueFlag<ServiceTypeOption> UPLOAD_FLAG = flag(CommandValueFlag
            .enumBuilder("--upload", ServiceTypeOption.class)
            .addAlias("-u")
            .build());

    // Does not upload logs or plugin list (except if --logs mclogs is there)
    private final CommandFlag PARANOID_FLAG = flag(CommandFlag.builder("--paranoid")
            .addAlias("-p")
            .build());

    @Inject
    DumpsCommand(@NotNull MVCommandManager commandManager,
                        @NotNull MultiverseCore plugin,
                        @NotNull WorldManager worldManager) {
        super(commandManager);
        this.plugin = plugin;
        this.worldManager = worldManager;
    }

    private enum ServiceTypeOption {
        PASTEGG,
        PASTESDEV
    }

    private enum LogsTypeOption {
        APPEND,
        MCLOGS
    }

    @Subcommand("dumps")
    @CommandPermission("multiverse.core.dumps")
    @CommandCompletion("@flags:groupName=mvdumpscommand")
    @Syntax("[--logs <mclogs | append>] [--upload <pastesdev | pastegg>] [--paranoid]")
    @Description("{@@mv-core.dumps.description}")
    void onDumpsCommand(
            CommandIssuer issuer,

            @Optional
            @Syntax("[--logs <mclogs | append>] [--upload <pastesdev | pastegg>] [--paranoid]")
            String[] flags) {
        final ParsedCommandFlags parsedFlags = parseFlags(flags);

        // Grab all our flags
        final boolean paranoid = parsedFlags.hasFlag(PARANOID_FLAG);
        final LogsTypeOption logsType = parsedFlags.flagValue(LOGS_FLAG, LogsTypeOption.MCLOGS);
        final ServiceTypeOption servicesType = parsedFlags.flagValue(UPLOAD_FLAG, ServiceTypeOption.PASTEGG);

        // Initialise and add info to the debug event
        MVVersionEvent versionEvent = new MVVersionEvent();
        this.addDebugInfoToEvent(versionEvent);
        plugin.getServer().getPluginManager().callEvent(versionEvent);

        // Add plugin list if user isn't paranoid
        if (!paranoid) {
            versionEvent.putDetailedVersionInfo("plugins.md", "# Plugins\n\n" + getPluginList());
        }

        BukkitRunnable logPoster = new BukkitRunnable() {
            @Override
            public void run() {
                // TODO: Refactor into smaller methods
                Logging.finer("Logs type is: " + logsType);
                Logging.finer("Services is: " + servicesType);

                // Deal with logs flag
                if (!paranoid) {
                    switch (logsType) {
                        case MCLOGS -> issuer.sendInfo(MVCorei18n.DUMPS_URL_LIST,
                                "{service}", "Logs",
                                "{link}", postToService(PasteServiceType.MCLOGS, true, getLogs(), null));
                        case APPEND -> versionEvent.putDetailedVersionInfo("latest.log", getLogs());
                    }
                }

                // Get the files from the event
                final Map<String, String> files = versionEvent.getDetailedVersionInfo();

                // Deal with uploading debug info
                switch (servicesType) {
                    case PASTEGG -> issuer.sendInfo(MVCorei18n.DUMPS_URL_LIST,
                            "{service}", "paste.gg",
                            "{link}", postToService(PasteServiceType.PASTEGG, true, null, files));
                    case PASTESDEV -> issuer.sendInfo(MVCorei18n.DUMPS_URL_LIST,
                            "{service}", "pastes.dev",
                            "{link}", postToService(PasteServiceType.PASTESDEV, true, null, files));
                }

            }
        };

        // Run the uploader async as it could take some time to upload the debug info
        logPoster.runTaskAsynchronously(plugin);
    }

    /**
     * Get the contents of the latest.log file
     *
     * @return A string containing the latest.log file
     */
    private String getLogs() {
        // Get the Path of latest.log
        Path logsPath = plugin.getServer().getWorldContainer().toPath().resolve("logs").resolve("latest.log");
        File logsFile = logsPath.toFile();

        if (!logsFile.exists()) {
            Logging.warning("Could not read logs/latest.log");
            return "Could not find log";
        }

        // Try reading as ANSI encoded
        try {
            return Files.readString(logsPath, StandardCharsets.ISO_8859_1);
        } catch (IOException e) {
            Logging.finer("Log is not ANSI encoded. Trying UTF-8");
            // Must be a UTF-8 encoded log then
        }

        // Try reading as UTF-8 encoded
        try {
            return Files.readString(logsPath, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            // It is some other strange encoding
            Logging.severe("Could not read ./logs/latest.log. See below for stack trace");
            ex.printStackTrace();
        }
        return "Could not read log";
    }

    private String getVersionString() {
        return "# Multiverse-Core Version info" + "\n\n"
                + " - Multiverse-Core Version: " + this.plugin.getDescription().getVersion() + '\n'
                + " - Bukkit Version: " + this.plugin.getServer().getVersion() + '\n'
                + " - Loaded Worlds: " + worldManager.getLoadedWorlds() + '\n'
                + " - Multiverse Plugins Loaded: " + this.plugin.getPluginCount() + '\n';
    }

    private void addDebugInfoToEvent(MVVersionEvent event) {
        // Add the legacy file, but as markdown, so it's readable
        event.putDetailedVersionInfo("version.md", this.getVersionString());

        // add config.yml
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        event.putDetailedVersionInfo("multiverse-core/config.yml", configFile);

        // add worlds.yml
        File worldsFile = new File(plugin.getDataFolder(), "worlds.yml");
        event.putDetailedVersionInfo("multiverse-core/worlds.yml", worldsFile);

        // Add bukkit.yml if we found it
        if (getBukkitConfig() != null) {
            event.putDetailedVersionInfo(getBukkitConfig().getPath(), getBukkitConfig());
        } else {
            Logging.warning("/mv version could not find bukkit.yml. Not including file");
        }

        // Add server.properties if we found it
        if (getServerProperties() != null) {
            event.putDetailedVersionInfo(getServerProperties().getPath(), getServerProperties());
        } else {
            Logging.warning("/mv version could not find server.properties. Not including file");
        }

    }

    private String getPluginList() {
        return " - " + StringUtils.join(plugin.getServer().getPluginManager().getPlugins(), "\n - ");
    }

    /**
     * Turns a list of files in to a string containing askii art.
     *
     * @param files Map of filenames/contents
     * @return The askii art
     */
    private String encodeAsString(Map<String, String> files) {
        StringBuilder uploadData = new StringBuilder();
        for (String file : files.keySet()) {
            String data = files.get(file);
            uploadData.append("# ---------- ")
                    .append(file)
                    .append(" ----------\n\n")
                    .append(data)
                    .append("\n\n");
        }

        return uploadData.toString();
    }

    /**
     * Send the current contents of this.pasteBinBuffer to a web service.
     *
     * @param type Service type to send paste data to.
     * @param isPrivate Should the paste be marked as private.
     * @param rawPasteData Legacy string containing only data to post to a service.
     * @param pasteFiles Map of filenames/contents of debug info.
     * @return URL of visible paste
     */
    private String postToService(@NotNull PasteServiceType type, boolean isPrivate, @Nullable String rawPasteData, @Nullable Map<String, String> pasteFiles) {
        PasteService pasteService = PasteServiceFactory.getService(type, isPrivate);

        try {
            // Upload normally when multi file is supported
            if (pasteService.supportsMultiFile()) {
                return pasteService.postData(pasteFiles);
            }

            // When there is raw paste data, use that
            if (rawPasteData != null) { // For the logs
                return pasteService.postData(rawPasteData);
            }

            // If all we have are files and the paste service does not support multi file then encode them
            if (pasteFiles != null) {
                return pasteService.postData(this.encodeAsString(pasteFiles));
            }

            // Should never get here
            return "No data specified in code";

        } catch (PasteFailedException e) {
            e.printStackTrace();
            return "Error posting to service.";
        } catch (NullPointerException e) {
            e.printStackTrace();
            return "That service isn't supported yet.";
        }
    }
}
