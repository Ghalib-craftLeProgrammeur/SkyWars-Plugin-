import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class skywarsplugin extends JavaPlugin implements Listener {

    private Map<Player, String> playerQueueMap = new HashMap<>();
    private Map<String, Location> queueLocations = new HashMap<>();
    private List<Player> gameQueue = new ArrayList<>();
    private boolean gameInProgress = false;
    private List<Location> chestLocations = new ArrayList<>();
    private Map<Player, Location> playerCages = new HashMap<>();
    private Map<Player, Integer> team1Players = new HashMap<>();
    private Map<Player, Integer> team2Players = new HashMap<>();
    private List<Player> team1Eliminated = new ArrayList<>();
    private List<Player> team2Eliminated = new ArrayList<>();
    private List<Location> cages = new ArrayList<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("SkyWars plugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("SkyWars plugin disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        if (cmd.getName().equalsIgnoreCase("start")) {
            if (!player.hasPermission("skywars.admin")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }

            if (gameInProgress) {
                player.sendMessage(ChatColor.RED + "A game is already in progress.");
                return true;
            }
            gameQueue.add(player);
            startGame();
            return true;
        }
        if (cmd.getName().equalsIgnoreCase("sendplayertoheaven")) {
            if (args.length == 1) {
                World lobbyWorld = Bukkit.getWorld("Alejo");
                if (lobbyWorld == null) {
                    lobbyWorld = Bukkit.createWorld(new WorldCreator("Alejo"));
                    if (lobbyWorld == null) {
                        getLogger().warning("Could not create the Alejo world!");
                    } else {
                        getLogger().info("Alejo world created successfully!");
                    }
                    Player targetPlayer = Bukkit.getPlayer(args[0]);
                    if (targetPlayer != null) {
                        targetPlayer.teleport(lobbyWorld.getSpawnLocation());
                    } else {
                        getLogger().warning("Player not found: " + args[0]);
                    }
                }
            } else {
                player.sendMessage(ChatColor.RED + "Usage: /sendplayertoheaven <name>");
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("summonnpc")) {
            if (args.length != 2) {
                player.sendMessage("Usage: /summonnpc <name> <type>");
                return true;
            }

            String npcName = args[0];
            String type = args[1];

            if (!type.equalsIgnoreCase("1v1") && !type.equalsIgnoreCase("2v2")) {
                player.sendMessage("Invalid NPC type. Supported types: 1v1, 2v2");
                return true;
            }

            Villager npc = (Villager) player.getWorld().spawnEntity(player.getLocation(), EntityType.VILLAGER);
            npc.setCustomName(ChatColor.translateAlternateColorCodes('&', "&lSkyWars &2&l&l&o&n" + type));
            npc.setCustomNameVisible(true);
            npc.setAI(false);
            npc.setInvulnerable(true);
            npc.setCollidable(false);

            queueLocations.put(npcName, player.getLocation());

            player.sendMessage("Summoned NPC \"" + npcName + "\" successfully.");
            return true;
        }
        return false;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock.getType() == Material.CHEST && !chestLocations.contains(clickedBlock.getLocation()) && (player.getWorld().getName().contains("Ajelo"))) {
                chestLocations.add(clickedBlock.getLocation());
                Chest chest = (Chest) clickedBlock.getState();
                Inventory chestInventory = chest.getBlockInventory();
                chestInventory.clear();
                generateChestLoot(chestInventory);
            }
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getRightClicked().getType() == EntityType.VILLAGER) {
            Player player = event.getPlayer();
            Villager npc = (Villager) event.getRightClicked();

            if (queueLocations.containsValue(npc.getLocation())) {
                for (Map.Entry<String, Location> entry : queueLocations.entrySet()) {
                    if (entry.getValue().equals(npc.getLocation())) {
                        String npcName = entry.getKey();

                        if (playerQueueMap.containsKey(player)) {
                            player.sendMessage("You are already queued for a game.");
                            return;
                        }

                        playerQueueMap.put(player, npcName);
                        gameQueue.add(player);
                        player.sendMessage("You've joined the queue for \"" + npcName + "\".");

                        if (gameQueue.size() >= 2 && !gameInProgress) {
                            startGame();
                        }
                    }
                }
            }
        }
    }
    public void startGame() {
        gameInProgress = true;
        World lobbyWorld = Bukkit.getWorld("hbwl");
        if (lobbyWorld == null) {
            lobbyWorld = Bukkit.createWorld(new WorldCreator("hbwl"));
            if (lobbyWorld == null) {
                getLogger().warning("Could not create the hbwl world!");
            } else {
                getLogger().info("hbwl world created successfully!");
            }
        } else {
            getLogger().info("hbwl world loaded successfully!");
        }

        for (Player player : gameQueue) {
            player.setGameMode(GameMode.ADVENTURE);
            player.teleport(lobbyWorld.getSpawnLocation());
        }
        Bukkit.broadcastMessage("SkyWars match starting in 30 seconds!");
        new BukkitRunnable() {
            @Override
            public void run() {
                World lobbysWorld = Bukkit.getWorld("Ajelo");
                if (lobbysWorld == null) {
                    lobbysWorld = Bukkit.createWorld(new WorldCreator("Ajelo"));
                    if (lobbysWorld == null) {
                        getLogger().warning("Could not create the Ajelo world!");
                        return;
                    } else {
                        getLogger().info("Ajelo world created successfully!");
                    }
                } else {
                    getLogger().info("Ajelo world loaded successfully!");
                }

                for (Player player : gameQueue) {
                    player.setGameMode(GameMode.SURVIVAL);
                    player.teleport(lobbysWorld.getSpawnLocation());
                }
                cages.add(new Location(gameQueue.get(0).getWorld(), -28.561, 92, -15.364));
                cages.add(new Location(gameQueue.get(0).getWorld(), 25.300, 92, -16.300));
                cages.add(new Location(gameQueue.get(0).getWorld(), 29.300, 92, 22.300));

                int cageIndex = 0;
                playerCages.clear();
                for (Player player : gameQueue) {
                    if (cageIndex < cages.size()) {
                        Location cageLocation = cages.get(cageIndex++);

                        assignCage(player, cageLocation);
                        player.setGameMode(GameMode.ADVENTURE);

                        if (!playerCages.containsKey(player)) {
                            playerCages.put(player, cageLocation);
                            player.setGameMode(GameMode.ADVENTURE);

                            new BukkitRunnable() {
                                int countdown = 15;
                                boolean countdownStarted = false;

                                @Override
                                public void run() {
                                    if (!countdownStarted) {
                                        Bukkit.broadcastMessage("Countdown started for " + player.getName());
                                        countdownStarted = true;
                                    }

                                    if (countdown > 0) {
                                        Bukkit.broadcastMessage("Time remaining: " + countdown + " seconds");
                                        countdown--;
                                    } else {
                                        Location playerLocation = player.getLocation();
                                        Block blockBelowPlayer = playerLocation.getBlock().getRelative(BlockFace.DOWN);
                                        blockBelowPlayer.setType(Material.AIR);
                                        player.setGameMode(GameMode.SURVIVAL);
                                        playerCages.remove(player);
                                        cancel();
                                    }
                                }
                            }.runTaskTimer(skywarsplugin.this, 0, 20);
                        }
                    } else {
                        getLogger().warning("Not enough cages for all players!");
                        break;
                    }
                }

                gameQueue.clear();
                gameInProgress = false;
            }
        }.runTaskLater(this, 20 * 30); // 30 seconds
    }

    private void generateChestLoot(Inventory chestInventory) {
        Random random = new Random();
        Material[] skyWarsItems = {
                Material.DIAMOND_SWORD,
                Material.IRON_SWORD,
                Material.BOW,
                Material.ARROW,
                Material.GOLDEN_APPLE,
                Material.GOLDEN_CHESTPLATE,
                Material.IRON_LEGGINGS,
                Material.BIRCH_PLANKS
        };

        int maxItems = random.nextInt(2) + 3;

        List<Material> itemList = Arrays.asList(skyWarsItems);
        Collections.shuffle(itemList);

        for (int i = 0; i < maxItems && i < itemList.size(); i++) {
            Material skyWarsItem = itemList.get(i);
            ItemStack itemStack = new ItemStack(skyWarsItem, 1);

            int emptySlot = -1;
            for (int j = 0; j < chestInventory.getSize(); j++) {
                if (chestInventory.getItem(j) == null) {
                    emptySlot = j;
                    break;
                }
            }

            if (emptySlot != -1) {
                chestInventory.setItem(emptySlot, itemStack);
            } else {
                break;
            }
        }
    }

    public void assignCage(Player player, Location cageLocation) {
        if (cageLocation.getWorld() != null && cageLocation.getWorld().getEnvironment() != World.Environment.THE_END) {
            playerCages.put(player, cageLocation);
            player.teleport(cageLocation);
        } else {
            getLogger().warning("Invalid world specified for cage location.");
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        player.teleport(player.getLastDeathLocation());
        player.setGameMode(GameMode.SPECTATOR);
        if (playerCages.containsKey(player)) {
            Location cageLocation = playerCages.get(player);
            player.teleport(cageLocation);
            if (team1Players.containsKey(player)) {
                team1Eliminated.add(player);
            } else if (team2Players.containsKey(player)) {
                team2Eliminated.add(player);
            }
            checkWinCondition();
        }
    }

    public void checkWinCondition() {
        if (team1Eliminated.size() == team1Players.size()) {
            Bukkit.broadcastMessage("Team 2 wins!");
            resetGame();
        } else if (team2Eliminated.size() == team2Players.size()) {
            Bukkit.broadcastMessage("Team 1 wins!");
            resetGame();
        } else if (team1Eliminated.size() == team1Players.size() - 1 && team2Players.size() > 1) {
            Bukkit.broadcastMessage("Team 2 wins!");
            resetGame();
        } else if (team2Eliminated.size() == team2Players.size() - 1 && team1Players.size() > 1) {
            Bukkit.broadcastMessage("Team 1 wins!");
            resetGame();
        }
    }

    public void resetGame() {
        gameInProgress = false;
        team1Players.clear();
        team2Players.clear();
        team1Eliminated.clear();
        team2Eliminated.clear();
        playerQueueMap.clear();
        playerCages.clear();
    }
}
