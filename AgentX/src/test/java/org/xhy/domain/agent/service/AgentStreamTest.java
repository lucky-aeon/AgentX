package org.xhy.domain.agent.service;

import dev.langchain4j.service.TokenStream;

public interface AgentStreamTest {


    TokenStream chat(String prompt);
}
