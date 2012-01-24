package com.sk89q.commandbook.locations;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.util.yaml.YAMLFormat;
import com.sk89q.util.yaml.YAMLProcessor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class WrappedSpawnManager {

    /**
     * The configuration that stores world spawn pitches and yaws
     */
    private final YAMLProcessor config;

    /**
     * The configuration file's header
     */
    private static final String CONFIG_HEADER = "#\r\n" +
                "# * CommndBook world spawn enrichment file\r\n" +
                "#\r\n" +
                "# WARNING: THIS FILE IS AUTOMATICALLY GENERATED. If you modify this file by\r\n" +
                "# hand, be aware that A SINGLE MISTYPED CHARACTER CAN CORRUPT THE FILE. If\r\n" +
                "# CommandBook is unable to parse the file, your world spawns will FAIL TO LOAD and\r\n" +
                "# the contents of this file may reset. Please use a YAML validator such as\r\n" +
                "# http://yaml-online-parser.appspot.com (for smaller files).\r\n" +
                "#\r\n" +
                "# REMEMBER TO KEEP PERIODICAL BACKUPS.\r\n" +
                "#\r\n";

    /**
     * The map that stores enriched spawns loaded from the config file
     */
    private final Map<String, WrappedSpawn> storedSpawns = new HashMap<String, WrappedSpawn>();

    public WrappedSpawnManager(File configFile) {
        configFile.getParentFile().mkdirs();
        if (!configFile.exists())
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                CommandBook.logger().log(Level.SEVERE, "CommandBook: Spawn storage file creation error: {0}", e.getMessage());
            }
        config = new YAMLProcessor(configFile, true, YAMLFormat.COMPACT);
        load();
    }

    public void load() {
        storedSpawns.clear();
        try {
            config.load();
        } catch (IOException ignore) {}
        for (World world : Bukkit.getServer().getWorlds())
            loadWorld(world);
    }

    private WrappedSpawn loadWorld(World world) {
        WrappedSpawn wrapper = new WrappedSpawn(world,
                Double.valueOf(config.getDouble(world.getName() + ".pitch", 0)).floatValue(),
                Double.valueOf(config.getDouble(world.getName() + ".yaw", 0)).floatValue());
        storedSpawns.put(world.getName(), wrapper);
        return wrapper;
    }

    public Location getWorldSpawn(World world) {
        WrappedSpawn wrapper = getEnrichment(world);
        return wrapper.getLocation();
    }

    public WrappedSpawn setWorldSpawn(Location loc) {
        WrappedSpawn spawn = getEnrichment(loc.getWorld());
        loc.getWorld().setSpawnLocation(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        spawn.setPitch(loc.getPitch());
        spawn.setYaw(loc.getYaw());
        config.setProperty(spawn.getWorldName() + ".pitch", spawn.getPitch());
        config.setProperty(spawn.getWorldName() + ".yaw", spawn.getYaw());
        config.setHeader(CONFIG_HEADER);
        config.save();
        return spawn;
    }

    private WrappedSpawn getEnrichment(World world) {
        WrappedSpawn wrapper = storedSpawns.get(world.getName());
        if (wrapper == null) {
            wrapper = loadWorld(world);
        }
        return wrapper;
    }
}
