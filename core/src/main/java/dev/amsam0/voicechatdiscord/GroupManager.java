package dev.amsam0.voicechatdiscord;

import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.events.CreateGroupEvent;
import de.maxhenkel.voicechat.api.events.JoinGroupEvent;
import de.maxhenkel.voicechat.api.events.LeaveGroupEvent;
import de.maxhenkel.voicechat.api.events.RemoveGroupEvent;
import dev.amsam0.voicechatdiscord.util.BiMap;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;

import static dev.amsam0.voicechatdiscord.Core.api;
import static dev.amsam0.voicechatdiscord.Core.platform;

public final class GroupManager {
    public static final BiMap<UUID, Integer> groupFriendlyIds = new BiMap<>();
    public static final Map<UUID, List<ServerPlayer>> groupPlayers = new HashMap<>();

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
            platform.warn("Could not get password of group \"" + group.getName() + "\" (" + group.getId() + "): " + e.getMessage());
            platform.debug(e);
            return null;
        }
    }

    private static List<ServerPlayer> getPlayers(Group group) {
        List<ServerPlayer> players = groupPlayers.putIfAbsent(group.getId(), new ArrayList<>());
        if (players == null) players = groupPlayers.get(group.getId()); // java is bad
        return players;
    }

    @SuppressWarnings("DataFlowIssue")
    public static void onJoinGroup(JoinGroupEvent event) {
        Group group = event.getGroup();
        ServerPlayer player = event.getConnection().getPlayer();

        List<ServerPlayer> players = getPlayers(group);
        if (players.stream().noneMatch(serverPlayer -> serverPlayer.getUuid() == player.getUuid())) {
            platform.debug(player.getUuid() + " (" + platform.getName(player) + ") joined " + group.getId() + " (" + group.getName() + ")");
            players.add(player);
        } else {
            platform.debug(player.getUuid() + " (" + platform.getName(player) + ") already joined " + group.getId() + " (" + group.getName() + ")");
        }
    }

    @SuppressWarnings("DataFlowIssue")
    public static void onLeaveGroup(LeaveGroupEvent event) {
        Group group = event.getGroup();
        ServerPlayer player = event.getConnection().getPlayer();
        if (group == null) {
            for (var groupEntry : groupPlayers.entrySet()) {
                List<ServerPlayer> playerList = groupEntry.getValue();
                if (playerList.stream().anyMatch(serverPlayer -> serverPlayer.getUuid() == player.getUuid())) {
                    UUID playerGroup = groupEntry.getKey();
                    platform.debug(player.getUuid() + " (" + platform.getName(player) + ") left " + playerGroup + " (" + api.getGroup(playerGroup).getName() + ")");
                    playerList.remove(player);
                    return;
                }
            }
            platform.debug(player.getUuid() + " (" + platform.getName(player) + ") left a group but we couldn't find the group they left");
            return;
        }

        platform.debug(player.getUuid() + " (" + platform.getName(player) + ") left " + group.getId() + " (" + group.getName() + ")");

        List<ServerPlayer> players = getPlayers(group);
        players.remove(player);
    }

    @SuppressWarnings("DataFlowIssue")
    public static void onGroupCreated(CreateGroupEvent event) {
        Group group = event.getGroup();
        UUID groupId = group.getId();

        if (groupFriendlyIds.get(groupId) == null) {
            int friendlyId = 1;
            Collection<Integer> friendlyIds = groupFriendlyIds.values();
            while (friendlyIds.contains(friendlyId)) {
                friendlyId++;
            }
            groupFriendlyIds.put(groupId, friendlyId);
        }

        VoicechatConnection connection = event.getConnection();
        if (connection == null) {
            platform.debug("someone created " + groupId + " (" + group.getName() + ")");
            return;
        }
        ServerPlayer player = connection.getPlayer();

        platform.debug(player.getUuid() + " (" + platform.getName(player) + ") created " + groupId + " (" + group.getName() + ")");

        List<ServerPlayer> players = getPlayers(group);
        players.add(player);
    }

    @SuppressWarnings("DataFlowIssue")
    public static void onGroupRemoved(RemoveGroupEvent event) {
        Group group = event.getGroup();
        UUID groupId = group.getId();

        platform.debug(groupId + " (" + groupFriendlyIds.get(groupId) + ", " + group.getName() + ")" + " was removed");

        groupPlayers.remove(groupId);
        groupFriendlyIds.remove(groupId);
    }
}
