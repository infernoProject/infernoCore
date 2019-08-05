package ru.infernoproject.worldd.world.invite;

import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.utils.ByteConvertible;
import ru.infernoproject.common.utils.ByteWrapper;
import ru.infernoproject.worldd.constants.WorldEventType;
import ru.infernoproject.worldd.map.WorldCell;
import ru.infernoproject.worldd.map.WorldMap;
import ru.infernoproject.worldd.map.WorldMapManager;
import ru.infernoproject.worldd.world.guild.GuildManager;
import ru.infernoproject.worldd.world.guild.sql.Guild;
import ru.infernoproject.worldd.world.player.WorldPlayer;

import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class InviteManager {

    private volatile long inviteId;
    private final Map<Long, Invite> inviteMap;

    private final WorldMapManager worldMapManager;
    private final GuildManager guildManager;

    public InviteManager(WorldMapManager worldMapManager, GuildManager guildManager) {
        this.inviteId = 1L;
        this.inviteMap = new ConcurrentHashMap<>();

        this.worldMapManager = worldMapManager;
        this.guildManager = guildManager;
    }

    public synchronized void sendInvite(InviteType type, WorldPlayer sender, WorldPlayer target, ByteConvertible data) {
        WorldMap map = worldMapManager.getMap(target.getPosition());
        WorldCell cell = map.getCellByPosition(target.getPosition());

        Invite invite = new Invite(inviteId, type, sender, data);

        inviteMap.put(inviteId, invite);
        inviteId++;

        target.onEvent(cell, WorldEventType.INVITE, new ByteArray()
            .put(sender.getAttributes())
            .put(invite)
        );
    }

    public void respondToInvite(long id, boolean accepted, WorldPlayer respondent) throws SQLException {
        Invite invite = inviteMap.get(id);

        if (Objects.isNull(invite))
            return;

        if (accepted) {
            switch (invite.getType()) {
                case GUILD:
                    ByteWrapper guildInfo = invite.getData();
                    int guildId = guildInfo.getInt();
                    Guild guild = guildManager.getGuild(guildId);

                    guildManager.addGuildMember(guild, respondent.getCharacterInfo(), -1);
                    break;
            }
        }

        WorldPlayer sender = invite.getSender();
        WorldMap map = worldMapManager.getMap(sender.getPosition());
        WorldCell cell = map.getCellByPosition(sender.getPosition());

        sender.onEvent(cell, WorldEventType.INVITE_RESPONSE, new ByteArray()
            .put(respondent.getAttributes())
            .put(accepted)
        );

        inviteMap.remove(id);
    }
}
