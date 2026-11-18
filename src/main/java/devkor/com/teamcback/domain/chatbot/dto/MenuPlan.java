package devkor.com.teamcback.domain.chatbot.dto;

public record MenuPlan(Intent intent, String cafeteriaQuery, String dateExpression) {
    public enum Intent { MENU, OTHER }
    public MenuPlan { intent = intent == null ? Intent.OTHER : intent; }
    public boolean isMenu() { return intent == Intent.MENU && cafeteriaQuery != null && !cafeteriaQuery.isBlank(); }
    public static MenuPlan other() { return new MenuPlan(Intent.OTHER, null, null); }
}
