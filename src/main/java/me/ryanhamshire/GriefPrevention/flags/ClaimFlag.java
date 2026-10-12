package me.ryanhamshire.GriefPrevention.flags;

public enum ClaimFlag {
    PVP("Permitir combate PvP dentro de la protección", "deny", FlagType.BOOLEAN),
    FLY("Modo vuelo automático para miembros (con 8s inmunidad caída al salir)", "deny", FlagType.BOOLEAN),
    EXPLOSIONS("Permitir explosiones de TNT, Creepers y cristales", "deny", FlagType.BOOLEAN),
    MOB_SPAWNING("Aparición de monstruos y criaturas hostiles", "deny", FlagType.BOOLEAN),
    PASSIVE_SPAWNING("Aparición de animales y criaturas pasivas", "allow", FlagType.BOOLEAN),
    FIRE_SPREAD("Propagación y combustión de fuego", "deny", FlagType.BOOLEAN),
    LAVA_FLOW("Flujo y daño por lava dentro de la protección", "deny", FlagType.BOOLEAN),
    WATER_FLOW("Flujo de agua dentro de la protección", "allow", FlagType.BOOLEAN),
    LEAF_DECAY("Desaparición o caída natural de hojas de árboles", "deny", FlagType.BOOLEAN),
    PISTONS("Movimiento de pistones en los límites de la protección", "deny", FlagType.BOOLEAN),
    ITEM_DROP("Permitir a jugadores soltar objetos al suelo", "allow", FlagType.BOOLEAN),
    ITEM_PICKUP("Permitir a jugadores recoger objetos del suelo", "allow", FlagType.BOOLEAN),
    MONSTER_DAMAGE("Daño causado por monstruos hacia jugadores", "deny", FlagType.BOOLEAN),
    LIGHTNING("Impacto de rayos dentro del área protegida", "deny", FlagType.BOOLEAN),
    WITHER_DAMAGE("Daño y destrucción de bloques por el Wither", "deny", FlagType.BOOLEAN),
    CHORUS_FRUIT("Teletransportación usando Fruta Coral", "deny", FlagType.BOOLEAN),
    ENDER_PEARL("Teletransportación usando Perlas de Ender", "allow", FlagType.BOOLEAN),
    SLEEP("Permitir dormir en camas dentro del terreno", "allow", FlagType.BOOLEAN),
    ICE_MELT("Derretimiento natural del hielo", "deny", FlagType.BOOLEAN),
    SNOW_FALL("Acumulación de capas de nieve", "deny", FlagType.BOOLEAN),
    VILLAGER_TRADING("Comerciar con aldeanos dentro de la protección", "allow", FlagType.BOOLEAN),
    ENTRY("Permitir acceso a jugadores no autorizados (campo de fuerza/aislamiento)", "allow", FlagType.BOOLEAN),
    ENTRY_MESSAGE("Mensaje personalizado mostrado al entrar a la protección", "", FlagType.STRING),
    EXIT_MESSAGE("Mensaje personalizado mostrado al salir de la protección", "", FlagType.STRING);

    public enum FlagType {
        BOOLEAN,
        STRING
    }

    private final String description;
    private final String defaultValue;
    private final FlagType type;

    ClaimFlag(String description, String defaultValue, FlagType type) {
        this.description = description;
        this.defaultValue = defaultValue;
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public FlagType getType() {
        return type;
    }

    public static ClaimFlag fromString(String name) {
        if (name == null) return null;
        String normalized = name.toUpperCase().replace("-", "_").trim();
        if (normalized.equals("LAVA") || normalized.equals("LAVADAMAGE") || normalized.equals("LAVA_DAMAGE")) {
            normalized = "LAVA_FLOW";
        }
        if (normalized.equals("WATER")) {
            normalized = "WATER_FLOW";
        }
        if (normalized.equals("ENTRADA") || normalized.equals("ACCESS") || normalized.equals("ACCESO") || normalized.equals("FORCEFIELD") || normalized.equals("CAMPO_DE_FUERZA")) {
            normalized = "ENTRY";
        }
        for (ClaimFlag flag : values()) {
            if (flag.name().equals(normalized)) {
                return flag;
            }
        }
        return null;
    }
}
