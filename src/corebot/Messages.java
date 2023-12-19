package corebot;

import arc.files.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.Nullable;
import arc.util.CommandHandler.*;
import arc.util.io.Streams;
import arc.util.serialization.*;
import arc.util.serialization.Jval.*;
import io.github.cdimascio.dotenv.Dotenv;
import mindustry.*;
import mindustry.game.*;
import mindustry.type.*;
import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.Message.*;
import net.dv8tion.jda.api.events.guild.member.*;
import net.dv8tion.jda.api.events.message.*;
import net.dv8tion.jda.api.events.message.react.*;
import net.dv8tion.jda.api.hooks.*;
import net.dv8tion.jda.api.requests.*;
import net.dv8tion.jda.api.utils.*;
import net.dv8tion.jda.api.utils.cache.*;
import org.jetbrains.annotations.*;

import javax.imageio.*;
import java.awt.image.*;
import java.io.*;
import java.text.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import java.util.stream.*;
import java.util.zip.*;

import static corebot.CoreBot.*;

public class Messages extends ListenerAdapter{
    private static final String prefix = "!";
    private static final int scamAutobanLimit = 3, pingSpamLimit = 20, minModStars = 10, naughtyTimeoutMins = 20;
    private static final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
    private static final String[] warningStrings = {"вперше", "вдруге", "втретє", "надто багато разів"};

    private static final String
    cyrillicFrom = "абсдефгнигклмпоркгзтюушхуз",
    cyrillicTo =   "abcdefghijklmnopqrstuvwxyz";

    // https://stackoverflow.com/a/48769624
    private static final Pattern urlPattern = Pattern.compile("(?:(?:https?):\\/\\/)?[\\w/\\-?=%.]+\\.[\\w/\\-&?=%.]+");
    private static final Set<String> trustedDomains = Set.of(
        "discord.com",
        "discord.co",
        "discord.gg",
        "discord.media",
        "discord.gift",
        "discordapp.com",
        "discordapp.net",
        "discordstatus.com"
    );

    //yes it's base64 encoded, I don't want any of these words typed here
    private static final Pattern badWordPattern = Pattern.compile(new String(Base64Coder.decode("KD88IVthLXpBLVpdKSg/OmN1bXxzZW1lbnxjb2NrfHB1c3N5fGN1bnR8bmlnZy5yfNCx0LvRjy580YHRg9C60LB80LvQvtGFfNC00L4uLi7QudC+0LF80L8u0LQu0YAuLnwu0LHQu9Cw0L0pKD8hW2EtekEtWl0p")));
    private static final Pattern invitePattern = Pattern.compile("(discord\\.gg/\\w|discordapp\\.com/invite/\\w|discord\\.com/invite/\\w)");
    private static final Pattern linkPattern = Pattern.compile("http(s?)://");
    private static final Pattern notScamPattern = Pattern.compile("discord\\.py|discord\\.js|nitrome\\.com");
    private static final Pattern scamPattern = Pattern.compile(String.join("|",
        "stea.*co.*\\.ru",
        "http.*stea.*c.*\\..*trad",
        "csgo.*kni[fv]e",
        "cs.?go.*inventory",
        "cs.?go.*cheat",
        "cheat.*cs.?go",
        "cs.?go.*skins",
        "skins.*cs.?go",
        "stea.*com.*partner",
        "скин.*partner",
        "steamcommutiny",
        "di.*\\.gift.*nitro",
        "http.*disc.*gift.*\\.",
        "free.*nitro.*http",
        "http.*free.*nitro.*",
        "nitro.*free.*http",
        "discord.*nitro.*free",
        "free.*discord.*nitro",
        "@everyone.*http",
        "http.*@everyone",
        "discordgivenitro",
        "http.*gift.*nitro",
        "http.*nitro.*gift",
        "http.*n.*gift",
        "бесплат.*нитро.*http",
        "нитро.*бесплат.*http",
        "nitro.*http.*disc.*nitro",
        "http.*click.*nitro",
        "http.*st.*nitro",
        "http.*nitro",
        "stea.*give.*nitro",
        "discord.*nitro.*steam.*get",
        "gift.*nitro.*http",
        "http.*discord.*gift",
        "discord.*nitro.*http",
        "personalize.*your*profile.*http",
        "nitro.*steam.*http",
        "steam.*nitro.*http",
        "nitro.*http.*d",
        "http.*d.*gift",
        "gift.*http.*d.*s",
        "discord.*steam.*http.*d",
        "nitro.*steam.*http",
        "steam.*nitro.*http",
        "dliscord.com",
        "free.*nitro.*http",
        "discord.*nitro.*http",
        "@everyone.*http",
        "http.*@everyone",
        "@everyone.*nitro",
        "nitro.*@everyone",
        "discord.*gi.*nitro"
    ));

    private final ObjectMap<String, UserData> userData = new ObjectMap<>();
    private final CommandHandler handler = new CommandHandler(prefix);
    private final CommandHandler adminHandler = new CommandHandler(prefix);
    private final JDA jda;

    public Guild guild;
    public TextChannel
    announcementsChannel;
    public TextChannel artChannel;
    public TextChannel mapsChannel;
    public TextChannel moderationChannel;
    public TextChannel schematicsChannel;
    public TextChannel baseSchematicsChannel;
    public static TextChannel logChannel;
    public TextChannel logDeletedMessagesChannel;
    public TextChannel joinChannel;
    public TextChannel testingChannel;
    public TextChannel alertsChannel;
    public TextChannel botsChannel;

    public Role modderRole;

    LongSeq schematicChannels = new LongSeq();
    Dotenv dotenv = Dotenv.load();

    public Messages(){

        String token = dotenv.get("MESSAGE_BOT_TOKEN");

        register();

        try{
            jda = JDABuilder.createDefault(token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_EMOJIS, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MESSAGE_REACTIONS)
                .setMemberCachePolicy(MemberCachePolicy.ALL).disableCache(CacheFlag.VOICE_STATE).build();
            jda.awaitReady();
            jda.addEventListener(this);

            loadChannels();

            Log.info("Hello There!");
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    TextChannel channel(long id){
        return guild.getTextChannelById(id);
    }

    void loadChannels(){
        //all guilds and channels are loaded here for faster lookup
        guild = jda.getGuildById(1182814296690925620L);

        modderRole = guild.getRoleById(1184222717323509890L);

        announcementsChannel = channel(1182819594323366049L);
        artChannel = channel(1182828396351344681L);
        mapsChannel = channel(1182832710562107483L);
        moderationChannel = channel(1182821095582220338L);
        schematicsChannel = channel(1182832750194081802L);
        baseSchematicsChannel = channel(1182832750194081802L);
        logChannel = channel(1184223462307410020L);
        logDeletedMessagesChannel = channel(1186332530945818744L);
        joinChannel = channel(1182814297810812953L);
        testingChannel = channel(1182821095582220338L);
        alertsChannel = channel(1182826185034567741L);
        botsChannel = channel(1182823522372948049L);

        schematicChannels.add(schematicsChannel.getIdLong(), baseSchematicsChannel.getIdLong());
    }

    void printCommands(CommandHandler handler, StringBuilder builder){
        for(Command command : handler.getCommandList()){
            builder.append(prefix);
            builder.append("**");
            builder.append(command.text);
            builder.append("**");
            if(command.params.length > 0){
                builder.append(" *");
                builder.append(command.paramText);
                builder.append("*");
            }
            builder.append(" - ");
            builder.append(command.description);
            builder.append("\n");
        }
    }

    void register(){
        handler.<Message>register("help", "Показує всі команди бота.", (args, msg) -> {
            StringBuilder builder = new StringBuilder();
            printCommands(handler, builder);
            info(msg.getChannel(), "Команди", builder.toString());
        });

        handler.<Message>register("ping", "<ip>", "Про пінгує сервер і видасть деяку інформацію про нього.", (args, msg) -> {
            if(msg.getChannel().getIdLong() != botsChannel.getIdLong()){
                errDelete(msg, "Використовуйте цю команду в " + botsChannel.getAsMention() + ".");
                return;
            }

            net.pingServer(args[0], result -> {
                if(result.name != null){
                    info(msg.getChannel(), "Сервер онлайн", "Хост: @\n" + "Гравці: @\n" + "Карта: @\n" + "Хвиля: @\n" + "Версія: @\n" + "Пінг: @мс",
                    Strings.stripColors(result.name), result.players, Strings.stripColors(result.mapname), result.wave, result.version, result.ping);
                }else{
                    errDelete(msg, "Сервер офлайн", "Час вичерпано.");
                }
            });
        });

        handler.<Message>register("info", "<links/beta/rules>", "Показує інформацію на певну тему.", (args, msg) -> {
            try{
                Info info = Info.valueOf(args[0]);
                infoDesc(msg.getChannel(), info.title, info.text);
            }catch(IllegalArgumentException e){
                errDelete(msg, "О ні!", "Недопустима тема '@'. " + "\n Допустимі теми: *@*", args[0], Arrays.toString(Info.values()));
            }
        });

        handler.<Message>register("postmap", "Опублікуйте файл .msav на каналі #мапи.", (args, msg) -> {

            if(msg.getAttachments().size() != 1 || !msg.getAttachments().get(0).getFileName().endsWith(".msav")){
                errDelete(msg, "Ви повинні мати .msav файл у тому самому повідомленні, що й команда!");
                return;
            }

            Attachment a = msg.getAttachments().get(0);

            try{
                ContentHandler.Map map = contentHandler.readMap(net.download(a.getUrl()));
                new File("cache/").mkdir();
                File mapFile = new File("cache/" + a.getFileName());
                Fi imageFile = Fi.get("cache/image_" + a.getFileName().replace(".msav", ".png"));
                Streams.copy(net.download(a.getUrl()), new FileOutputStream(mapFile));
                ImageIO.write(map.image, "png", imageFile.file());

                EmbedBuilder builder = new EmbedBuilder().setColor(normalColor).setColor(normalColor)
                .setImage("attachment://" + imageFile.name())
                .setAuthor(msg.getAuthor().getName(), msg.getAuthor().getEffectiveAvatarUrl(), msg.getAuthor().getEffectiveAvatarUrl())
                .setTitle(map.name == null ? a.getFileName().replace(".msav", "") : map.name);

                if(map.description != null) builder.setFooter(map.description);

                mapsChannel.sendFile(mapFile).addFile(imageFile.file()).setEmbeds(builder.build()).queue();

                text(msg, "*Мапу успішно опубліковано.*");
            }catch(Exception e){
                String err = Strings.neatError(e, true);
                int max = 900;
                errDelete(msg, "Помилка розбору мапи.", err.length() < max ? err : err.substring(0, max));
            }
        });

        handler.<Message>register("verifymodder", "[user/repo]", "Підтвердіть, що ви є розробником модифікацій, показавши ваш репозиторій з модифікацією. Викликайте команду без аргументів для отримання вимог.", (args, msg) -> {
            if(msg.getChannel().getIdLong() != botsChannel.getIdLong()){
                errDelete(msg, "Використовуйте цю команду в " + botsChannel.getAsMention() + ".");
                return;
            }

            if(msg.getMember() == null){
                errDelete(msg, "Ніяких привидів не допускається.");
                return;
            }

            String rawSearchString = (msg.getAuthor().getName() + "#" + msg.getAuthor().getDiscriminator());

            if(args.length == 0){
                info(msg.getChannel(), "Верифікація розробника модифікацій", """
                Щоб отримати роль *розробника модифікацій*, вам потрібно зробити наступне:
                
                1. Мати власний репозиторій на Github з тегом `mindustry-mod`.
                2. Мати принаймні @ зірок на репозиторії вашої модифікації.
                3. Тимчасово додайте ваш Discord `USERNAME#DISCRIMINATOR` (`@`) до опису репозиторію або біографії користувача, щоб підтвердити право власності.
                4. Запустіть цю команду з URL-адресою репозиторію або `Username/Repo` як аргументом.
                """, minModStars, rawSearchString);
            }else{
                if(msg.getMember().getRoles().stream().anyMatch(r -> r.equals(modderRole))){
                    errDelete(msg, "Ви вже маєте цю роль.");
                    return;
                }

                String repo = args[0];
                int offset = "https://github.com/".length();
                if(repo.startsWith("https://") && repo.length() > offset + 1){
                    repo = repo.substring(offset);
                }

                Http.get("https://api.github.com/repos/" + repo)
                .header("Accept", "application/vnd.github.v3+json")
                .error(err -> errDelete(msg, "Помилка при завантаженні репозиторію (Чи правильно ви ввели назву?)", Strings.getSimpleMessage(err)))
                .block(res -> {
                    Jval val = Jval.read(res.getResultAsString());
                    String searchString = rawSearchString.toLowerCase(Locale.ROOT);

                    boolean contains = val.getString("description").toLowerCase(Locale.ROOT).contains(searchString);
                    boolean[] actualContains = {contains};

                    //check bio if not found
                    if(!contains){
                        Http.get(val.get("owner").getString("url"))
                        .error(Log::err) //why would this ever happen
                        .block(user -> {
                            Jval userVal = Jval.read(user.getResultAsString());
                            if(userVal.getString("bio", "").toLowerCase(Locale.ROOT).contains(searchString)){
                                actualContains[0] = true;
                            }
                        });
                    }

                    if(!val.get("topics").asArray().contains(j -> j.asString().contains("mindustry-mod"))){
                        errDelete(msg, "Не вдалося знайти `mindustry-mod` у списку тегів репозиторію.\n" + "Додайте його до тегів *(теги можна відредагувати поруч з розділом \"Про репозиторій\")*.");
                        return;
                    }

                    if(!actualContains[0]){
                        errDelete(msg, "Не вдалося знайти ваше ім'я користувача Discord + дискримінатор в описі репозиторію або біографії власника.\n\nПереконайтеся, що `" + rawSearchString + "` записано в одному з цих місць.");
                        return;
                    }

                    if(val.getInt("stargazers_count", 0) < minModStars){
                        errDelete(msg, "Щоб отримати роль Розробник модифікацій, вам потрібно щонайменше " + minModStars + " зірок на вашому репозиторії.");
                        return;
                    }

                    guild.addRoleToMember(msg.getMember(), modderRole).queue();

                    info(msg.getChannel(), "Вітаємо!", "Ви отримали роль розробника модифікацій!");
                });
            }
        });

        handler.<Message>register("google", "<питання...>", "Дозвольте мені погуглити за вас.", (args, msg) -> {
            text(msg, "https://lmgt.org/?q=@", Strings.encode(args[0]));
        });

        handler.<Message>register("cleanmod", "Очищення zip-архіву модифікації. Змінює json на hjson та форматує код.", (args, msg) -> {

            if(msg.getAttachments().size() != 1 || !msg.getAttachments().get(0).getFileName().endsWith(".zip")){
                errDelete(msg, "Ви повинні мати один .zip файл у тому ж повідомленні, що і команда!");
                return;
            }

            Attachment a = msg.getAttachments().get(0);

            if(a.getSize() > 1024 * 1024 * 6){
                errDelete(msg, "Zip-файли не повинні перевищувати 6 МБ.");
            }

            try{
                new File("cache/").mkdir();
                File baseFile = new File("cache/" + a.getFileName());
                Fi destFolder = new Fi("cache/dest_mod" + a.getFileName());
                Fi destFile = new Fi("cache/" + new Fi(baseFile).nameWithoutExtension() + "-cleaned.zip");

                if(destFolder.exists()) destFolder.deleteDirectory();
                if(destFile.exists()) destFile.delete();

                Streams.copy(net.download(a.getUrl()), new FileOutputStream(baseFile));
                ZipFi zip = new ZipFi(new Fi(baseFile.getPath()));
                zip.walk(file -> {
                    Fi output = destFolder.child(file.extension().equals("json") ? file.pathWithoutExtension() + ".hjson" : file.path());
                    output.parent().mkdirs();

                    if(file.extension().equals("json") || file.extension().equals("hjson")){
                        output.writeString(fixJval(Jval.read(file.readString())).toString(Jformat.hjson));
                    }else{
                        file.copyTo(output);
                    }
                });

                try(OutputStream fos = destFile.write(false, 2048); ZipOutputStream zos = new ZipOutputStream(fos)){
                    for(Fi add : destFolder.findAll(f -> true)){
                        if(add.isDirectory()) continue;
                        zos.putNextEntry(new ZipEntry(add.path().substring(destFolder.path().length())));
                        Streams.copy(add.read(), zos);
                        zos.closeEntry();
                    }

                }

                msg.getChannel().sendFile(destFile.file()).queue();

                text(msg, "*Мод успішно конвертовано.*");
            }catch(Throwable e){
                errDelete(msg, "Помилка розбору модифікації.", Strings.neatError(e, false));
            }
        });

        handler.<Message>register("file", "<назва...>", "Знайти вихідний файл Mindustry за назвою.", (args, msg) -> {
            //epic asynchronous code, I know
            Http.get("https://api.github.com/search/code?q=" +
            "filename:" + Strings.encode(args[0]) + "%20" +
            "repo:Anuken/Mindustry")
            .header("Accept", "application/vnd.github.v3+json")
            .header("Authorization", "token " + dotenv.get("GITHUB_SEARCH_TOKEN"))
            .error(err -> errDelete(msg, "Помилка запиту до Github", Strings.getSimpleMessage(err)))
            .block(result -> {
                msg.delete().queue();
                Jval val = Jval.read(result.getResultAsString());

                //merge with arc results
                Http.get("https://api.github.com/search/code?q=" +
                "filename:" + Strings.encode(args[0]) + "%20" +
                "repo:Anuken/Arc")
                .header("Accept", "application/vnd.github.v3+json")
                .header("Authorization", "token " + dotenv.get("GITHUB_SEARCH_TOKEN"))
                .block(arcResult -> {
                    Jval arcVal = Jval.read(arcResult.getResultAsString());

                    val.get("items").asArray().addAll(arcVal.get("items").asArray());
                    val.put("total_count", val.getInt("total_count", 0) + arcVal.getInt("total_count", 0));
                });

                int count = val.getInt("total_count", 0);

                if(count > 0){
                    val.get("items").asArray().removeAll(j -> !j.getString("name").contains(args[0]));
                    count = val.get("items").asArray().size;
                }

                if(count == 0){
                    errDelete(msg, "Нічого не знайдено.");
                    return;
                }

                EmbedBuilder embed = new EmbedBuilder();
                embed.setColor(normalColor);
                embed.setAuthor(msg.getAuthor().getName() + ": Результати пошуку на Github", val.get("items").asArray().first().getString("html_url"), "https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png");
                embed.setTitle("Результати пошуку на Github");

                if(count == 1){
                    Jval item = val.get("items").asArray().first();
                    embed.setTitle(item.getString("name"));
                    embed.setDescription("[Переглянути на Github](" + item.getString("html_url") + ")");
                }else{
                    int maxResult = 5, i = 0;
                    StringBuilder results = new StringBuilder();
                    for(Jval item : val.get("items").asArray()){
                        if(i++ > maxResult){
                            break;
                        }
                        results.append("[").append(item.getString("name")).append("]").append("(").append(item.getString("html_url")).append(")\n");
                    }

                    embed.setTitle((count > maxResult ? maxResult + "+" : count) + " Вихідних результати(ів)");
                    embed.setDescription(results.toString());
                }

                msg.getChannel().sendMessageEmbeds(embed.build()).queue();
            });
        });


        handler.<Message>register("mywarnings", "Отримати інформацію про власні попередження. Доступно тільки в #команди.", (args, msg) -> {
            if(msg.getChannel().getIdLong() != botsChannel.getIdLong()){
                errDelete(msg, "Використовуйте цю команду в " + botsChannel.getAsMention() + ".");
                return;
            }

            sendWarnings(msg, msg.getAuthor());
        });

        handler.<Message>register("avatar", "[@user]", "Показує повний аватар користувача.", (args, msg) -> {
            if (msg.getChannel().getIdLong() != botsChannel.getIdLong() && !isAdmin(msg.getAuthor())) {
                errDelete(msg, "Використовуйте цю команду в " + botsChannel.getAsMention() + ".");
                return;
            }

            try {
                User user;
                if (args.length > 0) {
                    long id;
                    try {
                        id = Long.parseLong(args[0]);
                    } catch (NumberFormatException e) {
                        String author = args[0].substring(2, args[0].length() - 1);
                        if (author.startsWith("!")) author = author.substring(1);
                        id = Long.parseLong(author);
                    }

                    user = jda.retrieveUserById(id).complete();
                } else {
                    user = msg.getAuthor();
                }

                String link = user.getEffectiveAvatarUrl() + "?size=1024";

                EmbedBuilder embed = new EmbedBuilder();
                embed.setColor(normalColor);
                embed.setTitle("Аватар: " + user.getName() + "#" + user.getDiscriminator());
                embed.setImage(link);
                embed.setDescription("[Посилання](" + link + ")");
                embed.setFooter("Надано за запитом " + msg.getAuthor().getName() + "#" + msg.getAuthor().getDiscriminator());
                msg.getChannel().sendMessageEmbeds(embed.build()).queue();

            } catch (Exception e) {
                errDelete(msg, "Некоректе ім'я або ID");
            }
        });

        adminHandler.<Message>register("adminhelp", "Відображає всі команди бота.", (args, msg) -> {
            StringBuilder builder = new StringBuilder();
            printCommands(adminHandler, builder);
            info(msg.getChannel(), "Команди адміністратора", builder.toString());
        });

        adminHandler.<Message>register("userinfo", "<@user>", "Отримати інформацію про користувача.", (args, msg) -> {
            String author = args[0].substring(2, args[0].length() - 1);
            if(author.startsWith("!")) author = author.substring(1);
            try{
                long l = Long.parseLong(author);
                User user = jda.retrieveUserById(l).complete();

                if(user == null){
                    errDelete(msg, "Цього користувача (ID @) немає в кеші. Як це могло статися?", l);
                }else{
                    Member member = guild.retrieveMember(user).complete();

                    info(msg.getChannel(), "Інформація про " + member.getEffectiveName(),
                        "Відображуване ім'я: @\nІм'я користувача: @\nID: @\nСтатус: @\nРолі: @\nАдмін: @\nЧас приєднання: @",
                        member.getNickname(),
                        user.getName(),
                        member.getIdLong(),
                        member.getOnlineStatus(),
                        member.getRoles().stream().map(Role::getName).collect(Collectors.toList()),
                        isAdmin(user),
                        member.getTimeJoined()
                    );
                }
            }catch(Exception e){
                errDelete(msg, "Неправильний формат імені або користувач відсутній.");
            }
        });

        adminHandler.<Message>register("warnings", "<@user>", "Отримати кількість попереджень, які має користувач.", (args, msg) -> {
            String author = args[0].substring(2, args[0].length() - 1);
            if(author.startsWith("!")) author = author.substring(1);
            try{
                long l = Long.parseLong(author);
                User user = jda.retrieveUserById(l).complete();
                sendWarnings(msg, user);
            }catch(Exception e){
                errDelete(msg, "Неправильний формат імені.");
            }
        });

        adminHandler.<Message>register("testemoji", "<ID>", "Надіслати емодзі за ID.", (args, msg) -> {
            Emote emoji = null;

            try{
                emoji = guild.getEmoteById(args[0]);
            }catch(Exception ignored){
            }

            if(emoji == null){
                var emotes = guild.getEmotesByName(args[0], true);
                if(emotes.size() > 0){
                    emoji = emotes.get(0);
                }
            }

            if(emoji == null){
                errDelete(msg, "Емодзі не знайдено.");
            }else{
                msg.delete().queue();
                text(msg.getChannel(), emoji.getAsMention());
            }
        });

        adminHandler.<Message>register("delete", "<кількість>", "Видалити кілька повідомлень.", (args, msg) -> {
            try{
                int number = Integer.parseInt(args[0]);
                MessageHistory hist = msg.getChannel().getHistoryBefore(msg, number).complete();
                msg.delete().queue();
                msg.getTextChannel().deleteMessages(hist.getRetrievedHistory()).queue();
            }catch(NumberFormatException e){
                errDelete(msg, "Недійсний номер.");
            }
        });

        adminHandler.<Message>register("warn", "<@user> [причина...]", "Видати попередження користувачеві.", (args, msg) -> {
            String author = args[0].substring(2, args[0].length() - 1);
            if(author.startsWith("!")) author = author.substring(1);
            try{
                long l = Long.parseLong(author);
                User user = jda.retrieveUserById(l).complete();
                var list = getWarnings(user);
                list.add(System.currentTimeMillis() + ":::" + msg.getAuthor().getName() + (args.length > 1 ? ":::" + args[1] : ""));
                text(msg, "**@**, вас попереджено *@*.", user.getAsMention(), warningStrings[Mathf.clamp(list.size - 1, 0, warningStrings.length - 1)]);
                prefs.putArray("warning-list-" + user.getIdLong(), list);
                if(list.size >= 3){
                    moderationChannel.sendMessage("Користувача " + user.getAsMention() + " було попереджено 3 або більше разів!").queue();
                }
            }catch(Exception e){
                errDelete(msg, "Неправильний формат імені.");
            }
        });

        adminHandler.<Message>register("unwarn", "<@user> <індекс>", "Зняти попередження.", (args, msg) -> {
            String author = args[0].substring(2, args[0].length() - 1);
            if(author.startsWith("!")) author = author.substring(1);
            try{
                int index = Math.max(Integer.parseInt(args[1]), 1);
                long l = Long.parseLong(author);
                User user = jda.retrieveUserById(l).complete();
                var list = getWarnings(user);
                if(list.size > index - 1){
                    list.remove(index - 1);
                    prefs.putArray("warning-list-" + user.getIdLong(), list);
                    text(msg, "Видалено попередження для користувача.");
                }else{
                    errDelete(msg, "Неправильний індекс. @ > @", index, list.size);
                }
            }catch(Exception e){
                errDelete(msg, "Неправильний формат імені/індексу.");
            }
        });

        adminHandler.<Message>register("clearwarnings", "<@user>", "Позбавити користувача усіх попереджень", (args, msg) -> {
            String author = args[0].substring(2, args[0].length() - 1);
            if(author.startsWith("!")) author = author.substring(1);
            try{
                long l = Long.parseLong(author);
                User user = jda.retrieveUserById(l).complete();
                prefs.putArray("warning-list-" + user.getIdLong(), new Seq<>());
                text(msg, "Очищено всі попередження для користувача '@'.", user.getName());
            }catch(Exception e){
                errDelete(msg, "Неправильний формат імені.");
            }
        });

        adminHandler.<Message>register("banid", "<id> [причина...]", "Заблокувати користувача за ID.", (args, msg) -> {
            try{
                long l = Long.parseLong(args[0]);
                User user = jda.retrieveUserById(l).complete();

                guild.ban(user, 0, args.length > 1 ? msg.getAuthor().getName() + " використаний banid: " + args[1] : msg.getAuthor().getName() + ": <причину бану не вказано>").queue();
                text(msg, "Заблоковано користувача: **@**", l);
            }catch(Exception e){
                errDelete(msg, "Неправильний формат імені або користувач відсутній.");
            }
        });
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event){
        try{
            if(event.getUser() != null && event.getChannel().equals(mapsChannel) && event.getReactionEmote().isEmoji() && event.getReactionEmote().getEmoji().equals("❌")){
                event.getChannel().retrieveMessageById(event.getMessageIdLong()).queue(m -> {
                    try{
                        String baseUrl = event.retrieveUser().complete().getEffectiveAvatarUrl();

                        for(var embed : m.getEmbeds()){
                            if(embed.getAuthor() != null && embed.getAuthor().getIconUrl() != null && embed.getAuthor().getIconUrl().equals(baseUrl)){
                                m.delete().queue();
                                return;
                            }
                        }
                    }catch(Exception e){
                        Log.err(e);
                    }
                });
            }
        }catch(Exception e){
            Log.err(e);
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event){
        try{

            var msg = event.getMessage();

            if(msg.getAuthor().isBot() || msg.getChannel().getType() != ChannelType.TEXT) return;

            EmbedBuilder log = new EmbedBuilder()
            .setAuthor(msg.getAuthor().getName(), msg.getAuthor().getEffectiveAvatarUrl(), msg.getAuthor().getEffectiveAvatarUrl())
            .setDescription(msg.getContentRaw().length() >= 2040 ? msg.getContentRaw().substring(0, 2040) + "..." : msg.getContentRaw())
            .addField("Автор", msg.getAuthor().getAsMention(), false)
            .addField("Канал", msg.getTextChannel().getAsMention(), false)
            .setColor(normalColor);

            for(var attach : msg.getAttachments()){
                log.addField("Файл: ", attach.getUrl(), false);
            }

            if(msg.getReferencedMessage() != null){
                log.addField("У відповідь на", msg.getReferencedMessage().getAuthor().getAsMention() + " [Перейти](" + msg.getReferencedMessage().getJumpUrl() + ")", false);
            }

            if(msg.getMentionedUsers().stream().anyMatch(u -> u.getIdLong() == 123539225919488000L)){
                log.addField("Примітка", "thisisamention", false);
            }

            if(msg.getChannel().getIdLong() != testingChannel.getIdLong()){
                logChannel.sendMessageEmbeds(log.build()).queue();
            }

            //delete stray invites
            if(!isAdmin(msg.getAuthor()) && checkSpam(msg, false)){
                return;
            }

            //delete non-art
            if(!isAdmin(msg.getAuthor()) && msg.getChannel().getIdLong() == artChannel.getIdLong() && msg.getAttachments().isEmpty()){
                msg.delete().queue();

                if(msg.getType() != MessageType.CHANNEL_PINNED_ADD){
                    try{
                        msg.getAuthor().openPrivateChannel().complete().sendMessage("Не надсилайте повідомлення без зображень у цьому каналі.").queue();
                    }catch(Exception e1){
                        e1.printStackTrace();
                    }
                }
            }

            String text = msg.getContentRaw();

            //schematic preview
            if((msg.getContentRaw().startsWith(ContentHandler.schemHeader) && msg.getAttachments().isEmpty()) ||
            (msg.getAttachments().size() == 1 && msg.getAttachments().get(0).getFileExtension() != null && msg.getAttachments().get(0).getFileExtension().equals(Vars.schematicExtension))){
                try{
                    Schematic schem = msg.getAttachments().size() == 1 ? contentHandler.parseSchematicURL(msg.getAttachments().get(0).getUrl()) : contentHandler.parseSchematic(msg.getContentRaw());
                    BufferedImage preview = contentHandler.previewSchematic(schem);
                    String sname = schem.name().replace("/", "_").replace(" ", "_");
                    if(sname.isEmpty()) sname = "empty";

                    new File("cache").mkdir();
                    File previewFile = new File("cache/img_" + UUID.randomUUID() + ".png");
                    File schemFile = new File("cache/" + sname + "." + Vars.schematicExtension);
                    Schematics.write(schem, new Fi(schemFile));
                    ImageIO.write(preview, "png", previewFile);

                    EmbedBuilder builder = new EmbedBuilder().setColor(normalColor).setColor(normalColor)
                    .setImage("attachment://" + previewFile.getName())
                    .setAuthor(msg.getAuthor().getName(), msg.getAuthor().getEffectiveAvatarUrl(), msg.getAuthor().getEffectiveAvatarUrl()).setTitle(schem.name());

                    if(!schem.description().isEmpty()) builder.setFooter(schem.description());

                    StringBuilder field = new StringBuilder();

                    for(ItemStack stack : schem.requirements()){
                        List<Emote> emotes = guild.getEmotesByName(stack.item.name.replace("-", ""), true);
                        Emote result = emotes.isEmpty() ? guild.getEmotesByName("ohno", true).get(0) : emotes.get(0);

                        field.append(result.getAsMention()).append(stack.amount).append("  ");
                    }
                    builder.addField("Вимоги", field.toString(), false);

                    msg.getChannel().sendFile(schemFile).addFile(previewFile).setEmbeds(builder.build()).queue();
                    msg.delete().queue();
                }catch(Throwable e){
                    if(schematicChannels.contains(msg.getChannel().getIdLong())){
                        msg.delete().queue();
                        try{
                            msg.getAuthor().openPrivateChannel().complete().sendMessage("Недійсна схема: " + e.getClass().getSimpleName() + (e.getMessage() == null ? "" : " (" + e.getMessage() + ")")).queue();
                            e.printStackTrace();
                        }catch(Exception e2){
                            e2.printStackTrace();
                        }
                    }
                    //ignore errors
                }
            }else if(schematicChannels.contains(msg.getChannel().getIdLong()) && !isAdmin(msg.getAuthor())){
                //delete non-schematics
                msg.delete().queue();
                try{
                    msg.getAuthor().openPrivateChannel().complete().sendMessage("Надсилайте лише коректні схеми у #схеми. Ви можете надсилати їх у вигляді тексту з буфера обміну або у вигляді файлу.").queue();
                }catch(Exception e){
                    e.printStackTrace();
                }
                return;
            }

            if(!text.replace(prefix, "").trim().isEmpty()){
                if(isAdmin(msg.getAuthor())){
                    boolean unknown = handleResponse(msg, adminHandler.handleMessage(text, msg), false);

                    handleResponse(msg, handler.handleMessage(text, msg), !unknown);
                }else{
                    handleResponse(msg, handler.handleMessage(text, msg), true);
                }
            }
        }catch(Exception e){
            Log.err(e);
        }
    }

    @Override
    public void onMessageUpdate(MessageUpdateEvent event){
        var msg = event.getMessage();

        if(isAdmin(msg.getAuthor()) || checkSpam(msg, true)){
            return;
        }

        if((msg.getChannel().getIdLong() == artChannel.getIdLong()) && msg.getAttachments().isEmpty()){
            msg.delete().queue();
            try{
                msg.getAuthor().openPrivateChannel().complete().sendMessage("Не надсилайте повідомлень без зображень у цьому каналі.").queue();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event){
        event.getUser().openPrivateChannel().complete().sendMessage(
        """
        **Ласкаво просимо до україномовної спільноти по Mindustry.**
                
        *Переконайтеся, що ви прочитали <#1182817614976794699> та теми каналу перед тим, як писати!:з*
        """
        ).queue();

        joinChannel
        .sendMessageEmbeds(new EmbedBuilder()
            .setAuthor(event.getUser().getName(), event.getUser().getAvatarUrl(), event.getUser().getAvatarUrl())
            .addField("Користувач", event.getUser().getAsMention(), false)
            .addField("ID", "`" + event.getUser().getId() + "`", false)
            .setColor(normalColor).build())
        .queue();
    }

    void sendWarnings(Message msg, User user){
        var list = getWarnings(user);
        text(msg, "Користувач '@' має **@** @.\n@", user.getName(), list.size, list.size == 1 ? "попередження" : "попереджень",
        list.map(s -> {
            String[] split = s.split(":::");
            long time = Long.parseLong(split[0]);
            String warner = split.length > 1 ? split[1] : null, reason = split.length > 2 ? split[2] : null;
            return "`" + fmt.format(new Date(time)) + "`: Термін дії закінчується через " + (warnExpireDays - Duration.ofMillis((System.currentTimeMillis() - time)).toDays()) + " дні(в)" +
            (warner == null ? "" : "\n  ↳ *Попередження від:* " + warner) +
            (reason == null ? "" : "\n  ↳ *Причина:* " + reason);
        }).toString("\n"));
    }

    public void text(MessageChannel channel, String text, Object... args){
        channel.sendMessage(Strings.format(text, args)).queue();
    }

    public void text(Message message, String text, Object... args){
        text(message.getChannel(), text, args);
    }

    public void info(MessageChannel channel, String title, String text, Object... args){
        channel.sendMessageEmbeds(new EmbedBuilder().addField(title, Strings.format(text, args), true).setColor(normalColor).build()).queue();
    }

    public void infoDesc(MessageChannel channel, String title, String text, Object... args){
        channel.sendMessageEmbeds(new EmbedBuilder().setTitle(title).setDescription(Strings.format(text, args)).setColor(normalColor).build()).queue();
    }

    /** Sends an error, deleting the base message and the error message after a delay. */
    public void errDelete(Message message, String text, Object... args){
        errDelete(message, "О ні!", text, args);
    }

    /** Sends an error, deleting the base message and the error message after a delay. */
    public void errDelete(Message message, String title, String text, Object... args){
        message.getChannel().sendMessageEmbeds(new EmbedBuilder()
        .addField(title, Strings.format(text, args), true).setColor(errorColor).build())
        .queue(result -> result.delete().queueAfter(messageDeleteTime, TimeUnit.MILLISECONDS));

        //delete base message too
        message.delete().queueAfter(messageDeleteTime, TimeUnit.MILLISECONDS);
    }

    private Seq<String> getWarnings(User user){
        var list = prefs.getArray("warning-list-" + user.getIdLong());
        //remove invalid warnings
        list.removeAll(s -> {
            String[] split = s.split(":::");
            return Duration.ofMillis((System.currentTimeMillis() - Long.parseLong(split[0]))).toDays() >= warnExpireDays;
        });

        return list;
    }

    private Jval fixJval(Jval val){
        if(val.isArray()){
            Seq<Jval> list = val.asArray().copy();
            for(Jval child : list){
                if(child.isObject() && (child.has("item")) && child.has("amount")){
                    val.asArray().remove(child);
                    val.asArray().add(Jval.valueOf(child.getString("item", child.getString("liquid", "")) + "/" + child.getInt("amount", 0)));
                }else{
                    fixJval(child);
                }
            }
        }else if(val.isObject()){
            Seq<String> keys = val.asObject().keys().toArray();

            for(String key : keys){
                Jval child = val.get(key);
                if(child.isObject() && (child.has("item")) && child.has("amount")){
                    val.remove(key);
                    val.add(key, Jval.valueOf(child.getString("item", child.getString("liquid", "")) + "/" + child.getInt("amount", 0)));
                }else{
                    fixJval(child);
                }
            }
        }

        return val;
    }

    boolean isAdmin(User user){
        var member = guild.retrieveMember(user).complete();
        return member != null && member.getRoles().stream().anyMatch(role -> role.getName().equals("Модератор"));
    }

    String replaceCyrillic(String in){
        StringBuilder out = new StringBuilder(in.length());
        for(int i = 0; i < in.length(); i++){
            char c = in.charAt(i);
            int index = cyrillicFrom.indexOf(c);
            if(index == -1){
                out.append(c);
            }else{
                out.append(cyrillicTo.charAt(index));
            }
        }
        return out.toString();
    }

    boolean checkSpam(Message message, boolean edit){

        if(message.getChannel().getType() != ChannelType.PRIVATE){
            Seq<String> mentioned =
                //ignore reply messages, bots don't use those
                message.getReferencedMessage() != null ? new Seq<>() :
                //get all mentioned members and roles in one list
                Seq.with(message.getMentionedMembers()).map(IMentionable::getAsMention).add(Seq.with(message.getMentionedRoles()).map(IMentionable::getAsMention));

            var data = data(message.getAuthor());
            String content = message.getContentStripped().toLowerCase(Locale.ROOT);

            //go through every ping individually
            for(var ping : mentioned){
                if(data.idsPinged.add(ping) && data.idsPinged.size >= pingSpamLimit){
                    String banMessage = "Заблоковано за спам згадуваннями користувачів. Якщо ви вважаєте, що це сталося помилково, створіть скаргу на [Github](https://github.com/UkrainianMindustry/Message/issues) або зверніться до модераторів.";
                    Log.info("Автобан @ за спам @ пінгів поспіль.", message.getAuthor().getName() + "#" + message.getAuthor().getId(), data.idsPinged.size);
                    alertsChannel.sendMessage(message.getAuthor().getAsMention() + " **було автоматично заблоковано за згадування " + pingSpamLimit + " користувачів поспіль!**").queue();

                    Runnable banMember = () -> message.getGuild().ban(message.getAuthor(), 1, banMessage).queue();

                    try{
                        message.getAuthor().openPrivateChannel().complete().sendMessage(banMessage).queue(done -> banMember.run(), failed -> banMember.run());
                    }catch(Exception e){
                        //can fail to open PM channel sometimes.
                        banMember.run();
                    }
                }
            }

            if(mentioned.isEmpty()){
                data.idsPinged.clear();
            }

            //check for consecutive links
            if(!edit && linkPattern.matcher(content).find()){

                if(content.equals(data.lastLinkMessage) && !message.getChannel().getId().equals(data.lastLinkChannelId)){
                    Log.warn("Користувач @ щойно відправив посилання в @ (повідомлення: @): '@'", message.getAuthor().getName(), message.getChannel().getName(), message.getId(), content);

                    //only start deleting after 2 posts
                    if(data.linkCrossposts >= 1){
                        alertsChannel.sendMessage(
                            message.getAuthor().getAsMention() +
                            " **спамить посиланнями** в " + message.getTextChannel().getAsMention() +
                            ":\n\n" + message.getContentRaw()
                        ).queue();

                        message.delete().queue();
                        message.getAuthor().openPrivateChannel().complete().sendMessage("Ви опублікували посилання кілька разів. Не надсилайте більше подібних повідомлень, інакше **вас буде автоматично заблоковано.**.").queue();
                    }

                    //4 posts = ban
                    if(data.linkCrossposts ++ >= 3){
                        Log.warn("Користувача @ (@) було автоматично заблоковано за спам повідомленнями з посиланнями.", message.getAuthor().getName(), message.getAuthor().getAsMention());

                        alertsChannel.sendMessage(message.getAuthor().getAsMention() + " **було автоматично заблоковано за спам посиланнями!**").queue();
                        message.getGuild().ban(message.getAuthor(), 1, "[Автоматичне блокування] Спам посиланнями. Якщо ви не є ботом або спамером, будь ласка, повідомте про це модерації або, на https://github.com/UkrainianMindustry/Message/issues негайно!").queue();
                    }
                }

                data.lastLinkMessage = content;
                data.lastLinkChannelId = message.getChannel().getId();
            }else{
                data.linkCrossposts = 0;
                data.lastLinkMessage = null;
                data.lastLinkChannelId = null;
            }

            //zwj
            content = content.replaceAll("\u200B", "").replaceAll("\u200D", "");

            if(invitePattern.matcher(content).find()){
                Log.warn("Користувач @ щойно надіслав запрошення на discord в @.", message.getAuthor().getName(), message.getChannel().getName());
                message.delete().queue();
                message.getAuthor().openPrivateChannel().complete().sendMessage("Не надсилайте запрошення на сервер! Не порушуйте правила.").queue();
                return true;
            /*}else if((badWordPattern.matcher(content).find() || badWordPattern.matcher(replaceCyrillic(content)).find())){
                alertsChannel.sendMessage(
                    "`" + message.getAuthor().getName() + "`" +
                    " **надіслав повідомлення з неналежним лексиконом** у " + message.getTextChannel().getAsMention() +
                    ":\n\n" + message.getContentRaw()
                ).queue();*/

                message.delete().queue();
                message.getAuthor().openPrivateChannel().complete().sendMessage("вас тимчасово заблоковано на " + naughtyTimeoutMins +
                    " хвилин за використання неналежного лексикону в `#" + message.getChannel().getName() + "`.\nТвоє повідомлення:\n\n" + message.getContentRaw()).queue();
                message.getMember().timeoutFor(Duration.ofMinutes(naughtyTimeoutMins)).queue();

                return true;
            }else if(containsScamLink(message)){
                Log.warn("Користувач @ щойно надіслав потенційно шахрайське повідомлення в @: '@'", message.getAuthor().getName(), message.getChannel().getName(), message.getContentRaw());

                int count = data.scamMessages ++;

                alertsChannel.sendMessage(
                    "`" + message.getAuthor().getName() + "`" +
                    " **надіслав потенційно шахрайське повідомлення** в " + message.getTextChannel().getAsMention() +
                    ":\n\n" + message.getContentRaw()
                ).queue();

                message.delete().queue();
                message.getAuthor().openPrivateChannel().complete().sendMessage("Ваше повідомлення було позначено як потенційне шахрайство. Не надсилайте більше подібних повідомлень, інакше **вас буде автоматично заблоковано.**").queue();

                if(count >= scamAutobanLimit - 1){
                    Log.warn("Користувач @ (@) був автоматично заблокований після @ шахрайських повідомлень.", message.getAuthor().getName(), message.getAuthor().getAsMention(), count + 1);

                    alertsChannel.sendMessage(message.getAuthor().getAsMention() + " **було автоматично заблоковано через публікацію " + scamAutobanLimit + " шахрайських повідомлень поспіль!**").queue();
                    message.getGuild().ban(message.getAuthor(), 0, "[Автоблокування] Публікація декількох потенційно шахрайських повідомлень поспіль. Якщо ви не бот і не спамер, повідомте про це адміністрацію, або на https://github.com/UkrainianMindustry/Message/issues негайно!").queue();
                }

                return true;
            }else{
                //non-consecutive scam messages don't count
                data.scamMessages = 0;
            }

        }
        return false;
    }

    boolean handleResponse(Message msg, CommandResponse response, boolean logUnknown){
        if(response.type == ResponseType.unknownCommand){
            if(logUnknown){
                errDelete(msg, "О ні!", "Невідома команда. Введіть !help щоб отримати список команд.");
            }
            return false;
        }else if(response.type == ResponseType.manyArguments || response.type == ResponseType.fewArguments){
            if(response.command.params.length == 0){
                errDelete(msg, "Неприпустимі аргументи.", "Використання: @@", prefix, response.command.text);
            }else{
                errDelete(msg, "Неприпустимі аргументи.", "Використання: @@ *@*", prefix, response.command.text, response.command.paramText);
            }
        }
        return true;
    }

    boolean containsScamLink(Message message){
        String content = message.getContentRaw().toLowerCase(Locale.ROOT);

        //some discord-related keywords are never scams (at least, not from bots)
        if(notScamPattern.matcher(content).find()){
            return false;
        }

        // Regular check
        if(scamPattern.matcher(content.replace("\n", " ")).find()){
            return true;
        }

        // Extracts the urls of the message
        List<String> urls = urlPattern.matcher(content).results().map(MatchResult::group).toList();

        for(String url : urls){
            // Gets the domain and splits its different parts
            String[] rawDomain = url
                    .replace("https://", "")
                    .replace("http://", "")
                    .split("/")[0]
                    .split("\\.");

            // Gets rid of the subdomains
            rawDomain = Arrays.copyOfRange(rawDomain, Math.max(rawDomain.length - 2, 0), rawDomain.length);

            // Re-assemble
            String domain = String.join(".", rawDomain);

            // Matches slightly altered links
            if(!trustedDomains.contains(domain) && trustedDomains.stream().anyMatch(genuine -> Strings.levenshtein(genuine, domain) <= 2)){
                return true;
            }
        }

        return false;
    }

    UserData data(User user){
        return userData.get(user.getId(), UserData::new);
    }

    static class UserData{
        /** consecutive scam messages sent */
        int scamMessages;
        /** last message that contained any link */
        @Nullable String lastLinkMessage;
        /** channel ID of last link posted */
        @Nullable String lastLinkChannelId;
        /** link cross-postings in a row */
        int linkCrossposts;
        /** all members pinged in consecutive messages */
        ObjectSet<String> idsPinged = new ObjectSet<>();
    }
}
