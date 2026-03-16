package id.zxcoders.cekrank;

import id.zxcoders.cekrank.command.CheckRankCommand;
import net.luckperms.api.LuckPerms;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class CekRankPlugin extends JavaPlugin {

    private static volatile CekRankPlugin instance;
    private LuckPerms luckPerms;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        if (!setupLuckPerms()) {
            getLogger().severe("LuckPerms tidak ditemukan! Plugin dinonaktifkan.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        registerCommands();

        getLogger().info("CekRank berhasil diaktifkan!");
    }

    @Override
    public void onDisable() {
        getLogger().info("CekRank telah dinonaktifkan.");
    }

    private boolean setupLuckPerms() {
        RegisteredServiceProvider<LuckPerms> provider =
                getServer().getServicesManager().getRegistration(LuckPerms.class);
        if (provider == null) {
            return false;
        }
        luckPerms = provider.getProvider();
        return true;
    }

    private void registerCommands() {
        CheckRankCommand checkRankCommand = new CheckRankCommand(this);
        getCommand("checkrank").setExecutor(checkRankCommand);
        getCommand("checkrank").setTabCompleter(checkRankCommand);
    }

    public static CekRankPlugin getInstance() {
        return instance;
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }
}
