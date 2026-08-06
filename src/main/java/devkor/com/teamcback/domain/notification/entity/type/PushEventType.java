package devkor.com.teamcback.domain.notification.entity.type;

public enum PushEventType {
    CROWD("push:event:crowd-enabled"),
    REPORT("push:event:report-enabled"),
    CHARACTER("push:event:character-enabled"),
    SURVEY("push:event:survey-enabled");

    private final String redisKey;

    PushEventType(String redisKey) {
        this.redisKey = redisKey;
    }

    public String redisKey() {
        return redisKey;
    }
}
