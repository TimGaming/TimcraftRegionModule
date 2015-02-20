package com.turbogrimoire.purelysatanic.TimcraftRegionModule.Handlers;

import com.turbogrimoire.purelysatanic.TimcraftRegionModule.Protection;
import com.turbogrimoire.purelysatanic.TimcraftRegionModule.RegionModulePlugin;
import com.turbogrimoire.purelysatanic.TimcraftUnifiedMessaging.Handlers.MessageHandler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class ProtectionHandler
  implements Runnable
{
  private static List<Protection> protections = new ArrayList();
  private static HashMap<Player, Protection> teleports = new HashMap();
  private final RegionModulePlugin plugin;
  
  public ProtectionHandler(RegionModulePlugin plugin)
  {
    this.plugin = plugin;
  }
  
  public static void LoadProtections(RegionModulePlugin plugin)
    throws IOException
  {
    if (!plugin.getDataFolder().exists()) {
      plugin.getDataFolder().mkdir();
    }
    File save = new File(plugin.getDataFolder(), "protections.yml");
    if (!save.exists()) {
      save.createNewFile();
    }
    FileConfiguration config = YamlConfiguration.loadConfiguration(save);
    
    Set<String> keys = config.getKeys(true);
    for (String key : keys)
    {
      Protection protection = (Protection)config.get(key);
      AddProtection(protection);
    }
  }
  
  public static void SaveProtections(RegionModulePlugin plugin)
    throws IOException
  {
    if (!plugin.getDataFolder().exists()) {
      plugin.getDataFolder().mkdir();
    }
    File save = new File(plugin.getDataFolder(), "protections.yml");
    save.delete();
    save.createNewFile();
    FileConfiguration config = YamlConfiguration.loadConfiguration(save);
    for (Protection protection : protections) {
      config.set(protection.getProtection(), protection);
    }
    config.save(save);
  }
  
  public static void AddProtection(Protection protection)
  {
    protections.add(protection);
  }
  
  public static void RemoveProtection(Protection protection)
  {
    protections.remove(protection);
  }
  
  public static Protection GetProtectionByID(int id)
  {
    for (Protection protection : protections) {
      if (protection.getID() == id) {
        return protection;
      }
    }
    return null;
  }
  
  public static List<Protection> GetProtectionsByPlayer(String player)
  {
    List<Protection> protections = new ArrayList();
    for (Protection protection : protections) {
      if (protection.getPlayer().equalsIgnoreCase(player)) {
        protections.add(protection);
      }
    }
    return protections;
  }
  
  public static Protection GetProtectionByName(String name)
  {
    for (Protection protection : protections) {
      if (protection.getProtection().equalsIgnoreCase(name)) {
        return protection;
      }
    }
    return null;
  }
  
  public static int GetNewRegionID()
  {
    int id = 0;
    boolean found = true;
    do
    {
      if (!found) {
        id++;
      }
      found = true;
      for (Protection protection : protections) {
        if (protection.getID() == id) {
          found = false;
        }
      }
    } while (!found);
    return id;
  }
  
  public static void UpdateTeleport(Player player, Protection protection)
  {
    teleports.put(player, protection);
  }
  
  public static void RemoveTeleport(Player player)
  {
    teleports.remove(player);
  }
  
  public static Protection GetTeleport(Player player)
  {
    return (Protection)teleports.get(player);
  }
  
  public static List<Protection> GetAllUnispected()
  {
    List<Protection> uninspected = new ArrayList();
    for (Protection protection : protections) {
      if (!protection.isInspected()) {
        uninspected.add(protection);
      }
    }
    return uninspected;
  }
  
  public void run()
  {
    List<Protection> uninspected = GetAllUnispected();
    String[] message = new String[1 + uninspected.size()];
    List<Player> receivers = new ArrayList();
    if (uninspected.size() > 0)
    {
      Collection<? extends Player> players = this.plugin.getServer().getOnlinePlayers();
      for (Player player : players) {
        if (this.plugin.hasPermission(player, "TimcraftRegionModule.Protection.Moderate")) {
          receivers.add(player);
        }
      }
      message[0] = "{HL}Awaiting Protections:";
      for (int i = 0; i < uninspected.size(); i++) {
        message[(i + 1)] = ("{N}[{HL}" + ((Protection)uninspected.get(i)).getID() + "{N}]{IT}" + ((Protection)uninspected.get(i)).getProtection() + " {N}Player: {IT}" + ((Protection)uninspected.get(i)).getPlayer());
      }
      for (Player player : receivers) {
        MessageHandler.SendMessage(player, message);
      }
    }
  }
}
