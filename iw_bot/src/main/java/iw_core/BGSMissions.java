package iw_core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.entities.User;

import provider.Connections;

public class BGSMissions {

	public static int getNumberOfMissions(String id) {
		int no = 0;
		Connection connect = new Connections().getConnection();
		try {
			PreparedStatement ps = connect.prepareStatement("SELECT SUM(no) AS total FROM bgs_mission WHERE userid = ?");
			ps.setString(1, id);
			ResultSet rs = ps.executeQuery();
			
			if (rs.next())
				no = rs.getInt("total");
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return no;
	}

	public static List<String> getBoard() {
		List<String> board = new ArrayList<String>();
		Connection connect = new Connections().getConnection();
		try {
			PreparedStatement ps = connect.prepareStatement("SELECT username, SUM(no) AS total FROM bgs_mission GROUP BY userid ORDER BY total DESC LIMIT 5");
			ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
				String entry = rs.getString("username") + ": " + rs.getInt("total");
				board.add(entry);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return board;
	}

	public static void addMissions(User author, int no) {
		Connection connect = new Connections().getConnection();
		try {
			PreparedStatement ps = connect.prepareStatement("INSERT INTO bgs_mission(username, userid, no) VALUES (?, ?, ?)");
			ps.setString(1, author.getUsername());
			ps.setString(2, author.getId());
			ps.setInt(3, no);
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static int getTotalCompleted() {
		int total = 0;

		Connection connect = new Connections().getConnection();
		try {
			PreparedStatement ps = connect.prepareStatement("SELECT SUM(no) AS total FROM bgs_mission");
			ResultSet rs = ps.executeQuery();
			
			if (rs.next()) 
				total = rs.getInt("total");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return total;
	}

	public static int getTotalParticipants() {
		int total = 0;

		Connection connect = new Connections().getConnection();
		try {
			PreparedStatement ps = connect.prepareStatement("SELECT COUNT(DISTINCT userid) AS total FROM bgs_mission");
			ResultSet rs = ps.executeQuery();
			
			if (rs.next()) 
				total = rs.getInt("total");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return total;
	}

}
