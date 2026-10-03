package com.main.user;

public interface PasswordResetService {

    /**
     * Request a password reset. Generates a token and sends email/SMS with instructions.
     *
     * @param email User's email address
     * @return Confirmation message
     */
    String requestPasswordReset(String email);

    /**
     * Confirm password reset with the token and set the new password.
     *
     * @param token       Reset token
     * @param newPassword New password string
     */
    void confirmPasswordReset(String token, String newPassword);
}
