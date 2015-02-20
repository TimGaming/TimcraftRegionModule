package com.turbogrimoire.purelysatanic.TimcraftRegionModule;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.turbogrimoire.purelysatanic.TimcraftRegionModule.Commands.ExpandCommand;
import com.turbogrimoire.purelysatanic.TimcraftRegionModule.Commands.NameColorCommand;
import com.turbogrimoire.purelysatanic.TimcraftRegionModule.Commands.ProtectionCommand;
import com.turbogrimoire.purelysatanic.TimcraftRegionModule.Commands.RegionCleanupCommand;
import com.turbogrimoire.purelysatanic.TimcraftRegionModule.Exceptions.PluginNotFoundException;
import com.turbogrimoire.purelysatanic.TimcraftRegionModule.Handlers.ProtectionHandler;
import com.turbogrimoire.purelysatanic.TimcraftRegionModule.Listeners.PlayerListener;
import com.turbogrimoire.purelysatanic.TimcraftUnifiedMessaging.Handlers.MessageHandler;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Server;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.FileConfigurationOptions;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

public class RegionModulePlugin
  extends JavaPlugin
{
  private final PlayerListener playerListener = new PlayerListener(this);
  private final ProtectionHandler protectionHandler = new ProtectionHandler(this);
  private WorldEditPlugin worldedit;
  private WorldGuardPlugin worldguard;
  public Economy economy = null;
  public Permission permission = null;
  public Chat chat = null;
  
  public void onEnable()
  {
    ConfigurationSerialization.registerClass(Protection.class);
    try
    {
      initializeVault();
      initializeWorldEdit();
      initializeWorldGuard();
    }
    catch (PluginNotFoundException e1)
    {
      MessageHandler.SendConsoleMessage(Level.SEVERE, this, e1.getMessage());
      getServer().getPluginManager().disablePlugin(this);
    }
    if (!getDataFolder().exists()) {
      getDataFolder().mkdir();
    }
    File file = new File(getDataFolder(), "config.yml");
    if (!file.exists()) {
      try
      {
        file.createNewFile();
        getConfig().options().copyDefaults(true);
        getConfig().save(file);
        getConfig().options().copyDefaults(false);
      }
      catch (IOException e)
      {
        MessageHandler.SendConsoleMessage(Level.SEVERE, this, "Could not create configuration file.");
        
        getServer().getPluginManager().disablePlugin(this);
      }
    }
    getServer().getScheduler().scheduleSyncRepeatingTask(this, this.protectionHandler, 12000L, 12000L);
    getServer().getPluginManager().registerEvents(this.playerListener, this);
    
    getCommand("Protect").setExecutor(new ProtectionCommand(this));
    getCommand("Expand").setExecutor(new ExpandCommand(this));
    getCommand("NameColor").setExecutor(new NameColorCommand(this));
    getCommand("RegionCleanup").setExecutor(new RegionCleanupCommand(this));
    try
    {
      ProtectionHandler.LoadProtections(this);
    }
    catch (IOException e)
    {
      MessageHandler.SendConsoleMessage(Level.SEVERE, this, "Could not load protections from file.");
      
      getServer().getPluginManager().disablePlugin(this);
    }
  }
  
  private void initializeVault()
    throws PluginNotFoundException
  {
    RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
    RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(Permission.class);
    RegisteredServiceProvider<Chat> chatProvider = getServer().getServicesManager().getRegistration(Chat.class);
    if (economyProvider != null)
    {
      this.economy = ((Economy)economyProvider.getProvider());
      this.permission = ((Permission)permissionProvider.getProvider());
      this.chat = ((Chat)chatProvider.getProvider());
    }
    else
    {
      throw new PluginNotFoundException("Vault");
    }
  }
  
  private void initializeWorldEdit()
    throws PluginNotFoundException
  {
    Plugin plugin = getPlugin("WorldEdit");
    if (plugin == null) {
      throw new PluginNotFoundException("WorldEdit");
    }
    this.worldedit = ((WorldEditPlugin)plugin);
  }
  
  private void initializeWorldGuard()
    throws PluginNotFoundException
  {
    Plugin plugin = getPlugin("WorldGuard");
    if (plugin == null) {
      throw new PluginNotFoundException("WorldGuard");
    }
    this.worldguard = ((WorldGuardPlugin)plugin);
  }
  
  private Plugin getPlugin(String name)
  {
    return getServer().getPluginManager().getPlugin(name);
  }
  
  public boolean hasPermission(Player player, String permission)
  {
    return this.permission.has(player, permission);
  }
  
  public WorldEditPlugin getWorldEdit()
  {
    return this.worldedit;
  }
  
  public WorldGuardPlugin getWorldGuard()
  {
    return this.worldguard;
  }
  
  public void notifyPlayerOfProtectionInspection(Player player, Protection protection)
  {
    if (protection.isAccepted()) {
      MessageHandler.SendMessage(player, "The protection " + protection.getProtection() + " has been accepted.");
    } else {
      MessageHandler.SendMessage(player, "The protection " + protection.getProtection() + " has been denied.");
    }
    ProtectionHandler.RemoveProtection(protection);
  }
  
  public void runProtectionNotification()
  {
    this.protectionHandler.run();
  }
}
