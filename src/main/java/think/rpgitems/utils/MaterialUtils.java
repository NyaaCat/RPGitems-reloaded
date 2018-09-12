package think.rpgitems.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import think.rpgitems.I18n;
import think.rpgitems.power.Getter;
import think.rpgitems.power.Setter;

public class MaterialUtils implements Setter<Material> {

    @SuppressWarnings("deprecation")
    public static Material getMaterial(String name, CommandSender sender) {
        Material m = Material.matchMaterial(name, false);
        if (m == null) {
            m = Material.matchMaterial(name, true);
            if (m != null) {
                m = Bukkit.getUnsafe().fromLegacy(m);
                sender.sendMessage(I18n.format("message.error.legacy_name", name, m.toString()));
            }
        }
        return m;
    }

    @Override
    public Material set(String value) throws IllegalArgumentException {
        return getMaterial(value, Bukkit.getConsoleSender());
    }
}
