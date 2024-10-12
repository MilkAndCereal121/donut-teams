package net.luxcube.minecraft.listener;

import lombok.RequiredArgsConstructor;
import net.luxcube.minecraft.TeamsPlugin;
import net.luxcube.minecraft.entity.TeamEntity;
import net.luxcube.minecraft.entity.member.TeamMember;
import net.luxcube.minecraft.service.TeamService;
import net.luxcube.minecraft.util.home.Homes;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@RequiredArgsConstructor
public class TeamConnectionListener implements Listener {

  private final TeamsPlugin teamsPlugin;

  @EventHandler(priority = EventPriority.LOWEST)
  public void onAsyncPlayerPreLogin(@NotNull AsyncPlayerPreLoginEvent event) {
    UUID uniqueId = event.getUniqueId();

    TeamService teamService = teamsPlugin.getTeamService();
    if (teamService.contains(uniqueId)) {
      return;
    }

    TeamMember teamMember = teamsPlugin.getSqlStorage()
      .retrieveMember(uniqueId);

    if (teamMember == null) {
      return;
    }

    teamService.put(teamMember);
    if (teamService.contains(teamMember.getTeamName())) {
      return;
    }

    TeamEntity teamEntity = teamsPlugin.getSqlStorage()
      .retrieveTeam(teamMember.getTeamName());

    if (teamEntity == null) {
      teamService.remove(teamMember);
      return;
    }

    teamService.put(teamEntity);

    for (
      @NotNull TeamMember member : teamsPlugin.getSqlStorage()
      .retrieveAllMembers(teamMember.getTeamName())
    ) {
      teamEntity.addMember(member);
      if (teamService.contains(member.getUniqueId())) {
        continue;
      }

      teamService.put(member);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
    Player player = event.getPlayer();
    Homes.ensureHome(player);
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
    Player player = event.getPlayer();

    TeamService teamService = teamsPlugin.getTeamService();

    TeamMember teamMember = teamService.getTeamMember(player.getUniqueId());
    if (teamMember == null) {
      return;
    }

    teamService.remove(teamMember);

    TeamEntity teamEntity = teamService.getTeamEntity(teamMember.getTeamName());
    if (teamEntity == null) {
      return;
    }

    if (teamEntity.isSomeoneOnline(player)) {
      return;
    }

    teamService.remove(teamEntity);
    teamsPlugin.getSqlStorage()
      .updateTeam(teamEntity);
  }
}
