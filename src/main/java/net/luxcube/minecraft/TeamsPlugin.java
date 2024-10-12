package net.luxcube.minecraft;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import lombok.Getter;
import me.saiintbrisson.bukkit.command.BukkitFrame;
import me.saiintbrisson.minecraft.ViewFrame;
import net.luxcube.minecraft.command.TeamCommand;
import net.luxcube.minecraft.entity.TeamEntity;
import net.luxcube.minecraft.entity.member.TeamMember;
import net.luxcube.minecraft.extension.TeamPlaceholderExtension;
import net.luxcube.minecraft.listener.TeamChatListener;
import net.luxcube.minecraft.listener.TeamConnectionListener;
import net.luxcube.minecraft.listener.TeamPvpListener;
import net.luxcube.minecraft.service.TeamService;
import net.luxcube.minecraft.storage.SQLStorage;
import net.luxcube.minecraft.view.ConfirmTeamDisbandView;
import net.luxcube.minecraft.view.TeamInfoView;
import net.luxcube.minecraft.vo.TeamsVO;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Getter
public class TeamsPlugin extends JavaPlugin {

  public static StateFlag ALLOWED_SET_HOME;

  @NotNull
  public static TeamsPlugin getInstance() {
    return getPlugin(TeamsPlugin.class);
  }

  private SQLStorage sqlStorage;
  private TeamService teamService;

  private Economy economy;

  private ViewFrame viewFrame;

  private TeamsVO teamsVO;

  private boolean hasDonutHomes;

  @Override
  public void onLoad() {
    saveDefaultConfig();

    teamsVO = TeamsVO.construct(getConfig());

    FlagRegistry registry = WorldGuard.getInstance()
      .getFlagRegistry();
    try {
      // create a flag with the name "my-custom-flag", defaulting to true
      StateFlag flag = new StateFlag("allowed-set-home", true);
      registry.register(flag);
      ALLOWED_SET_HOME = flag; // only set our field if there was no error
    } catch (FlagConflictException e) {
      // some other plugin registered a flag by the same name already.
      // you can use the existing flag, but this may cause conflicts - be sure to check type
      Flag<?> existing = registry.get("allowed-set-home");
      if (existing instanceof StateFlag) {
        ALLOWED_SET_HOME = (StateFlag) existing;
      }
    }
  }

  @Override
  public void onEnable() {

    economy = Bukkit.getServicesManager()
      .load(Economy.class);

    this.teamService = new TeamService();

    List.of(
      new TeamConnectionListener(this),
      new TeamPvpListener(this),
            new TeamChatListener(this)
    ).forEach(listener -> Bukkit.getPluginManager().registerEvents(listener, this));

    sqlStorage = new SQLStorage(this);
    sqlStorage.setup();

    viewFrame = ViewFrame.of(
      this,
      new TeamInfoView(this, teamsVO.getTeamInfoView()),
      new ConfirmTeamDisbandView(this, teamsVO.getConfirmTeamDisbandView())
    );
    viewFrame.register();

    BukkitFrame bukkitFrame = new BukkitFrame(this);
    bukkitFrame.registerCommands(
      new TeamCommand(this)
    );

    hasDonutHomes = Bukkit.getPluginManager().getPlugin("DonutHomes") != null;

    if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
      new TeamPlaceholderExtension(this).register();
    }
  }

  @Override
  public void onDisable() {
    for (@NotNull TeamEntity team : teamService.getAllTeams()) {
      sqlStorage.updateTeam(team);
    }

    sqlStorage.close();
  }
}
