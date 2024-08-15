package dev.amsam0.voicechatdiscord;

import org.bukkit.Bukkit;

public final class BukkitHelper {
    public static Class<?> getCraftServer() throws ClassNotFoundException {
        return Class.forName(Bukkit.getServer().getClass().getPackage().getName() + ".CraftServer");
    }

    public static Class<?> getCraftWorld() throws ClassNotFoundException {
        return Class.forName(Bukkit.getServer().getClass().getPackage().getName() + ".CraftWorld");
    }

    public static Class<?> getVanillaCommandWrapper() throws ClassNotFoundException {
        return Class.forName(Bukkit.getServer().getClass().getPackage().getName() + ".command.VanillaCommandWrapper");
    }
}
