package com.evdealer.evdealermanagement.dto.account.profile;

import java.time.LocalDate;

import com.evdealer.evdealermanagement.entity.account.Account;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class AccountUpdateRequest {

    @Size(max = 120, message = "Full name must not exceed 120 characters")
    private String fullName;

    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;

    private MultipartFile avatarUrl;

    @Pattern(regexp = "^(0|\\+84)[0-9]{9}$",
            message = "Phone must be 10 digits starting with 0, or +84 followed by 9 digits")
    private String phone;

    @Email(message = "Email format is invalid")
    private String email;

    @Size(max = 50, message = "National ID must not exceed 50 characters")
    private String nationalId;

    private Account.Gender gender;

    private LocalDate dateOfBirth;

    @Size(max = 50, message = "Tax code must not exceed 50 characters")
    private String taxCode;
}