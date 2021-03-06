package provider;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.concurrent.TimeUnit;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.influxdb.dto.Pong;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import net.dv8tion.jda.JDA;
import net.dv8tion.jda.OnlineStatus;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.message.guild.GuildMessageReceivedEvent;

public class Statistics extends Thread {
	private static Statistics instance;
	private static InfluxDB influxDB;
	private static String dbName = "iw_monitor";
	private static JDA jda;

	class InfluxInfo {
		String IP;
		String US;
		String PW;
	}

	private Statistics() {
		
	}
	
	static {
		instance = new Statistics();
	}
	
	public static Statistics getInstance() {
		return instance;
	}
	
	public void connect(JDA jda) {
		Gson gson = new Gson();
		try {
			JsonReader jReader = new JsonReader(new FileReader("./influxinfo.json"));
			InfluxInfo info = gson.fromJson(jReader, InfluxInfo.class);
			
			Statistics.influxDB = InfluxDBFactory.connect(info.IP, info.US, info.PW);
			Statistics.jda = jda;
			
			boolean connected = false;
			do {
				Pong response;
				try {
					response = influxDB.ping();
					if (!response.getVersion().equalsIgnoreCase("unknown")) {
						connected = true;
					}

					Thread.sleep(10L);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} while (!connected);
			influxDB.enableBatch(2000, 1000, TimeUnit.MILLISECONDS);
			System.out.println("[InfluxDB] connected. Version: " + influxDB.version());
			
			this.start();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public void run() {
		Thread.currentThread().setName("BOT - PROVIDER - Statistics");
		while (true) {
			try {
				//update statistics
				int onlineUser = 0;
				for(User user : jda.getUsers()) {
					if (!user.getOnlineStatus().equals(OnlineStatus.OFFLINE))
						onlineUser++;
				}
				Point users = Point.measurement("users")
						.time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
						.addField("online", onlineUser)
						.addField("total", jda.getUsers().size())
						.build();
				influxDB.write(dbName, "default", users);
				
				Point system = Point.measurement("system")
						.time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
						.addField("used_ram", (double)(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024)
						.addField("total_ram", (double) Runtime.getRuntime().maxMemory() / 1024 / 1024)
						.addField("no_threads", Thread.getAllStackTraces().keySet().size())
						.build();
				influxDB.write(dbName, "default", system);
				
				Thread.sleep(1000);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void logMessage(GuildMessageReceivedEvent event) {
		Point messages = Point.measurement("messages")
				.time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
				.addField("content_length", event.getMessage().getContent().length())
				.addField("author", event.getAuthor().getUsername())
				.addField("channel", event.getChannel().getName())
				.build();
		influxDB.write(dbName, "default", messages);
	}
	
	public void logCommandReceived(String commandName, String author) {
		Point commands = Point.measurement("commands")
				.time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
				.addField("name", commandName)
				.addField("author", author)
				.build();
		influxDB.write(dbName, "default", commands);
	}

	public void logBGSActivity(long time, String userid, String username, String activity, int ammount) {
		Point bgs = Point.measurement("bgs")
				.time(time, TimeUnit.MILLISECONDS)
				.addField("activity", activity)
				.addField("userid", userid)
				.addField("username", username)
				.addField("ammount", ammount)
				.build();
		influxDB.write(dbName, "default", bgs);
	}
}
