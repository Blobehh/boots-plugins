import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import net.minecraft.server.MinecraftServer;

public class Achievements extends Plugin
{
   private String name = "Achievements";
   private int version = 4;
   private boolean stopTimer = false;
   private String directory = "achievements";
   private String listLocation = "achievements.txt";
   private int delay = 300;
   private HashMap<String, AchievementListData> achievementList = new HashMap<String, AchievementListData>();
   private HashMap<String, HashSet<PlayerAchievementData>> playerAchievements = new HashMap<String, HashSet<PlayerAchievementData>>();
	private Plugin statsPlugin = null;
	
   static final Logger log = Logger.getLogger("Minecraft");

   private void startTimer()
	{
      stopTimer = false;
      final Timer timer = new Timer();
      timer.schedule(
            new TimerTask() {
               @Override
               	public void run() {
                  if (stopTimer) {
                     timer.cancel();
                     return;
                  }
                  checkStats();
               }
            }, 3000, delay*1000);
   }

   private void stopTimer()
	{
      stopTimer = true;
   }

   private void checkStats()
   {
		if (statsPlugin == null) {
			log.log(Level.SEVERE, name + ": no Stats plugin found.");
			return;
		}
	
      for  (Player p: etc.getServer().getPlayerList())
      {
         if (!playerAchievements.containsKey(p.getName())) // add player to achievement list
            loadPlayerAchievements(p.getName());
      
         for (String name2: achievementList.keySet())
         {
            AchievementListData ach = achievementList.get(name2);
				if (!ach.isEnabled()) // disabled, skip
					continue;

				int playerValue = statsPlugin.get(p.getName(), ach.getCategory(), ach.getKey());
          	if (playerValue < ach.getValue()) // doesn't meet requirements, skip
           		continue;

				PlayerAchievementData pad = null;
				
				for (PlayerAchievementData tpad: playerAchievements.get(p.getName())) {
					if (tpad.getName().equals(ach.getName())) {
						pad = tpad;
						break;
					}
				}

				//award achievement
				if (pad != null) {
					// already awarded
					if (pad.getCount() >= ach.getMaxawards())
						continue;

					if (pad.getCount() > 0 && playerValue < ((pad.getCount() + 1) * ach.getValue()))
						continue;
					
					pad.incrementCount();							
				} else {
					// not already found
          		playerAchievements.get(p.getName()).add(new PlayerAchievementData(ach.getName(), 1));
				}

            broadcast(Colors.LightBlue + "ACHIEVEMENT: " + p.getName() + " has been awarded " + ach.getName() + "!");
            p.sendMessage(Colors.LightBlue + "(" + ach.getDescription() + ")");
				
            savePlayerAchievements(p.getName());
				
				ach.commands.run(p);
         }
      }
   }

   private void saveAchievementList()
   {
      BufferedWriter writer = null;
      try {
         log.info("Saving " + listLocation);
         writer = new BufferedWriter(new	FileWriter(listLocation));
         writer.write("# Achievements");
         writer.newLine();
         writer.write("# Format is: enabled:name:maxawards:category:key:value:description[:commands]");
         writer.newLine();
         writer.write("# commands are optional, separated by semicolons (;), available commands: item group");
         writer.newLine();
         writer.write("# Example: 1:ClownPuncher:1:stats:armswing:1000:Awarded for punching the air 1000 times.[:item goldblock 1]");
         writer.newLine();
         for (String name: achievementList.keySet())
         {
            writer.write(achievementList.get(name).toString());
            writer.newLine();
         }
      } 
         catch (Exception e) {
            log.log(Level.SEVERE, "Exception while creating "	+ listLocation,	e);
         } 
      finally {
         try {
            if (writer != null) {
               writer.close();
            }
         }	
            catch	(IOException e) {
            }
      }
   }

   private void loadAchievementList()
   {
      achievementList = new HashMap<String, AchievementListData>();
      if (!new File(listLocation).exists())
      {
         saveAchievementList();
         return;
      }
      try {
         Scanner scanner =	new Scanner(new File(listLocation));
         while	(scanner.hasNextLine())
         {
            String line =	scanner.nextLine();
            if (line.startsWith("#") || line.equals(""))
               continue;
            String[] split = line.split(":");
            if (split.length < 7) {
               log.log(Level.SEVERE, "Malformed line (" + line + ") in " + listLocation);
               continue;
            }
				int enabled = Integer.parseInt(split[0]);
            int maxawards = Integer.parseInt(split[2]);
            int value = Integer.parseInt(split[5]);
				String commands = null;
				if (split.length == 8)
					commands = split[7];
            achievementList.put(split[1], new AchievementListData(enabled, split[1], maxawards, split[3], split[4], value, split[6], commands));
         }
         scanner.close();
      } 
         catch (Exception e) {
            log.log(Level.SEVERE, "Exception	while	reading " +	listLocation, e);
         }
      if (achievementList.isEmpty())
         disable();
   }

   private void savePlayerAchievements(String player)
   {
      String location = directory + "/" + player + ".txt";
      BufferedWriter writer = null;
      try {
         log.info("Saving " + location);
         writer = new BufferedWriter(new	FileWriter(location));
         writer.write("# " + location);
         writer.newLine();
         for (PlayerAchievementData pad: playerAchievements.get(player))
         {
            writer.write(pad.toString());
            writer.newLine();
         }
      } 
         catch (Exception e) {
            log.log(Level.SEVERE, "Exception while creating "	+ location,	e);
         } 
      finally {
         try {
            if (writer != null) {
               writer.close();
            }
         }	
            catch	(IOException e) {
            }
      }
   }

   private void loadPlayerAchievements(String player)
   {
      if (playerAchievements.containsKey(player))
         playerAchievements.remove(player);
      playerAchievements.put(player, new HashSet<PlayerAchievementData>());
   
      String location = directory + "/" + player + ".txt";
      if (!new File(location).exists())
         return;
      try {
         Scanner scanner =	new Scanner(new File(location));
         while	(scanner.hasNextLine())
         {
            String line =	scanner.nextLine();
            if (line.startsWith("#") || line.equals(""))
               continue;
            String[] split = line.split(":");
            if (split.length < 1) {
               log.log(Level.SEVERE, "Malformed line (" + line + ") in " + location);
               continue;
            }
            int count = 1;
            if (split.length >= 2)
               count = Integer.parseInt(split[1]);
            playerAchievements.get(player).add(new PlayerAchievementData(split[0], count));
         }
         scanner.close();
      } 
         catch (Exception e) {
            log.log(Level.SEVERE, "Exception	while	reading " +	location, e);
         }
   }

   private void broadcast(String message)
   {
      for (Player	p : etc.getServer().getPlayerList()) {
         if	(p	!=	null) {
            p.sendMessage(message);
         }
      }
   }
	
   public void enable()
   {
	   statsPlugin = etc.getLoader().getPlugin("Stats");
      if (statsPlugin == null)
      {
         log.log(Level.SEVERE, "Stats plugin not found, aborting load of " + name);
         return;
      }
		log.info(name + ": Found required plugin: " + statsPlugin.getName());

      PropertiesFile properties = new PropertiesFile("server.properties");
      try {
         directory = properties.getString("achievements-directory", "achievements");
         listLocation = properties.getString("achievements-list", "achievements.txt");
         delay = properties.getInt("achievements-delay", 300);
      } 
         catch (Exception e) {
            log.log(Level.SEVERE, "Exception while reading from server.properties", e);
         }
      etc.getInstance().addCommand("/achievements", " - Lists your achievements.");
      etc.getInstance().addCommand("/listachievements", " - Lists all achievements.");
      etc.getInstance().addCommand("/checkachievements", " - Checks achievements.");
      etc.getInstance().addCommand("/reloadachievements", " - Reloads achievements.");
      loadAchievementList();
      startTimer();
      log.info(name + " v" + version + " Mod Enabled.");
   }

   public void disable()
	{
      stopTimer();
      log.info(name + " v" + version + " Mod Disabled.");
   }
	
	public void initialize()
	{
		AchievementsListener listener = new AchievementsListener();
		etc.getLoader().addListener(PluginLoader.Hook.LOGIN, listener, this, PluginListener.Priority.LOW);
		etc.getLoader().addListener(PluginLoader.Hook.DISCONNECT, listener, this, PluginListener.Priority.LOW);
		etc.getLoader().addListener(PluginLoader.Hook.COMMAND, listener, this, PluginListener.Priority.LOW);
	}
	
	public class AchievementsListener extends PluginListener
	{
	   public void onLogin(Player player)
	   {
	      loadPlayerAchievements(player.getName());
	   }
	
	   public void onDisconnect(Player player)
	   {
	   }
	
	   public boolean onCommand(Player player, String[] split)
	   {
	      if (split[0].equalsIgnoreCase("/achievements") || split[0].equalsIgnoreCase("/ach")) {
	         if (playerAchievements.get(player.getName()).isEmpty())
	         {
	            player.sendMessage(Colors.Rose + "You have no achievements.");
	            return true;
	         }
	         for (PlayerAchievementData pad: playerAchievements.get(player.getName()))
	         {
	         	AchievementListData ach = achievementList.get(pad.getName());
					if (ach == null) {
						player.sendMessage(Colors.LightBlue + pad.getName() + " (OLD)");
						continue;
					}
	         	if (pad.getCount() > 1)
	         		player.sendMessage(Colors.LightBlue + pad.getName() + " (" + pad.getCount() + "): " + ach.getDescription());
	         	else
	         		player.sendMessage(Colors.LightBlue + pad.getName() + ": " + ach.getDescription());
	         }
	         return true;
	      }
			else if (split[0].equalsIgnoreCase("/listachievements")) {
				player.sendMessage(Colors.Rose + "Enabled Name Maxawards Category Key Value");
				for (String name: achievementList.keySet()) {
					AchievementListData ach = achievementList.get(name);
					player.sendMessage(Colors.LightBlue + ach.isEnabled() + " " + ach.getName() + " " + ach.getMaxawards() + " " + ach.getCategory() + " " + ach.getKey() + " " + ach.getValue());
				}
	         return true;
	      }
	      else if (split[0].equalsIgnoreCase("/checkachievements")) {
	         checkStats();
	         player.sendMessage(Colors.Rose + "Achievements updated.");
	         return true;
	      }
	      else if (split[0].equalsIgnoreCase("/reloadachievements")) {
	         loadAchievementList();
	         player.sendMessage(Colors.Rose + "Achievements reloaded.");
	         return true;
	      }
	      return false;
	   }
	}
}
