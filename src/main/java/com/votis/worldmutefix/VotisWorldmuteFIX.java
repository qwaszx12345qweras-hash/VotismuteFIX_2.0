package com.votis.worldmutefix;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.Objects;

public final class VotisWorldmuteFIX extends JavaPlugin implements Listener, CommandExecutor {

    private Object liteBansDatabaseInstance = null;
    private Method isPlayerMutedMethod = null;
    private boolean liteBansLinked = false;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        getServer().getConsoleSender().sendMessage(ChatColor.GOLD + "====================================================");
        getServer().getConsoleSender().sendMessage(ChatColor.YELLOW + "      VotisWorldmuteFIX v" + getDescription().getVersion() + " by VotisWorld");
        
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("LiteBans")) {
                Class<?> databaseClass = Class.forName("litebans.api.Database");
                Method getMethod = databaseClass.getMethod("get");
                liteBansDatabaseInstance = getMethod.invoke(null);
                
                isPlayerMutedMethod = liteBansDatabaseInstance.getClass().getMethod("isPlayerMuted", String.class, String.class);
                liteBansLinked = true;
                
                getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "  [+] Успешно подключено к API LiteBans через Reflection API!");
                getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "  [🔒] Защита чата slayrw-chat от обхода мута НАМЕРТВО АКТИВНА.");
            } else {
                getServer().getConsoleSender().sendMessage(ChatColor.RED + "  [-] LiteBans не найден на сервере! Плагин отключается.");
                Bukkit.getPluginManager().disablePlugin(this);
                return;
            }
        } catch (Exception e) {
            getServer().getConsoleSender().sendMessage(ChatColor.RED + "  [-] Критическая ошибка подключения к LiteBans API: " + e.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        getServer().getConsoleSender().sendMessage(ChatColor.GOLD + "====================================================");

        Bukkit.getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("worldmutefix")).setExecutor(this);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChatEarly(AsyncPlayerChatEvent event) {
        checkAndLockMute(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onChatLate(AsyncPlayerChatEvent event) {
        checkAndLockMute(event);
    }

    private void checkAndLockMute(AsyncPlayerChatEvent event) {
        if (!liteBansLinked || liteBansDatabaseInstance == null || isPlayerMutedMethod == null) {
            return;
        }

        Player player = event.getPlayer();
        try {
            String uuid = player.getUniqueId().toString();
            String ip = Objects.requireNonNull(player.getAddress()).getAddress().getHostAddress();

            boolean isMuted = (boolean) isPlayerMutedMethod.invoke(liteBansDatabaseInstance, uuid, ip);

            if (isMuted) {
                // Выбиваем почву из-под ног slayrw-chat: зануляем коллекцию получателей пакетов чата
                event.getRecipients().clear();
                event.setCancelled(true);

                String text = getConfig().getString("mute-message", "&cВы замучены!");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', text));
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("worldmutefix.admin")) {
                String noPerm = getConfig().getString("no-permission", "&cНет прав.");
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', noPerm));
                return true;
            }

            reloadConfig();
            String reloadMsg = getConfig().getString("reload-message", "&aКонфиг перезагружен.");
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', reloadMsg));
            return true;
        }
        
        sender.sendMessage(ChatColor.GOLD + "Используйте: " + ChatColor.YELLOW + "/worldmutefix reload");
        return true;
    }
}
