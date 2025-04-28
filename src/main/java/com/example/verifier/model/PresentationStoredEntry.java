package com.example.verifier.model;

public class PresentationStoredEntry {
    private final Presentation.Requested presentation;
    private final Object somethingElse;

    public PresentationStoredEntry(Presentation.Requested presentation, Object somethingElse) {
        this.presentation = presentation;
        this.somethingElse = somethingElse;
    }

    public Presentation.Requested getPresentation() {
        return presentation;
    }

    public Object getSomethingElse() {
        return somethingElse;
    }

    public PresentationStoredEntry copyWithNewPresentation(Presentation.Requested newPresentation) {
        return new PresentationStoredEntry(newPresentation, this.somethingElse);
    }
}
