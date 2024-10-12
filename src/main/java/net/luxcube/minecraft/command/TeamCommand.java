package net.luxcube.minecraft.command;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import lombok.RequiredArgsConstructor;
import me.saiintbrisson.minecraft.command.annotation.Command;
import me.saiintbrisson.minecraft.command.annotation.Completer;
import me.saiintbrisson.minecraft.command.annotation.Optional;
import me.saiintbrisson.minecraft.command.command.Context;
import me.saiintbrisson.minecraft.command.target.CommandTarget;
import net.luxcube.minecraft.TeamsPlugin;
import net.luxcube.minecraft.entity.TeamEntity;
import net.luxcube.minecraft.entity.member.TeamMember;
import net.luxcube.minecraft.entity.permission.TeamPermission;
import net.luxcube.minecraft.service.TeamService;
import net.luxcube.minecraft.task.PendingTeleportTask;
import net.luxcube.minecraft.util.home.Homes;
import net.luxcube.minecraft.util.WorldGuardUtil;
import net.luxcube.minecraft.view.ConfirmTeamDisbandView;
import net.luxcube.minecraft.view.TeamInfoView;
import net.luxcube.minecraft.view.sort.SortType;
import net.luxcube.minecraft.vo.InviteVO;
import net.luxcube.minecraft.vo.TeamsVO;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static net.luxcube.minecraft.util.Colors.translateHex;

public class TeamCommand {

  private static final Table<UUID, String, InviteVO> PENDING_INVITES = HashBasedTable.create();

  private final TeamsPlugin teamsPlugin;
  private final TeamsVO teamsVO;

  public TeamCommand(@NotNull TeamsPlugin teamsPlugin) {
    this.teamsPlugin = teamsPlugin;
    this.teamsVO = teamsPlugin.getTeamsVO();
  }

  @Command(
    name = "team",
    aliases = {"teams"}
  )
  public void handleTeamCommand(@NotNull Context<CommandSender> context) {
    CommandSender commandSender = context.getSender();
    if (!(commandSender instanceof Player player)) {
      return;
    }

    TeamService teamService = teamsPlugin.getTeamService();

    TeamMember teamMember = teamService.getTeamMember(player.getUniqueId());
    if (teamMember == null) {
      String message = teamsVO.getMessage("no-team");
      player.sendMessage(message);

      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
      return;
    }

    TeamEntity teamEntity = teamService.getTeamEntity(teamMember.getTeamName());
    if (teamEntity == null) {
      String message = teamsVO.getMessage("team-no-exist");
      player.sendMessage(message);

      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
      return;
    }


    teamsPlugin.getViewFrame()
      .open(
        TeamInfoView.class,
        player,
        Map.of(
          "team_entity", teamEntity,
          "sort_type", SortType.JOIN_DATE
        )
      );
  }

  @Command(
    name = "team.create",
    target = CommandTarget.PLAYER
  )
  public void handleTeamCreateCommand(@NotNull Context<Player> context, @Optional String teamName) {
    Player player = context.getSender();
    TeamService teamService = teamsPlugin.getTeamService();

    if (teamName == null) {
      return;
    }

    if (teamService.contains(teamName)) {
      String message = teamsVO.getMessage("team-taken-name");

      player.sendMessage(message);
      player.sendActionBar(message);

      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
      return;
    }

    TeamMember teamMember = teamService.getTeamMember(player.getUniqueId());
    if (teamMember != null) {
      String message = teamsVO.getMessage("do-not-have-team");

      player.sendMessage(message);
      player.sendActionBar(message);

      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
      return;
    }

    TeamEntity teamEntity = new TeamEntity(
      teamName,
      new ArrayList<>(),
      Instant.now()
    );

    teamMember = new TeamMember(
      player.getUniqueId(),
      player.getName(),
      teamName,
      Instant.now(),
      List.of(
        TeamPermission.DELETE,
        TeamPermission.INVITE,
        TeamPermission.HOME,
        TeamPermission.SET_HOME
      )
    );

    teamsPlugin.getSqlStorage()
      .createTeam(teamEntity);

    teamsPlugin.getSqlStorage()
      .addUser(teamMember);

    teamEntity.addMember(teamMember);

    teamService.put(teamMember);
    teamService.put(teamEntity);

    String message = teamsVO.getMessage("team-created");

    player.sendMessage(message);
    player.sendActionBar(message);

    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
  }

  @Command(
    name = "team.join",
    target = CommandTarget.PLAYER
  )
  public void handleTeamJoin(@NotNull Context<Player> context, @Optional String username) {
    Player player = context.getSender();

    if (username == null) {
      return;
    }

    TeamMember teamMember = teamsPlugin.getTeamService()
      .getTeamMember(player.getUniqueId());
    if (teamMember != null) {
      String message = teamsVO.getMessage("already-have-team");

      player.sendMessage(message);
      player.sendActionBar(message);

      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
      return;
    }

    InviteVO inviteVO = PENDING_INVITES.get(player.getUniqueId(), username);
    if (inviteVO == null) {
      String message = teamsVO.getMessage("no-pending-invite");

      player.sendMessage(message);
      player.sendActionBar(message);

      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
      return;
    }

    PENDING_INVITES.row(player.getUniqueId())
      .clear();

    TeamEntity teamEntity = inviteVO.teamEntity();
    teamMember = new TeamMember(
      player.getUniqueId(),
      player.getName(),
      teamEntity.getName(),
      Instant.now(),
      List.of(TeamPermission.HOME)
    );

    teamsPlugin.getSqlStorage()
      .addUser(teamMember);
    teamsPlugin.getTeamService()
      .put(teamMember);

    teamEntity.addMember(teamMember);

    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);

    String message = teamsVO.getMessage("joined-team");

    player.sendMessage(message);
    player.sendActionBar(message);
  }

  @Command(
    name = "team.chat",
    target = CommandTarget.PLAYER
  )
  public void handleTeamChat(@NotNull Context<Player> context) {
    Player player = context.getSender();

    TeamMember teamMember = teamsPlugin.getTeamService()
      .getTeamMember(player.getUniqueId());
    if (teamMember == null) {
      String message = teamsVO.getMessage("do-not-have-team");

      player.sendMessage(message);
      player.sendActionBar(message);

      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
      return;
    }

    TeamEntity teamEntity = teamsPlugin.getTeamService()
      .getTeamEntity(teamMember.getTeamName());
    if (teamEntity == null) {
      String message = teamsVO.getMessage("team-no-exist");

      player.sendMessage(message);
      player.sendActionBar(message);

      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
      return;
    }

    String message = String.join(" ", context.getArgs());
    teamEntity.getMembers().stream()
      .map(TeamMember::getBukkitPlayer)
      .filter(member -> member != null)
      .forEach(member -> {
        member.sendMessage(translateHex("#2e5bf0[TEAM] " + player.getName() + ": &f" + message));
        member.playSound(member.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
      });
  }

  @Command(
    name = "team.delhome",
    target = CommandTarget.PLAYER
  )
  public void handleDelHomeCommand(@NotNull Context<Player> context) {
    Player player = context.getSender();

    TeamMember teamMember = teamsPlugin.getTeamService()
      .getTeamMember(player.getUniqueId());
    if (teamMember == null) {
      String message = teamsVO.getMessage("do-not-have-team");

      player.sendMessage(message);
      player.sendActionBar(message);

      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
      return;
    }

    TeamEntity teamEntity = teamsPlugin.getTeamService()
      .getTeamEntity(teamMember.getTeamName());
    if (teamEntity == null) {
      String message = teamsVO.getMessage("team-no-exist");

      player.sendMessage(message);
      player.sendActionBar(message);

      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
      return;
    }

    if (!teamEntity.getLeader().getUniqueId().equals(player.getUniqueId())) {
      String message = teamsVO.getMessage("no-leader");

      player.sendMessage(message);
      player.sendActionBar(message);

      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
      return;
    }

    if (teamEntity.getHome() == null) {
      String message = teamsVO.getMessage("team-no-home");

      player.sendMessage(message);
      player.sendActionBar(message);

      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
      return;
    }

    teamEntity.setHome(null);
    for (@NotNull TeamMember member : teamEntity.getMembers()) {
      Player targetPlayer = member.getBukkitPlayer();
      if (targetPlayer != null) {
        Homes.ensureHome(targetPlayer);
      }
    }

    String message = teamsVO.getMessage("team-home-deleted");

    player.sendMessage(message);
    player.sendActionBar(message);

    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);

    teamsPlugin.getSqlStorage()
      .updateTeam(teamEntity);
  }

  @Command(
    name = "team.home",
    target = CommandTarget.PLAYER
  )
  public void handleHomeCommand(@NotNull Context<Player> context) {
    Player player = context.getSender();

    TeamMember teamMember = teamsPlugin.getTeamService()
      .getTeamMember(player.getUniqueId());
    if (teamMember == null) {
      String message = teamsVO.getMessage("do-not-have-team");

      player.sendMessage(message);
      player.sendActionBar(message);

      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
      return;
    }

    TeamEntity teamEntity = teamsPlugin.getTeamService()
      .getTeamEntity(teamMember.getTeamName());

    if (teamEntity == null) {
      String message = teamsVO.getMessage("team-no-exist");

      player.sendMessage(message);
      player.sendActionBar(message);

      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
      return;
    }

    if (!teamMember.hasPermission(TeamPermission.HOME)) {
      String message = teamsVO.getMessage("no-permission-command");

      player.sendMessage(message);
      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
      return;
    }

    if (teamEntity.getHome() == null) {
      String message = teamsVO.getMessage("team-no-home");

      player.sendMessage(message);
      player.sendActionBar(message);

      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
      return;
    }

    if (PendingTeleportTask.PENDING_PLAYERS.contains(player.getUniqueId())) {
      return;
    }

    PendingTeleportTask.PENDING_PLAYERS.add(player.getUniqueId());

    scheduleTeleport(player, teamEntity.getHome());
  }

  @Command(
    name = "team.info",
    target = CommandTarget.PLAYER
  )
  public void handleTeamInfoCommand(@NotNull Context<Player> context, @Optional String playerName) {
    Player player = context.getSender();
    if (playerName == null) {
      return;
    }

    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
    if (!offlinePlayer.hasPlayedBefore()) {
      String message = teamsVO.getMessage("player-not-found");

      player.sendMessage(message);
      player.sendActionBar(message);

      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
      return;
    }

    TeamMember teamMember = teamsPlugin.getTeamService()
      .getTeamMember(offlinePlayer.getUniqueId());
    if (teamMember == null) {
      String message = teamsVO.getMessage("user-not-exist");

      player.sendMessage(message);
      player.sendActionBar(message);

      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
      return;
    }

    TeamEntity teamEntity = teamsPlugin.getTeamService()
      .getTeamEntity(teamMember.getTeamName());
    if (teamEntity == null) {
      String message = teamsVO.getMessage("team-no-exist");

      player.sendMessage(message);
      player.sendActionBar(message);

      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
      return;
    }

    teamsPlugin.getViewFrame()
      .open(
        TeamInfoView.class,
        player,
        Map.of(
          "team_entity", teamEntity,
          "sort_type", SortType.JOIN_DATE
        )
      );
  }

  @Command(
          name = "team.pvp",
          target = CommandTarget.PLAYER
  )
  public void handlePvPCommand(
          @NotNull Context<Player> context
  ) {
    Player player = context.getSender();

    @Nullable TeamMember teamMember = teamsPlugin.getTeamService()
            .getTeamMember(player.getUniqueId());

    if (teamMember == null) {
      return;
    }

    @Nullable TeamEntity teamEntity = teamsPlugin.getTeamService().getTeamEntity(teamMember.getTeamName());

    if (teamEntity == null) {
      return;
    }

    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);

    teamEntity.setFriendlyFire(!teamEntity.isFriendlyFire());

    player.sendMessage(
            translateHex("&7Toggled PVP successfully! (&a%s&7)".formatted(teamEntity.isFriendlyFire() ? "ON" : "OFF"))
    );
  }


  @Command(
    name = "team.invite",
    target = CommandTarget.PLAYER
  )
  public void handleInviteCommand(@NotNull Context<Player> context, @Optional String playerUsername) {
    Player player = context.getSender();

    if (playerUsername == null) {
      return;
    }

    Player targetPlayer = Bukkit.getPlayerExact(playerUsername);
    if (targetPlayer == null) {
      String message = teamsVO.getMessage("player-not-found");

      player.sendMessage(message);
      player.sendActionBar(message);

      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
      return;
    }

    TeamMember teamMember = teamsPlugin.getTeamService()
      .getTeamMember(player.getUniqueId());
    if (teamMember == null) {
      String message = teamsVO.getMessage("no-team");

      player.sendMessage(message);
      player.sendActionBar(message);

      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
      return;
    }

    if (!teamMember.hasPermission(TeamPermission.INVITE)) {
      String message = teamsVO.getMessage("no-permission-command");

      player.sendMessage(message);

      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
      return;
    }

    TeamEntity teamEntity = teamsPlugin.getTeamService()
      .getTeamEntity(teamMember.getTeamName());
    if (teamEntity == null) {
      String message = teamsVO.getMessage("team-no-exist");

      player.sendMessage(message);
      player.sendActionBar(message);

      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
      return;
    }

    InviteVO inviteVO = PENDING_INVITES.get(targetPlayer.getUniqueId(), player.getName());
    if (inviteVO != null) {
      if (!inviteVO.isExpired()) {
        String message = teamsVO.getMessage("user-has-pending-invite");

        player.sendMessage(message);
        player.sendActionBar(message);

        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
        return;
      }

      PENDING_INVITES.remove(targetPlayer.getUniqueId(), player.getName());
    }

    inviteVO = new InviteVO(
      player.getUniqueId(),
      teamEntity,
      System.currentTimeMillis() + 30000
    );

    PENDING_INVITES.put(targetPlayer.getUniqueId(), player.getName(), inviteVO);

    String message = teamsVO.getMessage("invite-receive")
                    .replace("%team%", teamEntity.getName())
                            .replace("%inviter%", player.getName());

    targetPlayer.sendMessage(message);
    targetPlayer.sendActionBar(message);

    message = teamsVO.getMessage("invite-sent");

    player.sendMessage(message);
    player.sendActionBar(message);

  }

  @Command(
    name = "team.kick",
    target = CommandTarget.PLAYER
  )
  public void handleKickCommand(@NotNull Context<Player> context, @Optional String playerName) {
    Player player = context.getSender();

    TeamMember teamMember = teamsPlugin.getTeamService()
      .getTeamMember(player.getUniqueId());
    if (teamMember == null) {
      String message = teamsVO.getMessage("no-team");

      player.sendMessage(message);
      player.sendActionBar(message);
      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
      return;
    }

    TeamEntity teamEntity = teamsPlugin.getTeamService()
      .getTeamEntity(teamMember.getTeamName());
    if (teamEntity == null) {
      String message = teamsVO.getMessage("team-no-exist");

      player.sendMessage(message);
      player.sendActionBar(message);

      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
      return;
    }

    if (!teamMember.hasPermission(TeamPermission.DELETE)) {
      String message = teamsVO.getMessage("no-permission-command");

      player.sendMessage(message);
      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
      return;
    }

    if (playerName == null) {
      return;
    }

    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);

    TeamMember targetMember = teamsPlugin.getTeamService()
      .getTeamMember(offlinePlayer.getUniqueId());

    if (targetMember == null) {
      String message = teamsVO.getMessage("user-not-in-team");

      player.sendMessage(message);
      player.sendActionBar(message);

      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
      return;
    }

    if (!teamMember.getTeamName().equals(targetMember.getTeamName())) {
      String message = teamsVO.getMessage("user-not-in-team");

      player.sendMessage(message);
      player.sendActionBar(message);

      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
      return;
    }

    teamEntity.removeMember(targetMember);
    teamsPlugin.getTeamService()
      .remove(targetMember);

    teamsPlugin.getSqlStorage()
      .removeUser(targetMember);

    String message = teamsVO.getMessage("user-kicked-from-team");

    player.sendMessage(message);
  }

  @Command(
    name = "team.leave",
    target = CommandTarget.PLAYER
  )
  public void handleLeaveCommand(@NotNull Context<Player> context) {
    Player player = context.getSender();

    TeamMember teamMember = teamsPlugin.getTeamService()
      .getTeamMember(player.getUniqueId());
    if (teamMember == null) {
      String message = teamsVO.getMessage("no-team");

      player.sendMessage(message);
      player.sendActionBar(message);

      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
      return;
    }

    TeamEntity teamEntity = teamsPlugin.getTeamService()
      .getTeamEntity(teamMember.getTeamName());
    if (teamEntity == null) {
      String message = teamsVO.getMessage("team-no-exist");

      player.sendMessage(message);
      player.sendActionBar(message);

      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
      return;
    }

    if (teamEntity.getLeader().getUniqueId().equals(player.getUniqueId())) {
      teamsPlugin.getViewFrame()
        .open(
          ConfirmTeamDisbandView.class,
          player,
          Map.of(
            "team_entity", teamEntity
          )
        );
      return;
    }

    teamEntity.removeMember(teamMember);
    teamsPlugin.getTeamService()
      .remove(teamMember);

    teamsPlugin.getSqlStorage()
      .removeUser(teamMember);

    String message = teamsVO.getMessage("team-left");

    player.sendMessage(message);
    player.sendActionBar(message);
  }

  @Command(
    name = "team.sethome",
    target = CommandTarget.PLAYER
  )
  public void handleSetHomeCommand(@NotNull Context<Player> context) {
    Player player = context.getSender();

    TeamMember teamMember = teamsPlugin.getTeamService()
      .getTeamMember(player.getUniqueId());
    if (teamMember == null) {
      String message = teamsVO.getMessage("no-team");

      player.sendMessage(message);
      player.sendActionBar(message);

      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
      return;
    }

    TeamEntity teamEntity = teamsPlugin.getTeamService()
      .getTeamEntity(teamMember.getTeamName());

    if (teamEntity == null) {
      String message = teamsVO.getMessage("team-no-exist");

      player.sendMessage(message);
      player.sendActionBar(message);

      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
      return;
    }

    if (!teamMember.hasPermission(TeamPermission.SET_HOME)) {
      String message = teamsVO.getMessage("no-permission-command");

      player.sendMessage(message);
      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
      return;
    }

    if (!WorldGuardUtil.canSetHome(player.getLocation(), player)) {
      String message = teamsVO.getMessage("home-blocked-region");

      player.sendMessage(message);
      player.sendActionBar(message);
      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
      return;
    }

    teamEntity.setHome(player.getLocation());
    for (@NotNull TeamMember member : teamEntity.getMembers()) {
      Player targetPlayer = member.getBukkitPlayer();
      if (targetPlayer != null) {
        Homes.ensureHome(targetPlayer);
      }
    }

    String message = teamsVO.getMessage("team-home-set");

    player.sendMessage(message);
    player.sendActionBar(message);

    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);

    teamsPlugin.getSqlStorage()
      .updateTeam(teamEntity);
  }

  private void scheduleTeleport(@NotNull Player from, @NotNull Location location) {
    PendingTeleportTask task = new PendingTeleportTask(from, location, 5);
    task.setBukkitTask(
      Bukkit.getScheduler()
        .runTaskTimer(teamsPlugin, task, 0, 20)
    );
  }

  @Completer(name = "team")
  public List<String> tabComplete(@NotNull Context<Player> context) {
    String[] args = context.getArgs();

    if (args.length == 1) {
      Player player = context.getSender();

      TeamMember teamMember = teamsPlugin.getTeamService()
        .getTeamMember(player.getUniqueId());
      if (teamMember != null) {
        return List.of("chat", "delhome", "home", "info", "invite", "kick", "leave", "sethome", "pvp");
      } else {
        return List.of("create", "join");
      }
    }

    if (args[0].equalsIgnoreCase("create")) {
      return Collections.emptyList();
    }

    if (args[0].equalsIgnoreCase("join")) {
      if (args.length == 2) {
        return Bukkit.getOnlinePlayers()
          .stream()
          .map(Player::getName)
          .collect(Collectors.toList());
      }

      return Collections.emptyList();
    }

    if (args[0].equalsIgnoreCase("chat")) {
      return Collections.emptyList();
    }

    if (args[0].equalsIgnoreCase("delhome")) {
      return Collections.emptyList();
    }

    if (args[0].equalsIgnoreCase("home")) {
      return Collections.emptyList();
    }

    if (args[0].equalsIgnoreCase("info")) {
      if (args.length == 2) {
        return Bukkit.getOnlinePlayers()
          .stream()
          .map(Player::getName)
          .collect(Collectors.toList());
      }

      return Collections.emptyList();
    }

    if (args[0].equalsIgnoreCase("invite")) {
      if (args.length == 2) {
        return Bukkit.getOnlinePlayers()
          .stream()
          .map(Player::getName)
          .collect(Collectors.toList());
      }

      return Collections.emptyList();
    }

    if (args[0].equalsIgnoreCase("leave")) {
      return Collections.emptyList();
    }

    if (args[0].equalsIgnoreCase("sethome")) {
      return Collections.emptyList();
    }

    return Collections.emptyList();
  }

}
