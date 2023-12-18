package corebot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.HashMap;
import java.util.List;

public class Logging extends ListenerAdapter {
    static HashMap<Long, String> messages = new HashMap<>();
    static HashMap<Long, String> names = new HashMap<>();

    static EmbedBuilder EB = new EmbedBuilder();



    public void onGuildMessageDelete(MessageDeleteEvent event) {
        Guild guild = event.getGuild();
        TextChannel log = guild.getTextChannelById(String.valueOf(Messages.logChannel));
        if (messages.containsKey(event.getMessageIdLong()) & names.containsKey(event.getMessageIdLong())) {
            log.sendMessage(names.get(event.getMessageIdLong())  + "\nТекст:\n" + messages.get(event.getMessageIdLong()));
        }
    }


    public void onGuildMessageReceived(MessageReceivedEvent event) {
        if (!event.getAuthor().isBot()) {
            messages.put(event.getMessageIdLong(), event.getMessage().getContentRaw());
            names.put(event.getMessageIdLong(), event.getAuthor().getName());
        }
    }

    @Override
    public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) {
        Guild guild = event.getGuild();
        TextChannel log = guild.getTextChannelById(String.valueOf(Messages.logChannel));
        StringBuilder parseRoles = new StringBuilder();
        List<Role> roles = event.getRoles();
        for (Role role : roles) {
            parseRoles.append(role.getAsMention()).append("\n");
        }
        log.sendMessage("Видалено ролi учасника " + event.getUser().getName() + ":" +
                parseRoles).queue();
    }

    @Override
    public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event) {
        Guild guild = event.getGuild();
        TextChannel log = guild.getTextChannelById(String.valueOf(Messages.logChannel));
        String parseRoles = "";
        List<Role> roles = event.getRoles();
        for (Role role : roles) {
            parseRoles = parseRoles + role.getAsMention() + "\n";
        }
        log.sendMessage("Додано ролi учаснику " + event.getUser().getName() + ":" +
                parseRoles).queue();
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        Guild guild = event.getGuild();
        TextChannel log = guild.getTextChannelById(String.valueOf(Messages.logChannel));
        log.sendMessage(event.getUser().getName() + " зайшов(-ла) на сервер").queue();
    }


    public void onGuildMemberLeave(GuildMemberRemoveEvent event) {
        Guild guild = event.getGuild();
        TextChannel log = guild.getTextChannelById(String.valueOf(Messages.logChannel));
        log.sendMessage(event.getUser().getName() + " вийшов(-ла) з сервера").queue();
    }
}