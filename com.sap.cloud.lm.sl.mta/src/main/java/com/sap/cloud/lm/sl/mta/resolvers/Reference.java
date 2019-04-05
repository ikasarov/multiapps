package com.sap.cloud.lm.sl.mta.resolvers;

public class Reference {

    private final String key;
    private final String matchedPattern;
    private final String dependencyName;

    public Reference(String matchedPattern, String propertyName) {
        this(matchedPattern, propertyName, null);
    }

    public Reference(String matchedPattern, String propertyName, String dependencyName) {
        this.key = propertyName;
        this.matchedPattern = matchedPattern;
        this.dependencyName = dependencyName;
    }

    public String getKey() {
        return key;
    }

    public String getMatchedPattern() {
        return matchedPattern;
    }

    public String getDependencyName() {
        return dependencyName;
    }

}
