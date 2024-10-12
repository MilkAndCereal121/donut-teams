package net.luxcube.minecraft.listener;

import lombok.RequiredArgsConstructor;
import net.luxcube.minecraft.TeamsPlugin;
import net.luxcube.minecraft.entity.TeamEntity;
import net.luxcube.minecraft.entity.member.TeamMember;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class TeamPvpListener implements Listener {

  private final TeamsPlugin teamsPlugin;

  @EventHandler(priority = EventPriority.LOWEST)
  public void onEntityDamageByEntity(@NotNull EntityDamageByEntityEvent event) {
    Entity damager = event.getDamager(),
      entity = event.getEntity();

    if (
      !(damager instanceof Player) ||
      !(entity instanceof Player)
    ) {
      return;
    }

    Player player = (Player) damager,
      target = (Player) entity;

    TeamMember playerMember = teamsPlugin.getTeamService()
      .getTeamMember(player.getUniqueId()),
      targetMember = teamsPlugin.getTeamService()
      .getTeamMember(target.getUniqueId());

    if (
      playerMember == null ||
      targetMember == null ||
      !playerMember.getTeamName().equals(targetMember.getTeamName())
    ) {
      return;
    }

    TeamEntity teamEntity = teamsPlugin.getTeamService()
      .getTeamEntity(playerMember.getTeamName());

    if (teamEntity == null || teamEntity.isFriendlyFire()) {
      return;
    }

    event.setCancelled(true);
  }

}
