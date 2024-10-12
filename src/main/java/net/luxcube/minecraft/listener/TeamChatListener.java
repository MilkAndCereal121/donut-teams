package net.luxcube.minecraft.listener;

import lombok.RequiredArgsConstructor;
import net.luxcube.minecraft.TeamsPlugin;
import net.luxcube.minecraft.entity.TeamEntity;
import net.luxcube.minecraft.entity.member.TeamMember;
import net.luxcube.minecraft.view.TeamInfoView;
import net.luxcube.minecraft.view.sort.SortType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.Map;

@RequiredArgsConstructor
public class TeamChatListener implements Listener {

  private final TeamsPlugin teamsPlugin;

  @EventHandler
  private void onChat(AsyncPlayerChatEvent event) {

    Player player = event.getPlayer();
    if (!player.hasMetadata("team_searching")) {
      return;
    }

    event.setCancelled(true);

    player.removeMetadata("team_searching", teamsPlugin);

    TeamMember teamMember = teamsPlugin.getTeamService()
            .getTeamMember(player.getUniqueId());

    if (teamMember == null) {
      return;
    }

    TeamEntity teamEntity = teamsPlugin.getTeamService().getTeamEntity(teamMember.getTeamName());
    if (teamEntity == null) {
      return;
    }

    teamsPlugin.getViewFrame()
            .open(
                    TeamInfoView.class,
                    player,
                    Map.of(
                            "team_entity", teamEntity,
                            "sort_type", SortType.JOIN_DATE,
                            "filtered_name", event.getMessage()
                    )
            );

  }

}
