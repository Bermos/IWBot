package iw_bot;

import java.io.FileNotFoundException;
import java.util.Date;

import iw_bot.Commands;
import iw_core.Users;
import misc.StatusGenerator;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.events.ReadyEvent;
import net.dv8tion.jda.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.events.user.UserAvatarUpdateEvent;
import net.dv8tion.jda.events.user.UserNameUpdateEvent;
import net.dv8tion.jda.events.user.UserOnlineStatusUpdateEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;
import provider.DiscordInfo;
import provider.Statistics;

public class Listener extends ListenerAdapter {
	private Commands commands;
	public static long startupTime;
	public static final String VERSION_NUMBER = "2.0.0_12 alpha";
	
	public Listener() {
		this.commands = new Commands();
	}
	
	@Override
	public void onReady(ReadyEvent event) {
		System.out.println("[Info] Listener v" + VERSION_NUMBER + " ready!");
		System.out.println("[Info] Connected to:");
		for (Guild guild : event.getJDA().getGuilds()) {
			System.out.println("	" + guild.getName());
		}

		Statistics stats = Statistics.getInstance();
		stats.connect(event.getJDA());
		
		Listener.startupTime = new Date().getTime();
		new StatusGenerator(event.getJDA().getAccountManager());
		
		new Users();
		Users.sync(event);
	}
	
	@Override
	public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
		System.out.printf("[PM] %s: %s\n", 	event.getAuthor().getUsername(),
											event.getMessage().getContent());
		
		//Check for command
				if (event.getMessage().getContent().startsWith("/") && !event.getAuthor().equals(event.getJDA().getSelfInfo())) {
					String content = event.getMessage().getContent();
					String commandName = content.replaceFirst("/", "").split(" ")[0];
					String[] args = {};
					if (content.replaceFirst("/" + commandName, "").trim().length() > 0) {
						args = content.replaceFirst("/" + commandName, "").trim().split(",");
						for (int i = 0; i < args.length; i++)
							args[i] = args[i].trim();
					}
					
					event.getChannel().sendTyping();
					if (commands.pmCommands.containsKey(commandName)) {
						commands.pmCommands.get(commandName).runCommand(event, args);
					}
				}
	}
	
	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent  event) {
		System.out.printf("[%s][%s] %s: %s\n", 	event.getGuild().getName(),
												event.getChannel().getName(),
												event.getAuthor().getUsername(),
												event.getMessage().getContent());
		
		//Check for command
		if (event.getMessage().getContent().startsWith("/") && !event.getAuthor().equals(event.getJDA().getSelfInfo())) {
			String content = event.getMessage().getContent();
			String commandName = content.replaceFirst("/", "").split(" ")[0];
			String[] args = {};
			if (content.replaceFirst("/" + commandName, "").trim().length() > 0) {
				args = content.replaceFirst("/" + commandName, "").trim().split(",");
				for (int i = 0; i < args.length; i++)
					args[i] = args[i].trim();
			}
			
			event.getChannel().sendTyping();
			if (commands.guildCommands.containsKey(commandName)) {
				Statistics.getInstance().logCommandReceived(commandName, event.getAuthor().getUsername());
				commands.guildCommands.get(commandName).runCommand(event, args);
			}
		}
		Statistics.getInstance().logMessage(event);
	}
	
	@Override
	public void onGuildMemberJoin(GuildMemberJoinEvent event) {
		TextChannel channel = event.getGuild().getPublicChannel(); 
		channel.sendTyping();
		try {
			channel.sendMessageAsync(new DiscordInfo().getNewMemberInfo().replaceAll("<user>", event.getUser().getUsername()), null);
			event.getJDA().getTextChannelById(new DiscordInfo().getAdminChanID())
				.sendMessageAsync("New user, " + event.getUser().getUsername() + ", just joined!", null);
		} catch (FileNotFoundException e) {
			event.getGuild().getPublicChannel().sendMessageAsync("[Error] Couldn't find the new member message, sorry. ", null);
		}
		
		Users.joined(event);
	}
	
	@Override
	public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
		Users.left(event);
	}
	
	@Override
	public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event) {
		Users.roleUpdate(event);
	}
	
	@Override
	public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) {
		Users.roleUpdate(event);
	}
	
	@Override
	public void onUserOnlineStatusUpdate(UserOnlineStatusUpdateEvent event) {
		Users.setOnlineStatus(event);
	}
	
	@Override
	public void onUserNameUpdate(UserNameUpdateEvent event) {
		Users.nameUpdate(event);
	}
	
	@Override
	public void onUserAvatarUpdate(UserAvatarUpdateEvent event) {
		Users.avatarUpdate(event);
	}
}
