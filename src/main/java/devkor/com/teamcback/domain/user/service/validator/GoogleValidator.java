package devkor.com.teamcback.domain.user.service.validator;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import devkor.com.teamcback.global.exception.GlobalException;
import devkor.com.teamcback.global.response.ResultCode;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GoogleValidator {
    private final GoogleIdTokenVerifier verifier;
    @Value("${jwt.social.google.client-id}")
    private String CLIENT_ID;
    public GoogleValidator() {
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), jsonFactory)
            .setAudience(Collections.singletonList(CLIENT_ID))
            //.setAudience(Arrays.asList(CLIENT_ID_1, CLIENT_ID_2, CLIENT_ID_3))
            .build();
    }

    public String validateToken(String token) {
        try{
            GoogleIdToken idToken = verifier.verify(token);
            if (idToken != null) {
                Payload payload = idToken.getPayload();

                // Print user identifier
//            String userId = payload.getSubject();
//            System.out.println("User ID: " + userId);

                // Get profile information from payload
                String email = payload.getEmail();
//            boolean emailVerified = payload.getEmailVerified();
//            String name = (String) payload.get("name");
//            String pictureUrl = (String) payload.get("picture");
//            String locale = (String) payload.get("locale");
//            String familyName = (String) payload.get("family_name");
//            String givenName = (String) payload.get("given_name");

                return email;

            } else {
                throw new GlobalException(ResultCode.UNAUTHORIZED);
            }
        } catch (Exception e) {
            throw new GlobalException(ResultCode.UNAUTHORIZED);
        }
    }
}
