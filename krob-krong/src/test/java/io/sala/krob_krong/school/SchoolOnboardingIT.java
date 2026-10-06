package io.sala.krob_krong.school;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.sala.krob_krong.account.dto.AuthResponse;
import io.sala.krob_krong.account.dto.RegisterRequest;
import io.sala.krob_krong.account.service.AuthService;
import io.sala.krob_krong.common.utils.s3.*;
import io.sala.krob_krong.iam.security.TokenService;
import io.sala.krob_krong.iam.security.dto.SessionClaims;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.*;
import software.amazon.awssdk.services.s3.S3Client;
import tools.jackson.databind.*;

@SpringBootTest(
        properties = {
            "spring.datasource.url=${ONBOARDING_TEST_DB_URL}",
            "spring.mail.host=127.0.0.1",
            "spring.mail.port=1025",
            "spring.mail.properties.mail.smtp.auth=false",
            "spring.mail.properties.mail.smtp.starttls.enable=false",
            "spring.mail.properties.mail.smtp.starttls.required=false",
            "krob-krong.storage.s3.bucket=krob-krong-onboarding-it",
            "krob-krong.storage.s3.auto-create-bucket=true"
        })
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "ONBOARDING_IT", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SchoolOnboardingIT {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private AuthService auth;

    @Autowired
    private TokenService tokens;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private S3Client s3;

    @MockitoSpyBean
    private JavaMailSender mail;

    @MockitoSpyBean
    private S3Util storage;

    private String admin;
    private String adminWithoutMfa;
    private UUID adminId;

    @BeforeAll
    void bootstrapReviewer() {
        var request = new RegisterRequest();
        request.setEmail("reviewer-" + UUID.randomUUID() + "@example.test");
        request.setPassword("long reviewer test password");
        request.setDisplayName("Independent reviewer");
        var registered = auth.register(request, null, null);
        adminId = registered.getUserId();
        jdbc.sql("UPDATE app_user SET verified_at=now(),mfa_enabled_at=now() WHERE id=?")
                .param(adminId)
                .update();
        var previousAdmin = jdbc.sql(
                        "SELECT ur.user_id FROM user_role ur JOIN role r ON r.id=ur.role_id WHERE r.code='super_admin' LIMIT 1")
                .query(UUID.class)
                .optional();
        if (previousAdmin.isPresent()) adminId = previousAdmin.get();
        else
            jdbc.sql(
                            "INSERT INTO user_role(user_id,role_id,source) SELECT ?,id,'bootstrap' FROM role WHERE code='super_admin'")
                    .param(adminId)
                    .update();
        var refresh = tokens.issueRefreshToken(adminId, null, null, null);
        admin = tokens.issueAccessToken(SessionClaims.builder()
                        .userId(adminId)
                        .sessionId(refresh.getEntity().getFamilyId())
                        .kind("person")
                        .mfaVerified(true)
                        .build())
                .getValue();
        adminWithoutMfa = tokens.issueAccessToken(SessionClaims.builder()
                        .userId(adminId)
                        .sessionId(refresh.getEntity().getFamilyId())
                        .kind("person")
                        .mfaVerified(false)
                        .build())
                .getValue();
    }

    @AfterAll
    void cleanObjects() {
        // Only this test bucket; the disposable database is removed by the caller.
        s3.listObjectsV2Paginator(r -> r.bucket("krob-krong-onboarding-it"))
                .contents()
                .forEach(o -> s3.deleteObject(
                        r -> r.bucket("krob-krong-onboarding-it").key(o.key())));
    }

    @Test
    void smtpVerificationIsSingleUseAndRequiredForSchoolRegistration() throws Exception {
        AuthResponse user = signup();
        postJson("/api/schools", user.getAccessToken(), profile())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("EMAIL_VERIFICATION_REQUIRED"));
        String token = requestVerification(user);
        postJson("/api/me/email-verification", user.getAccessToken(), "{}").andExpect(status().isTooManyRequests());
        assertThat(jdbc.sql("SELECT token_hash FROM email_verification_token WHERE user_id=?")
                        .param(user.getUserId())
                        .query(String.class)
                        .single())
                .isNotEqualTo(token);
        mvc.perform(get("/api/auth/verify-email").param("token", token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
        assertThat(jdbc.sql("SELECT consumed_at IS NULL FROM email_verification_token WHERE user_id=?")
                        .param(user.getUserId())
                        .query(Boolean.class)
                        .single())
                .isTrue();
        mvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("token", token))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Email verified")));
        postJson("/api/auth/verify-email", null, "{\"token\":\"" + token + "\"}")
                .andExpect(status().isBadRequest());
        postJson("/api/me/email-verification", user.getAccessToken(), "{}").andExpect(status().isConflict());
        JsonNode school = registerSchool(user);
        assertThat(school.path("status").asText()).isEqualTo("draft");
        assertThat(jdbc.sql(
                                "SELECT count(*) FROM user_role ur JOIN role r ON r.id=ur.role_id WHERE ur.user_id=? AND ur.school_id=? AND r.code='school_owner' AND ur.accepted_at IS NOT NULL")
                        .param(user.getUserId())
                        .param(UUID.fromString(school.path("id").asText()))
                        .query(Long.class)
                        .single())
                .isEqualTo(1);
    }

    @Test
    void fullLifecycleLocksReviewAllowsResubmissionAndMakesSuspendedWorkspacesDark() throws Exception {
        AuthResponse owner = verified();
        JsonNode school = registerSchool(owner);
        String id = school.path("id").asText();
        String access = owner.getAccessToken();
        postJson("/api/schools/" + id + "/submit", access, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.missing_fields").isArray());
        JsonNode before = getData("/api/schools/" + id + "/onboarding", access);
        String branch = before.path("default_branch").path("id").asText();
        configureBranch(id, branch, access);
        JsonNode logo = upload(id, "logo", access), cover = upload(id, "cover", access);
        assertThat(logo.path("status").asText()).isEqualTo("ready");
        assertThat(logo.path("sha256").asText()).hasSize(64);
        assertThat(logo.path("width").asInt()).isEqualTo(8);
        String slug = "school-" + UUID.randomUUID();
        claim(id, branch, slug, access);
        assertThat(getData("/api/schools/" + id + "/onboarding", access)
                        .path("can_submit")
                        .asBoolean())
                .isTrue();
        mvc.perform(get("/api/workspaces/" + slug)).andExpect(status().isNotFound());
        postJson("/api/schools/" + id + "/branches/" + branch + "/open", access, "{}")
                .andExpect(status().isBadRequest());
        postJson("/api/schools/" + id + "/submit", access, "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_review"));
        mvc.perform(put("/api/schools/" + id + "/profile")
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profile()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("SCHOOL_PROFILE_LOCKED"));
        mvc.perform(put("/api/schools/" + id + "/default-branch")
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(branchBody("Main branch")))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/schools/" + id + "/cover").file(file()).header("Authorization", "Bearer " + access))
                .andExpect(status().isBadRequest());
        claimRequest(id, branch, "other-" + UUID.randomUUID(), access).andExpect(status().isBadRequest());
        String review = latestReview(id);
        postJson("/api/admin/schools/" + id + "/review", access, decision(review, "approved", null))
                .andExpect(status().isForbidden());
        postJson("/api/admin/schools/" + id + "/review", adminWithoutMfa, decision(review, "approved", null))
                .andExpect(status().isForbidden());
        postJson("/api/admin/schools/" + id + "/review", admin, decision(review, "changes_requested", " "))
                .andExpect(status().isBadRequest());
        postJson(
                        "/api/admin/schools/" + id + "/review",
                        admin,
                        decision(review, "changes_requested", "Improve the description"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("changes_requested"));
        mvc.perform(put("/api/schools/" + id + "/profile")
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profile()))
                .andExpect(status().isOk());
        postJson("/api/schools/" + id + "/submit", access, "{}").andExpect(status().isOk());
        postJson("/api/admin/schools/" + id + "/review", admin, decision(review, "approved", null))
                .andExpect(status().isConflict());
        postJson("/api/admin/schools/" + id + "/review", admin, decision(latestReview(id), "approved", null))
                .andExpect(status().isOk());
        postJson("/api/schools/" + id + "/branches/" + branch + "/open", access, "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("active"));
        mvc.perform(get("/api/workspaces/" + slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.canonical_slug").value(slug));
        mvc.perform(get(logo.path("content_url").asText()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
        // Additional branches need their own workspace, but no second school review.
        JsonNode extra = data(postJson("/api/schools/" + id + "/branches", access, branchBody("Second branch"))
                .andExpect(status().isCreated()));
        String extraId = extra.path("id").asText();
        postJson("/api/schools/" + id + "/branches/" + extraId + "/open", access, "{}")
                .andExpect(status().isBadRequest());
        claim(id, extraId, "branch-" + UUID.randomUUID(), access);
        postJson("/api/schools/" + id + "/branches/" + extraId + "/open", access, "{}")
                .andExpect(status().isOk());
        AuthResponse student = signup();
        UUID role = jdbc.sql("SELECT id FROM role WHERE code='student'")
                .query(UUID.class)
                .single();
        postJson(
                        "/api/schools/" + id + "/members/invite",
                        access,
                        "{\"user_id\":\"" + student.getUserId() + "\",\"role_id\":\"" + role + "\"}")
                .andExpect(status().isCreated());
        postJson("/api/admin/schools/" + id + "/suspend", admin, "{\"reason\":\"Compliance review\"}")
                .andExpect(status().isOk());
        mvc.perform(get("/api/workspaces/" + slug)).andExpect(status().isNotFound());
        mvc.perform(get("/api/schools/" + id).header("Authorization", "Bearer " + access))
                .andExpect(status().isForbidden());
        mvc.perform(get(cover.path("content_url").asText())).andExpect(status().isUnauthorized());
        postJson("/api/admin/schools/" + id + "/reinstate", admin, "{}").andExpect(status().isOk());
        mvc.perform(get("/api/workspaces/" + slug)).andExpect(status().isOk());
        assertThat(jdbc.sql("SELECT count(*) FROM school_review WHERE school_id=?")
                        .param(UUID.fromString(id))
                        .query(Long.class)
                        .single())
                .isEqualTo(2);
    }

    @Test
    void rejectsReservedAndRetiredNamesAndCrossSchoolWrites() throws Exception {
        AuthResponse owner = verified(), other = verified();
        String id = registerSchool(owner).path("id").asText();
        String otherId = registerSchool(other).path("id").asText();
        String branch = getData("/api/schools/" + id + "/onboarding", owner.getAccessToken())
                .path("default_branch")
                .path("id")
                .asText();
        String otherBranch = getData("/api/schools/" + otherId + "/onboarding", other.getAccessToken())
                .path("default_branch")
                .path("id")
                .asText();
        claimRequest(id, branch, "admin", owner.getAccessToken()).andExpect(status().isConflict());
        // Reserved names cannot be claimed.
        String first = "name-" + UUID.randomUUID();
        claim(id, branch, first, owner.getAccessToken());
        claim(id, branch, "renamed-" + UUID.randomUUID(), owner.getAccessToken());
        claimRequest(otherId, otherBranch, first, other.getAccessToken()).andExpect(status().isConflict());
        mvc.perform(multipart("/api/schools/" + id + "/logo")
                        .file(file())
                        .header("Authorization", "Bearer " + other.getAccessToken()))
                .andExpect(status().isForbidden());
        claimRequest(id, otherBranch, "bad-" + UUID.randomUUID(), owner.getAccessToken())
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsFakeImagesAndRecordsFailedStorageWithoutAttachingMedia() throws Exception {
        AuthResponse owner = verified();
        String id = registerSchool(owner).path("id").asText();
        mvc.perform(multipart("/api/schools/" + id + "/logo")
                        .file(new MockMultipartFile("file", "x.png", "image/png", "<svg/>".getBytes()))
                        .header("Authorization", "Bearer " + owner.getAccessToken()))
                .andExpect(status().isBadRequest());
        doThrow(new IllegalStateException("test storage failure"))
                .when(storage)
                .upload(anyString(), any(InputStream.class), anyString());
        try {
            mvc.perform(multipart("/api/schools/" + id + "/logo")
                            .file(file())
                            .header("Authorization", "Bearer " + owner.getAccessToken()))
                    .andExpect(status().isInternalServerError());
        } finally {
            doCallRealMethod().when(storage).upload(anyString(), any(InputStream.class), anyString());
        }
        assertThat(jdbc.sql("SELECT status FROM media_asset WHERE school_id=?")
                        .param(UUID.fromString(id))
                        .query(String.class)
                        .single())
                .isEqualTo("failed");
        assertThat(getData("/api/schools/" + id + "/onboarding", owner.getAccessToken())
                        .path("school")
                        .path("logo_asset_id")
                        .isNull())
                .isTrue();
    }

    @Test
    void failedSmtpDeliveryInvalidatesTheUnsentTokenAndAllowsRetry() throws Exception {
        AuthResponse user = signup();
        doThrow(new MailSendException("test mail failure")).when(mail).send(any(SimpleMailMessage.class));
        try {
            postJson("/api/me/email-verification", user.getAccessToken(), "{}")
                    .andExpect(status().isServiceUnavailable());
        } finally {
            doCallRealMethod().when(mail).send(any(SimpleMailMessage.class));
        }
        assertThat(jdbc.sql("SELECT count(*) FROM email_verification_token WHERE user_id=?")
                        .param(user.getUserId())
                        .query(Long.class)
                        .single())
                .isZero();
        assertThat(requestVerification(user)).hasSize(64);
    }

    @Test
    void expiredAndSupersededVerificationLinksAreRejected() throws Exception {
        AuthResponse user = signup();
        String old = requestVerification(user);
        jdbc.sql(
                        "UPDATE email_verification_token SET expires_at=now()-interval '1 minute',created_at=now()-interval '2 minutes' WHERE user_id=?")
                .param(user.getUserId())
                .update();
        postJson("/api/auth/verify-email", null, "{\"token\":\"" + old + "\"}").andExpect(status().isBadRequest());
        clearInvocations(mail);
        String next = requestVerification(user);
        postJson("/api/auth/verify-email", null, "{\"token\":\"" + old + "\"}").andExpect(status().isBadRequest());
        postJson("/api/auth/verify-email", null, "{\"token\":\"" + next + "\"}").andExpect(status().isOk());
    }

    @Test
    void rejectionIsTerminalAndOwnerCanStillReadTheReason() throws Exception {
        AuthResponse owner = verified();
        String id = completeDraft(owner.getAccessToken());
        postJson("/api/schools/" + id + "/submit", owner.getAccessToken(), "{}").andExpect(status().isOk());
        postJson(
                        "/api/admin/schools/" + id + "/review",
                        admin,
                        decision(latestReview(id), "rejected", "Registration could not be confirmed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("rejected"));
        postJson("/api/schools/" + id + "/submit", owner.getAccessToken(), "{}").andExpect(status().isBadRequest());
        postJson("/api/admin/schools/" + id + "/reinstate", admin, "{}").andExpect(status().isBadRequest());
        JsonNode mine = getData("/api/me/schools", owner.getAccessToken()).path("content");
        assertThat(mine.get(0).path("status_reason").asText()).isEqualTo("Registration could not be confirmed");
    }

    @Test
    void superAdminCannotReviewTheirOwnSchool() throws Exception {
        String id = completeDraft(admin);
        postJson("/api/schools/" + id + "/submit", admin, "{}").andExpect(status().isOk());
        postJson("/api/admin/schools/" + id + "/review", admin, decision(latestReview(id), "approved", null))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("REVIEW_CONFLICT_OF_INTEREST"));
    }

    @Test
    void concurrentConfirmationsConsumeTheTokenOnlyOnce() throws Exception {
        AuthResponse owner = signup();
        String token = requestVerification(owner);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var ready = new CountDownLatch(2);
            var go = new CountDownLatch(1);
            Callable<Integer> confirm = () -> {
                ready.countDown();
                if (!go.await(10, TimeUnit.SECONDS)) throw new AssertionError("Start timed out");
                return postJson("/api/auth/verify-email", null, "{\"token\":\"" + token + "\"}")
                        .andReturn()
                        .getResponse()
                        .getStatus();
            };
            var first = executor.submit(confirm);
            var second = executor.submit(confirm);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            go.countDown();
            assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 400);
        }
    }

    private String completeDraft(String token) throws Exception {
        String id = data(postJson("/api/schools", token, profile()).andExpect(status().isCreated()))
                .path("id")
                .asText();
        String branch = getData("/api/schools/" + id + "/onboarding", token)
                .path("default_branch")
                .path("id")
                .asText();
        configureBranch(id, branch, token);
        upload(id, "logo", token);
        upload(id, "cover", token);
        claim(id, branch, "ready-" + UUID.randomUUID(), token);
        return id;
    }

    private AuthResponse signup() throws Exception {
        JsonNode result = data(postJson(
                        "/api/auth/register",
                        null,
                        "{\"email\":\"" + UUID.randomUUID()
                                + "@example.test\",\"password\":\"long test passphrase for owner\",\"display_name\":\"Owner\"}")
                .andExpect(status().isCreated()));
        return json.treeToValue(result, AuthResponse.class);
    }

    private AuthResponse verified() throws Exception {
        AuthResponse user = signup();
        clearInvocations(mail);
        String token = requestVerification(user);
        postJson("/api/auth/verify-email", null, "{\"token\":\"" + token + "\"}")
                .andExpect(status().isOk());
        return user;
    }

    private String requestVerification(AuthResponse user) throws Exception {
        postJson("/api/me/email-verification", user.getAccessToken(), "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").doesNotExist());
        var captured = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail, atLeastOnce()).send(captured.capture());
        SimpleMailMessage message = captured.getAllValues().stream()
                .filter(m -> Arrays.asList(m.getTo()).contains(user.getEmail()))
                .reduce((a, c) -> c)
                .orElseThrow();
        var match = Pattern.compile("[?&]token=([0-9a-f]{64})").matcher(message.getText());
        assertThat(match.find()).isTrue();
        return match.group(1);
    }

    private JsonNode registerSchool(AuthResponse user) throws Exception {
        return data(postJson("/api/schools", user.getAccessToken(), profile()).andExpect(status().isCreated()));
    }

    private void configureBranch(String id, String branch, String token) throws Exception {
        mvc.perform(put("/api/schools/" + id + "/default-branch")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(branchBody("Main branch")))
                .andExpect(status().isOk());
    }

    private JsonNode upload(String id, String type, String token) throws Exception {
        return data(mvc.perform(multipart("/api/schools/" + id + "/" + type)
                        .file(file())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated()));
    }

    private void claim(String school, String branch, String slug, String token) throws Exception {
        claimRequest(school, branch, slug, token).andExpect(status().isOk());
    }

    private ResultActions claimRequest(String school, String branch, String slug, String token) throws Exception {
        return mvc.perform(put("/api/schools/" + school + "/branches/" + branch + "/workspace")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"slug\":\"" + slug + "\"}"));
    }

    private String latestReview(String id) throws Exception {
        return getData("/api/schools/" + id + "/reviews", admin)
                .get(0)
                .path("id")
                .asText();
    }

    private JsonNode getData(String url, String token) throws Exception {
        return data(
                mvc.perform(get(url).header("Authorization", "Bearer " + token)).andExpect(status().isOk()));
    }

    private ResultActions postJson(String url, String token, String body) throws Exception {
        var request = post(url).contentType(MediaType.APPLICATION_JSON).content(body);
        if (token != null) request.header("Authorization", "Bearer " + token);
        return mvc.perform(request);
    }

    private JsonNode data(ResultActions response) throws Exception {
        return json.readTree(response.andReturn().getResponse().getContentAsString())
                .get("data");
    }

    private static String profile() {
        return "{\"display_name\":\"School Example\",\"description\":\"A school profile\",\"contact_email\":\"school@example.test\"}";
    }

    private static String branchBody(String name) {
        return "{\"name\":\"" + name
                + "\",\"address_line\":\"12 Example Street\",\"province\":\"Phnom Penh\",\"country_code\":\"KH\",\"latitude\":11.5564,\"longitude\":104.9282,\"timezone\":\"Asia/Phnom_Penh\"}";
    }

    private static String decision(String id, String decision, String reason) {
        return "{\"review_id\":\"" + id + "\",\"decision\":\"" + decision + "\",\"reason\":"
                + (reason == null ? "null" : "\"" + reason + "\"") + "}";
    }

    private static MockMultipartFile file() throws Exception {
        var out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(8, 4, BufferedImage.TYPE_INT_RGB), "png", out);
        return new MockMultipartFile("file", "school.png", "image/png", out.toByteArray());
    }
}
