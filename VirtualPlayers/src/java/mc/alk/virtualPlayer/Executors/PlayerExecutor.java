package mc.alk.virtualPlayer.Executors;

import mc.alk.virtualPlayer.Util.InventoryUtil;
import mc.alk.virtualPlayer.Util.Util;
import mc.alk.virtualPlayer.VirtualPlayer;
import mc.alk.virtualPlayer.VirtualPlayers;
import net.minecraft.server.v1_7_R3.DamageSource;
import net.minecraft.server.v1_7_R3.EntityPlayer;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_7_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_7_R3.event.CraftEventFactory;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * @author alkarin
 */
public class PlayerExecutor extends VPBaseExecutor {
    public PlayerExecutor(Plugin plugin) {
        super(plugin,1);
        super.useAlias = 0;
    }


    @MCCommand(op = true)
    public boolean runCommand(CommandSender sender, VirtualPlayer vp, String... args) {
        String command = StringUtils.join(args, " ");
        sendMessage(sender, "&6 " + vp.getName() + "&2 executing '&6" + command + "&2'");
        PlayerCommandPreprocessEvent pcpe = new PlayerCommandPreprocessEvent(vp, "/" + command);
        Bukkit.getPluginManager().callEvent(pcpe);
        if (pcpe.isCancelled()) {
            return sendMessage(sender, "&6 " + vp.getName() + "&c command cancelled : &6" + command);
        }
        Bukkit.getServer().dispatchCommand(vp, command);
        return true;
    }

    @MCCommand(cmds = {"showScoreboard"}, op = true)
    public boolean showScoreboard(CommandSender sender, VirtualPlayer vp) {
        Scoreboard sc = vp.getScoreboard();
        if (sc == null)
            return sendMessage(sender, "&4Scoreboard for " + vp.getName() + " is null");
        sendMessage(sender, "&4Scoreboard &f" + sc.hashCode());
        sendMessage(sender, "&e -- Teams -- ");
        Collection<OfflinePlayer> ops = sc.getPlayers();
        for (Team t : sc.getTeams()) {
            sendMessage(sender, t.getName() + " - " + t.getDisplayName());
        }
        for (Objective o : sc.getObjectives()) {
            sendMessage(sender, "&2 -- Objective &e" + o.getName() + " - " + o.getDisplayName());
            for (OfflinePlayer op : ops) {
                Score score = o.getScore(op);
                if (score == null)
                    continue;
                sendMessage(sender, op.getName() + " : " + score.getScore());
            }
        }
        return true;

    }

    @MCCommand(cmds = {"status"}, op = true)
    public boolean constructStatusMessages(CommandSender sender, VirtualPlayer vp) {
        return sendMessage(sender, "&4" + vp);
    }

    @MCCommand(cmds = {"op"}, op = true)
    public boolean opPlayer(CommandSender sender, VirtualPlayer vp) {
        return opPlayer(sender, vp, true);
    }

    @MCCommand(cmds = {"deop"}, op = true)
    public boolean deopPlayer(CommandSender sender, VirtualPlayer vp) {
        return opPlayer(sender, vp, false);
    }

    private boolean opPlayer(CommandSender sender, VirtualPlayer vp, boolean op) {
        vp.setOp(op);
        String opped = op ? "opped" : "deopped";
        return sendMessage(sender, "&6" + vp.getName() + "&2 " + opped + "!");
    }

    @MCCommand(cmds = {"click", "interact"}, op = true, usage = "/dc <vp> click <left|right> <location>")
    public boolean interactEvent(CommandSender sender, VirtualPlayer vp, String lr, Location l) {
        if (l.getWorld() == null) {
            l.setWorld(vp.getLocation().getWorld());
        }
        boolean left = lr.equalsIgnoreCase("left");
        Action action;
        if (l.getBlock().getType() == Material.AIR) {
            action = left ? Action.LEFT_CLICK_AIR : Action.RIGHT_CLICK_AIR;
        } else {
            action = left ? Action.LEFT_CLICK_BLOCK : Action.RIGHT_CLICK_BLOCK;
        }
        ItemStack inhand = vp.getItemInHand();
        // Block b = w.getBlockAt(l);
        Block b = l.getWorld().getBlockAt(l);
        // PlayerInteractEvent(final Player who, final Action action, final
        // ItemStack item,
        // final Block clickedBlock, final BlockFace clickedFace) {
        PlayerInteractEvent ede = new PlayerInteractEvent(vp, action, inhand, b, BlockFace.EAST);
        Bukkit.getPluginManager().callEvent(ede);
        sendMessage(sender, "&6" + vp.getName() + "&e " + action.name() + " &4"
                + b.getType() + "&2  with a &a" + inhand.getType().name());
        if (b.getType() == Material.SIGN || b.getType() == Material.SIGN_POST) {
            Sign s = (Sign) b.getState();
            String[] lines = s.getLines();
            for (int i = 1; i <= lines.length; i++) {
                sendMessage(sender, "&6Line: " + i + "=&f'" + lines[i - 1] + "&f'");
            }
        }

        return true;
    }

    @MCCommand(cmds = {"sneak"}, op = true)
    public boolean sneak(CommandSender sender, VirtualPlayer vp, boolean sneak) {
        vp.setSneaking(sneak);
        if (sneak){
            return sendMessage(sender, "&6" + vp.getName() + "&e is now &8sneaking");
        } else {
            return sendMessage(sender, "&6" + vp.getName() + "&e is now &fnot sneaking");
        }
    }


    @MCCommand(cmds = {"examine"}, op = true)
    public boolean examine(CommandSender sender, VirtualPlayer vp, Location l) {
        if (l.getWorld() == null) {
            l.setWorld(vp.getLocation().getWorld());
        }
        Block b = l.getWorld().getBlockAt(l);

        sendMessage(sender, "&6" + vp.getName() + "&e examining location " + l + " &4" + b.getType());
        if (b.getType() == Material.SIGN || b.getType() == Material.SIGN_POST) {
            Sign s = (Sign) b.getState();
            String[] lines = s.getLines();
            for (int i = 1; i <= lines.length; i++) {
                sendMessage(sender, "&6Line: " + i + "=&f'" + lines[i - 1] + "&f'");
            }
        }
        return true;
    }

    @MCCommand(cmds = {"tp", "teleport"}, op = true)
    public boolean teleportPlayer(CommandSender sender, VirtualPlayer vp, Location l) {
        if (l.getWorld() == null) {
            l.setWorld(vp.getLocation().getWorld());
        }
        PlayerTeleportEvent pte = new PlayerTeleportEvent(vp, vp.getLocation(),
                l, TeleportCause.COMMAND);
        Bukkit.getPluginManager().callEvent(pte);
        if (pte.isCancelled()) {
            sendMessage(sender, "&cTeleport of " + vp.getDisplayName() + " was cancelled by some plugin");
        } else {
            vp.teleport(l);
        }
        return true;
    }

    @MCCommand(cmds = {"gm", "gamemode"}, op = true)
    public boolean setGameMode(CommandSender sender, VirtualPlayer vp, GameMode gm) {
        vp.setGameMode(gm);
        sendMessage(sender, "&6" + vp.getName() + "&2 gamemode &6" + gm);
        return true;
    }

    @MCCommand(cmds = {"chat"}, op = true)
    public boolean chatEvent(CommandSender sender, final VirtualPlayer vp, String... args) {
        final String msg = Util.colorChat(StringUtils.join(args, " "));
        final HashSet<Player> players = new HashSet<Player>(
                Arrays.asList(VirtualPlayers.getOnlinePlayers()));
        Runnable r = new Runnable() {
            @Override
            public void run() {
                AsyncPlayerChatEvent apce = new AsyncPlayerChatEvent(true, vp, msg, players);
                Bukkit.getPluginManager().callEvent(apce);
            }
        };
        new Thread(r).start();

        sendMessage(sender, "&6" + vp.getName() + "&2 said " + msg);
        return true;
    }

    @MCCommand(cmds = {"respawn"}, op = true)
    public boolean playerRespawn(CommandSender sender, VirtualPlayer vp) {
        World w = vp.getWorld();
        vp.respawn(w.getSpawnLocation());
        return sendMessage(sender, "&6" + vp.getName() + "&2 respawned!");
    }

    @MCCommand(cmds = {"bpe", "BlockPlaceEvent"}, op = true)
    public boolean blockPlaceEvent(CommandSender sender, VirtualPlayer vp, Material mat, Location loc) {
        if (loc.getWorld() == null) {
            loc.setWorld(vp.getLocation().getWorld());
        }
        Block replaced = loc.getBlock();
        Material old = replaced.getType();
        replaced.setType(mat);
        BlockPlaceEvent bpe = new BlockPlaceEvent(replaced,
                replaced.getState(), replaced.getRelative(BlockFace.NORTH),
                vp.getItemInHand(), vp, true);
        Bukkit.getPluginManager().callEvent(bpe);
        if (bpe.isCancelled()) {
            replaced.setType(old);
            return sendMessage(sender, "&cBlockPlaceEvent was cancelled for &6"
                    + vp.getName());
        } else {
            return sendMessage(sender,
                    "&6" + vp.getName() + "&e placed " + mat + " on "
                            + old + "  at &4" + Util.getLocString(loc)
            );
        }
    }

    @MCCommand(cmds = {"bbe", "BlockBreakEvent"}, op = true)
    public boolean blockBreakEvent(CommandSender sender, VirtualPlayer vp, Location loc) {
        if (loc.getWorld() == null) {
            loc.setWorld(vp.getLocation().getWorld());
        }
        Block replaced = loc.getBlock();
        Material old = replaced.getType();
        BlockBreakEvent bpe = new BlockBreakEvent(replaced, vp);
        Bukkit.getPluginManager().callEvent(bpe);
        if (bpe.isCancelled()) {
            return sendMessage(sender, "&cBlockPlaceEvent was cancelled for &6" + vp.getName());
        } else {
            replaced.setType(Material.AIR);
            return sendMessage(sender, "&6" + vp.getName() + "&e broke the "
                    + old + " at &4" + Util.getLocString(loc));
        }
    }

    @MCCommand(cmds = {"giveInv"}, op = true)
    public boolean giveInv(CommandSender sender, VirtualPlayer vp, ItemStack is) {
        int amount = is.getAmount() > 0 ? is.getAmount() : 1;
        InventoryUtil.addItemToInventory(vp.getInventory(), is, amount);
        return sendMessage(sender, "&2Added &6" + is + "&2 to inventory");
    }

    @MCCommand(cmds = {"giveHelm"}, op = true)
    public boolean giveHelm(CommandSender sender, VirtualPlayer vp, ItemStack is) {
        vp.getInventory().setHelmet(is);
        return sendMessage(sender, "&2Added &6" + is + "&2 as the helm");
    }

    @MCCommand(cmds = {"inv", "printInv"}, op = true)
    public boolean printInv(CommandSender sender, VirtualPlayer vp) {
        PlayerInventory inv = vp.getInventory();
        if (inv != null) {
            sendMessage(sender, "&bHelm:&6" + InventoryUtil.getItemString(inv.getHelmet()));
            sendMessage(sender, "&bChest:&6" + InventoryUtil.getItemString(inv.getChestplate()));
            sendMessage(sender, "&bLegs:&6" + InventoryUtil.getItemString(inv.getLeggings()));
            sendMessage(sender, "&bBoot:&6" + InventoryUtil.getItemString(inv.getBoots()));
            sendMessage(sender, "&4InHand:&6" + InventoryUtil.getItemString(inv.getItemInHand()));
            ItemStack[] contents = inv.getContents();
            for (int i = 0; i < contents.length; i++) {
                ItemStack is = contents[i];
                if (is != null && is.getType() != Material.AIR)
                    sendMessage(sender, "&2" + i + ":&e" + InventoryUtil.getItemString(is));
            }
        }
        return true;
    }

    @MCCommand(cmds = {"health"}, op = true)
    public boolean health(CommandSender sender, VirtualPlayer damagee, double h, String... args) {
        boolean force = (args.length > 0 && args[0].equalsIgnoreCase("force"));
        EntityDamageEvent ede = CraftEventFactory.callEntityDamageEvent(
                null,
                damagee.getHandle(),
                EntityDamageEvent.DamageCause.ENTITY_ATTACK, damagee.getHealth() - h);
        if (!ede.isCancelled() || force) {
            damagee.setLastDamageCause(ede);
            damagee.setHealth(damagee.getHealth() - ede.getDamage());
            return sendMessage(sender, damagee, "&6" + damagee.getName() + "&2, life=&4" + damagee.getHealth());

        } else {
            return sendMessage(sender, damagee, "&cHealth set was cancelled for &6" + damagee.getName());
        }
    }

    @MCCommand(cmds = {"hit"}, op = true)
    public boolean damageEvent(CommandSender sender, Player damager, Player damagee, int damage) {
        if (damagee.getHealth() <= 0) {
            return sendMessage(sender, "&6" + damagee.getName() + "&c is already dead!");
        }
        EntityDamageEvent ede = CraftEventFactory.callEntityDamageEvent(
                ((CraftPlayer) damager).getHandle(),
                ((CraftPlayer) damagee).getHandle(),
                EntityDamageEvent.DamageCause.ENTITY_ATTACK, damage);

        if (!ede.isCancelled()) {
            damagee.setLastDamageCause(ede);
            EntityPlayer ep = ((CraftPlayer) damagee).getHandle();
            if (damagee instanceof VirtualPlayer) {
                ((VirtualPlayer) damagee).setHealth(damagee.getHealth() - ede.getDamage());
            } else {
                ep.damageEntity(DamageSource.GENERIC, (float) ede.getDamage());
            }
            return sendMessage(sender, "&6" + damager.getName() + "&2 hit &6"
                    + damagee.getName() + "&2 for &6" + damage + "&2, life=&4"
                    + damagee.getHealth());
        } else {
            return sendMessage(sender, "&cDamage Event was cancelled for &6"
                    + damager.getName() + "&c hitting &6" + damagee.getName());
        }
    }

    @MCCommand(cmds = {"kill"}, op = true)
    public boolean playerKill(VirtualPlayer vp) {
        Server cserver = Bukkit.getServer();
        List<ItemStack> is = new LinkedList<ItemStack>();
        vp.setHealth(0.0);
        vp.damage(100000.0);
        PlayerDeathEvent ede = new PlayerDeathEvent(vp, is, 0, "");
        cserver.getPluginManager().callEvent(ede);
        return true;
    }

    @MCCommand(cmds = {"connect"}, op = true)
    public boolean playerConnect(CommandSender sender, final VirtualPlayer vp) {
        return playerConnection(sender, vp, true);
    }

    @MCCommand(cmds = {"dc","disconnect"}, op = true)
    public boolean playerDisconnect(CommandSender sender, final VirtualPlayer vp) {
        return playerConnection(sender, vp, false);
    }

    private boolean playerConnection(CommandSender sender, final VirtualPlayer vp, boolean connecting)
    {
        vp.setOnline(connecting);
        if (connecting)
        {
            // PreLogin Event has to be called from a thread other than the
            // main.
            final String playerName = vp.getName();
            Runnable r = new Runnable()
            {
                @Override
                public void run()
                {
                    try{
                        AsyncPlayerPreLoginEvent playerPreLoginEvent = new AsyncPlayerPreLoginEvent(
                                playerName, InetAddress.getLocalHost());
                        Bukkit.getPluginManager().callEvent(playerPreLoginEvent);
                    } catch (UnknownHostException ex){/* say nothing */}
                    /// After we are done Asynchronously handling the
                    /// preloginevent
                    /// Sync back up and call the Login and Join events
                    Bukkit.getScheduler().scheduleSyncDelayedTask(
                            VirtualPlayers.getSelf(), new Runnable(){
                                @Override
                                public void run()
                                {
                                    // Then the Login Event.
                                    Server cserver = Bukkit.getServer();
                                    try
                                    {
                                        PlayerLoginEvent playerLoginEvent = new PlayerLoginEvent(
                                                vp, "localhost", InetAddress
                                                .getLocalHost());
                                        cserver.getPluginManager().callEvent(
                                                playerLoginEvent);
                                    } catch (UnknownHostException ex){/* do nothing */}

                                    // Finally, the player join event.
                                    PlayerJoinEvent playerJoinEvent = new PlayerJoinEvent(
                                            vp, "\u00A7e" + vp.getName()
                                            + " joined the game.");
                                    cserver.getPluginManager().callEvent( playerJoinEvent);
                                }
                            });

                }
            };
            new Thread(r).start();
        }
        else
        { /// Disconnecting
            Server cserver = Bukkit.getServer();
            PlayerQuitEvent playerQuitEvent = new PlayerQuitEvent(vp, "\u00A7e"
                    + vp.getName() + " left the game.");
            cserver.getPluginManager().callEvent(playerQuitEvent);
        }
        String msg = "&6" + vp.getName() + "&2 " +
                (connecting ? "connecting.  Details:&6" + vp : "&cdisconnecting");
        sendMessage(sender, vp, msg);
        return true;
    }

    @MCCommand(cmds = {"movetome"}, op = true)
    public boolean moveVirtualPlayerToMe(CommandSender sender, VirtualPlayer vp) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "You must be a player to use this!");
            return false;
        }
        Player player = (Player) sender;
        sendMessage(player, "&6" + vp.getName() + " is now trying to come to you!");
        vp.moveTo(player.getLocation());
        return true;
    }
}
