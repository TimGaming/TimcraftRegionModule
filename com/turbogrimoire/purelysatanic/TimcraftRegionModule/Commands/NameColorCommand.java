package com.turbogrimoire.purelysatanic.TimcraftRegionModule.Commands;

import com.turbogrimoire.purelysatanic.TimcraftRegionModule.RegionModulePlugin;
import com.turbogrimoire.purelysatanic.TimcraftUnifiedMessaging.Handlers.MessageHandler;
import java.util.logging.Level;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class NameColorCommand
  implements CommandExecutor
{
  private final RegionModulePlugin plugin;
  
  public NameColorCommand(RegionModulePlugin plugin)
  {
    this.plugin = plugin;
  }
  
  private ChatColor[] restricted = { ChatColor.RED, ChatColor.AQUA, ChatColor.RESET, ChatColor.ITALIC, ChatColor.STRIKETHROUGH, ChatColor.UNDERLINE, ChatColor.BOLD, ChatColor.MAGIC, ChatColor.BLACK };
  
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
  {
    if (!(sender instanceof Player))
    {
      MessageHandler.SendConsoleMessage(Level.WARNING, this.plugin, "This command cannot be used from console.");
      
      return true;
    }
    Player player = (Player)sender;
    if (args.length != 1) {
      return false;
    }
    if (!this.plugin.hasPermission(player, "TimcraftRegionModule.NameColor"))
    {
      MessageHandler.SendErrorMessage(Level.WARNING, this.plugin, player, "Sorry, you don't have permission to use this command.");
      
      return true;
    }
    ChatColor color = null;
    try
    {
      color = ChatColor.valueOf(args[0].toUpperCase());
    }
    catch (IllegalArgumentException e)
    {
      MessageHandler.SendErrorMessage(Level.WARNING, this.plugin, player, "That color does not exist.");
      sendApplicableColors(player);
      return true;
    }
    if (color == null)
    {
      MessageHandler.SendErrorMessage(Level.WARNING, this.plugin, player, "That color does not exist.");
      sendApplicableColors(player);
      return true;
    }
    for (ChatColor r : this.restricted) {
      if (color.equals(r))
      {
        MessageHandler.SendErrorMessage(Level.WARNING, this.plugin, player, "Sorry, this color is restricted.");
        sendApplicableColors(player);
        return true;
      }
    }
    this.plugin.chat.setPlayerPrefix((Player)sender, "&" + color.getChar());
    MessageHandler.SendMessage(player, "Name color has been changed to: " + color + color.name());
    return true;
  }
  
  private void sendApplicableColors(Player player)
  {
    String[] message = new String[2];
    
    message[0] = "Applicable Name Colors: ";
    message[1] = "";
    for (int i = 0; i < ChatColor.values().length; i++)
    {
      boolean restrictedValue = false;
      for (int j = 0; j < this.restricted.length; j++) {
        if (ChatColor.values()[i].equals(this.restricted[j])) {
          restrictedValue = true;
        }
      }
      if (!restrictedValue)
      {
        if (message[1].length() != 0)
        {
          int tmp79_78 = 1;
          String[] tmp79_77 = message;
          tmp79_77[tmp79_78] = (tmp79_77[tmp79_78] + ",");
        }
        int tmp103_102 = 1;
        String[] tmp103_101 = message;
        tmp103_101[tmp103_102] = (tmp103_101[tmp103_102] + ChatColor.values()[i] + ChatColor.values()[i].name());
      }
    }
    MessageHandler.SendMessage(player, message);
  }
}
