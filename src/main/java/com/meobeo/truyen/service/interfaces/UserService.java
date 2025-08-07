package com.meobeo.truyen.service.interfaces;

import com.meobeo.truyen.domain.request.auth.ForgotPasswordDto;
import com.meobeo.truyen.domain.request.auth.OtpVerificationDto;
import com.meobeo.truyen.domain.request.auth.ResendOtpDto;
import com.meobeo.truyen.domain.request.auth.ResetPasswordDto;
import com.meobeo.truyen.domain.request.auth.UserRegistrationDto;
import com.meobeo.truyen.domain.response.auth.UserResponseDto;

public interface UserService {

    UserResponseDto registerUser(UserRegistrationDto registrationDto);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean verifyOtpAndActivateAccount(OtpVerificationDto otpVerificationDto);

    void resendOtp(ResendOtpDto resendOtpDto);

    void sendForgotPasswordOtp(ForgotPasswordDto forgotPasswordDto);

    boolean resetPassword(ResetPasswordDto resetPasswordDto);
}