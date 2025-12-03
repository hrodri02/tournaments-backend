package com.example.tournaments_backend.auth;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.transaction.annotation.Transactional;

import com.example.tournaments_backend.app_user.AppUser;
import com.example.tournaments_backend.app_user.AppUserRole;
import com.example.tournaments_backend.app_user.AppUserService;
import com.example.tournaments_backend.auth.tokens.confirmationToken.ConfirmationToken;
import com.example.tournaments_backend.auth.tokens.confirmationToken.ConfirmationTokenService;
import com.example.tournaments_backend.auth.tokens.resetToken.ResetToken;
import com.example.tournaments_backend.auth.tokens.resetToken.ResetTokenService;
import com.example.tournaments_backend.email.EmailSender;
import com.example.tournaments_backend.exception.ClientErrorKey;
import com.example.tournaments_backend.exception.ServiceException;
import com.example.tournaments_backend.player.Player;
import com.example.tournaments_backend.security.JwtService;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class AuthService {
    private final AppUserService appUserService;
    private final ConfirmationTokenService confirmationTokenService;
    private final ResetTokenService resetTokenService;
    private JwtService jwtService;
    private final EmailSender emailSender;
    private final AuthenticationManager authenticationManager;

    public String signUp(RegistrationRequest request) throws ServiceException {
        AppUser newUser;
        if (request.getRole().equals(AppUserRole.PLAYER)) {
            newUser = new Player(
                        request.getFirstName(), 
                        request.getLastName(),
                        request.getEmail(),
                        request.getPassword(),
                        request.getRole(),
                        request.getPosition());
        }
        else {
            newUser = new AppUser(
                        request.getFirstName(), 
                        request.getLastName(),
                        request.getEmail(),
                        request.getPassword(),
                        request.getRole());
        }
        String token = appUserService.signUp(newUser);

        String link = "http://localhost:8080/api/v1/auth/confirm?token=" + token;
        emailSender.send(
            "Confirm email",
            request.getEmail(), 
            buildEmail(request.getFirstName(), link)
        );

        return token;
    }

    public AppUser authenticate(AuthenticationRequest authRequest)  throws AuthenticationException, UsernameNotFoundException {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    authRequest.getEmail(),
                    authRequest.getPassword()
                )
        );

        return appUserService.getAppUserByEmail(authRequest.getEmail());
    }

    @Transactional
    public String confirmToken(String token) throws ServiceException {
        ConfirmationToken confirmationToken = confirmationTokenService
            .getToken(token)
            .orElseThrow(
                () -> new ServiceException(
                            // 404 NOT_FOUND for a resource (token) not found
                            HttpStatus.NOT_FOUND,
                            ClientErrorKey.CONFIRMATION_TOKEN_NOT_FOUND, 
                            "Auth", 
                            "Token not found."
            ));

        if (confirmationToken.getConfirmedAt() != null) {
            throw new ServiceException(
                        // 400 BAD_REQUEST for a business logic violation
                        HttpStatus.BAD_REQUEST,
                        ClientErrorKey.EMAIL_ALREADY_CONFIRMED, 
                        "App user",
                        "Email is already confirmed.");
        }

        LocalDateTime expiredAt = confirmationToken.getExpiresAt();
        if (expiredAt.isBefore(LocalDateTime.now())) {
            throw new ServiceException(
                // 401 UNAUTHORIZED for an expired token
                HttpStatus.UNAUTHORIZED,
                ClientErrorKey.TOKEN_EXPIRED, 
                "Auth", 
                "Token expired. Please request a new confirmation email.");
        }

        confirmationTokenService.setConfirmedAt(token);
        AppUser user = confirmationToken.getAppUser();
        appUserService.enableAppUser(user.getEmail());

        // create jwt with username and user roles
        return generateJWS(user);
    }

    public String generateJWS(AppUser user) {
        return jwtService.createToken(user.getEmail(), user.getAppUserRole());
    }

    public void resendEmail(String email) throws UsernameNotFoundException {
        AppUser appUser = appUserService.getAppUserByEmail(email);
        if (appUser.isEnabled()) {
            throw new ServiceException(
                HttpStatus.BAD_REQUEST,
                ClientErrorKey.EMAIL_ALREADY_CONFIRMED, 
                "App user",
                "Email is already confirmed.");
        }
        else {
            String token = appUserService.generateNewTokenForUser(appUser);
            String link = "http://localhost:8080/api/v1/auth/confirm?token=" + token;
            emailSender.send(
                "Confirm email",
                email, 
                buildEmail(appUser.getFirstName(), link)
            );
        }
    }

    public void sendResetPasswordEmail(String email) {
        AppUser appUser = appUserService.getAppUserByEmail(email);
        String resetToken = appUserService.generateNewResetTokenForUser(appUser);
        String link = "http://localhost:8080/api/v1/auth/reset-password?token=" + resetToken + "&email=" + email;
        emailSender.send(
            "Reset password",
            email, 
            buildResetPasswordEmail(appUser.getFirstName(), link)
        );
    }

    @Transactional
    public void validateResetToken(String token, String email) throws ServiceException {
        AppUser appUser = appUserService.getAppUserByEmail(email);
        ResetToken resetToken = resetTokenService
            .getToken(token, appUser)
            .orElseThrow(() -> new ServiceException(
                HttpStatus.NOT_FOUND, 
                ClientErrorKey.RESET_TOKEN_NOT_FOUND, 
                "Auth", 
                "Token not found."
            ));

        if (resetToken.getResetAt() != null) {
            throw new ServiceException(
                HttpStatus.BAD_REQUEST,
                ClientErrorKey.PASSWORD_ALREADY_RESET, 
                "App user",
                "Your password was already reset.");
        }

        LocalDateTime expiredAt = resetToken.getExpiresAt();
        if (expiredAt.isBefore(LocalDateTime.now())) {
            throw new ServiceException(
                HttpStatus.UNAUTHORIZED, 
                ClientErrorKey.TOKEN_EXPIRED, 
                "Auth", 
                "Token expired. Please request a new reset password email.");
        }
    }

    @Transactional
    public void saveUsersNewPassword(String token, String email, String newPassword) {
        appUserService.saveNewPassword(email, newPassword);
        resetTokenService.setResetAt(token);
    }

    private String buildEmail(String name, String link) {
        return "<div style=\"font-family:Helvetica,Arial,sans-serif;font-size:16px;margin:0;color:#0b0c0c\">\n" +
                "\n" +
                "<span style=\"display:none;font-size:1px;color:#fff;max-height:0\"></span>\n" +
                "\n" +
                "  <table role=\"presentation\" width=\"100%\" style=\"border-collapse:collapse;min-width:100%;width:100%!important\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">\n" +
                "    <tbody><tr>\n" +
                "      <td width=\"100%\" height=\"53\" bgcolor=\"#0b0c0c\">\n" +
                "        \n" +
                "        <table role=\"presentation\" width=\"100%\" style=\"border-collapse:collapse;max-width:580px\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" align=\"center\">\n" +
                "          <tbody><tr>\n" +
                "            <td width=\"70\" bgcolor=\"#0b0c0c\" valign=\"middle\">\n" +
                "                <table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse\">\n" +
                "                  <tbody><tr>\n" +
                "                    <td style=\"padding-left:10px\">\n" +
                "                  \n" +
                "                    </td>\n" +
                "                    <td style=\"font-size:28px;line-height:1.315789474;Margin-top:4px;padding-left:10px\">\n" +
                "                      <span style=\"font-family:Helvetica,Arial,sans-serif;font-weight:700;color:#ffffff;text-decoration:none;vertical-align:top;display:inline-block\">Confirm your email</span>\n" +
                "                    </td>\n" +
                "                  </tr>\n" +
                "                </tbody></table>\n" +
                "              </a>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "        </tbody></table>\n" +
                "        \n" +
                "      </td>\n" +
                "    </tr>\n" +
                "  </tbody></table>\n" +
                "  <table role=\"presentation\" class=\"m_-6186904992287805515content\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse;max-width:580px;width:100%!important\" width=\"100%\">\n" +
                "    <tbody><tr>\n" +
                "      <td width=\"10\" height=\"10\" valign=\"middle\"></td>\n" +
                "      <td>\n" +
                "        \n" +
                "                <table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse\">\n" +
                "                  <tbody><tr>\n" +
                "                    <td bgcolor=\"#1D70B8\" width=\"100%\" height=\"10\"></td>\n" +
                "                  </tr>\n" +
                "                </tbody></table>\n" +
                "        \n" +
                "      </td>\n" +
                "      <td width=\"10\" valign=\"middle\" height=\"10\"></td>\n" +
                "    </tr>\n" +
                "  </tbody></table>\n" +
                "\n" +
                "\n" +
                "\n" +
                "  <table role=\"presentation\" class=\"m_-6186904992287805515content\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse;max-width:580px;width:100%!important\" width=\"100%\">\n" +
                "    <tbody><tr>\n" +
                "      <td height=\"30\"><br></td>\n" +
                "    </tr>\n" +
                "    <tr>\n" +
                "      <td width=\"10\" valign=\"middle\"><br></td>\n" +
                "      <td style=\"font-family:Helvetica,Arial,sans-serif;font-size:19px;line-height:1.315789474;max-width:560px\">\n" +
                "        \n" +
                "            <p style=\"Margin:0 0 20px 0;font-size:19px;line-height:25px;color:#0b0c0c\">Hi " + name + ",</p><p style=\"Margin:0 0 20px 0;font-size:19px;line-height:25px;color:#0b0c0c\"> Thank you for registering. Please click on the below link to activate your account: </p><blockquote style=\"Margin:0 0 20px 0;border-left:10px solid #b1b4b6;padding:15px 0 0.1px 15px;font-size:19px;line-height:25px\"><p style=\"Margin:0 0 20px 0;font-size:19px;line-height:25px;color:#0b0c0c\"> <a href=\"" + link + "\">Activate Now</a> </p></blockquote>\n Link will expire in 15 minutes. <p>See you soon</p>" +
                "        \n" +
                "      </td>\n" +
                "      <td width=\"10\" valign=\"middle\"><br></td>\n" +
                "    </tr>\n" +
                "    <tr>\n" +
                "      <td height=\"30\"><br></td>\n" +
                "    </tr>\n" +
                "  </tbody></table><div class=\"yj6qo\"></div><div class=\"adL\">\n" +
                "\n" +
                "</div></div>";
    }

    private String buildResetPasswordEmail(String name, String link) {
        return "<div style=\"font-family:Helvetica,Arial,sans-serif;font-size:16px;margin:0;color:#0b0c0c\">\n" +
                "\n" +
                "<span style=\"display:none;font-size:1px;color:#fff;max-height:0\"></span>\n" +
                "\n" +
                "  <table role=\"presentation\" width=\"100%\" style=\"border-collapse:collapse;min-width:100%;width:100%!important\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">\n" +
                "    <tbody><tr>\n" +
                "      <td width=\"100%\" height=\"53\" bgcolor=\"#0b0c0c\">\n" +
                "        \n" +
                "        <table role=\"presentation\" width=\"100%\" style=\"border-collapse:collapse;max-width:580px\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" align=\"center\">\n" +
                "          <tbody><tr>\n" +
                "            <td width=\"70\" bgcolor=\"#0b0c0c\" valign=\"middle\">\n" +
                "                <table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse\">\n" +
                "                  <tbody><tr>\n" +
                "                    <td style=\"padding-left:10px\">\n" +
                "                  \n" +
                "                    </td>\n" +
                "                    <td style=\"font-size:28px;line-height:1.315789474;Margin-top:4px;padding-left:10px\">\n" +
                "                      <span style=\"font-family:Helvetica,Arial,sans-serif;font-weight:700;color:#ffffff;text-decoration:none;vertical-align:top;display:inline-block\">Reset Password</span>\n" +
                "                    </td>\n" +
                "                  </tr>\n" +
                "                </tbody></table>\n" +
                "              </a>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "        </tbody></table>\n" +
                "        \n" +
                "      </td>\n" +
                "    </tr>\n" +
                "  </tbody></table>\n" +
                "  <table role=\"presentation\" class=\"m_-6186904992287805515content\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse;max-width:580px;width:100%!important\" width=\"100%\">\n" +
                "    <tbody><tr>\n" +
                "      <td width=\"10\" height=\"10\" valign=\"middle\"></td>\n" +
                "      <td>\n" +
                "        \n" +
                "                <table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse\">\n" +
                "                  <tbody><tr>\n" +
                "                    <td bgcolor=\"#1D70B8\" width=\"100%\" height=\"10\"></td>\n" +
                "                  </tr>\n" +
                "                </tbody></table>\n" +
                "        \n" +
                "      </td>\n" +
                "      <td width=\"10\" valign=\"middle\" height=\"10\"></td>\n" +
                "    </tr>\n" +
                "  </tbody></table>\n" +
                "\n" +
                "\n" +
                "\n" +
                "  <table role=\"presentation\" class=\"m_-6186904992287805515content\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse;max-width:580px;width:100%!important\" width=\"100%\">\n" +
                "    <tbody><tr>\n" +
                "      <td height=\"30\"><br></td>\n" +
                "    </tr>\n" +
                "    <tr>\n" +
                "      <td width=\"10\" valign=\"middle\"><br></td>\n" +
                "      <td style=\"font-family:Helvetica,Arial,sans-serif;font-size:19px;line-height:1.315789474;max-width:560px\">\n" +
                "        \n" +
                "            <p style=\"Margin:0 0 20px 0;font-size:19px;line-height:25px;color:#0b0c0c\">Hi " + name + ",</p><p style=\"Margin:0 0 20px 0;font-size:19px;line-height:25px;color:#0b0c0c\"> Please click on the below link to reset your password: </p><blockquote style=\"Margin:0 0 20px 0;border-left:10px solid #b1b4b6;padding:15px 0 0.1px 15px;font-size:19px;line-height:25px\"><p style=\"Margin:0 0 20px 0;font-size:19px;line-height:25px;color:#0b0c0c\"> <a href=\"" + link + "\">Reset Password</a> </p></blockquote>\n Link will expire in 15 minutes. <p>See you soon</p>" +
                "        \n" +
                "      </td>\n" +
                "      <td width=\"10\" valign=\"middle\"><br></td>\n" +
                "    </tr>\n" +
                "    <tr>\n" +
                "      <td height=\"30\"><br></td>\n" +
                "    </tr>\n" +
                "  </tbody></table><div class=\"yj6qo\"></div><div class=\"adL\">\n" +
                "\n" +
                "</div></div>";
    }
}