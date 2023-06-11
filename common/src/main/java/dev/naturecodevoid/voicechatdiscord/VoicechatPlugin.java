package dev.naturecodevoid.voicechatdiscord;

import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.events.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static dev.naturecodevoid.voicechatdiscord.Common.api;
import static dev.naturecodevoid.voicechatdiscord.Common.platform;
import static dev.naturecodevoid.voicechatdiscord.GroupManager.groupFriendlyIds;
import static dev.naturecodevoid.voicechatdiscord.GroupManager.groupPlayers;

public class VoicechatPlugin implements de.maxhenkel.voicechat.api.VoicechatPlugin {
    @Override
    public String getPluginId() {
        return Constants.PLUGIN_ID;
    }

    @Override
    public void initialize(VoicechatApi serverApi) {
        api = (VoicechatServerApi) serverApi;
        platform.info("Successfully initialized Simple Voice Chat plugin");
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(JoinGroupEvent.class, this::onJoinGroup);
        registration.registerEvent(LeaveGroupEvent.class, this::onLeaveGroup);
        registration.registerEvent(CreateGroupEvent.class, this::onGroupCreated);
        registration.registerEvent(RemoveGroupEvent.class, this::onGroupRemoved);
    }

    private List<ServerPlayer> getPlayers(Group group) {
        List<ServerPlayer> players = groupPlayers.putIfAbsent(group.getId(), new ArrayList<>());
        if (players == null) players = groupPlayers.get(group.getId()); // java is bad
        return players;
    }

    @SuppressWarnings("DataFlowIssue")
    public void onJoinGroup(JoinGroupEvent event) {
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
    public void onLeaveGroup(LeaveGroupEvent event) {
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
    public void onGroupCreated(CreateGroupEvent event) {
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
    public void onGroupRemoved(RemoveGroupEvent event) {
        Group group = event.getGroup();
        UUID groupId = group.getId();

        platform.debug(groupId + " (" + groupFriendlyIds.get(groupId) + ", " + group.getName() + ")" + " was removed");

        groupPlayers.remove(groupId);
        groupFriendlyIds.remove(groupId);
    }
}
