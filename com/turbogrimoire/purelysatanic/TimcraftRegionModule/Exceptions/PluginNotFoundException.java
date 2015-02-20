package com.turbogrimoire.purelysatanic.TimcraftRegionModule.Exceptions;

public class PluginNotFoundException
  extends Exception
{
  private static final long serialVersionUID = 1L;
  private final String plugin;
  
  public PluginNotFoundException(String plugin)
  {
    this.plugin = plugin;
  }
  
  public String getMessage()
  {
    return this.plugin + " was not found on the server.";
  }
}
