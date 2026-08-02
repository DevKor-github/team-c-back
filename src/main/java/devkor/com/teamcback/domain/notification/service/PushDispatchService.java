package devkor.com.teamcback.domain.notification.service;

import devkor.com.teamcback.domain.notification.dto.payload.PushPayload;
import devkor.com.teamcback.domain.notification.dto.request.PushDispatchCommand;
import devkor.com.teamcback.domain.notification.dto.response.PushDispatchEnqueueRes;
import devkor.com.teamcback.domain.notification.entity.PushDispatch;
import devkor.com.teamcback.domain.notification.entity.PushInstallation;
import devkor.com.teamcback.domain.notification.entity.PushMessage;
import devkor.com.teamcback.domain.notification.repository.PushDispatchRepository;
import devkor.com.teamcback.domain.notification.repository.PushMessageRepository;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static devkor.com.teamcback.global.response.ResultCode.INVALID_INPUT;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PushDispatchService {

    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_BODY_LENGTH = 1024;
    private static final int MAX_TARGET_VALUE_LENGTH = 128;
    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 128;

    private final PushDispatchRepository pushDispatchRepository;
    private final PushMessageRepository pushMessageRepository;
    private final PushPayloadFactory pushPayloadFactory;
    private final PushTargetResolver pushTargetResolver;
    private final Clock clock;

    @Transactional
    public PushDispatchEnqueueRes enqueue(PushDispatchCommand command) {
        validateCommand(command);

        PushPayload payload = pushPayloadFactory.create(
                command.title(),
                command.body(),
                command.actionType(),
                command.actionParams(),
                command.appVariant()
        );

        return pushDispatchRepository.findByIdempotencyKey(command.idempotencyKey())
                .map(PushDispatchEnqueueRes::new)
                .orElseGet(() -> createDispatch(command, payload));
    }

    private PushDispatchEnqueueRes createDispatch(
            PushDispatchCommand command,
            PushPayload payload
    ) {
        List<PushInstallation> installations = pushTargetResolver.resolve(
                command.targetType(),
                command.targetValue(),
                command.appVariant()
        );

        LocalDateTime now = LocalDateTime.now(clock);

        PushDispatch dispatch = pushDispatchRepository.save(
                new PushDispatch(
                        command.notificationType(),
                        command.mode(),
                        command.appVariant(),
                        command.targetType(),
                        command.targetValue(),
                        command.title(),
                        command.body(),
                        command.actionType(),
                        pushPayloadFactory.serializeActionParams(payload.data().actionParams()),
                        command.idempotencyKey(),
                        command.createdBy(),
                        now
                )
        );

        List<PushMessage> messages = installations.stream()
                .map(installation -> new PushMessage(
                        dispatch,
                        installation,
                        now
                ))
                .toList();

        pushMessageRepository.saveAll(messages);
        dispatch.updateRecipientCount(messages.size());

        return new PushDispatchEnqueueRes(dispatch);
    }

    private void validateCommand(PushDispatchCommand command) {
        if (command == null
                || command.notificationType() == null
                || command.mode() == null
                || command.appVariant() == null
                || command.targetType() == null
                || command.actionType() == null
                || command.createdBy() == null) {
            throw new GlobalException(INVALID_INPUT);
        }

        validateText(command.targetValue(), MAX_TARGET_VALUE_LENGTH);
        validateText(command.title(), MAX_TITLE_LENGTH);
        validateText(command.body(), MAX_BODY_LENGTH);
        validateText(command.idempotencyKey(), MAX_IDEMPOTENCY_KEY_LENGTH);
    }

    private void validateText(
            String value,
            int maxLength
    ) {
        if (value == null || value.isBlank() || value.length() > maxLength) {
            throw new GlobalException(INVALID_INPUT);
        }
    }
}
