package devkor.com.teamcback.domain.user.service;

import com.apple.itunes.storekit.model.Environment;
import com.apple.itunes.storekit.model.JWSTransactionDecodedPayload;
import com.apple.itunes.storekit.model.ResponseBodyV2DecodedPayload;
import com.apple.itunes.storekit.verification.SignedDataVerifier;
import com.apple.itunes.storekit.verification.VerificationException;
import devkor.com.teamcback.domain.user.dto.request.AppleNotationReq;
import devkor.com.teamcback.domain.user.dto.response.AppleNotificationRes;
import devkor.com.teamcback.domain.user.repository.UserRepository;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import devkor.com.teamcback.global.response.ResultCode;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

@Service
public class AppleService {

    @Autowired
    private UserRepository userRepository;
    private SignedDataVerifier verifier;
    private static final String BUNDLE_ID = "com.devkor.kodaero";

    @PostConstruct
    public void init() throws FileNotFoundException {

        InputStream certInputStream  = getClass().getClassLoader().getResourceAsStream("static/apple/AppleRootCA-G3.cer");

        Set<InputStream> rootCertificates = new HashSet<>();
        rootCertificates.add(certInputStream);

        this.verifier = new SignedDataVerifier(
                rootCertificates,
                BUNDLE_ID,
                null,
                Environment.LOCAL_TESTING,
                // Environment.PRODUCTION,
                false
        );
    }


    @Transactional
    public AppleNotificationRes handleAppleNotification(AppleNotationReq request) {

        String signedPayload = request.getSignedPayload();

        if (signedPayload == null || signedPayload.isEmpty()) {
            throw new GlobalException(ResultCode.INVALID_INPUT);
        }

        try {
            ResponseBodyV2DecodedPayload payload = verifier.verifyAndDecodeNotification(signedPayload);

            String notificationType = payload.getNotificationType().toString();
            String uuid = payload.getNotificationUUID();

            System.out.println("알림 유형: " + notificationType);
            System.out.println("UUID: " + uuid);

            String signedTransactionInfo = payload.getData().getSignedTransactionInfo();
            String signedRenewalInfo = payload.getData().getSignedRenewalInfo();

            JWSTransactionDecodedPayload transactionInfo = verifier.verifyAndDecodeTransaction(signedTransactionInfo);
            String originalTransactionId = transactionInfo.getOriginalTransactionId();

            // 내부 사용자 ID 조회
            // String userId = userRepository.findUserIdByOriginalTransactionId(originalTransactionId);

            switch (notificationType) {
                case "account_delete":
                    break;
                case "email_change":
                    break;
                default:
                    System.out.println("Unhandled Apple notification: " + notificationType);
                    break;
            }

            return new AppleNotificationRes();

        } catch (Exception e) {
            System.err.println("Apple SiWA Notification processing failed: " + e.getMessage());
            throw new GlobalException(ResultCode.SYSTEM_ERROR);
        }
    }

}
