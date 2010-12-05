import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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

import net.minecraft.server.MinecraftServer;

public class Stats extends Plugin
{
	private String name = "Stats";
	private int version = 16;
	private PlayerMap playerStats = new PlayerMap();
	private boolean stopTimer = false;
	private String directory = "stats";
	private int savedelay = 30;
	private String[] ignoredGroups = new String[] {""};
	private final String defaultCategory = "stats";
	private Block lastface = null;

	static final Logger log	= Logger.getLogger("Minecraft");

	private void startTimer() {
		stopTimer =	false;
		final	Timer	timer	= new	Timer();
				  timer.schedule(new	TimerTask()	{
							@Override
							public void	run()	{
					if	(stopTimer)	{
						timer.cancel();
						return;
					}
					saveAll();
				}
			},	3000,	savedelay*1000);
	}

	private void stopTimer() {
		stopTimer =	true;
	}

	public void enable()
	{
		PropertiesFile properties	= new	PropertiesFile("server.properties");
		try {
			directory = properties.getString("stats-directory", "stats");
			String s = properties.getString("stats-ignored-groups", "default");
			ignoredGroups = s.split(",");
			savedelay = properties.getInt("stats-save-delay", 30);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Exception	while	reading from server.properties",	e);
		}
		startTimer();
	 	try {
			new File(directory).mkdir();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Exception while creating directory " + directory, e);
		}
		
      for  (Player p: etc.getServer().getPlayerList())
			load(p);

		log.info(name + " v" + version + " Plugin Enabled.");
	}

	public void disable()
	{
		stopTimer();
		saveAll();
		playerStats = new PlayerMap();
		log.info(name + " v" + version + " Plugin Disabled.");
	}

	public void initialize()
	{
		log.info(name + " initializing.");
		
		new VersionCheck(name, version);
	
		StatsListener listener = new StatsListener();
		etc.getLoader().addListener(PluginLoader.Hook.LOGIN, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.CHAT, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.COMMAND, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.BAN, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.IPBAN, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.KICK, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.BLOCK_PLACE, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.BLOCK_BROKEN, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.BLOCK_RIGHTCLICKED, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.DISCONNECT, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.PLAYER_MOVE, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.ARM_SWING, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.ITEM_DROP, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.ITEM_PICK_UP, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.ITEM_USE, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.TELEPORT, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.HEALTH_CHANGE, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.IGNITE, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.VEHICLE_ENTERED, listener, this, PluginListener.Priority.MEDIUM);
		
		try {
			etc.getLoader().addCustomListener(new StatsGet());
			etc.getLoader().addCustomListener(new StatsSet());
		} catch (NoClassDefFoundError ex) {
			log.info(name + "no class def");
		}
	}

	private void updateStat(Player player, String statType)
	{
		updateStat(player, statType, 1);
	}

	private void updateStat(Player player, String statType, int num)
	{
		updateStat(player.getName(), defaultCategory, statType, num);
	}
	
	private void updateStat(Player player, String statType, Block block)
	{
		updateStat(player, statType, block, 1);
	}
		
	private void updateStat(Player player, String statType, Block block, int num)
	{
	   if (block.getType() <= 0 || (block.getX() ==0 && block.getY() == 0 && block.getZ() == 0))
			return;
		String blockName = etc.getDataSource().getItem(block.getType());
		updateStat(player.getName(), statType, blockName, num);
	}

	private void updateStat(Player player, String statType, Item item)
	{
		String itemName = etc.getDataSource().getItem(item.getItemId());
		int amount = item.getAmount();
		if (amount == 0)
			amount = 1;
		updateStat(player.getName(), statType, itemName, amount);
	}

	private void updateStat(Player player, String category, String key, int val)
	{
		updateStat(player.getName(), category, key, val);
	}

	// primary updateStat
	private void updateStat(String player, String category, String key, int val)
	{
		int oldval = playerStats.get(player, category, key);
		if (oldval == -1)
			oldval = 0;
		playerStats.put(player, category, key, oldval + val);
	}

	private void setStat(String player, String category, String key, int val)
	{
		playerStats.put(player, category, key, val);
	}

	public int get(String player, String category, String key)
	{
		return playerStats.get(player, category, key);
	}

	private void load(Player player)
	{
		if (inIgnoredGroup(player))
			return;
		playerStats.load(directory, player.getName());
	}

	private void unload(Player player)
	{
		if (inIgnoredGroup(player))
			return;
		playerStats.unload(directory, player.getName());
	}

	private void saveAll()
	{
		int count = 0;
		for (Player p: etc.getServer().getPlayerList())
			if (!inIgnoredGroup(p))
			{
				playerStats.save(directory, p.getName());
				count++;
			}
//		log.info("Saved " + count + "/" + playerStats.size() + " stat files...");
	}
	
	private boolean inIgnoredGroup(Player player)
	{
		for (String ignored: ignoredGroups) {
			for (String group: player.getGroups()) {
				if (ignored.equalsIgnoreCase(group))
					return true;
			}
		}
		return false;
	}
	
	// custom listener class
	public class StatsGet implements PluginInterface
	{
		public String getName() { return "get stat"; }
		public int getNumParameters() { return 3; }

		public String checkParameters(Object[] parameters)
		{
			boolean good;

			if (parameters.length != getNumParameters())
				return getName() + ": incorrect number of parameters, got: " + parameters.length + " expected: " + getNumParameters();
			
			good = false;
			if (parameters[0] instanceof String)
				good = true;
			if (!good)
				return getName() + ": parameter 0 should be String";
			good = false;
			if (parameters[1] instanceof String)
				good = true;
			if (!good)
				return getName() + ": parameter 1 should be String";
			good = false;
			if (parameters[2] instanceof String)
				good = true;
			if (!good)
				return getName() + ": parameter 2 should be String";
				
			return null;
		}

		public Object run(Object[] parameters)
		{
			String name = (String)parameters[0];
			String category = (String)parameters[1];
			String key = (String)parameters[2];
			
			int data = get(name, category, key);
			return data;
		}
	}
	public class StatsSet implements PluginInterface
	{
		public String getName() { return "set stat"; }
		public int getNumParameters() { return 4; }

		public String checkParameters(Object[] parameters)
		{
			boolean good;

			if (parameters.length != getNumParameters())
				return getName() + ": incorrect number of parameters, got: " + parameters.length + " expected: " + getNumParameters();
			
			good = false;
			if (parameters[0] instanceof String)
				good = true;
			if (!good)
				return getName() + ": parameter 0 should be String";
			good = false;
			if (parameters[1] instanceof String)
				good = true;
			if (!good)
				return getName() + ": parameter 1 should be String";
			good = false;
			if (parameters[2] instanceof String)
				good = true;
			if (!good)
				return getName() + ": parameter 2 should be String";
			good = false;
			if (parameters[3] instanceof Integer)
				good = true;
			if (!good)
				return getName() + ": parameter 3 should be Integer";
				
			return null;
		}

		public Object run(Object[] parameters)
		{
			String name = (String)parameters[0];
			String category = (String)parameters[1];
			String key = (String)parameters[2];
			Integer value = (Integer)parameters[3];
			
			updateStat(name, category, key, value);
			return true;
		}
	}
	// Listener Class
	public class StatsListener extends PluginListener
	{
	   public void onPlayerMove(Player player, Location from, Location to)
		{
			updateStat(player, "move");
		}
	
		public void onLogin(Player player)
		{
			load(player);
			// TODO: rate limit to prevent abuse
			updateStat(player, "login");
			setStat(player.getName(), defaultCategory, "lastlogin", (int)(System.currentTimeMillis()/1000));
		}
	
		public void onDisconnect(Player player)
		{
			int now = (int)(System.currentTimeMillis()/1000L);
			setStat(player.getName(), defaultCategory, "lastlogout", now);

			int loginTime = get(player.getName(), defaultCategory, "lastlogin");
			if (loginTime > 0 && now > loginTime)
				updateStat(player, "playedfor", now - loginTime);

			unload(player);
		}
	
		public boolean onChat(Player player, String message)
		{
			updateStat(player, "chat");
			updateStat(player, "chatletters", message.length());
			return false;
		}
		
		public boolean onCommand(Player player, String[] split)
		{
			updateStat(player, "command");
			return false;
		}
	
		public void onBan(Player player, String reason)
		{
			updateStat(player, "ban");
		}
	
		public void onIpBan(Player player, String reason)
		{
			updateStat(player, "ipban");
		}
		
		public void onKick(Player player, String reason)
		{
			updateStat(player, "kick");
		}

		public void onBlockRightClicked(Player player, Block blockClicked, Item item)
		{
			lastface = blockClicked.getFace(blockClicked.getFaceClicked());
		}

		public boolean onBlockPlace(Player player, Block blockPlaced, Block blockClicked, Item itemInHand)
		{
			updateStat(player, "blockcreate", blockPlaced);
			updateStat(player, "totalblockcreate");
			if (lastface != null)
			{
				updateStat(player, "totalblockdestroy");
				updateStat(player, "blockdestroy", lastface);
			}
			return false;
		}
		
		public boolean onBlockBreak(Player player, Block block)
		{
			updateStat(player, "blockdestroy", block);
			updateStat(player, "totalblockdestroy");
			return false;
		}
		
		public void onArmSwing(Player player)
		{
			updateStat(player, "armswing");
		}

		public boolean onTeleport(Player player, Location from, Location to)
		{
			updateStat(player, "teleport");
			return false;
		}
		
		public boolean onItemDrop(Player player, Item item)
		{
			updateStat(player, "itemdrop", item);
			return false;
		}

		public boolean onItemPickUp(Player player, Item item)
		{
			updateStat(player, "itempickup", item);
			return false;
		}

		public boolean onHealthChange(Player player, int oldValue, int newValue)
		{
			int change = newValue - oldValue;
			if (change < 0) {
				int damagetaken = get(player.getName(), defaultCategory, "damagetaken");
				damagetaken += (change * -1);
				updateStat(player, "damagetaken", damagetaken);
			}
			else if (change > 0)
			{
				int damagehealed = get(player.getName(), defaultCategory, "damagehealed");
				damagehealed += change;
				updateStat(player, "damagehealed", damagehealed);
			}

			return false;
		}
		
		public boolean onItemUse(Player player, Item item)
		{
			updateStat(player, "itemuse", item);
			return false;
		}
		
		public boolean onIgnite(Block block, Player player)
		{
			if (player == null)
				return false;
			if (block.getStatus() != 2)
				return false;
			updateStat(player, "lighter");
			return false;
		}

		public void onVehicleEnter(BaseVehicle vehicle, HumanEntity player)
		{
			Player p = vehicle.getPassenger();
			if (p == null)
				return;

			String vehicleName = etc.getDataSource().getItem(vehicle.getId());

			updateStat(p, "vehicleenter", vehicleName, 1);
		}
	}
	// End Listener Class
	
	private class AgeBlock {
		public Block block;
		public long time;
		
		AgeBlock(Block block) {
			this.block = block;
			this.time = System.currentTimeMillis();
		}
		
		public boolean isOld(long now) {
			long delta = 1000 * 60; // 1 minute
			if (now > time + delta)
				return true;
			return false;
		}
	}
}
