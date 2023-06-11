package dev.naturecodevoid.voicechatdiscord;

import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static dev.naturecodevoid.voicechatdiscord.Common.platform;

public class GroupManager {
    public static final BiMap<UUID, Integer> groupFriendlyIds = new BiMap<>();
    public static final Map<UUID, List<ServerPlayer>> groupPlayers = new HashMap<>();

    @SuppressWarnings("CallToPrintStackTrace")
    public static @Nullable String getPassword(Group group) {
        // https://github.com/henkelmax/enhanced-groups/blob/f5535f84fc41a2b1798b2b43adddcd6b6b28c22a/src/main/java/de/maxhenkel/enhancedgroups/events/ForceGroupTypeEvents.java#LL46C53-L57C10
        try {
            Field groupField = group.getClass().getDeclaredField("group"); // https://github.com/henkelmax/simple-voice-chat/blob/6bdc2901f28b8bc7fc492871b644a2d5478e54dd/common/src/main/java/de/maxhenkel/voicechat/plugins/impl/GroupImpl.java#L13
            groupField.setAccessible(true);
            Object groupObject = groupField.get(group);
            Field passwordField = groupObject.getClass().getDeclaredField("password"); // https://github.com/henkelmax/simple-voice-chat/blob/6bdc2901f28b8bc7fc492871b644a2d5478e54dd/common/src/main/java/de/maxhenkel/voicechat/voice/server/Group.java#L13
            passwordField.setAccessible(true);
            return (String) passwordField.get(groupObject);
        } catch (Throwable e) {
            platform.warn("Could not get password of group \"" + group.getName() + "\" (" + group.getId() + "):");
            e.printStackTrace();
            return null;
        }
    }
}
