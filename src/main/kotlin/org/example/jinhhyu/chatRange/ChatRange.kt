package org.example.jinhhyu.chatRange

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scoreboard.Scoreboard
import org.bukkit.scoreboard.Team
import java.util.UUID

class ChatRange : JavaPlugin(), Listener {
    private val inviteTimeoutMillis = 2 * 60 * 1000L
    private val pendingInvites = mutableMapOf<UUID, TeamInvite>()
    private val teamChatMode = mutableSetOf<UUID>()

    private val mainScoreboard: Scoreboard
        get() = Bukkit.getScoreboardManager().mainScoreboard

    override fun onEnable() {
        server.pluginManager.registerEvents(this, this)
    }

    override fun onDisable() {
        pendingInvites.clear()
        teamChatMode.clear()
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!command.name.equals("t", ignoreCase = true)) {
            return false
        }

        val player = sender as? Player
        if (player == null) {
            sender.sendMessage("This command can only be used by players.")
            return true
        }

        if (args.isEmpty()) {
            sendUsage(player)
            return true
        }

        return when (args[0].lowercase()) {
            "invite" -> handleInvite(player, args)
            "join" -> handleJoin(player, args)
            "leave" -> handleLeave(player)
            "chat" -> handleChatToggle(player, args)
            else -> handleTeamMessage(player, args)
        }
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (!command.name.equals("t", ignoreCase = true)) {
            return emptyList()
        }

        val player = sender as? Player ?: return emptyList()
        return when (args.size) {
            1 -> {
                val options = listOf("invite", "join", "leave", "chat")
                options.filter { it.startsWith(args[0], ignoreCase = true) }
            }

            2 -> {
                when (args[0].lowercase()) {
                    "invite" -> Bukkit.getOnlinePlayers()
                        .asSequence()
                        .filter { it.uniqueId != player.uniqueId }
                        .map { it.name }
                        .filter { it.startsWith(args[1], ignoreCase = true) }
                        .toList()

                    "join" -> {
                        val invite = pendingInvites[player.uniqueId] ?: return emptyList()
                        listOf(invite.teamName).filter { it.startsWith(args[1], ignoreCase = true) }
                    }

                    else -> emptyList()
                }
            }

            else -> emptyList()
        }
    }

    private fun handleInvite(inviter: Player, args: Array<out String>): Boolean {
        if (args.size != 2) {
            inviter.sendMessage(Component.text("Usage: /t invite <name>", NamedTextColor.RED))
            return true
        }

        val target = Bukkit.getPlayerExact(args[1])
        if (target == null || !target.isOnline) {
            inviter.sendMessage(Component.text("That player is not online.", NamedTextColor.RED))
            return true
        }

        if (target.uniqueId == inviter.uniqueId) {
            inviter.sendMessage(Component.text("You cannot invite yourself.", NamedTextColor.RED))
            return true
        }

        if (mainScoreboard.getEntryTeam(target.name) != null) {
            inviter.sendMessage(Component.text("${target.name} is already in a team.", NamedTextColor.RED))
            return true
        }

        val inviterTeam = getOrCreateTeam(inviter)
        pendingInvites[target.uniqueId] = TeamInvite(
            teamName = inviterTeam.name,
            inviterId = inviter.uniqueId,
            createdAtMillis = System.currentTimeMillis()
        )

        inviter.sendMessage(Component.text("Invitation sent to ${target.name}.", NamedTextColor.GREEN))
        target.sendMessage(
            Component.text("${inviter.name} invited you to a team. ", NamedTextColor.YELLOW)
                .append(
                    Component.text("[Join Team]", NamedTextColor.GREEN, TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/t join ${inviterTeam.name}"))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to join ${inviter.name}'s team")))
                )
        )
        return true
    }

    private fun handleJoin(player: Player, args: Array<out String>): Boolean {
        if (mainScoreboard.getEntryTeam(player.name) != null) {
            player.sendMessage(Component.text("You are already in a team. Use /t leave first.", NamedTextColor.RED))
            return true
        }

        val invite = pendingInvites[player.uniqueId]
        if (invite == null) {
            player.sendMessage(Component.text("You do not have a pending invitation.", NamedTextColor.RED))
            return true
        }

        if (System.currentTimeMillis() - invite.createdAtMillis > inviteTimeoutMillis) {
            pendingInvites.remove(player.uniqueId)
            player.sendMessage(Component.text("Your invitation has expired.", NamedTextColor.RED))
            return true
        }

        if (args.size == 2 && !args[1].equals(invite.teamName, ignoreCase = true)) {
            player.sendMessage(Component.text("That invitation is no longer valid.", NamedTextColor.RED))
            return true
        }

        val team = mainScoreboard.getTeam(invite.teamName)
        if (team == null) {
            pendingInvites.remove(player.uniqueId)
            player.sendMessage(Component.text("That team no longer exists.", NamedTextColor.RED))
            return true
        }

        team.addEntry(player.name)
        if (mainScoreboard.getEntryTeam(player.name)?.name != team.name) {
            player.sendMessage(Component.text("Failed to join the team.", NamedTextColor.RED))
            return true
        }

        pendingInvites.remove(player.uniqueId)
        player.sendMessage(Component.text("You joined the team.", NamedTextColor.GREEN))
        Bukkit.getPlayer(invite.inviterId)?.sendMessage(
            Component.text("${player.name} accepted your team invitation.", NamedTextColor.GREEN)
        )
        broadcastToTeam(team, Component.text("${player.name} joined the team.", NamedTextColor.AQUA))
        return true
    }

    private fun handleLeave(player: Player): Boolean {
        val team = mainScoreboard.getEntryTeam(player.name)
        if (team == null) {
            player.sendMessage(Component.text("You are not in a team.", NamedTextColor.RED))
            return true
        }

        teamChatMode.remove(player.uniqueId)
        team.removeEntry(player.name)
        player.sendMessage(Component.text("You left the team.", NamedTextColor.YELLOW))
        broadcastToTeam(team, Component.text("${player.name} left the team.", NamedTextColor.YELLOW))

        if (team.entries.isEmpty()) {
            pendingInvites.entries.removeIf { it.value.teamName == team.name }
            team.unregister()
        }

        return true
    }

    private fun handleChatToggle(player: Player, args: Array<out String>): Boolean {
        if (args.size != 1) {
            player.sendMessage(Component.text("Usage: /t chat", NamedTextColor.RED))
            return true
        }

        val team = mainScoreboard.getEntryTeam(player.name)
        if (team == null) {
            player.sendMessage(Component.text("You are not in a team.", NamedTextColor.RED))
            return true
        }

        if (teamChatMode.contains(player.uniqueId)) {
            teamChatMode.remove(player.uniqueId)
            player.sendMessage(Component.text("Global chat mode enabled.", NamedTextColor.YELLOW))
        } else {
            teamChatMode.add(player.uniqueId)
            player.sendMessage(Component.text("Team chat mode enabled.", NamedTextColor.GREEN))
            player.sendMessage(Component.text("Use /t chat again to switch back to global chat.", NamedTextColor.GRAY))
        }

        return true
    }

    private fun handleTeamMessage(player: Player, args: Array<out String>): Boolean {
        val team = mainScoreboard.getEntryTeam(player.name)
        if (team == null) {
            player.sendMessage(Component.text("You are not in a team.", NamedTextColor.RED))
            return true
        }

        val message = args.joinToString(" ")
        if (message.isBlank()) {
            player.sendMessage(Component.text("Usage: /t <message>", NamedTextColor.RED))
            return true
        }

        val teamChatMessage = Component.text("[Team] ", NamedTextColor.GRAY)
            .append(Component.text(player.name, NamedTextColor.AQUA))
            .append(Component.text(": ", NamedTextColor.DARK_GRAY))
            .append(Component.text(message, NamedTextColor.WHITE))

        broadcastToTeam(team, teamChatMessage)
        return true
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onAsyncChat(event: AsyncChatEvent) {
        val sender = event.player
        if (!teamChatMode.contains(sender.uniqueId)) {
            return
        }

        val message = event.message()
        event.isCancelled = true

        server.scheduler.runTask(this, Runnable {
            val onlineSender = Bukkit.getPlayer(sender.uniqueId) ?: return@Runnable
            val team = mainScoreboard.getEntryTeam(onlineSender.name)
            if (team == null) {
                teamChatMode.remove(onlineSender.uniqueId)
                onlineSender.sendMessage(Component.text("You are not in a team. Team chat mode was disabled.", NamedTextColor.RED))
                return@Runnable
            }

            val teamChatMessage = Component.text("[Team] ", NamedTextColor.GRAY)
                .append(Component.text(onlineSender.name, NamedTextColor.AQUA))
                .append(Component.text(": ", NamedTextColor.DARK_GRAY))
                .append(message)

            broadcastToTeam(team, teamChatMessage)
        })
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        teamChatMode.remove(event.player.uniqueId)
    }

    private fun getOrCreateTeam(player: Player): Team {
        mainScoreboard.getEntryTeam(player.name)?.let { return it }

        val newTeamName = generateTeamName()
        val team = mainScoreboard.registerNewTeam(newTeamName)
        team.displayName(Component.text("${player.name}'s team"))
        team.addEntry(player.name)
        return team
    }

    private fun generateTeamName(): String {
        val scoreboard = mainScoreboard
        var teamName: String
        do {
            teamName = "t" + UUID.randomUUID().toString().replace("-", "").take(15)
        } while (scoreboard.getTeam(teamName) != null)
        return teamName
    }

    private fun broadcastToTeam(team: Team, message: Component) {
        for (entry in team.entries) {
            val teammate = Bukkit.getPlayerExact(entry)
            teammate?.sendMessage(message)
        }
    }

    private fun sendUsage(player: Player) {
        player.sendMessage(Component.text("/t invite <name> - Invite a player", NamedTextColor.GRAY))
        player.sendMessage(Component.text("/t chat - Toggle team/global chat", NamedTextColor.GRAY))
        player.sendMessage(Component.text("/t leave - Leave your team", NamedTextColor.GRAY))
        player.sendMessage(Component.text("/t <message> - Send team chat", NamedTextColor.GRAY))
    }

    private data class TeamInvite(
        val teamName: String,
        val inviterId: UUID,
        val createdAtMillis: Long
    )
}
