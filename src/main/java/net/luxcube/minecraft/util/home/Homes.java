package net.luxcube.minecraft.util.home;

import net.luxcube.minecraft.TeamsPlugin;
import net.luxcube.minecraft.entity.TeamEntity;
import net.luxcube.minecraft.entity.member.TeamMember;
import net.luxcube.minecraft.service.TeamService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Homes {

  private static HomesHelper homesHelper;

  public static void ensureHome(@NotNull Player player) {
    if (!TeamsPlugin.getInstance().isHasDonutHomes()) {
      return;
    }

    if (homesHelper == null) {
      homesHelper = new HomesHelper();
    }

    TeamService teamService = TeamsPlugin.getInstance()
      .getTeamService();

    TeamMember teamMember = teamService.getTeamMember(player.getUniqueId());
    if (teamMember == null) {
      return;
    }

    TeamEntity teamEntity = teamService.getTeamEntity(teamMember.getTeamName());
    if (teamEntity == null) {
      return;
    }

    homesHelper.setLocation(player, teamEntity);
  }

}
