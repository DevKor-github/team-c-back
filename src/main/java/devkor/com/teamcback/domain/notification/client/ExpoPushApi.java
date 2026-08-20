package devkor.com.teamcback.domain.notification.client;

import devkor.com.teamcback.domain.notification.config.ExpoPushFeignConfig;
import devkor.com.teamcback.domain.notification.dto.expo.ExpoPushRequest;
import devkor.com.teamcback.domain.notification.dto.expo.ExpoPushResponse;
import devkor.com.teamcback.domain.notification.dto.expo.ExpoReceiptRequest;
import devkor.com.teamcback.domain.notification.dto.expo.ExpoReceiptResponse;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "expoPushApi",
        url = "${push.expo.base-url}",
        configuration = ExpoPushFeignConfig.class
)
public interface ExpoPushApi {

    @PostMapping("/send")
    ExpoPushResponse send(@RequestBody List<ExpoPushRequest> requests);

    @PostMapping("/getReceipts")
    ExpoReceiptResponse getReceipts(@RequestBody ExpoReceiptRequest request);
}
