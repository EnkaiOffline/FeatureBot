package org.enkai.featurebot;

import org.enkai.featurebot.core.FeatureBot;
import org.enkai.featurebot.features.demotivator.DemotivatorFeature;
import org.enkai.featurebot.features.googleimage.GoogleImageFeature;
import org.enkai.featurebot.features.openai.OpenAiFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {

    private static Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            FeatureBot bot = new FeatureBot(System.getenv("TelegramBotToken"));
            bot.addFeature(new OpenAiFeature(bot));
            bot.addFeature(new GoogleImageFeature(bot));
            bot.addFeature(new DemotivatorFeature(bot));
            telegramBotsApi.registerBot(bot);
            log.info("Bot initialized");
        } catch (Exception e) {
            log.info("Bot initialization failed");
            e.printStackTrace();
        }
    }

}

