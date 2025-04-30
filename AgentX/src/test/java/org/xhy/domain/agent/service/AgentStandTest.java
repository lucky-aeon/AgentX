package org.xhy.domain.agent.service;

import dev.langchain4j.data.message.AiMessage;

public interface AgentStandTest {

    AiMessage chat(String prompt);
}
