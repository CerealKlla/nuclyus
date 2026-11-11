package com.github.cerealklla.nuclyus.tag;

import java.util.UUID;

/**
 * A caller's reaction to {@link TagRegistry#approve} -- registered via {@link
 * TagApprovalHooks#register}. Nuclyus itself does nothing with a tag approval beyond updating its
 * own registry; this is how a consumer (TinyWyrlds today, Blueprynts potentially later -- both draw
 * from this same shared vocabulary, see {@code api.Nuclyus}'s own tag-method docs) reacts to one,
 * e.g. retroactively rewriting its own content that used the old tag text, or notifying whoever
 * requested it.
 *
 * @param originalTag the pending text as originally requested
 * @param approvedTag the final text now in the approved vocabulary -- equal to {@code originalTag}
 *     if the admin approved it as-is, different if they corrected it
 * @param requestedBy  who originally requested {@code originalTag}, or {@code null} if that
 *     couldn't be resolved (shouldn't happen via the real review UI, but a listener must tolerate it)
 */
@FunctionalInterface
public interface TagApprovalListener {
    void onTagApproved(String originalTag, String approvedTag, UUID requestedBy);
}
