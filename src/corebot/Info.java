package corebot;

public enum Info{
    download("Mindustry доступна на таких платформах:",
    """
    [Steam](https://store.steampowered.com/app/1127400/Mindustry/)
    [Android, Itch.io](https://anuke.itch.io/mindustry)
    [IOS](https://itunes.apple.com/us/app/mindustry/id1385258906?mt=8&ign-mpt=uo%3D8)
    [Google Play](https://play.google.com/store/apps/details?id=mindustry)
            """),
    links("Посилання на ресурси пов'язані з Mindustry",
    """
    [Вихідний код на Github](https://github.com/Anuken/Mindustry/)
    [Форма для пропозицій](https://github.com/Anuken/Mindustry-Suggestions/issues/new/choose)
    [Форма для повідомлення про хибу](https://github.com/Anuken/Mindustry/issues/new/choose)
    [Trello](https://trello.com/b/aE2tcUwF)
    [TestFlight](https://testflight.apple.com/join/79Azm1hZ)
    [Mindustry Subreddit](https://www.reddit.com/r/mindustry)
    [Неофіційний Matrix Space](https://matrix.to/#/#mindustry-space:matrix.org)
    """),
    beta("Як завантажити бета версії на IOS/Android",
    """
    Щоб приєднатися до Android Beta в Google Play Store, просто прокрутіть до низу сторінки і натисніть 'join beta'.
    Приєднання до бета-тестування займе деякий час. Наберіться терпіння.
    [Direct Link](https://play.google.com/apps/testing/mindustry).
    Щоб долучитися до бета-тестування в IOS, натисніть на [TestFlight Link](https://testflight.apple.com/join/79Azm1hZ), потім установіть Apple TestFlight додаток, щоб грати в Mindustry.
    """),
    rules("Правила",
    """
    **1.** Спілкуватися та надсилати матеріали виключно українською, виняток – англійська. Англійською не зловживайте.
    **2.** Адміністратор/модератор може видати попередження за причиною, що не вказана в правилах, якщо вважатиме за потрібне.
    **3.** Серйозна політика (як-от обговорення ідеологій) лише в #політика.
    **Забороняється**
    **4.** Використовувати нецензурну лексику.
    **5.** Спам/флуд/капс/офтоп.
    **6.** Надсилати відео/картинки зі скримерами, гучними звуками без спойлеру і попередження.
    **7.** NSFW-обговорення.
    **8.** Расизм/сексизм/гомофобія тощо.
    **9.** Видавати себе за інших членів або навмисно редагувати свої повідомлення, щоб ввести інших в оману.
    **10.** Просити про ролі.
    **11.** Згадувати користувачів без причини.
    **12.** Ображати людей будь-яким способом.
    **13.** Використовувати прогалини в правилах.
    **14.** Використовувати інші обліківки на цьому сервері.
    **15.** Уникати покарання будь-яким способом.
    **16.** Уславлення комунізму/нацизму/російської агресії, заперечення територіальної цілісності України та поширення російської культури.
                                                                                               
    Якщо ви порушили правило вам видадуть попередження на 15 днів.
    3 попередження = блокування назавжди.
    Незнання правил не звільняє вас від відповідальності. Якщо ви не погоджуєтеся з правилами, то ви маєте покинути сервер.
            """);
    public final String text;
    public final String title;

    Info(String title, String text){
        this.text = text;
        this.title = title;
    }
}
