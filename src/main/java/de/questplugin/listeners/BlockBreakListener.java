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

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BlockBreakListener implements Listener {

    private final OraxenQuestPlugin plugin;

    // VeinMiner Session Tracking
    private final Map<UUID, VeinMinerSession> veinMinerSessions = new HashMap<>();

    // AdvancedEnchantments Kompatibilität
    private static final String AE_IGNORE_METADATA = "blockbreakevent-ignore";

    public BlockBreakListener(OraxenQuestPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        /*
        // AdvancedEnchantments Kompatibilität: Ignoriere Blöcke mit AE Metadata
        if (block.hasMetadata(AE_IGNORE_METADATA)) {
            plugin.getLogger().fine("Block ignoriert - AdvancedEnchantments Metadata gefunden");
            return;
        }

        // Alternative: AEAPI.ignoreBlockEvent() nutzen (falls verfügbar)
        if (AEAPIHelper.isAvailable() && shouldIgnoreBlock(block)) {
            plugin.getLogger().fine("Block ignoriert - AEAPI.ignoreBlockEvent() = true");
            return;
        }
        */
        ItemStack tool = player.getInventory().getItemInMainHand();

        // Hole Fortune/Luck Level (Vanilla + AdvancedEnchantments)
        int fortuneLevel = getFortuneLevel(tool);

        // VeinMiner Detection
        VeinMinerSession session = getOrCreateSession(player);
        boolean isVeinMining = session.isVeinMining();

        // Bei VeinMiner: Reduziere Drop-Chance drastisch
        int effectiveFortune = fortuneLevel;
        if (isVeinMining) {
            // Bei VeinMiner: Nur 10% der normalen Chance
            // Verhindert Exploit: 64 Blöcke = 64x Drops
            effectiveFortune = Math.max(0, fortuneLevel - 5);

            session.incrementBlocks();

            // Limitierung: Max 1 Custom Drop pro 5 Blöcke
            if (session.getBlocksMinedThisSession() % 5 != 0) {
                return; // Skip diesen Block
            }
        }

        // Debug
        if (fortuneLevel > 0) {
            //plugin.getLogger().fine("Block-Break mit Fortune/Luck " + fortuneLevel +
                    //(isVeinMining ? " (VeinMiner: " + session.getBlocksMinedThisSession() + " blocks)" : ""));
        }

        // Hole Custom Drops
        List<ItemStack> customDrops = plugin.getDropManager().getBlockDrops(block.getType(), effectiveFortune);

        // Droppe Custom Items
        for (ItemStack drop : customDrops) {
            block.getWorld().dropItemNaturally(block.getLocation(), drop);
        }

        // Session cleanup nach kurzer Zeit
        if (!isVeinMining) {
            session.reset();
        }
    }

    /**
     * Prüft ob Block von AdvancedEnchantments ignoriert werden soll
     */
    private boolean shouldIgnoreBlock(Block block) {
        try {
            // AEAPI.ignoreBlockEvent() nutzen falls verfügbar
            Class<?> aeapiClass = Class.forName("net.advancedplugins.ae.api.AEAPI");
            java.lang.reflect.Method method = aeapiClass.getMethod("ignoreBlockEvent", Block.class);
            return (boolean) method.invoke(null, block);
        } catch (Exception e) {
            // Methode nicht verfügbar oder Fehler - ignorieren
            return false;
        }
    }

    /**
     * Holt das Fortune/Luck-Level von der Spitzhacke
     * - Vanilla Fortune
     * - AdvancedEnchantments Fortune/Luck
     */
    private int getFortuneLevel(ItemStack tool) {
        if (tool == null || !tool.hasItemMeta()) {
            return 0;
        }

        // Vanilla Fortune
        int vanillaFortune = tool.getEnchantmentLevel(Enchantment.FORTUNE);

        // AdvancedEnchantments Fortune/Luck
        int aeFortune = AEAPIHelper.getFortuneLevel(tool);

        // Nutze das höchste Level
        int maxLevel = Math.max(vanillaFortune, aeFortune);

        if (aeFortune > 0) {
            plugin.getLogger().fine("AE Fortune/Luck Level " + aeFortune + " erkannt (Vanilla: " + vanillaFortune + ")");
        }

        return maxLevel;
    }

    /**
     * Holt oder erstellt eine VeinMiner Session
     */
    private VeinMinerSession getOrCreateSession(Player player) {
        UUID uuid = player.getUniqueId();
        VeinMinerSession session = veinMinerSessions.get(uuid);

        if (session == null || session.isExpired()) {
            session = new VeinMinerSession();
            veinMinerSessions.put(uuid, session);
        }

        session.update();
        return session;
    }

    /**
     * VeinMiner Session Tracking
     */
    private static class VeinMinerSession {
        private long lastBreak;
        private int blocksThisSession;
        private static final long VEINMINER_WINDOW_MS = 100; // 100ms zwischen Blöcken = VeinMiner
        private static final long SESSION_TIMEOUT_MS = 500; // Session endet nach 500ms

        public VeinMinerSession() {
            this.lastBreak = System.currentTimeMillis();
            this.blocksThisSession = 0;
        }

        public void update() {
            long now = System.currentTimeMillis();
            long timeSinceLastBreak = now - lastBreak;

            // Reset wenn zu lange her
            if (timeSinceLastBreak > SESSION_TIMEOUT_MS) {
                blocksThisSession = 0;
            }

            lastBreak = now;
        }

        public void incrementBlocks() {
            blocksThisSession++;
        }

        public boolean isVeinMining() {
            // VeinMiner erkannt wenn mehrere Blöcke sehr schnell abgebaut werden
            return blocksThisSession > 1;
        }

        public int getBlocksMinedThisSession() {
            return blocksThisSession;
        }

        public boolean isExpired() {
            return (System.currentTimeMillis() - lastBreak) > SESSION_TIMEOUT_MS;
        }

        public void reset() {
            blocksThisSession = 0;
        }
    }
}