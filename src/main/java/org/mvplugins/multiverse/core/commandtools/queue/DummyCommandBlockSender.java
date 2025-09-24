package org.mvplugins.multiverse.core.commandtools.queue;

import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.text.Component;

/**
 * Used by {@link CommandQueueManager}, so different commands block can be recognised as one.
 */
class DummyCommandBlockSender implements CommandSender {

    @Override
    public void sendMessage(@NotNull String message) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendMessage(@NotNull String[] messages) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendMessage(@Nullable UUID uuid, @NotNull String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendMessage(@Nullable UUID uuid, @NotNull String[] strings) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull Server getServer() {
        return Bukkit.getServer();
    }

    @Override
    public @NotNull String getName() {
        return "DummyCommandBlockSender";
    }

    @NotNull
    @Override
    public Spigot spigot() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isPermissionSet(@NotNull String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isPermissionSet(@NotNull Permission perm) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasPermission(@NotNull String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasPermission(@NotNull Permission perm) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String name, boolean value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull PermissionAttachment addAttachment(@NotNull Plugin plugin) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String name, boolean value, int ticks) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable PermissionAttachment addAttachment(@NotNull Plugin plugin, int ticks) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeAttachment(@NotNull PermissionAttachment attachment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void recalculatePermissions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull Set<PermissionAttachmentInfo> getEffectivePermissions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isOp() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setOp(boolean value) {
        throw new UnsupportedOperationException();
    }

	@Override
	public @NotNull Component name() {
		return Component.text(getName());
	}
	
}