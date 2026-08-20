package devkor.com.teamcback.domain.notification.dto.worker;

public record PushReceiptItem(
        Long pushMessageId,
        Long pushDispatchId,
        String expoTicketId
) {
}
