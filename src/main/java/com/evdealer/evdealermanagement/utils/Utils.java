package com.evdealer.evdealermanagement.utils;

import lombok.experimental.UtilityClass;
import java.util.regex.Pattern;

@UtilityClass
public class Utils {
        public static boolean isValidEmail(String email) {
            Pattern pattern = Pattern.compile(REGREX.EMAIL_REGEX);
            return email != null && pattern.matcher(email).matches();
        }

    public static String generateUsername(String phone, String fullName) {
        if (phone == null || fullName == null || phone.length() < 4) {
            throw new IllegalArgumentException("Invalid input");
        }

        // Lấy chữ cái đầu mỗi từ trong họ tên
        String[] parts = fullName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (String p : parts) {
            initials.append(Character.toUpperCase(p.charAt(0)));
        }

        // Lấy 4 số cuối điện thoại
        String lastDigits = phone.substring(phone.length() - 4);

        // Ghép lại
        return (initials.toString() + lastDigits).toLowerCase();
    }

    public boolean validatePhoneNumber(String phone) {
        if (phone == null || phone.isEmpty()) {
            return false;
        }
        return Pattern.matches(REGREX.PHONE_REGEX, phone);
    }
}
