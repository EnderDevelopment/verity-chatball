package com.enderdevelopment.plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin implements Listener {
    private NamespacedKey itemType;
    private final Map<String, UUID> placedVerity = new HashMap<>();

    @Override
    public void onEnable() {
        itemType = new NamespacedKey(this, "verity_item");
        for (String entry : getConfig().getStringList("placed")) {
            String[] parts = entry.split(";", 2);
            if (parts.length == 2) {
                try {
                    placedVerity.put(parts[0], UUID.fromString(parts[1]));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        getConfig().set("placed", placedVerity.entrySet().stream()
                .map(entry -> entry.getKey() + ";" + entry.getValue()).toList());
        saveConfig();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!event.getPlayer().hasPlayedBefore()) {
            event.getPlayer().getInventory().addItem(createItem(Material.BARREL, "Caja de Verity", "box"));
            event.getPlayer().sendMessage(Component.text("¡Has recibido una caja misteriosa! Ábrela con clic derecho.", NamedTextColor.YELLOW));
        }
    }

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND
                || (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                && event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)) return;
        ItemStack held = event.getItem();
        if (held == null || !held.hasItemMeta()) return;
        String kind = held.getItemMeta().getPersistentDataContainer().get(itemType, PersistentDataType.STRING);
        if (kind == null) return;
        event.setCancelled(true);
        if (kind.equals("box")) {
            held.setAmount(held.getAmount() - 1);
            event.getPlayer().getInventory().addItem(createItem(Material.YELLOW_GLAZED_TERRACOTTA, "Verity", "verity"));
            event.getPlayer().sendMessage(Component.text("¡La caja se abrió! Verity ya está contigo.", NamedTextColor.GOLD));
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (!isVerity(event.getItemInHand())) return;
        Block block = event.getBlockPlaced();
        placedVerity.put(key(block.getLocation()), event.getPlayer().getUniqueId());
        event.getPlayer().sendMessage(Component.text("Verity está aquí. Háblale escribiendo su nombre en el chat.", NamedTextColor.YELLOW));
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        String locationKey = key(event.getBlock().getLocation());
        if (!placedVerity.containsKey(locationKey)) return;
        placedVerity.remove(locationKey);
        event.setDropItems(false);
        event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(),
                createItem(Material.YELLOW_GLAZED_TERRACOTTA, "Verity", "verity"));
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!event.getMessage().toLowerCase().contains("verity")) return;
        Location origin = player.getLocation();
        boolean nearby = placedVerity.keySet().stream().anyMatch(locationKey -> isNearby(locationKey, origin));
        if (nearby) Bukkit.getScheduler().runTask(this, () -> player.sendMessage(response()));
    }

    private boolean isNearby(String locationKey, Location origin) {
        String[] parts = locationKey.split(";");
        if (parts.length != 4 || !parts[0].equals(origin.getWorld().getName())) return false;
        try {
            Location verityLocation = new Location(origin.getWorld(),
                    Double.parseDouble(parts[1]) + 0.5,
                    Double.parseDouble(parts[2]) + 0.5,
                    Double.parseDouble(parts[3]) + 0.5);
            return origin.distanceSquared(verityLocation) <= 100;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private Component response() {
        Component[] replies = {
                Component.text("¡Hola! Me alegra verte.", NamedTextColor.YELLOW),
                Component.text("Hoy es un buen día para explorar.", NamedTextColor.GOLD),
                Component.text("¿Encontraste algo interesante?", NamedTextColor.YELLOW),
                Component.text("¡Siempre estaré aquí para ti!", NamedTextColor.GOLD)
        };
        return replies[(int) (Math.random() * replies.length)];
    }

    private ItemStack createItem(Material material, String name, String kind) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.YELLOW));
        meta.getPersistentDataContainer().set(itemType, PersistentDataType.STRING, kind);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isVerity(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return "verity".equals(item.getItemMeta().getPersistentDataContainer()
                .get(itemType, PersistentDataType.STRING));
    }

    private String key(Location location) {
        return location.getWorld().getName() + ";" + location.getBlockX() + ";"
                + location.getBlockY() + ";" + location.getBlockZ();
    }
}
