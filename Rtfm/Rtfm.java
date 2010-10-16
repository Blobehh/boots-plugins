import java.util.logging.Logger;

import net.minecraft.server.MinecraftServer;

public class Rtfm extends Plugin
{
	private String name = "Rtfm";
	private int version = 2;
	
	static final Logger log = Logger.getLogger("Minecraft");
	
	public void enable()
	{
	        log.info("RTFM Mod Enabled.");
	        etc.getInstance().addCommand("/rtfm", " - Read the F***ing Manual.");
	}

	public void disable()
	{
	}

	public void initialize()
	{
		RtfmListener listener = new RtfmListener();
		etc.getLoader().addListener(PluginLoader.Hook.COMMAND, listener, this, PluginListener.Priority.MEDIUM);
	}

	public class RtfmListener extends PluginListener
	{
		public boolean onCommand(Player player, String[] split) {
			if (split[0].equalsIgnoreCase("/rtfm")) {
	 			if (!player.isInGroup("rtfm")) {
					boolean newUser = false;
					String playername = player.getName();
					// update player's group
					player.addGroup("rtfm");	
	
			                if (!etc.getDataSource().doesPlayerExist(player.getName())) {
	       			             newUser = true;
			                }
	
			                if (newUser) {
	       			             etc.getDataSource().addPlayer(player);
			                } else {
	       			             etc.getDataSource().modifyPlayer(player);
			                }
	
					for  (Player p : etc.getServer().getPlayerList() ) {
						if (player == p)
							continue;
						p.sendMessage(Colors.Yellow + "Congratulations to "+playername+" for reading the manual!");
						if (!p.isInGroup("rtfm"))
							p.sendMessage(Colors.Yellow + "Maybe you should try reading /motd too!");
					}
				}
	
				player.sendMessage(Colors.Rose + "You can now build (with certain exceptions)!");
				player.sendMessage(Colors.Yellow + "Certain areas are protected from building (like spawn).");
				player.sendMessage(Colors.Rose + "Tell people to read /motd, do NOT tell them to /rtfm");
				player.sendMessage(Colors.Yellow + "Visit the web site in /motd for rules and pictures.");
				player.sendMessage(Colors.Rose + "Helpful commands: /help, /spawn, /kit, /listwarps, /who");
				return true;
			}
			return false;
		}
	}

}
