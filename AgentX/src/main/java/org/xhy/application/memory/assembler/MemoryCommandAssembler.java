package org.xhy.application.memory.assembler;

import org.xhy.domain.memory.model.CandidateMemory;
import org.xhy.domain.memory.model.MemoryType;
import org.xhy.interfaces.dto.memory.CreateMemoryRequest;

public class MemoryCommandAssembler {

    public static CandidateMemory toCandidate(CreateMemoryRequest req) {
        CandidateMemory cm = new CandidateMemory();
        cm.setType(MemoryType.safeOf(req.getType()));
        cm.setText(req.getText());
        cm.setImportance(req.getImportance());
        cm.setTags(req.getTags());
        cm.setData(req.getData());
        return cm;
    }
}
