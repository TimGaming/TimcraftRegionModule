package com.turbogrimoire.purelysatanic.TimcraftRegionModule;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

public class Protection
  implements ConfigurationSerializable
{
  private final int id;
  private final String protection;
  private final String world;
  private final String player;
  private boolean inspected = false;
  private boolean accepted = false;
  
  public Protection(int id, String world, String protection, String player)
  {
    this.id = id;
    this.world = world;
    this.protection = protection;
    this.player = player;
  }
  
  public Protection(int id, String world, String protection, String player, boolean inspected, boolean accepted)
  {
    this.id = id;
    this.world = world;
    this.protection = protection;
    this.player = player;
    this.inspected = inspected;
    this.accepted = accepted;
  }
  
  public int getID()
  {
    return this.id;
  }
  
  public String getWorld()
  {
    return this.world;
  }
  
  public String getProtection()
  {
    return this.protection;
  }
  
  public String getPlayer()
  {
    return this.player;
  }
  
  public boolean isInspected()
  {
    return this.inspected;
  }
  
  public boolean isAccepted()
  {
    return this.accepted;
  }
  
  public void setAccepted(boolean accepted)
  {
    this.inspected = true;
    this.accepted = accepted;
  }
  
  public Map<String, Object> serialize()
  {
    Map<String, Object> map = new HashMap();
    map.put("ID", Integer.valueOf(this.id));
    map.put("World", this.world);
    map.put("Protection", this.protection);
    map.put("Player", this.player);
    map.put("Inspected", Boolean.valueOf(this.inspected));
    map.put("Accepted", Boolean.valueOf(this.accepted));
    
    return map;
  }
  
  public static Protection deserialize(Map<String, Object> map)
  {
    int id = map.get("ID") != null ? ((Integer)map.get("ID")).intValue() : -1;
    String world = (String)map.get("World");
    String protection = (String)map.get("Protection");
    String player = (String)map.get("Player");
    boolean inspected = map.get("Inspected") != null ? ((Boolean)map.get("Inspected")).booleanValue() : false;
    boolean accepted = map.get("Accepted") != null ? ((Boolean)map.get("Accepted")).booleanValue() : false;
    
    return new Protection(id, world, protection, player, inspected, accepted);
  }
}
