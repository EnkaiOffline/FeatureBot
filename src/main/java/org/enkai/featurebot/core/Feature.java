package org.enkai.featurebot.core;

public abstract class Feature {

    protected FeatureBot bot;

    public Feature(FeatureBot bot) {
        this.bot = bot;
    }

    public abstract String getName();

}
