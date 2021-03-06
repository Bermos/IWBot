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
				PreparedStatement ps = connect.prepareStatement("SELECT word1, word3, word5 FROM markov WHERE char_length(word1) > 3 AND char_length(word3) > 3 AND char_length(word5) > 3 ORDER BY rand() LIMIT 1");
				ResultSet rs = ps.executeQuery();
				
				rs.next();
				String newStatus = rs.getString("word1") + " " + rs.getString("word3") + " " + rs.getString("word5");
				
				accountManager.setGame(newStatus.trim());
				
				Thread.sleep(5*60*1000);
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
