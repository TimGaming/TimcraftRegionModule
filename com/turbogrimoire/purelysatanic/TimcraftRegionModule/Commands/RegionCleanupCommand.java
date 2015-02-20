package com.turbogrimoire.purelysatanic.TimcraftRegionModule.Commands;

import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.turbogrimoire.purelysatanic.TimcraftRegionModule.RegionModulePlugin;
import com.turbogrimoire.purelysatanic.TimcraftUnifiedMessaging.Handlers.MessageHandler;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RegionCleanupCommand
  implements CommandExecutor
{
  private final RegionModulePlugin plugin;
  
  public RegionCleanupCommand(RegionModulePlugin plugin)
  {
    this.plugin = plugin;
  }
  
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
  {
    Date playedByDate = new Date(System.currentTimeMillis() - 5184000000L);
    Player player = (sender instanceof Player) ? (Player)sender : null;
    RegionManager regionManager = player != null ? this.plugin.getWorldGuard().getRegionManager(player.getWorld()) : null;
    

    Iterator<Map.Entry<String, ProtectedRegion>> regions = regionManager != null ? regionManager.getRegions().entrySet().iterator() : null;
    


    int playersRemoved = 0;
    int regionsRemoved = 0;
    if ((player == null) || (regionManager == null) || (regions == null))
    {
      MessageHandler.SendErrorMessage(sender, "Sorry, you must be a player to use this command.");
      
      return true;
    }
    if (!this.plugin.hasPermission(player, "RegionModule.RegionCleanup"))
    {
      MessageHandler.SendErrorMessage(sender, "Sorry, you don't have permission to do this.");
      return true;
    }
    while (regions.hasNext())
    {
      Map.Entry<String, ProtectedRegion> entry = (Map.Entry)regions.next();
      ProtectedRegion region = (ProtectedRegion)entry.getValue();
      Iterator<String> owners = region.getOwners().getPlayers().iterator();
      Iterator<String> members = region.getMembers().getPlayers().iterator();
      boolean removedPlayer = false;
      if (region.getParent() != null) {}
      while (owners.hasNext())
      {
        String owner = (String)owners.next();
        OfflinePlayer offlinePlayer = this.plugin.getServer().getOfflinePlayer(owner);
        if ((!owner.equalsIgnoreCase("SERVER")) && (offlinePlayer != null))
        {
          Date lastPlayedDate = new Date(offlinePlayer.getLastPlayed());
          if (!lastPlayedDate.after(playedByDate))
          {
            owners.remove();
            playersRemoved++;
            removedPlayer = true;
          }
        }
      }
      while (members.hasNext())
      {
        String member = (String)members.next();
        OfflinePlayer offlinePlayer = this.plugin.getServer().getOfflinePlayer(member);
        if ((!member.equalsIgnoreCase("SERVER")) && (offlinePlayer != null))
        {
          Date lastPlayedDate = new Date(offlinePlayer.getLastPlayed());
          if (!lastPlayedDate.after(playedByDate))
          {
            members.remove();
            playersRemoved++;
            removedPlayer = true;
          }
        }
      }
      if ((removedPlayer) && 
        (region.getOwners().size() == 0) && (region.getMembers().getPlayers().size() == 0))
      {
        regions.remove();
        regionsRemoved++;
        MessageHandler.SendConsoleMessage(Level.INFO, this.plugin, "Region: " + region.getId() + " has been removed.");
      }
    }
    try
    {
      regionManager.save();
    }
    catch (Exception e)
    {
      MessageHandler.SendErrorMessage(sender, "Could not save regions to file.");
      return true;
    }
    MessageHandler.SendMessage(sender, "{HL}Cleanup complete. Players removed: {IT}" + playersRemoved + " {HL}Regions removed: {IT}" + regionsRemoved);
    

    return true;
  }
}
