package com.turbogrimoire.purelysatanic.TimcraftRegionModule.Commands;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.turbogrimoire.purelysatanic.TimcraftRegionModule.RegionModulePlugin;
import com.turbogrimoire.purelysatanic.TimcraftUnifiedMessaging.Handlers.MessageHandler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ExpandCommand
  implements CommandExecutor
{
  private final RegionModulePlugin plugin;
  
  static enum Direction
  {
    NORTH,  EAST,  SOUTH,  WEST,  NORTHEAST,  NORTHWEST,  SOUTHEAST,  SOUTHWEST,  ALL;
    
    private Direction() {}
  }
  
  public ExpandCommand(RegionModulePlugin plugin)
  {
    this.plugin = plugin;
  }
  
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
  {
    if (!(sender instanceof Player))
    {
      MessageHandler.SendConsoleMessage(Level.WARNING, this.plugin, "This command cannot be used from console.");
      return true;
    }
    Player player = (Player)sender;
    if (args.length == 0) {
      return false;
    }
    if (!this.plugin.hasPermission(player, "TimcraftRegionModule.Expand"))
    {
      MessageHandler.SendErrorMessage(Level.WARNING, this.plugin, player, "Sorry, you don't have permission to use this command.");
      
      return true;
    }
    return expandCommand(player, args);
  }
  
  private boolean expandCommand(Player player, String[] args)
  {
    if ((args.length < 3) || (args.length > 4)) {
      return false;
    }
    boolean calculateCost = false;
    if ((args.length == 4) && (args[3].equalsIgnoreCase("-c"))) {
      calculateCost = true;
    }
    String cleaned = args[0].replaceAll("[^\\p{L}\\p{N}]", "");
    if (!this.plugin.getWorldGuard().getRegionManager(player.getWorld()).hasRegion(cleaned))
    {
      MessageHandler.SendErrorMessage(Level.WARNING, this.plugin, player, "Sorry, a region doesn't exist with that name.");
      
      return true;
    }
    int expandBy = -1;
    try
    {
      expandBy = Integer.parseInt(args[1]);
    }
    catch (NumberFormatException e)
    {
      MessageHandler.SendErrorMessage(Level.WARNING, this.plugin, player, "Sorry, you did not enter an integer to expand by.");
      
      return true;
    }
    if (expandBy <= 0)
    {
      MessageHandler.SendErrorMessage(Level.WARNING, this.plugin, player, "Sorry, you must enter an integer that is bigger than 0 to expand by.");
      
      return true;
    }
    Direction direction = null;
    for (int i = 0; i < Direction.values().length; i++) {
      if (Direction.values()[i].name().equalsIgnoreCase(args[2].toUpperCase())) {
        direction = Direction.values()[i];
      }
    }
    if (direction == null)
    {
      MessageHandler.SendErrorMessage(Level.WARNING, this.plugin, player, "Sorry, applicable directions are: North,East,South,West,NorthEast,NorthWest,SouthEast,SouthWest,All");
      


      return true;
    }
    ProtectedRegion region = this.plugin.getWorldGuard().getRegionManager(player.getWorld()).getRegion(cleaned);
    if (!region.isOwner(player.getName()))
    {
      MessageHandler.SendErrorMessage(Level.WARNING, this.plugin, player, "Sorry, you are not the owner of this protection.");
      
      return true;
    }
    ProtectedRegion parent = region.getParent();
    if ((parent != null) && (!parent.isOwner(player.getName())))
    {
      MessageHandler.SendErrorMessage(Level.WARNING, this.plugin, player, "Sorry, you are not the owner of the parent protection.");
      
      return true;
    }
    ApplicableRegionSet associated = this.plugin.getWorldGuard().getRegionManager(player.getWorld()).getApplicableRegions(region);
    

    Vector minimum = region.getMinimumPoint();
    Vector maximum = region.getMaximumPoint();
    if (direction.equals(Direction.NORTH))
    {
      minimum = minimum.subtract(0, 0, expandBy);
    }
    else if (direction.equals(Direction.SOUTH))
    {
      maximum = maximum.add(0, 0, expandBy);
    }
    else if (direction.equals(Direction.EAST))
    {
      maximum = maximum.add(expandBy, 0, 0);
    }
    else if (direction.equals(Direction.WEST))
    {
      minimum = minimum.subtract(expandBy, 0, 0);
    }
    else if (direction.equals(Direction.NORTHEAST))
    {
      minimum = minimum.subtract(0, 0, expandBy);
      maximum = maximum.add(expandBy, 0, 0);
    }
    else if (direction.equals(Direction.NORTHWEST))
    {
      minimum = minimum.subtract(0, 0, expandBy);
      minimum = minimum.subtract(expandBy, 0, 0);
    }
    else if (direction.equals(Direction.SOUTHEAST))
    {
      maximum = maximum.add(0, 0, expandBy);
      maximum = maximum.add(expandBy, 0, 0);
    }
    else if (direction.equals(Direction.SOUTHWEST))
    {
      maximum = maximum.add(0, 0, expandBy);
      minimum = minimum.subtract(expandBy, 0, 0);
    }
    else if (direction.equals(Direction.ALL))
    {
      minimum = minimum.subtract(0, 0, expandBy);
      maximum = maximum.add(0, 0, expandBy);
      maximum = maximum.add(expandBy, 0, 0);
      minimum = minimum.subtract(expandBy, 0, 0);
    }
    ProtectedRegion expanded = new ProtectedCuboidRegion(region.getId(), minimum.toBlockVector(), maximum.toBlockVector());
    

    expanded.setFlags(region.getFlags());
    expanded.setOwners(region.getOwners());
    expanded.setMembers(region.getMembers());
    try
    {
      expanded.setParent(region.getParent());
    }
    catch (ProtectedRegion.CircularInheritanceException e1)
    {
      MessageHandler.SendErrorMessage(Level.SEVERE, this.plugin, player, "Sorry, something went wrong while setting the region's parent, please contact your Administrator.");
      
      return true;
    }
    expanded.setPriority(region.getPriority());
    
    int oldArea = region.volume() / (region.getMaximumPoint().getBlockY() - region.getMinimumPoint().getBlockY() + 1);
    

    int newArea = expanded.volume() / (expanded.getMaximumPoint().getBlockY() - expanded.getMinimumPoint().getBlockY() + 1);
    


    int difference = newArea - oldArea;
    
    double cost = difference * (parent == null ? this.plugin.getConfig().getDouble("Protection.PricePerBlock") : this.plugin.getConfig().getDouble("ChildProtection.PricePerBlock"));
    if (calculateCost)
    {
      MessageHandler.SendMessage(player, "You must have " + this.plugin.economy.format(cost) + " to expand this protection.");
      
      return true;
    }
    if (!this.plugin.economy.has(player, cost))
    {
      MessageHandler.SendErrorMessage(Level.WARNING, this.plugin, player, "Sorry, you must have " + this.plugin.economy.format(cost) + " to expand this protection.");
      

      return true;
    }
    ApplicableRegionSet newAssociated = this.plugin.getWorldGuard().getRegionManager(player.getWorld()).getApplicableRegions(expanded);
    if (parent != null)
    {
      ApplicableRegionSet pS1 = this.plugin.getWorldGuard().getRegionManager(player.getWorld()).getApplicableRegions(expanded.getMinimumPoint().toVector2D().toVector());
      


      ApplicableRegionSet pS2 = this.plugin.getWorldGuard().getRegionManager(player.getWorld()).getApplicableRegions(expanded.getMaximumPoint().toVector2D().toVector());
      



      boolean existsWithinParent = false;
      Iterator localIterator2;
      for (Iterator localIterator1 = pS1.iterator(); localIterator1.hasNext(); localIterator2.hasNext())
      {
        ProtectedRegion p1 = (ProtectedRegion)localIterator1.next();
        localIterator2 = pS2.iterator();
        ProtectedRegion p2 = (ProtectedRegion)localIterator2.next();
        if ((p1.getId().equals(parent.getId())) && (p2.getId().equals(parent.getId()))) {
          existsWithinParent = true;
        }
      }
      if (!existsWithinParent)
      {
        MessageHandler.SendErrorMessage(Level.WARNING, this.plugin, player, "Sorry, you cannot expand a child lot outside of it's parent protection.");
        
        return true;
      }
    }
    List<String> failedCause = new ArrayList();
    boolean failed = false;
    for (ProtectedRegion r1 : newAssociated)
    {
      boolean found = false;
      for (ProtectedRegion r2 : associated) {
        if (r1.getId().equals(r2.getId())) {
          found = true;
        }
      }
      if (!found)
      {
        failedCause.add(r1.getId());
        failed = true;
      }
    }
    if (failed)
    {
      String message = "Sorry, this region could not be expanded because it would overlap: ";
      for (int i = 0; i < failedCause.size(); i++)
      {
        message = message + (String)failedCause.get(i);
        if (i != failedCause.size() - 1) {
          message = message + ",";
        }
      }
      MessageHandler.SendErrorMessage(Level.WARNING, this.plugin, player, message);
      return true;
    }
    this.plugin.getWorldGuard().getRegionManager(player.getWorld()).addRegion(expanded);
    try
    {
      this.plugin.getWorldGuard().getRegionManager(player.getWorld()).save();
    }
    catch (Exception e)
    {
      MessageHandler.SendErrorMessage(Level.SEVERE, this.plugin, player, "Sorry, something went wrong when saving the region file, please contact your Administrator.");
      
      return true;
    }
    this.plugin.economy.withdrawPlayer(player, cost);
    
    MessageHandler.SendMessage(player, "You have expanded the protection: " + region.getId() + "! " + this.plugin.economy.format(cost) + " has been removed from your account.");
    

    return true;
  }
}
