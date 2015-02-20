package com.turbogrimoire.purelysatanic.TimcraftRegionModule.Listeners;

import com.turbogrimoire.purelysatanic.TimcraftRegionModule.Handlers.ProtectionHandler;
import com.turbogrimoire.purelysatanic.TimcraftRegionModule.Protection;
import com.turbogrimoire.purelysatanic.TimcraftRegionModule.RegionModulePlugin;
import java.util.List;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerListener
  implements Listener
{
  private final RegionModulePlugin plugin;
  
  public PlayerListener(RegionModulePlugin plugin)
  {
    this.plugin = plugin;
  }
  
  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent e)
  {
    Player player = e.getPlayer();
    if (this.plugin.hasPermission(player, "TimcraftRegionModule.Lightning")) {
      player.getWorld().strikeLightningEffect(player.getLocation());
    }
    List<Protection> protections = ProtectionHandler.GetProtectionsByPlayer(player.getName());
    for (Protection protection : protections) {
      if (protection.isInspected()) {
        this.plugin.notifyPlayerOfProtectionInspection(player, protection);
      }
    }
  }
  
  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent e)
  {
    if ((!e.isCancelled()) && (e.getAction().equals(Action.RIGHT_CLICK_BLOCK)))
    {
      Block block = e.getClickedBlock();
      if (block.getType().equals(Material.STONE_BUTTON))
      {
        Sign sign = null;
        
        Block sign1 = block.getRelative(2, 0, 0);
        Block sign2 = block.getRelative(-2, 0, 0);
        Block sign3 = block.getRelative(0, 0, 2);
        Block sign4 = block.getRelative(0, 0, -2);
        for (Block s : new Block[] { sign1, sign2, sign3, sign4 }) {
          if ((s.getState() instanceof Sign)) {
            sign = (Sign)s.getState();
          }
        }
        if ((sign != null) && (sign.getLine(1).equalsIgnoreCase("[SignRank]")) && (!sign.getLine(2).equals("")))
        {
          Player ply = e.getPlayer();
          if (this.plugin.permission.getPrimaryGroup(ply).equalsIgnoreCase("Beginner"))
          {
            this.plugin.permission.playerAddGroup(ply, "User");
            this.plugin.permission.playerRemoveGroup(ply, "Beginner");
          }
        }
      }
    }
  }
}
