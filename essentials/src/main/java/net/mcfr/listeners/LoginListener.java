package net.mcfr.listeners;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Optional;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import net.mcfr.Essentials;
import net.mcfr.death.CareService;
import net.mcfr.utils.McFrConnection;
import net.mcfr.utils.McFrPlayer;

public class LoginListener {
  private Essentials plugin;

  public LoginListener(Essentials plugin) {
    this.plugin = plugin;
  }

  @Listener
  public void onPlayerJoin(ClientConnectionEvent.Join e) {
    McFrPlayer player = new McFrPlayer(e.getTargetEntity());
    McFrPlayer.addPlayer(player);
    player.setConnectionTime(Calendar.getInstance().getTime().getTime());
    player.loadFromDataBase();

    Optional<CareService> optCareService = Sponge.getServiceManager().provide(CareService.class);
    if (optCareService.isPresent()) {
      if (optCareService.get().isInProtectedArea(player.getPlayer()))
        player.getPlayer().sendMessage(Text.of(TextColors.YELLOW, "Vous êtes dans une zone sécurisée."));
      else
        player.getPlayer().sendMessage(Text.of(TextColors.GOLD, "Attention, vous êtes encore dans une zone non sécurisée !"));
    }
    Sponge.getCommandManager().process(player.getPlayer(), "date");
    Sponge.getCommandManager().process(player.getPlayer(), "meteo");
  }

  @Listener
  public void onPlayerLogin(ClientConnectionEvent.Login e) {
    if (this.plugin.isServerLocked() && !e.getTargetUser().hasPermission("essentials.admin.log_when_lock"))
      e.setCancelled(true);
    else
      try (Connection jdrConnection = McFrConnection.getConnection()) {
        final String connectPermission = "essentials.admin.connect_without_character";
        int userId = -1;
        PreparedStatement forumAccountId = jdrConnection
            .prepareStatement("SELECT user_id FROM phpbb_users PU JOIN account_link AL ON AL.forum = PU.username WHERE AL.minecraft = ?");
        forumAccountId.setString(1, e.getTargetUser().getName());
        ResultSet user = forumAccountId.executeQuery();

        if (user.next()) {
          userId = user.getInt(1);
          PreparedStatement activeCharacterSheet = jdrConnection
              .prepareStatement("SELECT id FROM fiche_perso_personnage WHERE id_user = ? AND active = 1");
          activeCharacterSheet.setInt(1, userId);
          ResultSet characterSheet = activeCharacterSheet.executeQuery();

          if (characterSheet.next()) {
            PreparedStatement deathDataReq = jdrConnection
                .prepareStatement("SELECT avantage FROM fiche_perso_personnage_avantage WHERE avantage = \"mort\" AND id_fiche_perso_personnage = ?");
            deathDataReq.setInt(1, characterSheet.getInt(1));
            ResultSet deathData = deathDataReq.executeQuery();
            if (deathData.next())
              e.setCancelled(!e.getTargetUser().hasPermission(connectPermission));
            deathData.close();

          } else
            e.setCancelled(!e.getTargetUser().hasPermission(connectPermission));
          characterSheet.close();
        } else
          e.setCancelled(!e.getTargetUser().hasPermission(connectPermission));
        user.close();
      } catch (SQLException ex) {
        ex.printStackTrace();
      }
  }

  @Listener
  public void onPlayerDisconnect(ClientConnectionEvent.Disconnect e) {
    McFrPlayer.getMcFrPlayer(e.getTargetEntity()).logSession();
    McFrPlayer.removePlayer(e.getTargetEntity());
  }
}
