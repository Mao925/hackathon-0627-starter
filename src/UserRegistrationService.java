package com.youtrust.hackathon;

import java.util.logging.Logger;

/**
 * ユーザー登録サービス（スターターコード）
 *
 * TODO: このクラスをリファクタリングしてください。
 * どう設計するか、なぜそう設計するかを DESIGN.md と DECISIONS.md に記録してください。
 */
public class UserRegistrationService {

    private static final Logger logger = Logger.getLogger(UserRegistrationService.class.getName());

    // データベース接続（簡略化のためモック）
    private final Database database = new Database();

    // メール送信（簡略化のためモック）
    private final EmailClient emailClient = new EmailClient();

    // GitHub OAuth（簡略化のためモック）
    private final GitHubOAuthClient githubOAuthClient = new GitHubOAuthClient();

    /**
     * ユーザーを登録する
     *
     * @param input 登録情報
     * @return 登録結果
     * @throws Exception 何か問題が起きたとき
     */
    public RegisterResult register(RegisterInput input) throws Exception {

        RegistrationData registrationData = buildRegistrationData(input);

        validateCommonUserInfo(registrationData);

        // 重複チェック
        if (database.findByEmail(registrationData.getEmail()) != null) {
            throw new IllegalArgumentException("このメールアドレスはすでに登録されています");
        }

        // DBに保存
        User user = new User();
        user.setEmail(registrationData.getEmail());
        user.setName(registrationData.getName());
        user.setPassword(registrationData.getPassword());
        user.setAuthProvider(registrationData.getAuthProvider());
        user.setProviderUserId(registrationData.getProviderUserId());
        database.save(user);

        afterRegistration(user);

        return new RegisterResult(true, user.getId(), "登録が完了しました");
    }

    private RegistrationData buildRegistrationData(RegisterInput input) {
        String authProvider = input.getAuthProvider();
        if (authProvider == null || authProvider.trim().isEmpty()) {
            throw new IllegalArgumentException("登録方式は必須です");
        }

        authProvider = authProvider.trim().toLowerCase();

        if ("github".equals(authProvider)) {
            return buildGitHubRegistrationData(input);
        }

        if ("password".equals(authProvider)) {
            return buildPasswordRegistrationData(input);
        }

        throw new IllegalArgumentException("未対応の登録方式です: " + authProvider);
    }

    private RegistrationData buildGitHubRegistrationData(RegisterInput input) {
        if (input.getAuthorizationCode() == null || input.getAuthorizationCode().trim().isEmpty()) {
            throw new IllegalArgumentException("GitHub認可コードは必須です");
        }

        GitHubUserProfile profile = githubOAuthClient.fetchUserProfile(input.getAuthorizationCode());

        return new RegistrationData(
            profile.getEmail(),
            profile.getName(),
            null,
            "github",
            profile.getGithubId()
        );
    }

    private RegistrationData buildPasswordRegistrationData(RegisterInput input) {
        if (input.getPassword() == null || input.getPassword().length() < 8) {
            throw new IllegalArgumentException("パスワードは8文字以上必要です");
        }

        // パスワードハッシュ化（簡略化）
        String hashedPassword = input.getPassword() + "_hashed";

        return new RegistrationData(
            input.getEmail(),
            input.getName(),
            hashedPassword,
            "password",
            null
        );
    }

    private void validateCommonUserInfo(RegistrationData registrationData) {
        if (registrationData.getEmail() == null || !registrationData.getEmail().contains("@")) {
            throw new IllegalArgumentException("メールアドレスが無効です");
        }
        if (registrationData.getName() == null || registrationData.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("名前は必須です");
        }
    }

    private void afterRegistration(User user) {
        String subject = "【ハッカソン】登録完了のお知らせ";
        String body = user.getName() + " 様\n\nご登録ありがとうございます。";
        emailClient.send(user.getEmail(), subject, body);

        logger.info("ユーザー登録完了: " + user.getEmail());
    }


    // ---- 以下はモッククラス（変更不要） ----

    static class Database {
        public User findByEmail(String email) { return null; }
        public void save(User user) { user.setId("user_" + System.currentTimeMillis()); }
    }

    static class EmailClient {
        public void send(String to, String subject, String body) {
            System.out.println("Email sent to: " + to);
        }
    }

    static class User {
        private String id;
        private String email;
        private String name;
        private String password;
        private String authProvider;
        private String providerUserId;
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getAuthProvider() { return authProvider; }
        public void setAuthProvider(String authProvider) { this.authProvider = authProvider; }
        public String getProviderUserId() { return providerUserId; }
        public void setProviderUserId(String providerUserId) { this.providerUserId = providerUserId; }
    }

    static class RegisterInput {
        private String email;
        private String password;
        private String name;
        private String authProvider;
        private String authorizationCode;
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getAuthProvider() { return authProvider; }
        public void setAuthProvider(String authProvider) { this.authProvider = authProvider; }
        public String getAuthorizationCode() { return authorizationCode; }
        public void setAuthorizationCode(String authorizationCode) { this.authorizationCode = authorizationCode; }
    }

    static class GitHubOAuthClient {
        public GitHubUserProfile fetchUserProfile(String authorizationCode) {
            // 実際はGitHub APIを呼び出して、アクセストークン交換とユーザー情報取得を行う
            return new GitHubUserProfile("github_123", "github-user@example.com", "GitHub User");
        }
    }

    static class GitHubUserProfile {
        private final String githubId;
        private final String email;
        private final String name;
        public GitHubUserProfile(String githubId, String email, String name) {
            this.githubId = githubId;
            this.email = email;
            this.name = name;
        }
        public String getGithubId() { return githubId; }
        public String getEmail() { return email; }
        public String getName() { return name; }
    }

    static class RegistrationData {
        private final String email;
        private final String name;
        private final String password;
        private final String authProvider;
        private final String providerUserId;
        public RegistrationData(String email, String name, String password, String authProvider, String providerUserId) {
            this.email = email;
            this.name = name;
            this.password = password;
            this.authProvider = authProvider;
            this.providerUserId = providerUserId;
        }
        public String getEmail() { return email; }
        public String getName() { return name; }
        public String getPassword() { return password; }
        public String getAuthProvider() { return authProvider; }
        public String getProviderUserId() { return providerUserId; }
    }

    static class RegisterResult {
        private final boolean success;
        private final String userId;
        private final String message;
        public RegisterResult(boolean success, String userId, String message) {
            this.success = success;
            this.userId = userId;
            this.message = message;
        }
        public boolean isSuccess() { return success; }
        public String getUserId() { return userId; }
        public String getMessage() { return message; }
    }
}
