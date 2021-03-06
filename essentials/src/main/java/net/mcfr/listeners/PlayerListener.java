package net.mcfr.listeners;

import java.util.Calendar;
import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.manipulator.mutable.PotionEffectData;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.effect.potion.PotionEffectTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.entity.projectile.arrow.Arrow;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSources;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.entity.living.humanoid.player.RespawnPlayerEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import net.mcfr.chat.MessageData;
import net.mcfr.commands.roleplay.KeyCommand;
import net.mcfr.death.CareImp;
import net.mcfr.death.CareService;
import net.mcfr.expedition.ExpeditionService;
import net.mcfr.locks.LocksService;
import net.mcfr.mecanisms.keys.McfrCodedItem;
import net.mcfr.roleplay.Skill;
import net.mcfr.utils.McFrPlayer;
import net.minecraft.entity.player.EntityPlayer;

public class PlayerListener {
  private final long LAST_BREATH_INVICIBILITY = 2000;
  private final long LAST_BREATH_DELAY = 15000;
  
  @Listener
  public void onPlayerMove(MoveEntityEvent e, @First Player p) {
    if (p != null) {
      Optional<CareService> careService = Sponge.getServiceManager().provide(CareService.class);
      if (careService.isPresent() && careService.get() instanceof CareImp) {
        ((CareImp) careService.get()).trackPlayer(p);
      }

      Optional<ExpeditionService> optExpeditionService = Sponge.getServiceManager().provide(ExpeditionService.class);
      if (optExpeditionService.isPresent()) {
        optExpeditionService.get().actualizePlayerState(p);
      }

    }
  }

  @Listener
  public void onDamageEntity(DamageEntityEvent e) {
    if (e.getTargetEntity() instanceof Player) {
      Player player = (Player) e.getTargetEntity();

      if (McFrPlayer.getMcFrPlayer(player).isGod()) {
        e.setCancelled(true);
        return;
      }

      double health = player.health().get();
      double damage = e.getOriginalFinalDamage();

      if (damage >= health) {
        long lastBreathTime = Calendar.getInstance().getTime().getTime() - McFrPlayer.getMcFrPlayer(player).getLastBreathTime();

        if (lastBreathTime > this.LAST_BREATH_DELAY) {
          player.damage(health - 0.5D, DamageSources.GENERIC);
          e.setCancelled(true);
          McFrPlayer.getMcFrPlayer(player).updateLastBreathTime();

          // #f:0
          PotionEffectData effects = player.getOrCreate(PotionEffectData.class).get();
          effects.addElement(PotionEffect.builder()
              .potionType(PotionEffectTypes.SLOWNESS)
              .duration(300)
              .amplifier(3)
              .particles(false)
              .build());
          player.offer(effects);
          // #f:1

          player.sendMessage(Text.of(TextColors.DARK_RED, "Vous arrivez à votre dernier souffle. Encore un peu et vous mourrez."));

        } else if (lastBreathTime < this.LAST_BREATH_INVICIBILITY) {
          player.damage(health - 0.5D, DamageSources.GENERIC);
          e.setCancelled(true);
        }
      }

    }
  }

  @Listener
  public void onPlayerDeath(DestructEntityEvent.Death e) {
    if (e.getTargetEntity() instanceof Player) {
      Optional<CareService> optCareService = Sponge.getServiceManager().provide(CareService.class);
      if (optCareService.isPresent()) {
        optCareService.get().manageDeath((Player) e.getTargetEntity());
      }
    }
  }

  @Listener
  public void onPlayerRespawn(RespawnPlayerEvent e) {
    Optional<CareService> optCareService = Sponge.getServiceManager().provide(CareService.class);
    if (optCareService.isPresent()) {
      optCareService.get().manageRespawn(e.getTargetEntity());
    }
  }

  @Listener
  public void onPlayerRightClick(InteractEntityEvent.Secondary e, @First Player player) {
    Entity target = e.getTargetEntity();
    if (target instanceof Player) {
      if (Calendar.getInstance().getTime().getTime() - McFrPlayer.getMcFrPlayer(player).getReadDescriptionTime() > 100) {
        McFrPlayer.getMcFrPlayer(player).updateReadDescriptionTime();
        McFrPlayer otherPlayer = McFrPlayer.getMcFrPlayer((Player) e.getTargetEntity());
        player.sendMessage(Text.of(TextColors.DARK_GREEN, "* " + otherPlayer.getName() + " * " + otherPlayer.getDescription() + " *"));
      }
    }
  }
  
  @Listener
  public void onInteractBlock(InteractBlockEvent.Secondary e, @First Player player) {
    Optional<LocksService> optLocksService = Sponge.getServiceManager().provide(LocksService.class);
    
    if (e.getTargetBlock() != BlockSnapshot.NONE && optLocksService.isPresent()) {
      BlockType block = e.getTargetBlock().getState().getType();
      LocksService locksService = optLocksService.get();
      
      if (locksService.getLockableBlocks().contains(block)) {
        Optional<ItemStack> item = player.getItemInHand(HandTypes.MAIN_HAND);
            
        if (item.isPresent() && item.get().getItem() == KeyCommand.LOCK_ITEM) {
          switch (locksService.addLock(e.getTargetBlock().getPosition(), player.getWorld(), ((McfrCodedItem)item.get().getItem()).getCode((EntityPlayer)player))) {
          case ALREADY_ADDED:
            player.sendMessage(Text.of(TextColors.YELLOW, "Une serrure est déjà présente ici."));
            e.setCancelled(true);
            break;
          case ADDED:
            player.sendMessage(Text.of(TextColors.YELLOW, "La serrure a été installée."));
            player.setItemInHand(HandTypes.MAIN_HAND, null);
            e.setCancelled(true);
            break;
          case WRONG_CODE:
            player.sendMessage(Text.of(TextColors.YELLOW, "La serrure tenue en main est vierge."));
            e.setCancelled(true);
          default:
            break;
          }
        } else if (item.isPresent() && item.get().getItem() == KeyCommand.KEY_ITEM) {
          switch (locksService.switchLock(e.getTargetBlock().getPosition(), player.getWorld(), ((McfrCodedItem)item.get().getItem()).getCode((EntityPlayer)player))) {
          case UNLOCKED:
            player.sendMessage(Text.of(TextColors.YELLOW, "La serrure a été déverrouillée."));
            e.setCancelled(true);
            break;
          case LOCKED:
            player.sendMessage(Text.of(TextColors.YELLOW, "La serrure a été verrouillée."));
            e.setCancelled(true);
            break;
          case WRONG_CODE:
            player.sendMessage(Text.of(TextColors.YELLOW, "Ce n'est pas la bonne clé."));
            e.setCancelled(true);
            break;
          default:
            break;
          }
        } else {
          if (player.gameMode().get() != GameModes.CREATIVE && locksService.isLocked(e.getTargetBlock().getPosition(), player.getWorld())) {
            player.sendMessage(Text.of(TextColors.YELLOW, "La serrure est verrouillée."));
            e.setCancelled(true);
          }
        }
      }
    }
  }

  /**
   * Déclenché quand un item est looté depuis un bloc cassé ou une entité tuée
   */
  @Listener
  public void onLootItem(DropItemEvent.Destruct e) {
    boolean mustLoot = true;

    Optional<EntityDamageSource> optDamageSource = e.getCause().first(EntityDamageSource.class);

    if (optDamageSource.isPresent()) {
      mustLoot = false;
      Entity source = optDamageSource.get().getSource();

      if (source instanceof Player) {
        McFrPlayer player = McFrPlayer.getMcFrPlayer((Player) source);
        int skillLevel = player.getSkillLevel(Skill.getSkillByName("chasse"), Optional.empty());

        if (skillLevel >= 12) {
          mustLoot = true;
        }
      } else if (source instanceof Arrow) {
        Arrow sourceArrow = (Arrow) source;
        Optional<UUID> arrowSource = ((Arrow) source).getCreator();
        if (arrowSource.isPresent()) {
          Optional<Entity> optEntity = Sponge.getServer().getWorld("world").get().getEntity(sourceArrow.getCreator().get());
          if (optEntity.isPresent() && optEntity.get() instanceof Player) {
            McFrPlayer player = McFrPlayer.getMcFrPlayer((Player) optEntity.get());
            int skillLevel = player.getSkillLevel(Skill.getSkillByName("chasse"), Optional.empty());

            if (skillLevel >= 12) {
              mustLoot = true;
            }
          }
        }
      }
    }

    if (!mustLoot) {
      e.setCancelled(true);
    }
  }

  @Listener
  public void onClientConnectionMessage(MessageChannelEvent e) {
    if (e instanceof ClientConnectionEvent) {
      e.setMessageCancelled(true);
    }
  }

  @Listener
  public void onMessageChannelEvent(MessageChannelEvent.Chat e, @First CommandSource sender) {

    if (sender instanceof Player) {
      MessageData data = new MessageData((Player) sender, e.getRawMessage().toPlain());
      if (data.checkConditions()) {
        data.send();
      }

      e.setCancelled(true);
    }
  }
}
