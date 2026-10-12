package me.ryanhamshire.GriefPrevention.clans;

import me.ryanhamshire.GriefPrevention.ClaimPermission;

public enum ClanTrustLevel {
    BUILD,
    CONTAINER,
    ACCESS,
    OFF;

    public boolean allows(ClaimPermission permission) {
        if (this == OFF) return false;
        if (this == BUILD) {
            return permission == ClaimPermission.Build
                    || permission == ClaimPermission.Inventory
                    || permission == ClaimPermission.Access;
        }
        if (this == CONTAINER) {
            return permission == ClaimPermission.Inventory
                    || permission == ClaimPermission.Access;
        }
        if (this == ACCESS) {
            return permission == ClaimPermission.Access;
        }
        return false;
    }

    public static ClanTrustLevel fromString(String str) {
        if (str == null) return OFF;
        try {
            return ClanTrustLevel.valueOf(str.toUpperCase());
        } catch (IllegalArgumentException e) {
            return OFF;
        }
    }
}
