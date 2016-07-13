package iw_bot;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;

import misc.Dance;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import net.dv8tion.jda.entities.Message.Attachment;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.utils.AvatarUtil;
import net.dv8tion.jda.utils.AvatarUtil.Avatar;
import provider.Connections;
import provider.DiscordInfo;
import structs.EDSystem;

interface PMCommand {
	void runCommand(PrivateMessageReceivedEvent event, String[] args);
}

interface GuildCommand {
	void runCommand(GuildMessageReceivedEvent event, String[] args);
	String getHelp(GuildMessageReceivedEvent event);
}

public class Commands {
	public Map<String, PMCommand> pmCommands = new LinkedHashMap<String, PMCommand>();
	public Map<String, GuildCommand> guildCommands = new LinkedHashMap<String, GuildCommand>();
	
	public Commands() {
		//Private message commands
		pmCommands.put("ping", new PMCommand() {
			public void runCommand(PrivateMessageReceivedEvent event, String[] args) {
				event.getChannel().sendMessageAsync("pong", null);
			}
		});

		pmCommands.put("version", new PMCommand() {
			public void runCommand(PrivateMessageReceivedEvent event, String[] args) {
				event.getChannel().sendMessageAsync(Listener.VERSION_NUMBER, null);
			}
		});
		
		pmCommands.put("restart", new PMCommand() {
			public void runCommand(PrivateMessageReceivedEvent event, String[] args) {
				//Permission check
				DiscordInfo info = new DiscordInfo();
				if (!info.isOwner(event.getAuthor().getId())) {
					event.getChannel().sendMessageAsync("[Error] You aren't authorized to do this", null);
					return;
				}
				
				event.getChannel().sendMessage("Trying to restart...");
				System.exit(1);
			}
		});
		
		pmCommands.put("note", new PMCommand() {
			public void runCommand(PrivateMessageReceivedEvent event, String[] args) {
				try {
					if (args.length == 0) { // default case. Just list all notes
						PreparedStatement ps = new Connections().getConnection().prepareStatement("SELECT * FROM notes WHERE userid = ?");
						ps.setString(1, event.getAuthor().getId());
						ResultSet rs = ps.executeQuery();
						
						String message = "Here are all your notes:\n";
						while (rs.next()) {
							message += ("-" + rs.getString("name") + "\n");
						}
						if (!message.equals("Here are all saved notes:\n"))
							event.getChannel().sendMessageAsync(message, null);
						else
							event.getChannel().sendMessageAsync("No notes found", null);
					}	
					else if (args[0].equals("add")) { // add case. Create a new note
						PreparedStatement ps = new Connections().getConnection().prepareStatement("INSERT INTO notes(name, content, userid) VALUES(?, ?, ?)") ;
						ps.setString(1, args[1].trim());
						ps.setString(2, args[2].trim());
						ps.setString(3, event.getAuthor().getId());
						ps.executeUpdate();
						
						event.getChannel().sendMessageAsync("Note saved", null);
					}
					else if (args[0].equals("del")) { // delete case. Delete note with that name
						PreparedStatement ps = new Connections().getConnection().prepareStatement("DELETE FROM notes WHERE name = ? AND userid = ?") ;
						ps.setString(1, args[1]);
						ps.setString(2, event.getAuthor().getId());
						int rowsUpdated = ps.executeUpdate();
						
						if (rowsUpdated == 1)
							event.getChannel().sendMessageAsync("Note deleted", null);
						else
							event.getChannel().sendMessageAsync("No note with that name found", null);
					}
					else {
						if (args[0].equals("view"))
							args[0] = args[1];
						PreparedStatement ps = new Connections().getConnection().prepareStatement("SELECT * FROM notes WHERE name = ?");
						ps.setString(1, args[0]);
						ResultSet rs = ps.executeQuery();
						
						if (rs.next())
							event.getChannel().sendMessageAsync(rs.getString("content"), null);
						else
							event.getChannel().sendMessageAsync("No note found", null);
					}
				} catch (MySQLIntegrityConstraintViolationException e) {
					event.getChannel().sendMessageAsync("[Error] A note with that name already exists", null);
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		});
		
		//Guild message commands
		guildCommands.put("help", new GuildCommand() {
			public void runCommand(GuildMessageReceivedEvent event, String[] args) {
				String message = "Commands available:\n```html\n";
				for (Map.Entry<String, GuildCommand> entry : guildCommands.entrySet()) {
					if (!entry.getValue().getHelp(event).isEmpty())
						message += String.format("/%1$-12s | " + entry.getValue().getHelp(event) + "\n", entry.getKey());
				}
				message += "```";
				event.getChannel().sendMessageAsync(message, null);
			}
			
			public String getHelp(GuildMessageReceivedEvent event) {
				return "< ?> variables are optional, <a>|<b> either var a OR b";
			}
		});

		guildCommands.put("setavatar", new GuildCommand() {
			public void runCommand(GuildMessageReceivedEvent event, String[] args) {
				event.getChannel().sendTyping();
				//Permission check
				DiscordInfo info = new DiscordInfo();
				if (!(info.isOwner(event.getAuthor().getId()) || info.isAdmin(event.getGuild().getRolesForUser(event.getAuthor())))) {
					event.getChannel().sendMessageAsync("[Error] You aren't authorized to do this", null);
					return;
				}
				
				if (!event.getMessage().getAttachments().isEmpty()) {
					File avatarFile;
					Attachment attachment = event.getMessage().getAttachments().get(0);
					attachment.download(avatarFile = new File("./temp/newavatar.jpg"));
					try {
						Avatar avatar = AvatarUtil.getAvatar(avatarFile);
						event.getJDA().getAccountManager().setAvatar(avatar).update();
					} catch (UnsupportedEncodingException e) {
						event.getChannel().sendMessageAsync("[Error] Filetype", null);
					}
					event.getChannel().sendMessageAsync("[Success] Avatar changed.", null);
					avatarFile.delete();
				}
				else {
					event.getChannel().sendMessageAsync("[Error] No image attached", null);
				}
			}
			
			public String getHelp(GuildMessageReceivedEvent event) {
				//Permission check
				DiscordInfo info = new DiscordInfo();
				if (!(info.isOwner(event.getAuthor().getId()) || info.isAdmin(event.getGuild().getRolesForUser(event.getAuthor()))))
					return "";
				return "Upload desiered pic to discord and enter command in the description prompt";
			}
		});

		guildCommands.put("setname", new GuildCommand() {
			public void runCommand(GuildMessageReceivedEvent event, String[] args) {
				event.getChannel().sendTyping();
				//Permission check
				DiscordInfo info = new DiscordInfo();
				if (!(info.isOwner(event.getAuthor().getId()) || info.isAdmin(event.getGuild().getRolesForUser(event.getAuthor())))) {
					event.getChannel().sendMessageAsync("[Error] You aren't authorized to do this", null);
					return;
				}
				
				if (args.length == 0) {
					event.getChannel().sendMessageAsync("[Error] No name stated", null);
				} else {
					event.getJDA().getAccountManager().setUsername(args[0]).update();
					event.getChannel().sendMessageAsync("[Success] Name changed", null);
				}
			}
			
			public String getHelp(GuildMessageReceivedEvent event) {
				//Permission check
				DiscordInfo info = new DiscordInfo();
				if (!(info.isOwner(event.getAuthor().getId()) || info.isAdmin(event.getGuild().getRolesForUser(event.getAuthor()))))
					return "";
				return "<name>";
			}
		});

		guildCommands.put("setgame", new GuildCommand() {
			public void runCommand(GuildMessageReceivedEvent event, String[] args) {
				event.getChannel().sendTyping();
				//Permission check
				DiscordInfo info = new DiscordInfo();
				if (!(info.isOwner(event.getAuthor().getId()) || info.isAdmin(event.getGuild().getRolesForUser(event.getAuthor())))) {
					event.getChannel().sendMessageAsync("[Error] You aren't authorized to do this", null);
					return;
				}
				
				if (args.length == 0)
					event.getJDA().getAccountManager().setGame(null);
				else
					event.getJDA().getAccountManager().setGame(args[0]);
				event.getChannel().sendMessageAsync("[Success] Game changed", null);
			}
			
			public String getHelp(GuildMessageReceivedEvent event) {
				//Permission check
				DiscordInfo info = new DiscordInfo();
				if (!(info.isOwner(event.getAuthor().getId()) || info.isAdmin(event.getGuild().getRolesForUser(event.getAuthor()))))
					return "";
				return "<game?> - To set the Playing: ...";
			}
		});

		guildCommands.put("role", new GuildCommand() {
			public void runCommand(GuildMessageReceivedEvent event, String[] args) {
				event.getChannel().sendTyping();
				//Permission check
				DiscordInfo info = new DiscordInfo();
				if (!(info.isOwner(event.getAuthor().getId()) || info.isAdmin(event.getGuild().getRolesForUser(event.getAuthor())))) {
					event.getChannel().sendMessageAsync("[Error] You aren't authorized to do this", null);
					return;
				}
				
				if (args.length == 0) {
					event.getChannel().sendMessageAsync("[Error] No name stated", null);
				} else if (args.length == 1) {
					event.getChannel().sendMessageAsync("[Error] No action selected", null);
				} else {
					if (args[0].equalsIgnoreCase("add")) {
						event.getGuild().createRole().setName(args[1]).update();
					}
					if (args[0].equalsIgnoreCase("del")) {
						for (Role role : event.getGuild().getRoles()) {
							if (role.getName().equalsIgnoreCase(args[1]))
								role.getManager().delete();
						}
					}
					if (args[0].equalsIgnoreCase("color")) {
						if (args.length < 5) {
							event.getChannel().sendMessageAsync("[Error] you need to specify the RGB values for the new color. '0, 0, 0' for example", null);
							return;
						}
						for (Role role : event.getGuild().getRoles()) {
							if (role.getName().equalsIgnoreCase(args[1]))
								role.getManager().setColor(new Color(Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]))).update();
						}
					}
					if (args[0].equalsIgnoreCase("rename")) {
						if (args.length < 3) {
							event.getChannel().sendMessageAsync("[Error] no new name set", null);
							return;
						}
						for (Role role : event.getGuild().getRoles()) {
							if (role.getName().equalsIgnoreCase(args[1]))
								role.getManager().setName(args[2]).update();
						}
					}
				}
			}
			
			public String getHelp(GuildMessageReceivedEvent event) {
				//Permission check
				DiscordInfo info = new DiscordInfo();
				if (!(info.isOwner(event.getAuthor().getId()) || info.isAdmin(event.getGuild().getRolesForUser(event.getAuthor()))))
					return "";
				return "<name>, <add|del|rename|color>, <newname|#color> - Edits the role in the specified way.";
			}
		});

		guildCommands.put("dist", new GuildCommand() {
			public void runCommand(GuildMessageReceivedEvent event, String[] args) {
				if (args.length < 2) {
					event.getChannel().sendMessageAsync("[Error] Not enough systems specified", null);
					return;
				}
				Gson gson = new Gson();
				EDSystem sys1 = null;
				EDSystem sys2 = null;
				String jsonSys1 = "";
				String jsonSys2 = "";
				String urlSys1 = "http://www.edsm.net/api-v1/system?sysname=" + args[0].trim().replaceAll(" ", "+") + "&coords=1";
				String urlSys2 = "http://www.edsm.net/api-v1/system?sysname=" + args[1].trim().replaceAll(" ", "+") + "&coords=1";
				
				try {
					Document docSys1 = Jsoup.connect(urlSys1).ignoreContentType(true).get();
					Document docSys2 = Jsoup.connect(urlSys2).ignoreContentType(true).get();
					
					jsonSys1 = docSys1.body().text();
					jsonSys2 = docSys2.body().text();
					
					if (jsonSys1.contains("[]") || jsonSys2.contains("[]")) {
						event.getChannel().sendMessageAsync("[Error] System not found or coordinates not in db.", null);
						return;
					}
					
					sys1 = gson.fromJson(jsonSys1, EDSystem.class);
					sys2 = gson.fromJson(jsonSys2, EDSystem.class);
					
					if (sys1.coords == null || sys2.coords == null) {
						event.getChannel().sendMessageAsync("[Error] System not found or coordinates not in db.", null);
						return;
					}
					
					float x = sys2.coords.x - sys1.coords.x;
					float y = sys2.coords.y - sys1.coords.y;
					float z = sys2.coords.z - sys1.coords.z;
					
					double dist = Math.sqrt(x*x + y*y + z*z);

					event.getChannel().sendMessageAsync(String.format("Distance: %.1f ly", dist), null);
				} catch (JsonSyntaxException e) {
					event.getChannel().sendMessageAsync("[Error] Processing edsm result failed", null);
				} catch (SocketException e) {
					event.getChannel().sendMessageAsync("[Error] Failed connecting to edsm. You might want to retry in a few", null);
				} catch (IOException e) {
					event.getChannel().sendMessageAsync("[Error] Processing data failed", null);
				}
			}
			
			public String getHelp(GuildMessageReceivedEvent event) {
				return "<system1>, <system2> - Gives the distance between those systems.";
			}
		});

		guildCommands.put("new", new GuildCommand() {
			public void runCommand(GuildMessageReceivedEvent event, String[] args) {
				DiscordInfo info = new DiscordInfo();
				//Permission check
				if (!(info.isOwner(event.getAuthor().getId()) || info.isAdmin(event.getGuild().getRolesForUser(event.getAuthor())))) {
					event.getChannel().sendMessageAsync("[Error] You aren't authorized to do this", null);
					return;
				}
				
				try {
					if (args.length == 0) {
						event.getChannel().sendMessageAsync(info.getNewMemberInfo().replaceAll("<user>", event.getAuthorName()), null);
					}
					else {
						
						info.setNewMemberInfo(event.getMessage().getRawContent().replaceFirst("/new", "").trim());
						event.getChannel().sendMessageAsync("[Success] New member message changed", null);
					}
						
				} catch (FileNotFoundException e) {
					event.getChannel().sendMessageAsync("[Error] Couldn't find the message, sorry", null);
				} catch (IOException e) {
					event.getChannel().sendMessageAsync("[Error] Couldn't read required file", null);
				}
			}
			
			public String getHelp(GuildMessageReceivedEvent event) {
				//Permission check
				DiscordInfo info = new DiscordInfo();
				if (!(info.isOwner(event.getAuthor().getId()) || info.isAdmin(event.getGuild().getRolesForUser(event.getAuthor()))))
					return "";
				return "<information?> - sets information for new players or shows it";
			}
		});

		guildCommands.put("adminchannel", new GuildCommand() {
			public void runCommand(GuildMessageReceivedEvent event, String[] args) {
				DiscordInfo info = new DiscordInfo();
				
				//Permission check
				if (!(info.isOwner(event.getAuthor().getId()) || info.isAdmin(event.getGuild().getRolesForUser(event.getAuthor())))) {
					event.getChannel().sendMessageAsync("[Error] You aren't authorized to do this", null);
					return;
				}
				try {
					if (args.length == 0){
						event.getChannel().sendMessageAsync("Admin channel is: <#" + info.getAdminChanID() + ">", null);
					}
					else if (!event.getMessage().getMentionedChannels().isEmpty()) {
						info.setAdminChanID(event.getMessage().getMentionedChannels().get(0).getId());
						event.getChannel().sendMessageAsync("[Success] Admin channel saved", null);
					}
					else {
						TextChannel chan = event.getGuild().getTextChannels().stream().filter(vChan -> vChan.getName().equalsIgnoreCase(args[0].trim()))
								.findFirst().orElse(null);
						if (chan == null) {
							event.getChannel().sendMessageAsync("Channel not found", null);
							return;
						} else
							info.setAdminChanID(chan.getId());
						event.getChannel().sendMessageAsync("[Success] Admin channel saved", null);
					}
				} catch (IOException e) {
					event.getChannel().sendMessageAsync("[Error] Couldn't read required file", null);
				}
			}
			
			public String getHelp(GuildMessageReceivedEvent event) {
				//Permission check
				DiscordInfo info = new DiscordInfo();
				if (!(info.isOwner(event.getAuthor().getId()) || info.isAdmin(event.getGuild().getRolesForUser(event.getAuthor()))))
					return "";
				return "<channel> - sets admin channel";
			}
		});

		guildCommands.put("admin", new GuildCommand() {
			public void runCommand(GuildMessageReceivedEvent event, String[] args) {
				DiscordInfo info = new DiscordInfo();
				
				//Permission check
				if (!(info.isOwner(event.getAuthor().getId()) || info.isAdmin(event.getGuild().getRolesForUser(event.getAuthor())))) {
					event.getChannel().sendMessageAsync("[Error] You aren't authorized to do this", null);
					return;
				}
				
				try {
					if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("view"))) {
						String message = "";
						for (String id : info.getAdminRoleIDs())
							message += ( "-" + event.getGuild().getRoleById(id).getName() + "\n" );
						
						if (!message.isEmpty())
							event.getChannel().sendMessageAsync("Roles with admin privileges:\n" + message, null);
						else
							event.getChannel().sendMessageAsync("No admin roles defined", null);
					}
					else if (args[0].equalsIgnoreCase("add")) {
						if (!event.getMessage().getMentionedRoles().isEmpty()) {
							info.addAdminRoleID(event.getMessage().getMentionedRoles().get(0).getId());
						} else if (args.length == 2) {
							Role role = event.getGuild().getRoles().stream().filter(vrole -> vrole.getName().equalsIgnoreCase(args[1].trim())).findFirst().orElse(null);
							if (role != null) {
								info.addAdminRoleID(role.getId());
							} else {
								event.getChannel().sendMessageAsync("[Error] No role with this name found", null);
								return;
							}
						} else {
							event.getChannel().sendMessageAsync("[Error] No role specified", null);
							return;
						}
						event.getChannel().sendMessageAsync("[Success] Admin role saved", null);
					}
					else if (args[0].equalsIgnoreCase("del")) {
						if (!event.getMessage().getMentionedRoles().isEmpty()) {
							info.removeAdminRoleID(event.getMessage().getMentionedRoles().get(0).getId());
						} else if (args.length == 2) {
							Role role = event.getGuild().getRoles().stream().filter(vrole -> vrole.getName().equalsIgnoreCase(args[1].trim())).findFirst().orElse(null);
							if (role != null) {
								info.removeAdminRoleID(role.getId());
							} else {
								event.getChannel().sendMessageAsync("[Error] No role with this name found", null);
								return;
							}
						} else {
							event.getChannel().sendMessageAsync("[Error] No role specified", null);
							return;
						}
						event.getChannel().sendMessageAsync("[Success] Admin role removed", null);
					}
				} catch (IOException e) {
					event.getChannel().sendMessageAsync("[Error] Couldn't read required file", null);
				}
			}
			
			public String getHelp(GuildMessageReceivedEvent event) {
				//Permission check
				DiscordInfo info = new DiscordInfo();
				if (!(info.isOwner(event.getAuthor().getId()) || info.isAdmin(event.getGuild().getRolesForUser(event.getAuthor()))))
					return "";
				return "<add?>|<del?>, <role?> - shows, adds or delets a role in/to/from admins";
			}
		});

		guildCommands.put("dance", new GuildCommand() {
			public void runCommand(GuildMessageReceivedEvent event, String[] args) {
				Dance dance = new Dance(event);
				dance.setDance(Dance.ASCII.DANCE);
				dance.start();
			}
			
			public String getHelp(GuildMessageReceivedEvent event) {
				return "Makes the bot dance";
			}
		});

		guildCommands.put("topic", new GuildCommand() {
			public void runCommand(GuildMessageReceivedEvent event, String[] args) {
				event.getChannel().sendMessageAsync(event.getChannel().getTopic(), null);
			}
			
			public String getHelp(GuildMessageReceivedEvent event) {
				return "Shows the channel details";
			}
		});

		guildCommands.put("time", new GuildCommand() {
			public void runCommand(GuildMessageReceivedEvent event, String[] args) {
				Date date = new Date();
				SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
				sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
				event.getMessage().deleteMessage();
				event.getChannel().sendMessageAsync("UTC time:\n"
						+ "```html\n"
						+ "<" + sdf.format(date) + ">```", null);
			}
			
			public String getHelp(GuildMessageReceivedEvent event) {
				return "Shows the channel details";
			}
		});

		guildCommands.put("status", new GuildCommand() {
			public void runCommand(GuildMessageReceivedEvent event, String[] args) {
				Long diff 			 = (new Date().getTime() - Listener.startupTime);
				int days			 = (int) TimeUnit.MILLISECONDS.toDays(diff);
				int hours			 = (int) TimeUnit.MILLISECONDS.toHours(diff) % 24;
				int minutes			 = (int) TimeUnit.MILLISECONDS.toMinutes(diff) % 60;
				int seconds			 = (int) TimeUnit.MILLISECONDS.toSeconds(diff) % 60;
				NumberFormat nForm	 = NumberFormat.getInstance(Locale.GERMANY);
				int noThreads		 = Thread.getAllStackTraces().keySet().size();
				String uniqueSets	 = "";
				String totalSets	 = "";
				String totalMemory	 = String.format("%.2f",(double) Runtime.getRuntime().maxMemory() / 1024 / 1024);
				String usedMemory	 = String.format("%.2f",(double)(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024);
				
				try {
					PreparedStatement ps = new Connections().getConnection().prepareStatement("SELECT COUNT(idmarkov) AS unique_sets, sum(prob) AS total_sets FROM iwmembers.markov");
					ResultSet rs = ps.executeQuery();
					rs.next();
					uniqueSets = nForm.format(rs.getInt("unique_sets")).replace('.', '\'');
					totalSets = nForm.format(rs.getInt("total_sets")).replace('.', '\'');
					rs.close();
					ps.close();
				} catch (SQLException e) { e.printStackTrace(); }
				
				String contOut = "```"
						+ "Uptime              | " + String.format("%dd %02d:%02d:%02d\n", days, hours, minutes, seconds)
						+ "# Threads           | " + noThreads						+ "\n"
						+ "Memory usage        | " + usedMemory + "/" + totalMemory	+ " MB\n"
						+ "Unique AI Datasets  | " + uniqueSets						+ "\n"
						+ "Total AI Datasets   | " + totalSets						+ "\n"
						+ "Version             | " + Listener.VERSION_NUMBER		+ "```";
				
				event.getChannel().sendMessageAsync(contOut, null);
			}
			
			public String getHelp(GuildMessageReceivedEvent event) {
				return "Shows information about the bot";
			}
		});

		guildCommands.put("xkcd", new GuildCommand() {
			public void runCommand(GuildMessageReceivedEvent event, String[] args) {
				Image image = null;
				File file = null;
				Random RNJesus = new Random();
				int iRandom = RNJesus.nextInt(1662);
				String url = "http://xkcd.com/" + iRandom + "/";
				
				try {
					Document doc = Jsoup.connect(url).get();
					Elements images = doc.select("img[src$=.png]");
					
					for(Element eImage : images) {
						if(eImage.attr("src").contains("comic")) {
							URL uRl = new URL("http:" + eImage.attr("src"));
							image = ImageIO.read(uRl);
							ImageIO.write((RenderedImage) image, "png", file = new File("./temp/" + iRandom + ".png"));
							event.getChannel().sendFile(file, null);
							file.delete();
						}
					}
				} catch (SocketException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			public String getHelp(GuildMessageReceivedEvent event) {
				return "Post a random XKCD comic";
			}
		});
		
		guildCommands.put("stripme", new GuildCommand() {
			public void runCommand(GuildMessageReceivedEvent event, String[] args) {
				User author = event.getAuthor();
				Guild guild = event.getGuild();
				List<Role> rolesToStrip = new ArrayList<Role>();
				
				for (Role role : guild.getRoles()) {
					for (String roleName : args) {
						if (role.getName().equalsIgnoreCase(roleName.trim()))
							rolesToStrip.add(role);
					}
				}
				
				String output = "```Removed roles: ";
				for (Role role : rolesToStrip) {
					guild.getManager().removeRoleFromUser(author, role).update();
					output += "\n" + role.getName();
				}
				output += "```";
				
				event.getChannel().sendMessageAsync(output, null);
			}
			
			public String getHelp(GuildMessageReceivedEvent event) {
				return "Removes the specified roles";
			}
		});
		//end of commands
	}
}
