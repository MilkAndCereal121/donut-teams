package net.luxcube.minecraft.extension;

import lombok.RequiredArgsConstructor;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.luxcube.minecraft.TeamsPlugin;
import net.luxcube.minecraft.entity.member.TeamMember;
import net.luxcube.minecraft.vo.TeamsVO;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public class TeamPlaceholderExtension extends PlaceholderExpansion {

  private final TeamsPlugin teamsPlugin;

  @Override
  public @NotNull String getIdentifier() {
    return "team";
  }

  @Override
  public @NotNull String getAuthor() {
    return "Milk_And_Cereal";
  }

  @Override
  public @NotNull String getVersion() {
    return "1.0.0";
  }

  @Override
  public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {

    if (!params.equals("name")) {
      return "N/A";
    }

    TeamMember teamMember = teamsPlugin.getTeamService()
            .getTeamMember(player.getUniqueId());

    if (teamMember == null) {
      return "N/A";
    }

    return teamMember.getTeamName();
  }

}
