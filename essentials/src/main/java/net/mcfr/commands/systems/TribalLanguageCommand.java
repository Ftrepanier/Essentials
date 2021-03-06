package net.mcfr.commands.systems;

import java.util.Optional;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import net.mcfr.Essentials;
import net.mcfr.chat.TribalWord;
import net.mcfr.commands.AbstractCommand;

/**
 * Commande avec arborescence sur le langage tribal.<br/>
 *  - <strong>/tribal random</strong>    Envoit une liste aléatoire de mots à un joueur<br/>
 *  - <strong>/tribal add</strong>       Ajoute un mot au dictionnaire<br/>
 *  - <strong>/tribal tribal</strong>    Donne la traduction en langue tribale d'un mot commun<br/>
 *  - <strong>/tribal common</strong>    Donne la traduction en langue commune d'un mot tribal<br/>
 *  - <strong>/tribal list</strong>      Donne la liste des mots d'un certain niveau
 */
public class TribalLanguageCommand extends AbstractCommand {
  public TribalLanguageCommand(Essentials plugin) {
    super(plugin);
  }

  @Override
  public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
    src.sendMessage(Text.of(TextColors.RED, "Merci de renseigner l'action que vous souhaitez effectuer : random, add, tribal, common, list"));
    return CommandResult.empty();
  }

  @Override
  public CommandSpec getCommandSpec() {
 // #f:0
    return CommandSpec.builder()
        .description(Text.of("Commande de dictionnaire tribal."))
        .permission("essentials.command.tribal")
        .executor(this)
        .children(getChildrenList(new RandomWords(getPlugin()), 
            new Add(getPlugin()), 
            new Tribal(getPlugin()), 
            new Common(getPlugin()),
            new List(getPlugin())))
        .build();
    // #f:1
  }

  @Override
  public String[] getAliases() {
    return new String[] { "tribal" };
  }

  static class RandomWords extends AbstractCommand {
    private String message;

    public RandomWords(Essentials plugin) {
      super(plugin);
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
      if (args.hasAny("joueur") && args.hasAny("nombre de mots") && args.hasAny("niveau des mots")) {
        Player player = args.<Player>getOne("joueur").get();

        this.message = "Vous apprenez les mots tribaux suivants (prenez des notes !) :";

        TribalWord.getRandomsByLevel(args.<Integer>getOne("nombre de mots").get(), args.<Integer>getOne("niveau des mots").get())
            .forEach(w -> this.message += "\n- " + w.getTranslationString(false));

        player.sendMessage(Text.of(TextColors.BLUE, this.message));
      } else {
        src.sendMessage(Text.of(TextColors.RED, "Merci de renseigner les arguments : /tribal random <joueur> <nombre de mots> <niveau des mots>"));
      }
      return CommandResult.success();
    }

    @Override
    public CommandSpec getCommandSpec() {
      // #f:0
      return CommandSpec.builder()
          .description(Text.of("Affiche pour le joueur une sélection aléatoire de mots d'un certain niveau."))
          .permission("essentials.command.tribal.random")
          .arguments(GenericArguments.player(Text.of("joueur")),
              GenericArguments.integer(Text.of("nombre de mots")),
              GenericArguments.integer(Text.of("niveau des mots")))
          .executor(this)
          .build();
      // #f:1
    }

    @Override
    public String[] getAliases() {
      return new String[] { "random" };
    }
  }

  static class Add extends AbstractCommand {

    public Add(Essentials plugin) {
      super(plugin);
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
      if (args.hasAny("mot tribal") && args.hasAny("mot commun") && args.hasAny("niveau")) {
        Optional<TribalWord> optWord = TribalWord.add(args.<String>getOne("mot tribal").get(), args.<String>getOne("mot commun").get(),
            args.<Integer>getOne("niveau").get());

        if (optWord.isPresent()) {
          TribalWord word = optWord.get();
          src.sendMessage(
              Text.of(TextColors.YELLOW, "Mot de niveau " + word.getLevel() + " ajouté : " + word.getWord() + " = " + word.getTranslation()));
        } else {
          src.sendMessage(Text.of(TextColors.YELLOW, "Le mot que vous souhaitez ajouter existe déjà dans l'une des deux formes"));
        }
      } else {
        src.sendMessage(Text.of(TextColors.RED, "Merci de renseigner les arguments : /tribal add <mot tribal> <mot commun> <niveau>"));
      }
      return CommandResult.success();
    }

    @Override
    public CommandSpec getCommandSpec() {
      // #f:0
      return CommandSpec.builder()
          .description(Text.of("Ajoute un mot au dictionnaire tribal."))
          .permission("essentials.command.tribal.add")
          .arguments(GenericArguments.string(Text.of("mot tribal")),
              GenericArguments.string(Text.of("mot commun")),
              GenericArguments.integer(Text.of("niveau")))
          .executor(this)
          .build();
      // #f:1
    }

    @Override
    public String[] getAliases() {
      return new String[] { "add" };
    }
  }

  static class Tribal extends AbstractCommand {

    public Tribal(Essentials plugin) {
      super(plugin);
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
      if (args.hasAny("mots")) {
        String[] words = args.<String> getOne("mots").get().split(" ");
        String translatedMessage = "";
        
        for (String word : words) {
          Optional<TribalWord> translatedWord = TribalWord.getByCommonWord(word);
          if (translatedWord.isPresent()) {
            translatedMessage += translatedWord.get().getWord() + " ";
          } else {
            translatedMessage += "### ";
          }
        }
        src.sendMessage(Text.of(TextColors.YELLOW, "Message traduit : " + translatedMessage));
      } else {
        src.sendMessage(Text.of(TextColors.RED, "Merci de renseigner les arguments : /tribal tribal <mot commun>"));
      }
      return CommandResult.success();
    }

    @Override
    public CommandSpec getCommandSpec() {
      // #f:0
      return CommandSpec.builder()
          .description(Text.of("Donne la traduction tribale du mot commun renseigné."))
          .permission("essentials.command.tribal.tribal")
          .arguments(GenericArguments.remainingJoinedStrings(Text.of("mots")))
          .executor(this)
          .build();
      // #f:1
    }

    @Override
    public String[] getAliases() {
      return new String[] { "tribal" };
    }
  }

  static class Common extends AbstractCommand {

    public Common(Essentials plugin) {
      super(plugin);
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
      if (args.hasAny("mots")) {
        String[] words = args.<String> getOne("mots").get().split(" ");
        String translatedMessage = "";
        
        for (String word : words) {
          Optional<TribalWord> translatedWord = TribalWord.getByTribalWord(word);
          if (translatedWord.isPresent()) {
            translatedMessage += translatedWord.get().getTranslation() + " ";
          } else {
            translatedMessage += "### ";
          }
        }
        src.sendMessage(Text.of(TextColors.YELLOW, "Message traduit : " + translatedMessage));
      } else {
        src.sendMessage(Text.of(TextColors.RED, "Merci de renseigner les arguments : /tribal common <mot tribal>"));
      }
      return CommandResult.success();
    }

    @Override
    public CommandSpec getCommandSpec() {
      // #f:0
      return CommandSpec.builder()
          .description(Text.of("Donne la traduction commune des mots tribaux renseignés."))
          .permission("essentials.command.tribal.common")
          .arguments(GenericArguments.remainingJoinedStrings(Text.of("mots")))
          .executor(this)
          .build();
      // #f:1
    }

    @Override
    public String[] getAliases() {
      return new String[] { "common" };
    }
  }

  static class List extends AbstractCommand {
    private String message;

    public List(Essentials plugin) {
      super(plugin);
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
      if (args.hasAny("niveau")) {
        this.message = "Mots de niveau " + args.<Integer>getOne("niveau").get() + " :";

        TribalWord.getByLevel(args.<Integer>getOne("niveau").get()).forEach(w -> this.message += "\n- " + w.getTranslationString(false));

        src.sendMessage(Text.of(TextColors.BLUE, this.message));
      } else {
        src.sendMessage(Text.of(TextColors.RED, "Merci de renseigner les arguments : /tribal list <niveau>"));
      }
      return CommandResult.success();
    }

    @Override
    public CommandSpec getCommandSpec() {
      // #f:0
      return CommandSpec.builder()
          .description(Text.of("Affiche tous les mots d'un certain niveau."))
          .permission("essentials.command.tribal.list")
          .arguments(GenericArguments.integer(Text.of("niveau")))
          .executor(this)
          .build();
      // #f:1
    }

    @Override
    public String[] getAliases() {
      return new String[] { "list" };
    }
  }
}
