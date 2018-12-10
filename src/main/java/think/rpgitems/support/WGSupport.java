package think.rpgitems.support;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import think.rpgitems.RPGItems;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.Power;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class WGSupport {

    public static boolean useWorldGuard = true;
    public static boolean forceRefresh = false;
    static Map<UUID, Collection<String>> disabledPowerByPlayer;
    static Map<UUID, Collection<String>> enabledPowerByPlayer;
    static Map<UUID, Collection<String>> disabledItemByPlayer;
    static Map<UUID, Collection<String>> enabledItemByPlayer;
    static Map<UUID, Boolean> disabledByPlayer;
    static WorldGuardPlugin wgPlugin;
    private static RPGItems plugin;
    private static boolean hasSupport = false;

    public static void load() {
        try {
            WGHandler.init();
        } catch (NoClassDefFoundError ignored) {
        }
    }

    public static void init(RPGItems pl) {
        try {
            plugin = pl;
            useWorldGuard = plugin.cfg.useWorldGuard;
            forceRefresh = plugin.cfg.wgForceRefresh;
            Plugin wgPlugin = plugin.getServer().getPluginManager().getPlugin("WorldGuard");
            if (!useWorldGuard || !(wgPlugin instanceof WorldGuardPlugin)) {
                return;
            }
            WGSupport.wgPlugin = (WorldGuardPlugin) wgPlugin;
            String wgVersion = WGSupport.wgPlugin.getDescription().getVersion();
            RPGItems.logger.info("WorldGuard version: " + wgVersion + " found");
            if (!wgVersion.startsWith("7.")) {
                RPGItems.logger.warning("Requires WorldGuard 7.0.0-beta2 or later, disabling integration");
                hasSupport = false;
                return;
            }
            if (wgVersion.contains("0dc5781") && plugin.getServer().getPluginManager().getPlugin("FastAsyncWorldEdit") != null) {
                RPGItems.logger.warning("FastAsyncWorldEdit's WorldGuard is not supported, disabling integration");
                hasSupport = false;
                return;
            }
            hasSupport = true;
            WGHandler.registerHandler();
            disabledPowerByPlayer = new HashMap<>();
            enabledPowerByPlayer = new HashMap<>();
            disabledItemByPlayer = new HashMap<>();
            enabledItemByPlayer = new HashMap<>();
            disabledByPlayer = new HashMap<>();
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                WGHandler.refreshPlayerWG(p);
            }
            File file = new File(plugin.getDataFolder(), "worldguard_region.yml");
            if (file.exists() && !file.isDirectory()) {
                WGHandler.migrate(file);
            }
        } catch (Exception e) {
            RPGItems.logger.log(Level.WARNING, "Error enabling WorldGuard support", e);
        }
    }

    public static void reload() {
        hasSupport = false;
        try {
            unload();
        } catch (NoClassDefFoundError ignored) {
        }
        init(plugin);
    }

    public static boolean hasSupport() {
        return hasSupport;
    }

    private static boolean canNotPvP(Player player) {
        if (!hasSupport || !useWorldGuard)
            return false;

        LocalPlayer localPlayer = wgPlugin.wrapPlayer(player);
        State stat = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery().queryState(localPlayer.getLocation(), localPlayer, Flags.PVP);
        return stat != null && !stat.equals(State.ALLOW);
    }

    public static boolean canUse(Player player, RPGItem item, Collection<? extends Power> powers) {
        if (canNotPvP(player)) return false;
        if (item == null) return true;
        if (!hasSupport || !useWorldGuard || item.isIgnoreWorldGuard()) {
            return true;
        }
        if (forceRefresh) WGHandler.refreshPlayerWG(player);
        Boolean disabled = disabledByPlayer.get(player.getUniqueId());
        if (disabled != null && disabled) {
            return false;
        }
        Collection<String> disabledPower = disabledPowerByPlayer.get(player.getUniqueId());
        Collection<String> enabledPower = enabledPowerByPlayer.get(player.getUniqueId());
        Collection<String> disabledItem = disabledItemByPlayer.get(player.getUniqueId());
        Collection<String> enabledItem = enabledItemByPlayer.get(player.getUniqueId());

        String itemName = item.getName();
        if (notEnabled(disabledItem, enabledItem, itemName)) return false;
        if (powers == null) return true;
        for (Power power : powers) {
            String powerName = item.getPowerKey(power).toString();
            if (notEnabled(disabledPower, enabledPower, powerName)) return false;
        }
        return true;
    }

    private static boolean notEnabled(Collection<String> disabled, Collection<String> enabled, String name) {
        if (enabled == null || enabled.isEmpty()) {
            return disabled != null && (disabled.contains(name) || disabled.contains("*") || disabled.contains("all"));
        } else return !(enabled.contains(name) || enabled.contains("*"));
    }

    public static void unload() {
        if (!hasSupport) {
            return;
        }
        WGHandler.unregisterHandler();
    }
}
