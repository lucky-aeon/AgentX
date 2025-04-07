package org.xhy.domain.conversation.handler;

import dev.langchain4j.service.TokenStream;

public interface Agent {

    TokenStream chat(String prompt);

}
