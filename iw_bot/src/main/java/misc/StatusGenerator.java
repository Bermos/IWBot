package misc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import net.dv8tion.jda.managers.AccountManager;
import provider.Connections;

public class StatusGenerator extends Thread {
	private AccountManager accountManager;
	private Connection connect = new Connections().getConnection();
	
	public void run() {
		Thread.currentThread().setName("BOT - MISC - StatusGenerator");

		while(true) {
			try {
				PreparedStatement ps = connect.prepareStatement("SELECT word1 FROM markov WHERE char_length(word1) > 3 ORDER BY rand() LIMIT 3");
				ResultSet rs = ps.executeQuery();
				
				String newStatus = "";
				while (rs.next()) {
					newStatus += (rs.getString("word1") + " ");
				}
				
				accountManager.setGame(newStatus.trim());
				
				Thread.sleep(60*1000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public StatusGenerator (AccountManager accountManager) {
		this.accountManager = accountManager;
		
		this.start();
	}

}
