package com.example.verifier.storage;

import com.example.verifier.model.Presentation;
import com.example.verifier.model.PresentationStoredEntry;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PresentationStore {

    private final Map<String, PresentationStoredEntry> presentations = new ConcurrentHashMap<>();

    public void store(Presentation.Requested presentation) {
        String requestId = presentation.getRequestId();
        PresentationStoredEntry existing = presentations.get(requestId);

        if (existing != null) {
            presentations.put(requestId, existing.copyWithNewPresentation(presentation));
        } else {
            presentations.put(requestId, new PresentationStoredEntry(presentation, null));
        }
    }

    public PresentationStoredEntry getByRequestId(String requestId) {
        return presentations.get(requestId);
    }

    public boolean containsRequestId(String requestId) {
        return presentations.containsKey(requestId);
    }
}
