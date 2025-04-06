package org.xhy.domain.agent.service;

import dev.langchain4j.service.TokenStream;

public interface AgentTest {


    TokenStream chat(String prompt);
}
