package id.zxcoders.cekrank.util;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Utility class untuk berinteraksi dengan LuckPerms API.
 * Mendukung pengecekan rank untuk pemain online maupun offline
 * dengan mengambil data langsung dari database LuckPerms.
 */
public class LuckPermsUtil {

    private final LuckPerms luckPerms;

    public LuckPermsUtil(LuckPerms luckPerms) {
        this.luckPerms = luckPerms;
    }

    /**
     * Mengambil rank utama (primary group) seorang pemain secara asinkron.
     * Mendukung pemain online maupun offline.
     *
     * @param playerName nama pemain yang ingin dicek
     * @return CompletableFuture berisi nama rank, atau empty jika tidak ditemukan
     */
    public CompletableFuture<Optional<String>> getPrimaryRank(String playerName) {
        UUID uuid = resolveUUID(playerName);

        if (uuid == null) {
            // Coba cari via LuckPerms username lookup
            return luckPerms.getUserManager()
                    .lookupUniqueId(playerName)
                    .thenCompose(resolvedUuid -> {
                        if (resolvedUuid == null) {
                            return CompletableFuture.completedFuture(Optional.empty());
                        }
                        return loadUserAndGetRank(resolvedUuid);
                    });
        }

        return loadUserAndGetRank(uuid);
    }

    /**
     * Memuat data user dari database LuckPerms dan mengembalikan rank utamanya.
     *
     * @param uuid UUID pemain
     * @return CompletableFuture berisi nama rank
     */
    private CompletableFuture<Optional<String>> loadUserAndGetRank(UUID uuid) {
        return luckPerms.getUserManager()
                .loadUser(uuid)
                .thenApply(user -> {
                    if (user == null) {
                        return Optional.empty();
                    }
                    String primaryGroup = user.getPrimaryGroup();
                    return Optional.of(formatGroupName(primaryGroup));
                });
    }

    /**
     * Mengambil semua grup yang dimiliki pemain secara asinkron.
     *
     * @param playerName nama pemain
     * @return CompletableFuture berisi string daftar grup, atau empty jika tidak ditemukan
     */
    public CompletableFuture<Optional<String>> getAllGroups(String playerName) {
        UUID uuid = resolveUUID(playerName);

        CompletableFuture<UUID> uuidFuture;
        if (uuid != null) {
            uuidFuture = CompletableFuture.completedFuture(uuid);
        } else {
            uuidFuture = luckPerms.getUserManager().lookupUniqueId(playerName);
        }

        return uuidFuture.thenCompose((UUID resolvedUuid) -> {
            if (resolvedUuid == null) {
                return CompletableFuture.completedFuture(Optional.empty());
            }
            return luckPerms.getUserManager()
                    .loadUser(resolvedUuid)
                    .thenApply(user -> {
                        if (user == null) {
                            return Optional.empty();
                        }
                        StringBuilder groups = new StringBuilder();
                        for (Node node : user.getNodes()) {
                            if (node instanceof InheritanceNode) {
                                InheritanceNode inheritanceNode = (InheritanceNode) node;
                                if (groups.length() > 0) {
                                    groups.append(", ");
                                }
                                groups.append(formatGroupName(inheritanceNode.getGroupName()));
                            }
                        }
                        if (groups.length() == 0) {
                            groups.append(formatGroupName(user.getPrimaryGroup()));
                        }
                        return Optional.of(groups.toString());
                    });
        });
    }

    /**
     * Mencoba mendapatkan UUID pemain dari cache Bukkit (pemain yang pernah join).
     *
     * @param playerName nama pemain
     * @return UUID jika ditemukan, null jika tidak
     */
    private UUID resolveUUID(String playerName) {
        // Cek apakah pemain sedang online
        var onlinePlayer = Bukkit.getPlayerExact(playerName);
        if (onlinePlayer != null) {
            return onlinePlayer.getUniqueId();
        }

        // Cek cache offline player (pemain yang pernah join server)
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayerIfCached(playerName);
        if (offlinePlayer != null) {
            return offlinePlayer.getUniqueId();
        }

        return null;
    }

    /**
     * Memformat nama grup agar huruf pertama kapital.
     *
     * @param groupName nama grup mentah
     * @return nama grup yang telah diformat
     */
    private String formatGroupName(String groupName) {
        if (groupName == null || groupName.isEmpty()) {
            return "Default";
        }
        return Character.toUpperCase(groupName.charAt(0)) + groupName.substring(1);
    }

    /**
     * Mengecek apakah pemain benar-benar online saat ini.
     *
     * @param playerName nama pemain
     * @return true jika online
     */
    public boolean isOnline(String playerName) {
        return Bukkit.getPlayerExact(playerName) != null;
    }
}
