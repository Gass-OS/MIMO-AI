package ai.mimo.chat

/**
 * Shared user-facing identity for chat and image creation.
 * Keep model and engine implementation names out of normal UI.
 */
object MimoIdentity {
    const val assistantName = "MIMO AI"
    const val chatGreeting = "Hi 👋 I'm MIMO AI, your offline AI assistant. How can I help you today?"
    const val imageGreeting = "Hi 👋 I'm MIMO AI. Tell me what you'd like me to create. Your image will appear here in our chat."
    const val previewReply = "Thanks for your message! I'm still being connected to my on-device AI engine. This preview can show the interface, but real AI replies aren't available yet."
    const val imagePreviewReply = "I'm getting my creative tools ready. Image creation isn't available in this preview yet."
}
