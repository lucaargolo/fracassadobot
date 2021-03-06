package me.d4rk.fracassadobot.commands;

import javafx.util.Pair;
import me.d4rk.fracassadobot.core.economy.*;
import me.d4rk.fracassadobot.core.RankSystemHandler;
import me.d4rk.fracassadobot.core.permission.BotPerms;
import me.d4rk.fracassadobot.core.command.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.*;

import static java.util.stream.Collectors.toMap;

public class CmdProfile {

    @Command(name="profile", description = "Used to see your profile.", category = "Interaction", usage = "[user]", perms = {BotPerms.BASE})
    public static void profile(GuildMessageReceivedEvent event, String[] args) {
        Member mem = event.getMember();
        if(args.length > 0) {
            String shit = String.join(" ", args);
            List<Member> ata2 = event.getGuild().getMembersByNickname(shit, false);
            if (ata2.size() >= 1) mem = ata2.get(0);
            if (event.getMessage().getMentionedUsers().size() >= 1)
                mem = event.getMessage().getMentionedMembers().get(0);
            if (mem == null)
                event.getChannel().sendMessage("**Error: **Couldn't find a user that matches the arguments.").queue();
        }

        Message message = event.getChannel().sendMessage("Please wait while we profile "+mem.getEffectiveName()+".").complete();
        StringBuilder string = new StringBuilder();
        EconomyUser economyUser = EconomySystemHandler.getUser(event.getGuild().getId(), mem.getId());

        if(RankSystemHandler.isSystemEnabled(event.getGuild().getId())) {
            HashMap entries = RankSystemHandler.getEntries(event.getGuild().getId());
            if(!(entries.entrySet().size() == 0 || !entries.containsKey(mem.getId()))) {
                StringBuilder rank = new StringBuilder();
                HashMap<String, Long> contestants = new HashMap<>();
                for(Object userId : entries.keySet())
                    contestants.put(userId.toString(), (Long) entries.get(userId.toString()));
                LinkedHashMap<String, Long> sorted = contestants
                        .entrySet()
                        .stream()
                        .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));
                long userPoints = (long) entries.get(mem.getId());
                string.append("**User Points:** "+userPoints+"\n");
                long userPosition = (Arrays.asList(sorted.keySet().toArray()).indexOf(mem.getId())+1);
                string.append("**Position:** "+userPosition+"/"+sorted.keySet().size()+"\n");
                if(userPosition > 1) {
                    String aheadId = sorted.keySet().toArray()[(int) userPosition-2].toString();
                    string.append("("+(sorted.get(aheadId)-userPoints)+" points behind <@"+aheadId+">)\n");
                }
                HashMap<String, Long> roles = RankSystemHandler.getRoles(event.getGuild().getId());
                LinkedHashMap<String, Long> sortedRoles = roles
                        .entrySet()
                        .stream()
                        .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));
                String role = "None", lastName = null;
                long lastPoints = 0;
                for(String name : sortedRoles.keySet()) {
                    if(userPoints > sortedRoles.get(name)) {
                        role = "<@&"+name+">";
                        break;
                    }
                    lastName = name;
                    lastPoints = sortedRoles.get(name);
                }
                string.append("**Role:** "+role+"\n");
                if(lastName != null) {
                    string.append("("+(lastPoints-userPoints)+" points left for <@&"+lastName+">)\n");
                }
            }
        }

        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setAuthor(mem.getEffectiveName()+"'s Profile:", null, event.getGuild().getIconUrl())
                .setThumbnail(mem.getUser().getAvatarUrl())
                .addField("Money:", economyUser.getMoney()+" FracassoCoins", true)
                .setColor(mem.getColor())
                .setFooter("Requested by: " + event.getAuthor().getName() + "#" + event.getAuthor().getDiscriminator(), event.getAuthor().getAvatarUrl());

        if(economyUser.getEffectsPair().size() > 0) {
            StringBuilder efeitos = new StringBuilder();
            int idx = 0;
            boolean next = false;
            for (Pair<String, Long> pair : economyUser.getEffectsPair()) {
                idx++;
                EconomyEffect effect = null;
                try{effect = EconomyEffect.valueOf(pair.getKey());}catch (Exception ignored){}
                if(efeitos.length() > 0) efeitos.append(",   ");
                if(next) {
                    efeitos.append("\n");
                    next = false;
                }
                if(idx % 2 == 0) next = true;
                if(effect == null) efeitos.append("**:interrobang: Efeito Desconhecido**");
                else efeitos.append(effect.getItem().getEmote()).append(" (").append((pair.getValue()-System.currentTimeMillis())/60000).append(" Min)");
            }
            embedBuilder.addField("Effects: ", efeitos.toString(), true);
        }

        if (string.length() > 0) embedBuilder.addField("Point system:", string.toString(), false);
        if (economyUser.getInventory().size() > 0) {
            StringBuilder inventory = new StringBuilder();
            for (String id : economyUser.getInventory()) {
                EconomyItem item = null;
                try{item = EconomyItem.valueOf(id);}catch (Exception ignored){}
                if(item == null) item = EconomyItem.UNKNOWN;
                if(inventory.length() > 0) inventory.append(",   ");
                inventory.append("**").append(item.getEmote()).append(" ").append(item.getName()).append("**");
            }
            embedBuilder.addField("Inventory ("+economyUser.getInventory().size()+"/20):", inventory.toString(), false);
        }else{
            embedBuilder.addField("Inventory (0/20):", "Empty", false);
        }

        message.editMessage(embedBuilder.build()).append("Here is your profile:").queue();
    }
}
