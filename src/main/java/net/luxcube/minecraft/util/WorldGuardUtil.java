package net.luxcube.minecraft.util;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import lombok.val;
import net.luxcube.minecraft.TeamsPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class WorldGuardUtil {

  public static boolean canSetHome(@NotNull Location location, @NotNull Player player) {
    RegionContainer container = WorldGuard.getInstance()
      .getPlatform()
      .getRegionContainer();

    RegionQuery query = container.createQuery();
    val vector3 = BukkitAdapter.adapt(location);

    LocalPlayer localPlayer = WorldGuardPlugin.inst()
      .wrapPlayer(player);

    return query.testState(vector3, localPlayer, TeamsPlugin.ALLOWED_SET_HOME);
  }

}
