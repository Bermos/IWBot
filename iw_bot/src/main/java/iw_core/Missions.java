package iw_core;

import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.entities.TextChannel;

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
	
}
