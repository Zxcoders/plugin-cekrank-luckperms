package id.zxcoders.cekrank.command;

import id.zxcoders.cekrank.CekRankPlugin;
import id.zxcoders.cekrank.util.LuckPermsUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Handler untuk perintah /checkrank <player>.
 * Mendukung pemain online maupun offline dengan mengambil
 * data langsung dari database LuckPerms.
 */
public class CheckRankCommand implements CommandExecutor, TabCompleter {

    private final CekRankPlugin plugin;
    private final LuckPermsUtil luckPermsUtil;

    public CheckRankCommand(CekRankPlugin plugin) {
        this.plugin = plugin;
        this.luckPermsUtil = new LuckPermsUtil(plugin.getLuckPerms());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("cekrank.use")) {
            sendMessage(sender, "no_permission");
            return true;
        }

        if (args.length < 1) {
            sendMessage(sender, "usage");
            return true;
        }

        String targetName = args[0];

        // Cegah pemain mengecek pemain lain tanpa izin
        if (sender instanceof Player player) {
            if (!player.getName().equalsIgnoreCase(targetName)
                    && !player.hasPermission("cekrank.others")) {
                sendMessage(sender, "no_permission");
                return true;
            }
        }

        // Ambil data rank secara asinkron agar tidak memblokir thread utama
        luckPermsUtil.getPrimaryRank(targetName).thenAcceptAsync(optionalRank -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (optionalRank.isEmpty()) {
                    String notFoundMsg = getMsg("player_not_found")
                            .replace("{player}", targetName);
                    sender.sendMessage(colorize(getPrefix() + notFoundMsg));
                    return;
                }

                String rank = optionalRank.get();
                sender.sendMessage(rank);
            });
        }).exceptionally(ex -> {
            plugin.getLogger().warning("Gagal mengambil rank untuk " + targetName + ": " + ex.getMessage());
            Bukkit.getScheduler().runTask(plugin, () -> sendMessage(sender, "error"));
            return null;
        });

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("cekrank.use")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            // Saran tab-complete: daftar pemain online yang namanya sesuai prefix
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String getPrefix() {
        return plugin.getConfig().getString("messages.prefix", "&8[&bCekRank&8] ");
    }

    private String getMsg(String key) {
        return plugin.getConfig().getString("messages." + key, "");
    }

    private void sendMessage(CommandSender sender, String key) {
        String msg = getMsg(key);
        if (msg.isEmpty()) return;
        sender.sendMessage(colorize(getPrefix() + msg));
    }

    /**
     * Menerjemahkan kode warna Bukkit (&amp; -&gt; §) menggunakan ChatColor API.
     */
    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
