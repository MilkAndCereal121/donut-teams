package net.luxcube.minecraft.util.home;

import net.luxcube.minecraft.HomePlugin;
import net.luxcube.minecraft.entity.HomePlayer;
import net.luxcube.minecraft.entity.TeamEntity;
import net.luxcube.minecraft.entity.home.Home;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HomesHelper {

  public void setLocation(@NotNull Player player, @NotNull TeamEntity teamEntity) {
    HomePlayer homePlayer = HomePlugin.getInstance()
      .getHomeService()
      .getHomePlayer(player.getUniqueId());

    if (homePlayer == null) {
      return;
    }

    Location homeLocation = teamEntity.getHome();
    if (homeLocation == null) {
      homePlayer.setTeamHome(null);
      return;
    }

    homePlayer.setTeamHome(
      new Home(
        homeLocation.getWorld()
          .getName(),
        homeLocation.getBlockX(),
        homeLocation.getBlockY(),
        homeLocation.getBlockZ(),
        homeLocation.getYaw(),
        homeLocation.getPitch()
      )
    );
  }

}
