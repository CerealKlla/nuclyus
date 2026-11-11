package com.github.cerealklla.nuclyus.tag;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/** Backing registry for {@link TagApprovalListener} -- see that interface's own doc. Public so
 *  {@code api.Nuclyus#registerTagApprovalListener} (a different package) can reach {@link
 *  #register}, same precedent as TinyWyrlds' own {@code instance.WyrldExitHooks}. */
public final class TagApprovalHooks {

    private static final List<TagApprovalListener> LISTENERS = new CopyOnWriteArrayList<>();

    private TagApprovalHooks() {
    }

    public static void register(TagApprovalListener listener) {
        LISTENERS.add(listener);
    }

    static void notifyApproved(String originalTag, String approvedTag, UUID requestedBy) {
        for (TagApprovalListener listener : LISTENERS) {
            listener.onTagApproved(originalTag, approvedTag, requestedBy);
        }
    }
}
