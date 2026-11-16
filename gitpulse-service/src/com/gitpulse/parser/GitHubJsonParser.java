package com.gitpulse.parser;

import com.gitpulse.model.Repository;

public interface GitHubJsonParser {
    void parse (String json, Repository repository);
}
