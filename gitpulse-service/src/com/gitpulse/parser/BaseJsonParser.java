package com.gitpulse.parser;

import com.gitpulse.model.Repository;
import com.gitpulse.util.ErrorHandler;

public abstract class BaseJsonParser implements GitHubJsonParser {

    // Overriding the parse method of interface to use in subclasses
    @Override
    public void parse(String json, Repository repository) {
        // Exception handling
        try {
            // Method call depending on the type of definition provided
            process(json, repository);
        } catch (Exception e) {
            ErrorHandler.log(getParserName(), e.getMessage());
        }
    }

    // Abstract methods
    protected abstract void process(String json, Repository repository);
    protected abstract String getParserName();
}