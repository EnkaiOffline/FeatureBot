package org.enkai.featurebot.core;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

public abstract class Feature {

    protected FeatureBot bot;

    public Feature(FeatureBot bot) {
        this.bot = bot;
    }

    public void processCallbackQuery(CallbackQuery query) {}

    public abstract String getName();

}
