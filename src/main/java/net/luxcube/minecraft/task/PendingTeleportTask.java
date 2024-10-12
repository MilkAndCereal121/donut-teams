package net.luxcube.minecraft.task;

import lombok.Setter;
import net.luxcube.minecraft.TeamsPlugin;
import net.luxcube.minecraft.vo.TeamsVO;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PendingTeleportTask implements Runnable {

  public final static Set<UUID> PENDING_PLAYERS = new HashSet<>();

  private final Location baseLocation;
  private final Location targetLocation;

  private final Player player;

  private int remainingSeconds;

  @Setter
  private BukkitTask bukkitTask;

  public PendingTeleportTask(@NotNull Player player, @NotNull Location targetLocation, int remainingSeconds) {
    this.player = player;
    this.targetLocation = targetLocation;
    this.remainingSeconds = remainingSeconds;
    this.baseLocation = player.getLocation();
  }

  @Override
  public void run() {
    TeamsVO teamsVO = TeamsPlugin.getInstance().getTeamsVO();


    if (!player.isOnline()) {
      PENDING_PLAYERS.remove(player.getUniqueId());
      bukkitTask.cancel();
      return;
    }

    Location currentLocation = player.getLocation();
    if (!currentLocation.getWorld().equals(baseLocation.getWorld())
      || currentLocation.distanceSquared(baseLocation) > 1) {
      String message = teamsVO.getMessage("teleport-move-cancel");

      player.sendMessage(message);
      player.sendActionBar(message);

      PENDING_PLAYERS.remove(player.getUniqueId());
      bukkitTask.cancel();
      return;
    }

    if (remainingSeconds <= 0) {
      String message = teamsVO.getMessage("teleport-home-success");

      player.sendMessage(message);

      player.teleport(targetLocation);
      PENDING_PLAYERS.remove(player.getUniqueId());
      bukkitTask.cancel();
      return;
    }

    String message = teamsVO.getMessage("teleporting-pending")
                    .replace("%seconds%", String.valueOf(remainingSeconds));

    player.sendMessage(message);
    player.sendActionBar(message);

    remainingSeconds--;
  }
}
