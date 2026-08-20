package fyp_grading_platform.auth;

import fyp_grading_platform.common.exception.BusinessException;
import fyp_grading_platform.user.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SsoAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final SsoLoginService sso;

    public SsoAuthenticationSuccessHandler(SsoLoginService sso) {
        this.sso = sso;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        try {
            User user = sso.requireImportedInternalUser((OAuth2User) authentication.getPrincipal());
            response.sendRedirect(sso.successRedirect(sso.issueExchangeCode(user)));
        } catch (BusinessException exception) {
            response.sendRedirect(sso.failureRedirect(exception.getErrorCode()));
        }
    }
}
