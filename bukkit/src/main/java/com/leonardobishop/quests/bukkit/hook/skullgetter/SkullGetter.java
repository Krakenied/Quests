package com.leonardobishop.quests.bukkit.hook.skullgetter;

import com.leonardobishop.quests.bukkit.BukkitQuestsPlugin;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class SkullGetter {

    private static final String PAPI_PREFIX = "papi:";
    private static final int PAPI_PREFIX_LENGTH = PAPI_PREFIX.length();

    protected final BukkitQuestsPlugin plugin;

    public SkullGetter(final @NotNull BukkitQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    public final boolean apply(final @Nullable Player player, final @NotNull SkullMeta meta, @Nullable String name, @Nullable String uniqueIdString, @Nullable String base64) {
        if (name != null) {
            name = this.applyPlaceholderAPI(player, name);

            this.applyName(meta, name);
            return true;
        }

        if (uniqueIdString != null) {
            uniqueIdString = this.applyPlaceholderAPI(player, uniqueIdString);

            UUID uniqueId;
            try {
                uniqueId = UUID.fromString(uniqueIdString);
            } catch (IllegalArgumentException e) {
                uniqueId = null;
            }

            if (uniqueId != null) {
                this.applyUniqueId(meta, uniqueId);
                return true;
            }
        }

        if (base64 != null) {
            base64 = this.applyPlaceholderAPI(player, base64);

            this.applyBase64(meta, base64);
            return true;
        }

        return false;
    }

    @Contract(value = "null, _ -> param2", pure = true)
    private @NotNull String applyPlaceholderAPI(final @Nullable Player player, final @NotNull String s) {
        return player != null && s.startsWith(PAPI_PREFIX)
                ? this.plugin.getPlaceholderAPIProcessor().apply(player, s.substring(PAPI_PREFIX_LENGTH))
                : s;
    }

    abstract void applyName(final @NotNull SkullMeta meta, final @NotNull String name);

    abstract void applyUniqueId(final @NotNull SkullMeta meta, final @NotNull UUID uniqueId);

    abstract void applyBase64(final @NotNull SkullMeta meta, final @NotNull String base64);
}
