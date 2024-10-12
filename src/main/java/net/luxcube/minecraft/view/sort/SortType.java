package net.luxcube.minecraft.view.sort;

import net.luxcube.minecraft.TeamsPlugin;
import net.luxcube.minecraft.entity.member.TeamMember;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public enum SortType {
  JOIN_DATE {
    @Override
    public List<TeamMember> sort(@NotNull TeamsPlugin teamsPlugin, @NotNull List<TeamMember> teamMembers) {
      return teamMembers.stream()
        .sorted((a, b) -> Long.compare(b.getJoinedAt().toEpochMilli(), a.getJoinedAt().toEpochMilli()))
        .collect(Collectors.toList());
    }
  },
  PERMISSIONS {
    @Override
    public List<TeamMember> sort(@NotNull TeamsPlugin teamsPlugin, @NotNull List<TeamMember> teamMembers) {
      return teamMembers.stream()
        .sorted((a, b) -> Integer.compare(b.getPermissionPriority(), a.getPermissionPriority()))
        .collect(Collectors.toList());
    }
  },
  MONEY {
    @Override
    public List<TeamMember> sort(@NotNull TeamsPlugin teamsPlugin, @NotNull List<TeamMember> teamMembers) {
      return teamMembers.stream()
        .sorted((a, b) -> {
          OfflinePlayer playerA = a.getBukkitOfflinePlayer(),
            playerB = b.getBukkitOfflinePlayer();

          double balanceA = teamsPlugin.getEconomy()
            .getBalance(playerA),
            balanceB = teamsPlugin.getEconomy()
              .getBalance(playerB);

          return Double.compare(balanceB, balanceA);
        }).collect(Collectors.toList());
    }
  },
  ALPHABETICALLY {
    @Override
    public List<TeamMember> sort(@NotNull TeamsPlugin teamsPlugin, @NotNull List<TeamMember> teamMembers) {
      return teamMembers.stream()
        .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
        .collect(Collectors.toList());
    }
  };

  public abstract List<TeamMember> sort(@NotNull TeamsPlugin teamsPlugin, @NotNull List<TeamMember> teamMembers);

}
