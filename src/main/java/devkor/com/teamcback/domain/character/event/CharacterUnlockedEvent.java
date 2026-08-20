package devkor.com.teamcback.domain.character.event;

public record CharacterUnlockedEvent(
        Long userId,
        Long characterId,
        Long userCharacterId,
        String characterName
) {
}
