package devkor.com.teamcback.test;

import devkor.com.teamcback.domain.user.entity.Provider;
import devkor.com.teamcback.domain.user.entity.Role;
import devkor.com.teamcback.domain.user.entity.User;

public interface UserTest {
    String TEST_USER_USERNAME = "username";
    String TEST_USER_EMAIL = "test@gmail.com";
    Role TEST_USER_ROLE = Role.USER;
    Provider TEST_USER_PROVIDER = Provider.KAKAO;

    User TEST_USER = new User(TEST_USER_USERNAME, TEST_USER_EMAIL, TEST_USER_ROLE, TEST_USER_PROVIDER);

}
