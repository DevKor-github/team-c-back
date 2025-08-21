package devkor.com.teamcback.domain.common.service;

import devkor.com.teamcback.domain.suggestion.entity.SuggestionImage;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private static final String SUGGESTION_TITLE = "[고대로] 건의사항 추가됨 - 제목: ";
    private static final String SENDER = "고대로팀 <leeyejin113@gmail.com>";

    @Value("${staff.emails}")
    private String notifyEmails;

    @Value("${metrics.environment}")
    private String env;

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * 건의사항 이메일 전송
     */
    @Async
    public void sendNotificationMessage(String title, String writer, String type, String content, List<SuggestionImage> images) throws MessagingException {

        // 내용 설정
        Map<String, String> contents = new HashMap<>();
        contents.put("건의제목", title);
        contents.put("건의종류", type);
        contents.put("건의내용", content);

        // 전송
        sendHtmlEmail(SUGGESTION_TITLE + title, "건의", writer, contents, images);
    }

    /**
     * 일반 이메일 전송
     */
    private void sendSimpleEmail(String title, String type, String writer, Map<String, String> contents, List<SuggestionImage> images) {

        for(String notifyEmail : notifyEmails.split(",")) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(SENDER);  // 보내는 사람
            message.setTo(notifyEmail);  // 받는 사람
            message.setSubject(title);  // 제목

            // 내용
            String content = writer +"(으)로부터" + type + "가 추가되었습니다!";
            for(String key : contents.keySet()) {
                content += "\n - " + key + ": " + contents.get(key);
            }

            // 첨부파일
            if(images != null) {
                content += "\n - 첨부 파일:";
                for (int i = 0; i < images.size(); i++) {
                    content += "\n[" + (i+1) + "] link: " + images.get(i).getImageUrl();
                }
            }

            content += "\n* 서버 환경: " + env;

            message.setText(content);

            mailSender.send(message);
        }

    }

    /**
     * HTML 이메일 전송
     */
    private void sendHtmlEmail(String title, String type, String writer, Map<String, String> contents, List<SuggestionImage> images) throws MessagingException {

        for(String notifyEmail : notifyEmails.split(",")) {

            MimeMessage message = mailSender.createMimeMessage();

            // true: multipart 메시지 (첨부파일 가능), "UTF-8": 문자 인코딩
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(SENDER);
            helper.setTo(notifyEmail);
            helper.setSubject(title);

            // 내용
            String html = "<h2 style='color:green;'>" + writer +"(으)로부터" + type + "가 추가되었습니다!</h2>";
            for(String key : contents.keySet()) {
                html += "<p><strong>" + key + "</strong><br>" + contents.get(key) + "</p>";
            }

            // 첨부파일
            if(images != null) {
                html += "<p><strong>첨부 파일:</strong><br>";
                for (int i = 0; i < images.size(); i++) {
                    html += "<a href='" + images.get(i).getImageUrl() + "'>[" + (i+1) + "]</a> ";
                }
                html += "</p>";
            }

            html += "<p>* 서버 환경: " + env + "</p>";

            helper.setText(html, true);  // HTML 모드

            mailSender.send(message);
        }
    }
}
