package dev.naturecodevoid.voicechatdiscord;

import com.mojang.brigadier.context.CommandContext;
import de.maxhenkel.voicechat.api.Position;
import org.jetbrains.annotations.Nullable;

public class Util {
    public static double clamp(double val, double min, double max) {
        return Math.min(max, Math.max(min, val));
    }

    public static double distance(Position pos1, Position pos2) {
        return Math.sqrt(
                Math.pow(pos1.getX() - pos2.getX(), 2) +
                        Math.pow(pos1.getY() - pos2.getY(), 2) +
                        Math.pow(pos1.getZ() - pos2.getZ(), 2)
        );
    }

    public static String positionToString(Position pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    public static @Nullable <V> V getArgumentOr(CommandContext<?> context, final String name, final Class<V> clazz, @Nullable V or) {
        try {
            return context.getArgument(name, clazz);
        } catch (IllegalArgumentException ignored) {
            return or;
        }
    }
}
