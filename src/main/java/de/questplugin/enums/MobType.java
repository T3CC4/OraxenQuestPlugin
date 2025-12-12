package de.questplugin.enums;

import org.bukkit.entity.EntityType;

/**
 * Alle lebenden Mobs in Minecraft 1.21.10
 * Kategorisiert nach Typ (Hostile, Passive, Neutral, Boss)
 */
public enum MobType {

    // ========== HOSTILE (Feindlich) ==========
    ZOMBIE(EntityType.ZOMBIE, MobCategory.HOSTILE, "Zombie"),
    HUSK(EntityType.HUSK, MobCategory.HOSTILE, "Wüstenzombie"),
    DROWNED(EntityType.DROWNED, MobCategory.HOSTILE, "Ertrunkener"),
    ZOMBIE_VILLAGER(EntityType.ZOMBIE_VILLAGER, MobCategory.HOSTILE, "Zombie-Dorfbewohner"),

    SKELETON(EntityType.SKELETON, MobCategory.HOSTILE, "Skelett"),
    STRAY(EntityType.STRAY, MobCategory.HOSTILE, "Eiswanderer"),
    WITHER_SKELETON(EntityType.WITHER_SKELETON, MobCategory.HOSTILE, "Wither-Skelett"),
    BOGGED(EntityType.BOGGED, MobCategory.HOSTILE, "Sumpf-Skelett"),

    CREEPER(EntityType.CREEPER, MobCategory.HOSTILE, "Creeper"),
    SPIDER(EntityType.SPIDER, MobCategory.HOSTILE, "Spinne"),
    CAVE_SPIDER(EntityType.CAVE_SPIDER, MobCategory.HOSTILE, "Höhlenspinne"),
    SILVERFISH(EntityType.SILVERFISH, MobCategory.HOSTILE, "Silberfischchen"),
    ENDERMITE(EntityType.ENDERMITE, MobCategory.HOSTILE, "Endermite"),

    BLAZE(EntityType.BLAZE, MobCategory.HOSTILE, "Lohe"),
    GHAST(EntityType.GHAST, MobCategory.HOSTILE, "Ghast"),
    MAGMA_CUBE(EntityType.MAGMA_CUBE, MobCategory.HOSTILE, "Magmawürfel"),

    WITCH(EntityType.WITCH, MobCategory.HOSTILE, "Hexe"),
    SLIME(EntityType.SLIME, MobCategory.HOSTILE, "Schleim"),

    PHANTOM(EntityType.PHANTOM, MobCategory.HOSTILE, "Phantom"),
    SHULKER(EntityType.SHULKER, MobCategory.HOSTILE, "Shulker"),
    GUARDIAN(EntityType.GUARDIAN, MobCategory.HOSTILE, "Wächter"),
    ELDER_GUARDIAN(EntityType.ELDER_GUARDIAN, MobCategory.HOSTILE, "Großer Wächter"),

    BREEZE(EntityType.BREEZE, MobCategory.HOSTILE, "Brise"),
    CREAKING(EntityType.CREAKING, MobCategory.HOSTILE, "Knarrwesen"),

    // ========== ILLAGER ==========
    PILLAGER(EntityType.PILLAGER, MobCategory.ILLAGER, "Plünderer"),
    VINDICATOR(EntityType.VINDICATOR, MobCategory.ILLAGER, "Diener"),
    EVOKER(EntityType.EVOKER, MobCategory.ILLAGER, "Magier"),
    ILLUSIONER(EntityType.ILLUSIONER, MobCategory.ILLAGER, "Illusionist"),
    RAVAGER(EntityType.RAVAGER, MobCategory.ILLAGER, "Verwüster"),
    VEX(EntityType.VEX, MobCategory.ILLAGER, "Plagegeist"),

    // ========== NETHER ==========
    ZOMBIFIED_PIGLIN(EntityType.ZOMBIFIED_PIGLIN, MobCategory.NETHER, "Zombie-Piglin"),
    PIGLIN(EntityType.PIGLIN, MobCategory.NETHER, "Piglin"),
    PIGLIN_BRUTE(EntityType.PIGLIN_BRUTE, MobCategory.NETHER, "Piglin-Barbar"),
    HOGLIN(EntityType.HOGLIN, MobCategory.NETHER, "Hoglin"),
    ZOGLIN(EntityType.ZOGLIN, MobCategory.NETHER, "Zoglin"),
    STRIDER(EntityType.STRIDER, MobCategory.NETHER, "Schreiter"),

    // ========== END ==========
    ENDERMAN(EntityType.ENDERMAN, MobCategory.END, "Enderman"),
    SHULKER_END(EntityType.SHULKER, MobCategory.END, "Shulker"),

    // ========== BOSS ==========
    ENDER_DRAGON(EntityType.ENDER_DRAGON, MobCategory.BOSS, "Enderdrache"),
    WITHER(EntityType.WITHER, MobCategory.BOSS, "Wither"),
    WARDEN(EntityType.WARDEN, MobCategory.BOSS, "Wächter"),
    ELDER_GUARDIAN_BOSS(EntityType.ELDER_GUARDIAN, MobCategory.BOSS, "Großer Wächter"),

    // ========== PASSIVE (Friedlich) ==========
    PIG(EntityType.PIG, MobCategory.PASSIVE, "Schwein"),
    COW(EntityType.COW, MobCategory.PASSIVE, "Kuh"),
    MOOSHROOM(EntityType.MOOSHROOM, MobCategory.PASSIVE, "Pilzkuh"),
    SHEEP(EntityType.SHEEP, MobCategory.PASSIVE, "Schaf"),
    CHICKEN(EntityType.CHICKEN, MobCategory.PASSIVE, "Huhn"),
    RABBIT(EntityType.RABBIT, MobCategory.PASSIVE, "Kaninchen"),

    BAT(EntityType.BAT, MobCategory.PASSIVE, "Fledermaus"),
    SQUID(EntityType.SQUID, MobCategory.PASSIVE, "Tintenfisch"),
    GLOW_SQUID(EntityType.GLOW_SQUID, MobCategory.PASSIVE, "Leucht-Tintenfisch"),

    VILLAGER(EntityType.VILLAGER, MobCategory.PASSIVE, "Dorfbewohner"),
    WANDERING_TRADER(EntityType.WANDERING_TRADER, MobCategory.PASSIVE, "Fahrender Händler"),

    TURTLE(EntityType.TURTLE, MobCategory.PASSIVE, "Schildkröte"),
    FROG(EntityType.FROG, MobCategory.PASSIVE, "Frosch"),
    TADPOLE(EntityType.TADPOLE, MobCategory.PASSIVE, "Kaulquappe"),
    SNIFFER(EntityType.SNIFFER, MobCategory.PASSIVE, "Schnüffler"),
    ARMADILLO(EntityType.ARMADILLO, MobCategory.PASSIVE, "Gürteltier"),

    ALLAY(EntityType.ALLAY, MobCategory.PASSIVE, "Allay"),

    // ========== AQUATIC (Wassertiere) ==========
    COD(EntityType.COD, MobCategory.AQUATIC, "Kabeljau"),
    SALMON(EntityType.SALMON, MobCategory.AQUATIC, "Lachs"),
    TROPICAL_FISH(EntityType.TROPICAL_FISH, MobCategory.AQUATIC, "Tropenfisch"),
    PUFFERFISH(EntityType.PUFFERFISH, MobCategory.AQUATIC, "Kugelfisch"),
    DOLPHIN(EntityType.DOLPHIN, MobCategory.AQUATIC, "Delfin"),
    AXOLOTL(EntityType.AXOLOTL, MobCategory.AQUATIC, "Axolotl"),

    // ========== NEUTRAL ==========
    WOLF(EntityType.WOLF, MobCategory.NEUTRAL, "Wolf"),
    POLAR_BEAR(EntityType.POLAR_BEAR, MobCategory.NEUTRAL, "Eisbär"),
    PANDA(EntityType.PANDA, MobCategory.NEUTRAL, "Panda"),
    BEE(EntityType.BEE, MobCategory.NEUTRAL, "Biene"),
    SPIDER_NEUTRAL(EntityType.SPIDER, MobCategory.NEUTRAL, "Spinne"),
    CAVE_SPIDER_NEUTRAL(EntityType.CAVE_SPIDER, MobCategory.NEUTRAL, "Höhlenspinne"),
    GOAT(EntityType.GOAT, MobCategory.NEUTRAL, "Ziege"),

    // ========== TAMEABLE (Zähmbar) ==========
    HORSE(EntityType.HORSE, MobCategory.TAMEABLE, "Pferd"),
    DONKEY(EntityType.DONKEY, MobCategory.TAMEABLE, "Esel"),
    MULE(EntityType.MULE, MobCategory.TAMEABLE, "Maultier"),
    SKELETON_HORSE(EntityType.SKELETON_HORSE, MobCategory.TAMEABLE, "Skelett-Pferd"),
    ZOMBIE_HORSE(EntityType.ZOMBIE_HORSE, MobCategory.TAMEABLE, "Zombie-Pferd"),
    CAMEL(EntityType.CAMEL, MobCategory.TAMEABLE, "Kamel"),

    LLAMA(EntityType.LLAMA, MobCategory.TAMEABLE, "Lama"),
    TRADER_LLAMA(EntityType.TRADER_LLAMA, MobCategory.TAMEABLE, "Händler-Lama"),

    CAT(EntityType.CAT, MobCategory.TAMEABLE, "Katze"),
    OCELOT(EntityType.OCELOT, MobCategory.TAMEABLE, "Ozelot"),
    PARROT(EntityType.PARROT, MobCategory.TAMEABLE, "Papagei"),
    FOX(EntityType.FOX, MobCategory.TAMEABLE, "Fuchs"),
    WOLF_TAMEABLE(EntityType.WOLF, MobCategory.TAMEABLE, "Wolf"),

    // ========== UTILITY (Golems) ==========
    IRON_GOLEM(EntityType.IRON_GOLEM, MobCategory.UTILITY, "Eisengolem"),
    SNOW_GOLEM(EntityType.SNOW_GOLEM, MobCategory.UTILITY, "Schneegolem"),

    // ========== RARE ==========
    GIANT(EntityType.GIANT, MobCategory.RARE, "Riese");

    private final EntityType entityType;
    private final MobCategory category;
    private final String displayName;

    MobType(EntityType entityType, MobCategory category, String displayName) {
        this.entityType = entityType;
        this.category = category;
        this.displayName = displayName;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public MobCategory getCategory() {
        return category;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Konvertiert String zu MobType (case-insensitive)
     */
    public static MobType fromString(String name) {
        if (name == null) return null;

        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Findet MobType anhand des EntityTypes
     */
    public static MobType fromEntityType(EntityType entityType) {
        if (entityType == null) return null;

        for (MobType type : values()) {
            if (type.entityType == entityType) {
                return type;
            }
        }
        return null;
    }

    // ==================== KATEGORIEN ====================

    /**
     * Gibt alle Mobs einer Kategorie zurück
     */
    public static MobType[] getByCategory(MobCategory category) {
        return java.util.Arrays.stream(values())
                .filter(m -> m.category == category)
                .toArray(MobType[]::new);
    }

    /**
     * Gibt alle feindlichen Mobs zurück
     */
    public static MobType[] getHostileMobs() {
        return getByCategory(MobCategory.HOSTILE);
    }

    /**
     * Gibt alle friedlichen Mobs zurück
     */
    public static MobType[] getPassiveMobs() {
        return getByCategory(MobCategory.PASSIVE);
    }

    /**
     * Gibt alle Nether-Mobs zurück
     */
    public static MobType[] getNetherMobs() {
        return getByCategory(MobCategory.NETHER);
    }

    /**
     * Gibt alle End-Mobs zurück
     */
    public static MobType[] getEndMobs() {
        return getByCategory(MobCategory.END);
    }

    /**
     * Gibt alle Boss-Mobs zurück
     */
    public static MobType[] getBossMobs() {
        return getByCategory(MobCategory.BOSS);
    }

    /**
     * Prüft ob Mob in einer bestimmten Kategorie ist
     */
    public boolean isInCategory(MobCategory category) {
        return this.category == category;
    }

    /**
     * Prüft ob Mob feindlich ist
     */
    public boolean isHostile() {
        return category == MobCategory.HOSTILE ||
                category == MobCategory.ILLAGER ||
                category == MobCategory.NETHER ||
                category == MobCategory.BOSS;
    }

    /**
     * Prüft ob Mob friedlich ist
     */
    public boolean isPassive() {
        return category == MobCategory.PASSIVE ||
                category == MobCategory.AQUATIC ||
                category == MobCategory.TAMEABLE;
    }

    // ==================== ENUMS ====================

    /**
     * Mob-Kategorien
     */
    public enum MobCategory {
        HOSTILE("Feindlich"),
        ILLAGER("Illager"),
        NETHER("Nether"),
        END("End"),
        BOSS("Boss"),
        PASSIVE("Friedlich"),
        AQUATIC("Wasser"),
        NEUTRAL("Neutral"),
        TAMEABLE("Zähmbar"),
        UTILITY("Utility"),
        RARE("Selten");

        private final String displayName;

        MobCategory(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}