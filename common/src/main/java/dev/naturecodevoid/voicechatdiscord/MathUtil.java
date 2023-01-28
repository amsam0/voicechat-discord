package dev.naturecodevoid.voicechatdiscord;

import de.maxhenkel.voicechat.api.Position;

public class MathUtil {
    static double clamp(double val, double min, double max) {
        return Math.min(max, Math.max(min, val));
    }

    static double distance(Position pos1, Position pos2) {
        return Math.sqrt(
                Math.pow(pos1.getX() - pos2.getX(), 2) +
                        Math.pow(pos1.getY() - pos2.getY(), 2) +
                        Math.pow(pos1.getZ() - pos2.getZ(), 2)
        );
    }
}
