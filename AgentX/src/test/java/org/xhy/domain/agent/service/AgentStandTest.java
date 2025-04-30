package org.xhy.domain.agent.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.service.TokenStream;

public interface AgentStandTest {

    AiMessage chat(String prompt);
}
