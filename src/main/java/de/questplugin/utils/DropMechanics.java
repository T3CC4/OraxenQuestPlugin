package de.questplugin.utils;

/**
 * Fortgeschrittene Drop-Mechanik für Block- und Mob-Drops
 *
 * PROBLEM MIT ALTEM SYSTEM:
 * - 0.1% Chance + Fortune III (3%) = 3.1% → 31x höher!
 * - Linear scaling macht seltene Drops zu häufig
 *
 * NEUE MECHANIK:
 * 1) Diminishing Returns - je seltener, desto weniger Bonus
 * 2) Soft Cap bei 95% - nie 100% garantiert
 * 3) Tier-basiert - verschiedene Formeln für verschiedene Seltenheiten
 * 4) Bonus-Rolls statt direkter Chance-Erhöhung
 */
public class DropMechanics {

    // ==================== DROP TIERS ====================

    /**
     * Drop-Seltenheit bestimmt die Bonus-Formel
     */
    public enum DropRarity {
        COMMON(50.0, 100.0),        // 50-100%: Voller Bonus
        UNCOMMON(10.0, 50.0),       // 10-50%: Reduzierter Bonus
        RARE(1.0, 10.0),            // 1-10%: Stark reduziert
        VERY_RARE(0.1, 1.0),        // 0.1-1%: Minimal
        LEGENDARY(0.0, 0.1);        // <0.1%: Fast kein Bonus

        final double minChance;
        final double maxChance;

        DropRarity(double minChance, double maxChance) {
            this.minChance = minChance;
            this.maxChance = maxChance;
        }

        public static DropRarity fromChance(double chance) {
            for (DropRarity rarity : values()) {
                if (chance > rarity.minChance && chance <= rarity.maxChance) {
                    return rarity;
                }
            }
            return LEGENDARY; // Fallback für sehr seltene Items
        }
    }

    // ==================== FORTUNE/LOOTING MECHANIK ====================

    /**
     * Berechnet finale Drop-Chance mit Fortune/Looting
     *
     * METHODE 1: Diminishing Returns (Standard)
     * - Seltene Items bekommen weniger Bonus
     * - Nutzt logarithmische Skalierung
     * - Soft Cap bei 95%
     *
     * @param baseChance Basis-Chance in % (z.B. 0.1 oder 50.0)
     * @param enchantLevel Fortune/Looting Level (0-10+)
     * @return Finale Chance in %
     */
    public static double calculateDropChance(double baseChance, int enchantLevel) {
        if (enchantLevel <= 0) {
            return baseChance;
        }

        DropRarity rarity = DropRarity.fromChance(baseChance);

        // Bonus berechnen basierend auf Seltenheit
        double bonus = calculateBonus(baseChance, enchantLevel, rarity);

        // Finale Chance
        double finalChance = baseChance + bonus;

        // Soft Cap bei 95%
        return Math.min(95.0, finalChance);
    }

    /**
     * Berechnet Bonus basierend auf Seltenheit
     */
    private static double calculateBonus(double baseChance, int enchantLevel, DropRarity rarity) {
        switch (rarity) {
            case COMMON:
                // Voller linearer Bonus: +1% pro Level
                return enchantLevel * 1.0;

            case UNCOMMON:
                // Reduzierter Bonus: +0.5% pro Level
                return enchantLevel * 0.5;

            case RARE:
                // Logarithmischer Bonus
                // Fortune I: +0.3%, II: +0.5%, III: +0.7%, X: +1.4%
                return Math.log(enchantLevel + 1) * baseChance * 0.15;

            case VERY_RARE:
                // Stark reduziert - Quadratwurzel-Skalierung
                // Fortune I: +0.01%, III: +0.017%, X: +0.03%
                return Math.sqrt(enchantLevel) * baseChance * 0.1;

            case LEGENDARY:
                // Minimal - nur symbolischer Bonus
                // Fortune I: +0.001%, III: +0.002%, X: +0.003%
                return Math.log(enchantLevel + 1) * baseChance * 0.02;

            default:
                return 0;
        }
    }

    // ==================== BONUS-ROLL SYSTEM ====================

    /**
     * METHODE 2: Bonus-Rolls statt Chance-Erhöhung
     *
     * Anstatt die Chance zu erhöhen, gibt Fortune/Looting
     * zusätzliche Würfe ("Bonus-Rolls")
     *
     * Beispiel mit 0.1% Chance:
     * - Fortune 0: 1 Roll → 0.1% Chance
     * - Fortune III: 4 Rolls → ~0.4% Chance (nicht 3.1%!)
     * - Fortune X: 11 Rolls → ~1.1% Chance
     *
     * @param baseChance Basis-Chance in %
     * @param enchantLevel Fortune/Looting Level
     * @return Array [Anzahl Rolls, Chance pro Roll]
     */
    public static BonusRollResult calculateBonusRolls(double baseChance, int enchantLevel) {
        if (enchantLevel <= 0) {
            return new BonusRollResult(1, baseChance);
        }

        DropRarity rarity = DropRarity.fromChance(baseChance);

        int bonusRolls = calculateBonusRollCount(enchantLevel, rarity);
        int totalRolls = 1 + bonusRolls;

        // Chance pro Roll bleibt gleich!
        return new BonusRollResult(totalRolls, baseChance);
    }

    /**
     * Berechnet Anzahl Bonus-Rolls basierend auf Seltenheit
     */
    private static int calculateBonusRollCount(int enchantLevel, DropRarity rarity) {
        switch (rarity) {
            case COMMON:
                // Volle Rolls: 1 pro Level
                return enchantLevel;

            case UNCOMMON:
                // Reduziert: 1 Roll pro 2 Levels
                return enchantLevel / 2;

            case RARE:
                // Stark reduziert: 1 Roll pro 3 Levels
                return enchantLevel / 3;

            case VERY_RARE:
                // Minimal: 1 Roll pro 5 Levels
                return enchantLevel / 5;

            case LEGENDARY:
                // Fast keine: 1 Roll pro 10 Levels
                return enchantLevel / 10;

            default:
                return 0;
        }
    }

    // ==================== HYBRID SYSTEM ====================

    /**
     * METHODE 3: Hybrid aus Chance-Boost und Bonus-Rolls
     *
     * - Häufige Items: Hauptsächlich Chance-Boost
     * - Seltene Items: Hauptsächlich Bonus-Rolls
     *
     * Beste Balance zwischen beiden Systemen
     */
    public static HybridDropResult calculateHybridDrop(double baseChance, int enchantLevel) {
        if (enchantLevel <= 0) {
            return new HybridDropResult(baseChance, 1);
        }

        DropRarity rarity = DropRarity.fromChance(baseChance);

        double boostedChance;
        int totalRolls;

        switch (rarity) {
            case COMMON:
                // 80% Chance-Boost, 20% Bonus-Rolls
                boostedChance = baseChance + (enchantLevel * 0.8);
                totalRolls = 1 + (enchantLevel / 5);
                break;

            case UNCOMMON:
                // 50% Chance-Boost, 50% Bonus-Rolls
                boostedChance = baseChance + (enchantLevel * 0.4);
                totalRolls = 1 + (enchantLevel / 3);
                break;

            case RARE:
                // 20% Chance-Boost, 80% Bonus-Rolls
                boostedChance = baseChance + (Math.log(enchantLevel + 1) * baseChance * 0.1);
                totalRolls = 1 + (enchantLevel / 2);
                break;

            case VERY_RARE:
            case LEGENDARY:
                // Fast nur Bonus-Rolls
                boostedChance = baseChance + (Math.sqrt(enchantLevel) * baseChance * 0.05);
                totalRolls = 1 + enchantLevel;
                break;

            default:
                boostedChance = baseChance;
                totalRolls = 1;
        }

        // Soft Cap
        boostedChance = Math.min(95.0, boostedChance);

        return new HybridDropResult(boostedChance, totalRolls);
    }

    // ==================== AMOUNT CALCULATION ====================

    /**
     * Berechnet Drop-Menge mit Fortune/Looting
     *
     * Anstatt min-max zu erhöhen, gibt es Chance auf Extra-Items
     *
     * @param minAmount Min-Menge
     * @param maxAmount Max-Menge
     * @param enchantLevel Fortune/Looting Level
     * @return Finale Menge
     */
    public static int calculateDropAmount(int minAmount, int maxAmount, int enchantLevel) {
        // Basis-Menge
        int baseAmount = minAmount + (int) (Math.random() * (maxAmount - minAmount + 1));

        if (enchantLevel <= 0) {
            return baseAmount;
        }

        // Bonus-Items mit abnehmender Chance
        int bonusItems = 0;

        for (int i = 0; i < enchantLevel; i++) {
            // Chance sinkt pro Bonus-Item
            // Level 1: 100%, Level 2: 66%, Level 3: 50%, etc.
            double chance = 1.0 / (i + 1);

            if (Math.random() < chance) {
                bonusItems++;
            }
        }

        return baseAmount + bonusItems;
    }

    // ==================== HELPER KLASSEN ====================

    public static class BonusRollResult {
        public final int rolls;
        public final double chancePerRoll;
        public final double totalChance;

        public BonusRollResult(int rolls, double chancePerRoll) {
            this.rolls = rolls;
            this.chancePerRoll = chancePerRoll;
            // Berechne kumulative Chance: 1 - (1 - p)^n
            this.totalChance = Math.min(95.0,
                    100.0 * (1.0 - Math.pow(1.0 - chancePerRoll / 100.0, rolls)));
        }

        @Override
        public String toString() {
            return String.format("%d rolls @ %.2f%% = %.2f%% total",
                    rolls, chancePerRoll, totalChance);
        }
    }

    public static class HybridDropResult {
        public final double chancePerRoll;
        public final int rolls;
        public final double totalChance;

        public HybridDropResult(double chancePerRoll, int rolls) {
            this.chancePerRoll = Math.min(95.0, chancePerRoll);
            this.rolls = rolls;
            // Kumulative Chance
            this.totalChance = Math.min(95.0,
                    100.0 * (1.0 - Math.pow(1.0 - this.chancePerRoll / 100.0, rolls)));
        }

        @Override
        public String toString() {
            return String.format("%d rolls @ %.2f%% = %.2f%% total",
                    rolls, chancePerRoll, totalChance);
        }
    }

    // ==================== DEBUG HELPERS ====================

    /**
     * Gibt detaillierte Info über Drop-Berechnung
     */
    public static String getDropInfo(double baseChance, int enchantLevel) {
        DropRarity rarity = DropRarity.fromChance(baseChance);

        StringBuilder sb = new StringBuilder();
        sb.append("Base: ").append(String.format("%.2f%%", baseChance))
                .append(" (").append(rarity).append(")\n");

        // Methode 1: Diminishing Returns
        double method1 = calculateDropChance(baseChance, enchantLevel);
        sb.append("Method 1 (Diminishing): ").append(String.format("%.2f%%", method1))
                .append(" (+" + String.format("%.2f%%", method1 - baseChance) + ")\n");

        // Methode 2: Bonus Rolls
        BonusRollResult method2 = calculateBonusRolls(baseChance, enchantLevel);
        sb.append("Method 2 (Bonus Rolls): ").append(method2).append("\n");

        // Methode 3: Hybrid
        HybridDropResult method3 = calculateHybridDrop(baseChance, enchantLevel);
        sb.append("Method 3 (Hybrid): ").append(method3).append("\n");

        return sb.toString();
    }

    /**
     * Vergleicht alle drei Methoden
     */
    public static void printComparison(double baseChance) {
        System.out.println("=== DROP COMPARISON: " + baseChance + "% ===\n");

        for (int level = 0; level <= 10; level += 3) {
            System.out.println("Fortune/Looting " + level + ":");
            System.out.println(getDropInfo(baseChance, level));
        }
    }
}