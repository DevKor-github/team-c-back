package devkor.com.teamcback.domain.notification.service;

import devkor.com.teamcback.domain.notification.dto.payload.PushPayload;
import devkor.com.teamcback.domain.notification.dto.request.PushDispatchCommand;
import devkor.com.teamcback.domain.notification.dto.response.PushDispatchEnqueueRes;
import devkor.com.teamcback.domain.notification.entity.PushDispatch;
import devkor.com.teamcback.domain.notification.entity.PushInstallation;
import devkor.com.teamcback.domain.notification.entity.PushMessage;
import devkor.com.teamcback.domain.notification.factory.PushPayloadFactory;
import devkor.com.teamcback.domain.notification.repository.PushDispatchRepository;
import devkor.com.teamcback.domain.notification.repository.PushMessageRepository;
import devkor.com.teamcback.domain.notification.resolver.PushTargetResolver;
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
@Transactional(transactionManager = "pushTransactionManager", readOnly = true)
public class PushDispatchService {

    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_BODY_LENGTH = 1024;
    private static final int MAX_TARGET_VALUE_LENGTH = 128;
    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 160;
    private static final int MAX_SELECTED_TARGETS = 500;

    private final PushDispatchRepository pushDispatchRepository;
    private final PushMessageRepository pushMessageRepository;
    private final PushPayloadFactory pushPayloadFactory;
    private final PushTargetResolver pushTargetResolver;
    private final Clock clock;
    @org.springframework.beans.factory.annotation.Value("${push.storage:mysql}") private String storage = "mysql";
    @org.springframework.beans.factory.annotation.Value("${push.audience:INTERNAL_TEST}") private String audience = "INTERNAL_TEST";


    @Transactional("pushTransactionManager")
    public PushDispatchEnqueueRes enqueue(PushDispatchCommand command) {
        if ("neon".equals(storage)) {
            boolean internal = command.adminOnly() == null ? "INTERNAL_TEST".equals(audience) : command.adminOnly();
            if (!internal && !"LIVE".equals(audience)) throw new GlobalException(devkor.com.teamcback.global.response.ResultCode.FORBIDDEN);
            command = command.withAudience(internal);
        }
        validateCommand(command);
        if ("neon".equals(storage)) pushDispatchRepository.lockIdempotencyKey(command.idempotencyKey());

        PushPayload payload = pushPayloadFactory.createForPreDispatchValidation(
                command.title(),
                command.body(),
                command.mode(),
                command.appVariant(),
                command.actionType(),
                command.actionParams(),
                command.imageUrl()
        );

        final PushDispatchCommand scopedCommand = command;
        return pushDispatchRepository.findByIdempotencyKey(scopedCommand.idempotencyKey())
                .map(PushDispatchEnqueueRes::new)
                .orElseGet(() -> createDispatch(scopedCommand, payload));
    }

    private PushDispatchEnqueueRes createDispatch(
            PushDispatchCommand command,
            PushPayload payload
    ) {
        List<PushInstallation> installations = hasSelectedTargets(command)
                ? pushTargetResolver.resolveSelected(
                        command.targetValues(),
                        command.appVariant()
                )
                : pushTargetResolver.resolve(
                        command.targetType(),
                        command.targetValue(),
                        command.appVariant()
                );

        if (Boolean.TRUE.equals(command.adminOnly())) {
            installations = pushTargetResolver.restrictToAdmins(installations, true);
        }
        if (installations.isEmpty()) throw new GlobalException(INVALID_INPUT);

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
                        payload.image(),
                        command.actionType(),
                        pushPayloadFactory.serializeActionParams(payload.data().action().params()),
                        command.idempotencyKey(),
                        command.createdBy(),
                        now
                )
        );

        dispatch.setAdminOnly(command.adminOnly());

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

        if (hasSelectedTargets(command)) {
            if (command.targetValues().size() > MAX_SELECTED_TARGETS) {
                throw new GlobalException(INVALID_INPUT);
            }
            command.targetValues().forEach(value -> validateText(value, MAX_TARGET_VALUE_LENGTH));
        }
    }

    private boolean hasSelectedTargets(PushDispatchCommand command) {
        return command.targetValues() != null && !command.targetValues().isEmpty();
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
