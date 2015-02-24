package com.turbogrimoire.purelysatanic.TimcraftRegionModule.Commands;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.Selection;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion.CircularInheritanceException;
import com.turbogrimoire.purelysatanic.TimcraftRegionModule.Handlers.ProtectionHandler;
import com.turbogrimoire.purelysatanic.TimcraftRegionModule.Protection;
import com.turbogrimoire.purelysatanic.TimcraftRegionModule.RegionModulePlugin;
import com.turbogrimoire.purelysatanic.TimcraftUnifiedMessaging.Handlers.MessageHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class ProtectionCommand
  implements CommandExecutor
{
  private final RegionModulePlugin plugin;
  
  public ProtectionCommand(RegionModulePlugin plugin)
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
    if (args[0].equalsIgnoreCase("list"))
    {
      if (!this.plugin.hasPermission(player, "TimcraftRegionModule.List"))
      {
        MessageHandler.SendErrorMessage(Level.WARNING, this.plugin, player, "Sorry, you don't have permission to use this command.");
        
        return true;
      }
      return listCommand(player, args);
    }
    if (args[0].equalsIgnoreCase("tp"))
    {
      if (!this.plugin.hasPermission(player, "TimcraftRegionModule.Teleport"))
      {
        MessageHandler.SendErrorMessage(Level.WARNING, this.plugin, player, "Sorry, you don't have permission to use this command.");
        
        return true;
      }
      return teleportCommand(player, args);
    }
    if ((args[0].equalsIgnoreCase("accept")) || (args[0].equalsIgnoreCase("a")))
    {
      if (!this.plugin.hasPermission(player, "TimcraftRegionModule.Moderate"))
      {
        MessageHandler.SendErrorMessage(Level.WARNING, this.plugin, player, "Sorry, you don't have permission to use this command.");
        
        return true;
      }
      return acceptCommand(player, args);
    }
    if ((args[0].equalsIgnoreCase("deny")) || (args[0].equalsIgnoreCase("d")))
    {
      if (!this.plugin.hasPermission(player, "TimcraftRegionModule.Moderate"))
      {
        MessageHandler.SendErrorMessage(Level.WARNING, this.plugin, player, "Sorry, you don't have permission to use this command.");
        
        return true;
      }
      return denyCommand(player, args);
    }
    if (!this.plugin.hasPermission(player, "TimcraftRegionModule.Protect"))
    {
      MessageHandler.SendErrorMessage(Level.WARNING, this.plugin, player, "Sorry, you don't have permission to use this command.");
      
      return true;
    }
    return delegateProtectionCommand(player, args);
  }
  
  private boolean delegateProtectionCommand(Player player, String[] args)
  {
    if ((args.length < 1) || (args.length > 2))
    {
      MessageHandler.SendErrorMessage(Level.WARNING, this.plugin, player, "Invalid command syntax.");
      
      return false;
    }
    String cleaned = args[0].replaceAll("[^\\p{L}\\p{N}]", "");
    
    //Detects whether -c is tacked on the end, and whether it should calculate the region cost.
    
    boolean calculateCost = false;
    if ((args.length == 2) && (args[1].equalsIgnoreCase("-c"))) {
      calculateCost = true;
    }
    
    
    //See whether the region name is an integer - if it is, error, if not...
    try
    {
      Integer.parseInt(cleaned);
      MessageHandler.SendErrorMessage(Level.WARNING, this.plugin, player, "Sorry, your protection cannot be an integer.");
      
      return true;
    }
    catch (NumberFormatException localNumberFormatException)
    {
      Selection selection = this.plugin.getWorldEdit().getSelection(player);
      if (selection == null) //No selection has been made, error
      {
        MessageHandler.SendErrorMessage(Level.WARNING, this.plugin, player, "Sorry, you have you make a WorldEdit selection first.");
        
        return true;
      }
      if (this.plugin.getWorldGuard().getRegionManager(selection.getWorld()).hasRegion(cleaned)) //Or if a region with that name already exists...
      {
        MessageHandler.SendErrorMessage(Level.WARNING, this.plugin, player, "Sorry, a region already exists with that name.");
        
        return true;
      }
      //If none of the above, make a region with the specified name, and the selection. Not saved to disk yet. Has to do other checks on it.
      ProtectedRegion region = new ProtectedCuboidRegion(cleaned, selection.getNativeMinimumPoint().toBlockVector(), selection.getNativeMaximumPoint().toBlockVector());
      




      ApplicableRegionSet regions = this.plugin.getWorldGuard().getRegionManager(selection.getWorld()).getApplicableRegions(region);
      if (regions.size() == 0) { //If there are no overlapping protections, protect as per normal 
        return protection(player, selection, region, calculateCost);
      }
      return childProtection(player, selection, region, regions, calculateCost); //Overlapping protection? No worries! Just run the childlot routine!
    }
  }
  
  private boolean protection(Player player, Selection selection, ProtectedRegion region, boolean calculateCost)
  {
    
	//If the calculateCost flag has been previously set, list the price, then exit  
	double cost = selection.getWidth() * selection.getLength() * this.plugin.getConfig().getDouble("Protection.PricePerBlock");
    if (calculateCost) 
    {
      MessageHandler.SendMessage(player, "You must have " + this.plugin.economy.format(cost) + " to create this protection.");
      
      return true;
    }
    
    //Check whether the player has used up their maximum awaiting protections they can have
    int max = this.plugin.getConfig().getInt("Protection.MaximumAwaitingProtections");
    if (ProtectionHandler.GetProtectionsByPlayer(player.getName()).size() == max) //If they have, error and return
    {
      MessageHandler.SendErrorMessage(Level.WARNING, this.plugin, player, "Sorry, you have reached the max allowed awaiting protections.");
      
      return true;
    }
    
    //Do they have enough money? If not, error and return
    if (!this.plugin.economy.has(player.getName(), cost)) 
    {
      MessageHandler.SendErrorMessage(Level.WARNING, this.plugin, player, "Sorry, you must have " + this.plugin.economy.format(cost) + " to create this protection.");
      return true;
    }
    
    //Prot big enough?
    if (selection.getWidth() < this.plugin.getConfig().getInt("Protection.MinimumXLength"))
    {
      MessageHandler.SendErrorMessage(Level.WARNING, this.plugin, player, "Sorry, your selection's width (X axis) must be at least: " + this.plugin.getConfig().getInt("Protection.MinimumXLength") + ".");
      return true;
    }
    if (selection.getLength() < this.plugin.getConfig().getInt("Protection.MinimumZLength"))
    {
      MessageHandler.SendErrorMessage(Level.WARNING, this.plugin, player, "Sorry, your selection's length (Z axis) must be at least: " + this.plugin.getConfig().getInt("Protection.MinimumZLength") + ".");
      return true;
    }
    
    //Because you are a normal prot, redefine the min and max points to include from y0 to y256
    Vector minimum = new Vector(selection.getNativeMinimumPoint().getBlockX(), this.plugin.getConfig().getInt("Protection.LowerYValue"), selection.getNativeMinimumPoint().getBlockZ());
    Vector maximum = new Vector(selection.getNativeMaximumPoint().getBlockX(), this.plugin.getConfig().getInt("Protection.UpperYValue"), selection.getNativeMaximumPoint().getBlockZ());
    region = new ProtectedCuboidRegion(region.getId(), minimum.toBlockVector(), maximum.toBlockVector());
    

    ApplicableRegionSet regions = this.plugin.getWorldGuard().getRegionManager(selection.getWorld()).getApplicableRegions(region);
    if (regions.size() != 0)
    {
      System.out.println("Parent one");
      String message = "Sorry, your selection currently overlaps the following protections: ";
      
      Iterator<ProtectedRegion> iterator = regions.iterator();
      while (iterator.hasNext())
      {
        message = message + iterator.next();
        if (iterator.hasNext()) {
          message = message + ",";
        }
      }
      MessageHandler.SendErrorMessage(Level.WARNING, this.plugin, player, message);
      return true;
    }
    DefaultDomain domain = new DefaultDomain();
    domain.addPlayer(player.getUniqueId());
    
    region.setOwners(domain);
    
    this.plugin.getWorldGuard().getRegionManager(selection.getWorld()).addRegion(region);
    try
    {
      this.plugin.getWorldGuard().getRegionManager(selection.getWorld()).save();
    }
    catch (Exception e)
    {
      MessageHandler.SendErrorMessage(Level.SEVERE, this.plugin, player, "Sorry, something went wrong when saving the region file, please contact your Administrator.");
      
      return true;
    }
    this.plugin.economy.withdrawPlayer(player, cost); 
    //this.plugin.economy.withdrawPlayer(player.getName(), cost);
    
    Protection protection = new Protection(ProtectionHandler.GetNewRegionID(), selection.getWorld().getName(), region.getId(), player.getName());
    

    ProtectionHandler.AddProtection(protection);
    this.plugin.runProtectionNotification();
    
    MessageHandler.SendMessage(player, "{HL}You have created the protection: {IT}" + region.getId() + "! {POS}" + this.plugin.economy.format(cost) + " {HL}has been removed from your account.");
    


    return true;
  }
  
  //If it works properly, this should be called when a child plot is being made.
  	private boolean childProtection(Player player, Selection selection, ProtectedRegion region, ApplicableRegionSet regions, boolean calculateCost){
  	{
  		double cost = selection.getWidth() * selection.getLength() * this.plugin.getConfig().getDouble("ChildProtection.PricePerBlock");
  		if (calculateCost)
  		{
  			MessageHandler.SendMessage(player, "You must have " + this.plugin.economy.format(cost) + " to create this protection.");
  			return true;
  		}

  		if (!this.plugin.economy.has(player.getName(), cost))
  		{
  			MessageHandler.SendErrorMessage(Level.WARNING, this.plugin, player, "Sorry, you must have " + this.plugin.economy.format(cost) + " to create this protection.");    
  			return true;
  		}
  		
  		List<String> unwantedRegions = new ArrayList();
    
  		ProtectedRegion parent = null;
  		for (ProtectedRegion r : regions) 
  		{
  			if (r.isOwner(player.getName()))
  			{
  				Vector minimum = new Vector(region.getMinimumPoint().getBlockX(), region.getMinimumPoint().getBlockY(), region.getMinimumPoint().getBlockZ());
  				Vector maximum = new Vector(region.getMaximumPoint().getBlockX(), region.getMaximumPoint().getBlockY(), region.getMaximumPoint().getBlockZ());
  				ApplicableRegionSet rS1 = this.plugin.getWorldGuard().getRegionManager(selection.getWorld()).getApplicableRegions(minimum);
  				ApplicableRegionSet rS2 = this.plugin.getWorldGuard().getRegionManager(selection.getWorld()).getApplicableRegions(maximum);

  				boolean found = false;
  				Iterator localIterator3; //unknown iterator
  				//localIterator2: Iterates over regions at point 1.

  				//While 
  				for (Iterator localIterator2 = rS1.iterator(); localIterator2.hasNext(); localIterator3.hasNext())
  				{          

  					//localIterator3: Iterates over point two
  					localIterator3 = rS2.iterator();
      
  					ProtectedRegion compare1 = (ProtectedRegion)localIterator2.next();
  					ProtectedRegion compare2 = (ProtectedRegion)localIterator3.next();          
      
      
  					if ((r.getId().equals(compare1.getId())) && (r.getId().equals(compare2.getId()))) 
  					{
  						found = true;
  					}
  				}
  				
  				if (found)
  				{
  					if (parent == null) {
  						parent = r;
  					} else {
  						parent = parent.volume() > r.volume() ? r : parent;
  					}
  				} else {
  					unwantedRegions.add(r.getId());
  				}
  			} else {
  				unwantedRegions.add(r.getId());
  			}
  		}


  		//If no parents were found, and it's overlapping a region, send an error message
  		//containing which regions it's overlapping
  		if (parent == null)
  		{
  			System.out.println("Childone");
  			String message = "Sorry, your selection currently overlaps the following protections: ";
  			for (int i = 0; i < unwantedRegions.size(); i++)
  			{
  				message = message + (String)unwantedRegions.get(i);
  				if (i != unwantedRegions.size() - 1) {
  					message = message + ",";
  				}
  			}
  			MessageHandler.SendErrorMessage(Level.WARNING, this.plugin, player, message);
  			return true;
  		}



  		//Sets the parent of the region, sends error if circular inheritance
  		try
  		{
  			region.setParent(parent);
  		} 
  		catch (ProtectedRegion.CircularInheritanceException e)
  		{
  			MessageHandler.SendErrorMessage(Level.SEVERE, this.plugin, player, "Sorry, something went wrong while setting the region's parent, please contact your Administrator.");  
  			return true;
		}
  		
  		
  		//Tries to save the new region. If it doesn't work send user error.
  		this.plugin.getWorldGuard().getRegionManager(selection.getWorld()).addRegion(region);		
		try
		{
			this.plugin.getWorldGuard().getRegionManager(selection.getWorld()).save();
		} 
		catch (Exception e)
		{
			MessageHandler.SendErrorMessage(Level.SEVERE, this.plugin, player, "Sorry, something went wrong when saving the region file, please contact your Administrator.");  
  			return true;
		}
		
		//Withdraw the required amount
		this.plugin.economy.withdrawPlayer((OfflinePlayer) player, cost); 
		

		//Success message!
		MessageHandler.SendMessage(player, "{HL}You have created the protection: {IT}" + region.getId() + "! {POS}" + this.plugin.economy.format(cost) + " {HL}has been removed from your account.");
    	return true;
    }
  
  private boolean denyCommand(Player player, String[] args)
  {
    if (args.length == 2) {
      try
      {
        int id = Integer.parseInt(args[1]);
        Protection protection = ProtectionHandler.GetProtectionByID(id);
        if ((protection == null) || (protection.isInspected()))
        {
          MessageHandler.SendErrorMessage(Level.WARNING, this.plugin, player, "You did not enter a valid protection ID.");
          
          return true;
        }
        World world = this.plugin.getServer().getWorld(protection.getWorld());
        ProtectedRegion region = this.plugin.getWorldGuard().getRegionManager(world).getRegion(protection.getProtection());
        if (region != null) {
          this.plugin.getWorldGuard().getRegionManager(world).removeRegion(protection.getProtection());
        }
        protection.setAccepted(false);
        MessageHandler.SendMessage(player, "{ERR}The protection {IT}" + protection.getProtection() + " {ERR}has been denied.");
        if (this.plugin.getServer().getPlayer(protection.getPlayer()).isOnline()) {
          this.plugin.notifyPlayerOfProtectionInspection(this.plugin.getServer().getPlayer(protection.getPlayer()), protection);
        }
        try
        {
          ProtectionHandler.SaveProtections(this.plugin);
        }
        catch (IOException e)
        {
          MessageHandler.SendConsoleMessage(Level.SEVERE, this.plugin, "Could not save protections to file.");
        }
        return true;
      }
      catch (NumberFormatException e)
      {
        MessageHandler.SendErrorMessage(Level.WARNING, this.plugin, player, "You did not enter a valid protection ID.");
        
        return true;
      }
    }
    Protection protection = ProtectionHandler.GetTeleport(player);
    if (protection == null)
    {
      MessageHandler.SendErrorMessage(Level.WARNING, this.plugin, player, "You have no protection selected.");
      
      return true;
    }
    World world = this.plugin.getServer().getWorld(protection.getWorld());
    ProtectedRegion region = this.plugin.getWorldGuard().getRegionManager(world).getRegion(protection.getProtection());
    if (region != null) {
      this.plugin.getWorldGuard().getRegionManager(world).removeRegion(protection.getProtection());
    }
    protection.setAccepted(false);
    ProtectionHandler.RemoveTeleport(player);
    MessageHandler.SendMessage(player, "{ERR}The protection {IT}" + protection.getProtection() + " {ERR}has been denied.");
    if (this.plugin.getServer().getPlayer(protection.getPlayer()).isOnline()) {
      this.plugin.notifyPlayerOfProtectionInspection(this.plugin.getServer().getPlayer(protection.getPlayer()), protection);
    }
    try
    {
      ProtectionHandler.SaveProtections(this.plugin);
    }
    catch (IOException e)
    {
      MessageHandler.SendConsoleMessage(Level.SEVERE, this.plugin, "Could not save protections to file.");
    }
    return true;
  }
  
  private boolean acceptCommand(Player player, String[] args)
  {
    if (args.length == 2) {
      try
      {
        int id = Integer.parseInt(args[1]);
        Protection protection = ProtectionHandler.GetProtectionByID(id);
        if ((protection == null) || (protection.isInspected()))
        {
          MessageHandler.SendErrorMessage(Level.WARNING, this.plugin, player, "You did not enter a valid protection ID.");
          
          return true;
        }
        protection.setAccepted(true);
        MessageHandler.SendMessage(player, "{HL}The protection {IT}" + protection.getProtection() + " {HL}has been accepted.");
        if ((this.plugin.getServer().getPlayer(protection.getPlayer()) != null) && (this.plugin.getServer().getPlayer(protection.getPlayer()).isOnline())) {
          this.plugin.notifyPlayerOfProtectionInspection(this.plugin.getServer().getPlayer(protection.getPlayer()), protection);
        }
        try
        {
          ProtectionHandler.SaveProtections(this.plugin);
        }
        catch (IOException e)
        {
          MessageHandler.SendConsoleMessage(Level.SEVERE, this.plugin, "Could not save protections to file.");
        }
        return true;
      }
      catch (NumberFormatException e)
      {
        MessageHandler.SendErrorMessage(Level.WARNING, this.plugin, player, "You did not enter a valid protection ID.");
        
        return true;
      }
    }
    Protection protection = ProtectionHandler.GetTeleport(player);
    if (protection == null)
    {
      MessageHandler.SendErrorMessage(Level.WARNING, this.plugin, player, "You have no protection selected.");
      
      return true;
    }
    protection.setAccepted(true);
    ProtectionHandler.RemoveTeleport(player);
    MessageHandler.SendMessage(player, "{HL}The protection {IT}" + protection.getProtection() + " {HL}has been accepted.");
    if ((this.plugin.getServer().getPlayer(protection.getPlayer()) != null) && (this.plugin.getServer().getPlayer(protection.getPlayer()).isOnline())) {
      this.plugin.notifyPlayerOfProtectionInspection(this.plugin.getServer().getPlayer(protection.getPlayer()), protection);
    }
    try
    {
      ProtectionHandler.SaveProtections(this.plugin);
    }
    catch (IOException e)
    {
      MessageHandler.SendConsoleMessage(Level.SEVERE, this.plugin, "Could not save protections to file.");
    }
    return true;
  }
  
  private boolean listCommand(Player player, String[] args)
  {
    List<Protection> uninspected = ProtectionHandler.GetAllUnispected();
    String[] message = new String[1 + uninspected.size()];
    if (uninspected.size() > 0)
    {
      message[0] = "{HL}Awaiting Protections:";
      for (int i = 0; i < uninspected.size(); i++) {
        message[(i + 1)] = ("{N}[{HL}" + ((Protection)uninspected.get(i)).getID() + "{N}]{IT}" + ((Protection)uninspected.get(i)).getProtection() + " {N}Player: {IT}" + ((Protection)uninspected.get(i)).getPlayer());
      }
      MessageHandler.SendMessage(player, message);
    }
    else
    {
      MessageHandler.SendMessage(player, "No awaiting protections.");
    }
    return true;
  }
  
  private boolean teleportCommand(Player player, String[] args)
  {
    if (args.length != 2)
    {
      MessageHandler.SendErrorMessage(Level.WARNING, this.plugin, player, "Invalid command syntax.");
      
      return false;
    }
    World world = null;
    ProtectedRegion region = null;
    try
    {
      int id = Integer.parseInt(args[1]);
      Protection protection = ProtectionHandler.GetProtectionByID(id);
      if (protection == null)
      {
        MessageHandler.SendErrorMessage(Level.WARNING, this.plugin, player, "You did not enter a valid protection ID.");
        
        return true;
      }
      world = this.plugin.getServer().getWorld(protection.getWorld());
      region = this.plugin.getWorldGuard().getRegionManager(world).getRegion(protection.getProtection());
      if (region == null)
      {
        ProtectionHandler.RemoveProtection(protection);
        MessageHandler.SendErrorMessage(Level.WARNING, this.plugin, player, "You did not enter a valid protection ID.");
        
        return true;
      }
      teleportPlayer(player, world, region);
      ProtectionHandler.UpdateTeleport(player, protection);
      
      MessageHandler.SendMessage(player, new String[] { "This protection has now been teleported to and selected.", "Type /protect [accept/deny] to confirm this protection" });
      

      return true;
    }
    catch (NumberFormatException e)
    {
      world = player.getWorld();
      region = this.plugin.getWorldGuard().getRegionManager(world).getRegion(args[1]);
      if (region == null)
      {
        MessageHandler.SendErrorMessage(Level.WARNING, this.plugin, player, "The protection you entered does not exist.");
        
        return true;
      }
      teleportPlayer(player, world, region);
      MessageHandler.SendMessage(player, "{HL}The protection {IT}" + region.getId() + " {HL}has been teleported to.");
    }
    return true;
  }
  
  private boolean teleportPlayer(Player player, World world, ProtectedRegion region)
  {
    Vector middle = region.getMinimumPoint().add(region.getMaximumPoint().subtract(region.getMinimumPoint()).divide(2));
    

    int highest = world.getHighestBlockYAt(middle.getBlockX(), middle.getBlockZ());
    return player.teleport(new Location(world, middle.getBlockX(), highest + 2, middle.getBlockZ()));
  }
}
