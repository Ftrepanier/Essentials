package net.mcfr.utils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.service.sql.SqlService;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

public final class McFrConnection {
  private static String jdbcUrl;
  private static SqlService sql = Sponge.getServiceManager().provide(SqlService.class).get();

  /**
   * Initialise l'url de connection à la base de données.
   */
  static {
    File commandsFile = new File("config/essentials-config/database.json");
    if (commandsFile.exists()) {
      try {
        JsonObject databaseConfig = new JsonParser().parse(new JsonReader(new FileReader(commandsFile))).getAsJsonObject();

        String ip = databaseConfig.get("ip").getAsString();
        String user = databaseConfig.get("user").getAsString();
        String password = databaseConfig.get("password").getAsString();

        String database = databaseConfig.get("database").getAsString();
        jdbcUrl = "jdbc:mysql://" + user + ":" + password + "@" + ip + "/" + database;
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Fournit une connection pour pouvoir des requêtes.
   */
  public static Connection getConnection() {
    try {
      return sql.getDataSource(jdbcUrl).getConnection();
    } catch (SQLException e) {
      e.printStackTrace();
      return null;
    }
  }
}