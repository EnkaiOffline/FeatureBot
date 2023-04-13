package org.enkai.featurebot.features.openai;

import org.enkai.featurebot.core.Command;
import org.enkai.featurebot.core.Feature;
import org.enkai.featurebot.core.FeatureBot;
import org.telegram.telegrambots.meta.api.objects.Message;

public class OpenAiFeature extends Feature {

    public OpenAiFeature(FeatureBot bot) {
        super(bot);
    }

    @Command("test")
    public void test(Message message) {
        bot.sendText(message, "test", true);
    }

    @Override
    public String getName() {
        return "OpenAi";
    }
}
