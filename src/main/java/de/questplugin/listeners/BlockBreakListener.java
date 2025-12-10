package de.questplugin.listeners;

import de.questplugin.OraxenQuestPlugin;
import de.questplugin.utils.AEAPIHelper;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener für Block-Drops
 */
public class BlockBreakListener implements Listener {

    private final OraxenQuestPlugin plugin;
    private final Map<UUID, VeinMinerSession> veinMinerSessions = new ConcurrentHashMap<>();
    private BukkitRunnable cleanupTask;
    private boolean debugMode;

    public BlockBreakListener(OraxenQuestPlugin plugin) {
        this.plugin = plugin;
        this.debugMode = plugin.getConfig().getBoolean("debug-mode", false);
        startCleanupTask();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        // Debug-Header
        if (debugMode) {
            plugin.getPluginLogger().info("=== BlockBreak ===");
            plugin.getPluginLogger().info("Block: " + block.getType());
            plugin.getPluginLogger().info("Player: " + player.getName());
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        int fortuneLevel = getFortuneLevel(tool);

        // VeinMiner Detection
        VeinMinerSession session = getOrCreateSession(player);
        boolean isVeinMining = session.isVeinMining();

        int effectiveFortune = fortuneLevel;

        if (isVeinMining) {
            effectiveFortune = Math.max(0, fortuneLevel - 5);
            session.incrementBlocks();

            // Nur jeder 5. Block
            if (session.getBlocksMinedThisSession() % 5 != 0) {
                if (debugMode) {
                    plugin.getPluginLogger().info("VeinMiner Skip (" +
                            session.getBlocksMinedThisSession() + "/5)");
                }
                return;
            }

            if (debugMode) {
                plugin.getPluginLogger().info("VeinMiner: Fortune " +
                        fortuneLevel + " → " + effectiveFortune);
            }
        }

        // Custom Drops holen (über BlockDropManager)
        List<ItemStack> customDrops = plugin.getBlockDropManager()
                .getDrops(block.getType(), effectiveFortune);

        // Items droppen
        if (!customDrops.isEmpty()) {
            for (ItemStack drop : customDrops) {
                if (debugMode) {
                    String name = drop.hasItemMeta() && drop.getItemMeta().hasDisplayName()
                            ? drop.getItemMeta().getDisplayName()
                            : drop.getType().toString();
                    plugin.getPluginLogger().info("  → Drop: " + drop.getType() +
                            " x" + drop.getAmount() + " (" + name + ")");
                }
                block.getWorld().dropItemNaturally(block.getLocation(), drop);
            }
        }

        if (debugMode) {
            plugin.getPluginLogger().info("==================");
        }
    }

    private int getFortuneLevel(ItemStack tool) {
        if (tool == null || !tool.hasItemMeta()) {
            return 0;
        }

        int vanillaFortune = tool.getEnchantmentLevel(Enchantment.FORTUNE);
        int aeFortune = AEAPIHelper.getFortuneLevel(tool);

        return Math.max(vanillaFortune, aeFortune);
    }

    private VeinMinerSession getOrCreateSession(Player player) {
        UUID uuid = player.getUniqueId();

        return veinMinerSessions.compute(uuid, (key, existing) -> {
            if (existing == null || existing.isExpired()) {
                return new VeinMinerSession();
            }
            existing.update();
            return existing;
        });
    }

    private void startCleanupTask() {
        cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                veinMinerSessions.entrySet().removeIf(e -> e.getValue().isExpired());
            }
        };
        cleanupTask.runTaskTimerAsynchronously(plugin, 600L, 600L);
    }

    public void setDebugMode(boolean enabled) {
        this.debugMode = enabled;
    }

    public void shutdown() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
        veinMinerSessions.clear();
    }

    private static class VeinMinerSession {
        private volatile long lastBreak;
        private volatile int blocksThisSession;
        private static final long SESSION_TIMEOUT_MS = 500;

        public VeinMinerSession() {
            this.lastBreak = System.currentTimeMillis();
            this.blocksThisSession = 0;
        }

        public synchronized void update() {
            long now = System.currentTimeMillis();
            if ((now - lastBreak) > SESSION_TIMEOUT_MS) {
                blocksThisSession = 0;
            }
            lastBreak = now;
        }

        public synchronized void incrementBlocks() {
            blocksThisSession++;
        }

        public boolean isVeinMining() {
            return blocksThisSession > 1;
        }

        public int getBlocksMinedThisSession() {
            return blocksThisSession;
        }

        public boolean isExpired() {
            return (System.currentTimeMillis() - lastBreak) > SESSION_TIMEOUT_MS;
        }
    }
}