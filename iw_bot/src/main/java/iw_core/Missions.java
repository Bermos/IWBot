package iw_core;

import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.managers.ChannelManager;
import net.dv8tion.jda.managers.GuildManager;
import net.dv8tion.jda.managers.PermissionOverrideManager;
import net.dv8tion.jda.managers.RoleManager;
import provider.DiscordInfo;

public class Missions {
	private static List<MissionChannel> missionChannels = new ArrayList<MissionChannel>();

	private static MissionChannel getChannel(String textChanID) {
		for (MissionChannel chan : missionChannels) {
			if (chan.getId().equals(textChanID))
				return chan;
		}
		
		return null;
	}
	
	public static void newList(TextChannel channel, String list) {
		MissionChannel missionChannel = getChannel(channel.getId());
		if (missionChannel == null) {
			missionChannel = new MissionChannel(channel.getId(), channel.getGuild());
			missionChannels.add(missionChannel);
		}
		
		missionChannel.add(list);
	}
	
	public static void nextListEntry(String textChanID) {
		getChannel(textChanID).next();
	}
	
	public static void getList(String textChanID) {
		getChannel(textChanID).print(true);
	}
	
	public static void create(String name, GuildManager guildManager) {
		ChannelManager missionChannelManager = null;
		RoleManager missionRoleManager = null;
		Role everyoneRole = guildManager.getGuild().getPublicRole();
		Role moderatorRole = guildManager.getGuild().getRoleById(DiscordInfo.getAdminRoleIDs().get(0));
		Role missionRole = null;
		
		String channelName = "mission_" + name;
		String roleName = "Mission_" + name;
		
		missionRoleManager = guildManager.getGuild().createCopyOfRole(guildManager.getGuild().getRoleById("205515484223766528"));
		missionRole = missionRoleManager.getRole();
		missionRoleManager.setName(roleName).update();
		
		missionChannelManager = guildManager.getGuild().createTextChannel(channelName);
		PermissionOverrideManager permissionManager = missionChannelManager.getChannel().createPermissionOverride(moderatorRole);
		permissionManager.grant(Permission.MESSAGE_READ);
		permissionManager.grant(Permission.MESSAGE_WRITE);
		permissionManager.grant(Permission.MESSAGE_MENTION_EVERYONE);
		permissionManager.grant(Permission.MESSAGE_HISTORY);
		permissionManager.grant(Permission.MANAGE_PERMISSIONS);
		permissionManager.grant(Permission.MANAGE_CHANNEL);
		permissionManager.update();
		
		permissionManager = missionChannelManager.getChannel().createPermissionOverride(missionRole);
		permissionManager.grant(Permission.MESSAGE_READ);
		permissionManager.grant(Permission.MESSAGE_WRITE);
		permissionManager.grant(Permission.MESSAGE_MENTION_EVERYONE);
		permissionManager.grant(Permission.MESSAGE_HISTORY);
		permissionManager.update();
		
		permissionManager = missionChannelManager.getChannel().createPermissionOverride(everyoneRole);
		permissionManager.deny (Permission.MESSAGE_READ);
		permissionManager.deny (Permission.MESSAGE_WRITE);
		permissionManager.update();
	}


}
