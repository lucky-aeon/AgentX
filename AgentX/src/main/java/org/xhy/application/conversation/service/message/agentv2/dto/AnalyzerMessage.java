package org.xhy.application.conversation.service.message.agentv2.dto;

import org.xhy.application.conversation.service.message.agentv2.analysis.dto.AnalyzerMessageDTO;

public interface AnalyzerMessage {

    AnalyzerMessageDTO chat(String prompt);
}

