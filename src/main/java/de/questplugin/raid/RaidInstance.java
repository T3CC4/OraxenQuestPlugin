package de.questplugin.raid;

import de.questplugin.OraxenQuestPlugin;
import de.questplugin.managers.MobEquipmentManager;
import de.questplugin.mobs.api.CustomMob;
import de.questplugin.mobs.api.CustomMobAPI;
import de.questplugin.mobs.api.CustomMobBuilder;
import de.questplugin.utils.MobHelper;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Aktive Raid-Instanz mit Multiplayer & dynamischer Schwierigkeit
 *
 * FEATURES:
 * - Multiplayer-Raid (kein Spectator)
 * - Dynamische Schwierigkeit nach Spieleranzahl
 * - Bessere Belohnungen bei mehr Spielern
 * - BossBar mit Mob-Counter
 * - NETHER-DECKEN-SCHUTZ: Verhindert Spawns auf Y > 115
 */
public class RaidInstance {

    private final OraxenQuestPlugin plugin;
    private final RaidConfig config;
    private final Player player;
    private final Location spawnLocation;
    private final CustomMobAPI mobAPI;

    private BossBar bossBar;
    private RaidState state;
    private int currentWave;
    private final Set<UUID> aliveMobs = new HashSet<>();
    private final Set<UUID> customMobs = new HashSet<>();

    private BukkitTask currentTask;

    // MULTIPLAYER-SYSTEM
    private final Set<UUID> participants = new HashSet<>();
    private static final int PARTICIPATION_RADIUS = 50;
    private double difficultyMultiplier = 1.0;

    public RaidInstance(RaidConfig config, Player player, OraxenQuestPlugin plugin) {
        this.plugin = plugin;
        this.config = config;
        this.player = player;
        this.spawnLocation = player.getLocation().clone();
        this.mobAPI = plugin.getCustomMobAPI();
        this.state = RaidState.PREPARING;
        this.currentWave = 0;

        participants.add(player.getUniqueId());
    }

    public void start() {
        createBossBar();
        startPreparation();
    }

    public void stop() {
        cancel("Manuell gestoppt");
    }

    private void createBossBar() {
        String title = ChatColor.translateAlternateColorCodes('&',
                config.getDisplayName() + " &7- Vorbereitung");

        bossBar = Bukkit.createBossBar(title, BarColor.YELLOW, BarStyle.SOLID);
        bossBar.addPlayer(player);
        bossBar.setVisible(true);
    }

    private void startPreparation() {
        int prepTime = config.getPreparationTime();

        player.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getDisplayName()));
        player.sendMessage(ChatColor.YELLOW + "Vorbereitung: " + prepTime + " Sekunden");
        player.sendMessage(ChatColor.GRAY + "Wellen: " + config.getWaves().size());
        player.sendMessage(ChatColor.AQUA + "Andere Spieler können beitreten!");
        player.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━");

        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);

        final int[] countdown = {prepTime};

        currentTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || !isNearSpawnLocation()) {
                cancel("Spieler zu weit entfernt!");
                return;
            }

            countdown[0]--;
            double progress = (double) countdown[0] / prepTime;
            bossBar.setProgress(Math.max(0, Math.min(1, progress)));

            updateParticipants();

            String title = ChatColor.YELLOW + "Vorbereitung: " + countdown[0] + "s " +
                    ChatColor.GRAY + "[" + ChatColor.GREEN + participants.size() +
                    ChatColor.GRAY + " Spieler]";
            bossBar.setTitle(title);

            if (countdown[0] <= 10 && countdown[0] >= 1) {
                playParticipantSound(Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.5f);
            }

            if (countdown[0] <= 0) {
                currentTask.cancel();
                calculateDifficulty();
                startNextWave();
            }

        }, 0L, 20L);
    }

    /**
     * Berechnet Schwierigkeitsmultiplikator basierend auf Spieleranzahl
     */
    private void calculateDifficulty() {
        int playerCount = participants.size();

        // Formel: 1.0 + (Spieler - 1) * 0.4
        // 1 Spieler = 1.0x
        // 2 Spieler = 1.4x
        // 3 Spieler = 1.8x
        // 4 Spieler = 2.2x
        difficultyMultiplier = 1.0 + ((playerCount - 1) * 0.4);

        plugin.getPluginLogger().info("Schwierigkeit: " + playerCount + " Spieler = " +
                String.format("%.1f", difficultyMultiplier) + "x");

        sendParticipantMessage(ChatColor.YELLOW + "Schwierigkeit: " +
                ChatColor.RED + String.format("%.1f", difficultyMultiplier) + "x " +
                ChatColor.GRAY + "(" + playerCount + " Spieler)");
    }

    private void startNextWave() {
        currentWave++;

        if (currentWave > config.getWaves().size()) {
            complete();
            return;
        }

        state = RaidState.ACTIVE;
        WaveConfig wave = config.getWaves().get(currentWave - 1);

        plugin.getPluginLogger().debug("=== Start Wave " + currentWave + " ===");
        plugin.getPluginLogger().debug("Spieler: " + participants.size());
        plugin.getPluginLogger().debug("Schwierigkeit: " + difficultyMultiplier + "x");

        String title = ChatColor.translateAlternateColorCodes('&',
                config.getDisplayName() + " &7- " + wave.getDisplayName());
        bossBar.setTitle(title);
        bossBar.setColor(BarColor.RED);
        bossBar.setProgress(1.0);

        sendParticipantMessage("");
        sendParticipantMessage(ChatColor.translateAlternateColorCodes('&', wave.getDisplayName()));
        playParticipantSound(Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);

        spawnWave(wave);
        startWaveMonitoring(wave);
    }

    private void spawnWave(WaveConfig wave) {
        double totalMultiplier = config.getDifficultyMultiplier() * difficultyMultiplier;
        int spawnRadius = config.getSpawnRadius();

        plugin.getPluginLogger().debug("=== Spawn Wave " + currentWave + " ===");
        plugin.getPluginLogger().debug("Multiplikator: " + totalMultiplier + "x");

        for (WaveConfig.MobSpawn mobSpawn : wave.getMobs()) {
            // Erhöhe Mob-Anzahl basierend auf Spieleranzahl
            int baseAmount = mobSpawn.getAmount();
            int scaledAmount = (int) Math.ceil(baseAmount * Math.sqrt(participants.size()));

            plugin.getPluginLogger().debug("Spawne " + scaledAmount + "x " + mobSpawn.getType() +
                    " (Base: " + baseAmount + ")");

            for (int i = 0; i < scaledAmount; i++) {
                try {
                    Location loc = getRandomSpawnLocation(spawnRadius);

                    if (loc.getWorld() == null) {
                        plugin.getLogger().severe("Spawn-Location hat keine World!");
                        continue;
                    }

                    CustomMobBuilder builder = mobAPI.createMob(mobSpawn.getType())
                            .at(loc)
                            .withLevel((int) totalMultiplier)
                            .withHealth(mobSpawn.getHealth() * totalMultiplier)
                            .withDamage(mobSpawn.getDamage() * totalMultiplier);

                    if (mobSpawn.getCustomName() != null) {
                        builder.withName(ChatColor.translateAlternateColorCodes('&',
                                mobSpawn.getCustomName()));
                    }

                    if (mobSpawn.hasAbilities()) {
                        for (String abilityId : mobSpawn.getAbilities()) {
                            builder.withAbility(abilityId);
                        }
                    }

                    CustomMob customMob = builder.spawn();
                    LivingEntity entity = customMob.getEntity();

                    aliveMobs.add(entity.getUniqueId());
                    customMobs.add(entity.getUniqueId());

                    if (mobSpawn.hasEquipment()) {
                        applyEquipment(entity);
                    }

                    entity.setRemoveWhenFarAway(false);
                    entity.setPersistent(true);

                } catch (Exception e) {
                    plugin.getLogger().severe("Fehler beim Spawnen: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        plugin.getPluginLogger().info("Welle " + currentWave + ": " +
                aliveMobs.size() + " Mobs gespawnt");
    }

    /**
     * Holt zufällige Spawn-Location mit NETHER-DECKEN-SCHUTZ
     *
     * WICHTIG: Im Nether wird verhindert dass Mobs auf Y > 115 spawnen
     * (Bedrock-Decke ist bei Y=128, darüber ist das Dach)
     */
    private Location getRandomSpawnLocation(int radius) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double angle = random.nextDouble() * 2 * Math.PI;
        double distance = random.nextDouble(radius / 2.0, radius);

        double x = spawnLocation.getX() + distance * Math.cos(angle);
        double z = spawnLocation.getZ() + distance * Math.sin(angle);

        World world = spawnLocation.getWorld();
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);

        int startY;
        if (world.getEnvironment() == World.Environment.NETHER) {
            // WICHTIG: Nether-Decke ist bei Y=128
            // Verhindere Spawns darüber (z.B. auf Bedrock-Dach bei Y=127+)
            int playerY = spawnLocation.getBlockY();

            // Wenn Spieler über Y=120, spawne bei Y=100 (sicherer Bereich)
            if (playerY > 120) {
                startY = 100;
                plugin.getPluginLogger().debug("Nether: Spieler bei Y=" + playerY +
                        " (zu hoch), spawne bei Y=100");
            } else {
                // Normaler Fall: Spawne in der Nähe des Spielers
                // Aber maximal bei Y=115 (nicht zu nah an Decke)
                startY = Math.max(10, Math.min(115, playerY));
            }
        } else {
            startY = world.getHighestBlockYAt(blockX, blockZ) + 1;
        }

        Location safeLoc = findSafeSpawnPoint(world, blockX, startY, blockZ);

        if (safeLoc != null) {
            return safeLoc;
        }

        // Fallback: Nutze Spieler-Position aber limitiert
        Location fallback = spawnLocation.clone().add(
                random.nextDouble(-5, 5),
                0,
                random.nextDouble(-5, 5)
        );

        // WICHTIG: Auch Fallback auf Y=115 im Nether limitieren
        if (world.getEnvironment() == World.Environment.NETHER &&
                fallback.getBlockY() > 115) {
            fallback.setY(100);
            plugin.getPluginLogger().debug("Fallback: Y auf 100 gesetzt (Nether-Decken-Schutz)");
        }

        return fallback;
    }

    private Location findSafeSpawnPoint(World world, int x, int startY, int z) {
        for (int y = startY; y > Math.max(1, startY - 20); y--) {
            Location checkLoc = new Location(world, x, y, z);
            Material below = checkLoc.clone().subtract(0, 1, 0).getBlock().getType();
            Material at = checkLoc.getBlock().getType();
            Material above = checkLoc.clone().add(0, 1, 0).getBlock().getType();

            boolean solidBelow = below.isSolid() && !below.isInteractable();
            boolean airAt = at.isAir() || !at.isSolid();
            boolean airAbove = above.isAir() || !above.isSolid();
            boolean safeSurface = !isHazardous(below) && !isHazardous(at) && !isHazardous(above);

            if (solidBelow && airAt && airAbove && safeSurface) {
                return checkLoc.add(0.5, 0, 0.5);
            }
        }

        return null;
    }

    private boolean isHazardous(Material material) {
        return material == Material.LAVA ||
                material == Material.FIRE ||
                material == Material.SOUL_FIRE ||
                material == Material.MAGMA_BLOCK ||
                material == Material.WITHER_ROSE ||
                material == Material.SWEET_BERRY_BUSH ||
                material == Material.CACTUS;
    }

    private void applyEquipment(LivingEntity mob) {
        MobEquipmentManager equipmentManager = plugin.getMobEquipmentManager();
        List<MobEquipmentManager.EquipmentEntry> equipment =
                equipmentManager.getEquipment(mob.getType());

        if (equipment.isEmpty()) {
            return;
        }

        EntityEquipment entityEquipment = mob.getEquipment();
        if (entityEquipment == null) return;

        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (MobEquipmentManager.EquipmentEntry entry : equipment) {
            double roll = random.nextDouble() * 100;

            if (roll < entry.getChance()) {
                ItemStack item = buildOraxenItem(entry.getOraxenItemId());

                if (item != null) {
                    switch (entry.getSlot()) {
                        case MAIN_HAND:
                            entityEquipment.setItemInMainHand(item);
                            entityEquipment.setItemInMainHandDropChance(entry.getDropChance());
                            break;
                        case OFF_HAND:
                            entityEquipment.setItemInOffHand(item);
                            entityEquipment.setItemInOffHandDropChance(entry.getDropChance());
                            break;
                        case HELMET:
                            entityEquipment.setHelmet(item);
                            entityEquipment.setHelmetDropChance(entry.getDropChance());
                            break;
                        case CHESTPLATE:
                            entityEquipment.setChestplate(item);
                            entityEquipment.setChestplateDropChance(entry.getDropChance());
                            break;
                        case LEGGINGS:
                            entityEquipment.setLeggings(item);
                            entityEquipment.setLeggingsDropChance(entry.getDropChance());
                            break;
                        case BOOTS:
                            entityEquipment.setBoots(item);
                            entityEquipment.setBootsDropChance(entry.getDropChance());
                            break;
                    }
                }
            }
        }
    }

    private ItemStack buildOraxenItem(String oraxenId) {
        try {
            io.th0rgal.oraxen.items.ItemBuilder builder =
                    io.th0rgal.oraxen.api.OraxenItems.getItemById(oraxenId);

            if (builder != null) {
                return builder.build();
            }

            plugin.getPluginLogger().warn("Oraxen-Item nicht gefunden: " + oraxenId);
            return null;

        } catch (Exception e) {
            plugin.getLogger().warning("Fehler beim Laden von Item '" +
                    oraxenId + "': " + e.getMessage());
            return null;
        }
    }

    private void startWaveMonitoring(WaveConfig wave) {
        int totalMobs = aliveMobs.size();

        currentTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                cancel("Host offline!");
                return;
            }

            aliveMobs.removeIf(uuid -> {
                Entity entity = Bukkit.getEntity(uuid);
                return entity == null || entity.isDead();
            });

            double progress = (double) aliveMobs.size() / totalMobs;
            bossBar.setProgress(Math.max(0, Math.min(1, progress)));

            String title = ChatColor.translateAlternateColorCodes('&',
                    config.getDisplayName() + " &7- " + wave.getDisplayName());
            bossBar.setTitle(title + ChatColor.GRAY + " [" +
                    ChatColor.RED + aliveMobs.size() + ChatColor.GRAY + "/" +
                    ChatColor.WHITE + totalMobs + ChatColor.GRAY + "]");

            updateParticipants();

            if (aliveMobs.isEmpty()) {
                currentTask.cancel();
                onWaveComplete(wave);
            }

        }, 0L, 20L);
    }

    private void onWaveComplete(WaveConfig wave) {
        sendParticipantMessage(ChatColor.GREEN + "✓ " + wave.getDisplayName() + " abgeschlossen!");
        playParticipantSound(Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        if (currentWave >= config.getWaves().size()) {
            complete();
            return;
        }

        int delay = wave.getDelayAfterWave();
        if (delay > 0) {
            bossBar.setTitle(ChatColor.YELLOW + "Nächste Welle in " + delay + "s");
            bossBar.setColor(BarColor.YELLOW);
            bossBar.setProgress(1.0);

            Bukkit.getScheduler().runTaskLater(plugin, this::startNextWave, delay * 20L);
        } else {
            startNextWave();
        }
    }

    private void complete() {
        state = RaidState.COMPLETED;

        sendParticipantMessage("");
        sendParticipantMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sendParticipantMessage(ChatColor.GREEN + "✔ RAID ABGESCHLOSSEN!");
        sendParticipantMessage(ChatColor.translateAlternateColorCodes('&', config.getDisplayName()));
        sendParticipantMessage(ChatColor.AQUA + "Teilnehmer: " + participants.size());
        sendParticipantMessage(ChatColor.YELLOW + "Schwierigkeit: " +
                String.format("%.1f", difficultyMultiplier) + "x");
        sendParticipantMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━");

        playParticipantSound(Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.8f);

        // Belohnungen für alle Teilnehmer mit Spieleranzahl
        int playerCount = participants.size();
        for (UUID uuid : participants) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                config.getRewards().giveRewards(p, plugin, difficultyMultiplier, playerCount);
            }
        }

        cleanup();
    }

    public void cancel(String reason) {
        state = RaidState.CANCELLED;

        sendParticipantMessage(ChatColor.RED + "Raid abgebrochen: " + reason);
        playParticipantSound(Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);

        cleanup();
    }

    private void cleanup() {
        if (currentTask != null) {
            currentTask.cancel();
            currentTask = null;
        }

        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }

        for (UUID uuid : aliveMobs) {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null) {
                if (customMobs.contains(uuid)) {
                    mobAPI.removeCustomMob(uuid);
                }
                entity.remove();
            }
        }
        aliveMobs.clear();
        customMobs.clear();
        participants.clear();

        plugin.getRaidManager().removeRaid(player.getUniqueId());
    }

    private boolean isNearSpawnLocation() {
        return player.getLocation().distance(spawnLocation) <= 50;
    }

    // ==================== MULTIPLAYER-SYSTEM ====================

    private void updateParticipants() {
        Set<UUID> newParticipants = new HashSet<>();
        newParticipants.add(player.getUniqueId()); // Host immer dabei

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getUniqueId().equals(player.getUniqueId())) {
                continue;
            }

            if (!p.getWorld().equals(spawnLocation.getWorld())) {
                continue;
            }

            if (p.getLocation().distance(spawnLocation) <= PARTICIPATION_RADIUS) {
                newParticipants.add(p.getUniqueId());

                if (!participants.contains(p.getUniqueId())) {
                    onParticipantJoin(p);
                }

                if (bossBar != null && !bossBar.getPlayers().contains(p)) {
                    bossBar.addPlayer(p);
                }
            }
        }

        for (UUID uuid : participants) {
            if (!newParticipants.contains(uuid)) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && !uuid.equals(player.getUniqueId())) {
                    onParticipantLeave(p);
                    if (bossBar != null) {
                        bossBar.removePlayer(p);
                    }
                }
            }
        }

        participants.clear();
        participants.addAll(newParticipants);
    }

    private void onParticipantJoin(Player participant) {
        participant.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━");
        participant.sendMessage(ChatColor.GREEN + "Du nimmst am Raid teil!");
        participant.sendMessage(ChatColor.GRAY + "Host: " + ChatColor.WHITE + player.getName());
        participant.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getDisplayName()));
        participant.sendMessage(ChatColor.YELLOW + "Die Schwierigkeit wird erhöht!");
        participant.sendMessage(ChatColor.AQUA + "Du erhältst auch Belohnungen!");
        participant.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━");
        participant.playSound(participant.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);

        sendParticipantMessage(ChatColor.GREEN + participant.getName() + " nimmt am Raid teil!");
    }

    private void onParticipantLeave(Player participant) {
        participant.sendMessage(ChatColor.RED + "Du bist zu weit vom Raid entfernt!");
        sendParticipantMessage(ChatColor.RED + participant.getName() + " hat den Raid verlassen!");
    }

    private void playParticipantSound(Sound sound, float volume, float pitch) {
        for (UUID uuid : participants) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.playSound(p.getLocation(), sound, volume, pitch);
            }
        }
    }

    private void sendParticipantMessage(String message) {
        for (UUID uuid : participants) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage(message);
            }
        }
    }

    public Set<Player> getParticipants() {
        Set<Player> players = new HashSet<>();
        for (UUID uuid : participants) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                players.add(p);
            }
        }
        return players;
    }

    // ==================== GETTER ====================

    public Player getPlayer() { return player; }
    public RaidConfig getConfig() { return config; }
    public RaidState getState() { return state; }
    public int getCurrentWave() { return currentWave; }
    public int getAliveMobCount() { return aliveMobs.size(); }
    public int getParticipantCount() { return participants.size(); }
    public double getDifficultyMultiplier() { return difficultyMultiplier; }

    public enum RaidState {
        PREPARING,
        ACTIVE,
        COMPLETED,
        CANCELLED
    }
}