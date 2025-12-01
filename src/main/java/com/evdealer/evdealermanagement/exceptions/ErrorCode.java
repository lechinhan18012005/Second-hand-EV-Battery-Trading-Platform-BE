package com.evdealer.evdealermanagement.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.http.protocol.HTTP;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // --- Common Errors (Codes 400-9999) ---
    // Note: Internal 'code' must be unique, even if 'httpStatus' is the same.
    SUCCESS(200, "OK", HttpStatus.OK),
    BAD_REQUEST(400, "Bad request", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(401, "Invalid or expired token", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(403, "You do not have permission to access this resource", HttpStatus.FORBIDDEN),
    INVALID_CAPTCHA(404, "Invalid captcha", HttpStatus.BAD_REQUEST),
    INVALID_PHONE(405, "Invalid phone number", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL(406, "Invalid email", HttpStatus.BAD_REQUEST),

    // Adjusted 404 codes to ensure uniqueness for specific resource types
    USER_NOT_FOUND(4040, "User not found", HttpStatus.NOT_FOUND),
    PRODUCT_NOT_FOUND(4041, "Product not found", HttpStatus.NOT_FOUND),
    RESOURCE_NOT_FOUND(4042, "Requested resource not found", HttpStatus.NOT_FOUND),
    NOT_CURRENT_USER(4043, "Not current user", HttpStatus.FORBIDDEN),

    INTERNAL_ERROR(500, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE(503, "Service temporarily unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),

    // --- Login Errors (Codes 1000 - 1099) ---
    INVALID_CREDENTIALS(1001, "Invalid username or password", HttpStatus.UNAUTHORIZED),
    ACCOUNT_LOCKED(1002, "Your account has been locked", HttpStatus.FORBIDDEN),
    ACCOUNT_INACTIVE(1003, "Your account is not activated", HttpStatus.FORBIDDEN),
    TOO_MANY_ATTEMPTS(1004, "Too many failed login attempts. Please try again later", HttpStatus.TOO_MANY_REQUESTS),
    EMAIL_NOT_VERIFIED(1005, "Email not verified", HttpStatus.FORBIDDEN),

    // --- Register Errors (Codes 1100 - 1199) ---
    USERNAME_ALREADY_EXISTS(1101, "Username is already taken", HttpStatus.BAD_REQUEST),
    EMAIL_ALREADY_EXISTS(1102, "Email is already registered", HttpStatus.BAD_REQUEST),
    WEAK_PASSWORD(1103, "Password does not meet security requirements", HttpStatus.BAD_REQUEST),
    PASSWORDS_DO_NOT_MATCH(1104, "Passwords do not match", HttpStatus.BAD_REQUEST),
    PASSWORD_TOO_SHORT(1105, "Password must be at least 6 characters", HttpStatus.BAD_REQUEST),
    DUPLICATE_NATIONAL_ID(1106, "National ID is already registered", HttpStatus.BAD_REQUEST),
    DUPLICATE_TAX_CODE(1107, "Tax code is already registered", HttpStatus.BAD_REQUEST),
    PHONE_ALREADY_EXISTS(1108, "Phone number is already registered", HttpStatus.BAD_REQUEST),
    INVALID_PHONE_FORMAT(1109, "Invalid phone number format", HttpStatus.BAD_REQUEST),
    CONTRACT_BUILD_FAILED(1110, "Failed to build or create contract", HttpStatus.INTERNAL_SERVER_ERROR),

    // --- Validation Errors (Codes 1200 - 1299) ---
    INVALID_INPUT(1201, "Invalid input data", HttpStatus.BAD_REQUEST),
    MISSING_REQUIRED_FIELD(1202, "Missing required field", HttpStatus.BAD_REQUEST),
    INVALID_FORMAT(1203, "Invalid data format", HttpStatus.BAD_REQUEST),
    OUT_OF_RANGE(1204, "Value is out of allowed range", HttpStatus.BAD_REQUEST),
    MISSING_FULL_NAME(1205, "Full name is required", HttpStatus.BAD_REQUEST),
    EMAIL_REQUIRED(1206, "Email is required", HttpStatus.BAD_REQUEST),
    EMAIL_INVALID(1207, "Invalid email format", HttpStatus.BAD_REQUEST),
    PHONE_REQUIRED(1208, "Phone number is required", HttpStatus.BAD_REQUEST),
    PASSWORD_REQUIRED(1209, "Password is required", HttpStatus.BAD_REQUEST),
    PASSWORD_INVALID(1210,
            "Password must contain at least one uppercase letter, one lowercase letter, one digit, and be 8-32 characters long",
            HttpStatus.BAD_REQUEST),
    FULLNAME_INVALID_LENGTH(1211, "Full name must be between 4 and 50 characters", HttpStatus.BAD_REQUEST),

    // --- Security / Token Errors (Codes 1300 - 1399) ---
    INVALID_KEY(1301, "Invalid message key", HttpStatus.BAD_REQUEST),
    TOKEN_EXPIRED(1302, "Token has expired", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID(1303, "Token is invalid", HttpStatus.UNAUTHORIZED),
    TOKEN_MISSING(1304, "Token is missing", HttpStatus.UNAUTHORIZED),

    // --- File / Upload Errors (Codes 1400 - 1499) ---
    FILE_TOO_LARGE(1401, "Uploaded file is too large", HttpStatus.PAYLOAD_TOO_LARGE),
    UNSUPPORTED_FILE_TYPE(1402, "Unsupported file type", HttpStatus.BAD_REQUEST),
    FILE_UPLOAD_FAILED(1403, "File upload failed", HttpStatus.INTERNAL_SERVER_ERROR),

    // --- Payment / Transaction Errors (Codes 1500 - 1599) ---
    PAYMENT_FAILED(1501, "Payment processing failed", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_FUNDS(1502, "Insufficient balance", HttpStatus.BAD_REQUEST),
    TRANSACTION_DECLINED(1503, "Transaction was declined", HttpStatus.BAD_REQUEST),
    PRODUCT_NOT_DRAFT(1504, "Product is not in DRAFT state", HttpStatus.BAD_REQUEST),
    DURATION_DAYS_MORE_THAN_ZERO(1505, "Duration days must be more than 0", HttpStatus.BAD_REQUEST),
    PACKAGE_NOT_FOUND(1506, "Package not found", HttpStatus.NOT_FOUND),
    PAYMENT_NOT_FOUND(1507, "Payment not found", HttpStatus.NOT_FOUND),
    PACKAGE_INACTIVE(1508, "Package not found in DataBase", HttpStatus.NOT_FOUND),
    PACKAGE_OPTION_NOT_FOUND(1509, "Package option not found", HttpStatus.NOT_FOUND),
    PACKAGE_OPTION_NOT_BELONG_TO_PACKAGE(1510, "Package option not belong to backage", HttpStatus.BAD_REQUEST),
    PACKAGE_DAILY_PRICE_NOT_SET(1511, "Package daily price is not set", HttpStatus.BAD_REQUEST),
    PACKAGE_OPTION_REQUIRED(1512, "Package option is required", HttpStatus.BAD_REQUEST),
    PACKAGE_BILLING_MODE_INVALID(1513, "Package billing mode is invalid", HttpStatus.BAD_REQUEST),
    INVALID_STATUS_TRANSITION(1514, "Invalid Status Transition", HttpStatus.BAD_REQUEST),
    PACKAGE_INVALID_STATUS(1515, "Invalid Package Status", HttpStatus.BAD_REQUEST),
    INVALID_ID_PACKAGE(1516, "Invalid Id Package", HttpStatus.BAD_REQUEST),
    PURCHASE_REQUEST_NOT_FOUND(1550, "Purchase request not found", HttpStatus.NOT_FOUND),
    PAYMENT_NOT_RETRIABLE(1010, "Payment cannot be retried", HttpStatus.BAD_REQUEST),
    PRODUCT_NOT_PENDING_PAYMENT(1011, "Product is not pending payment", HttpStatus.BAD_REQUEST),
    PAYMENT_ALREADY_COMPLETED(1012, "Payment is already completed", HttpStatus.BAD_REQUEST),
    PAYMENT_METHOD_MISSING(1013, "Payment method is missing", HttpStatus.BAD_REQUEST),
    INVALID_PAYMENT_METHOD(1014, "Payment method is invalid", HttpStatus.BAD_REQUEST),
    PAYMENT_METHOD_UNSUPPORTED(1015, "Payment method is not supported", HttpStatus.BAD_REQUEST),
    PRODUCT_NOT_REJECTED(1016, "Product is not rejected", HttpStatus.BAD_REQUEST),

    // --- Wishlist Errors (Codes 1600 - 1699) ---
    WISHLIST_NOT_FOUND(1601, "Wishlist not found", HttpStatus.NOT_FOUND),
    DUPLICATE_RESOURCE(1602, "Resource already exists", HttpStatus.CONFLICT),
    LOCAL_CANNOT_CHANGE(1603, "Local account cannot change phone number", HttpStatus.BAD_REQUEST),
    PROVIDER_CANNOT_CHANGE(1604, "Provider cannot change email", HttpStatus.BAD_REQUEST),

    // --- Posting Errors (Codes 1700 - 1799) ---
    // Moved from conflicting 10xx range to dedicated 17xx range
    MIN_1_IMAGE(1701, "At least 1 image is required", HttpStatus.BAD_REQUEST),
    MAX_10_IMAGES(1702, "At most 10 images allowed", HttpStatus.BAD_REQUEST),
    IMAGE_TOO_LARGE(1703, "Image too large", HttpStatus.BAD_REQUEST),
    UNSUPPORTED_IMAGE_TYPE(1704, "Unsupported image type", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    IMAGE_UPLOAD_FAILED(1705, "Upload image failed", HttpStatus.INTERNAL_SERVER_ERROR),

    // --- Brand/Battery Errors (Codes 4000 - 4099) ---
    // Separated the conflicting 4004 code into unique codes 4000 and 4001
    BRAND_NOT_FOUND(4000, "Brand not found with the provided ID", HttpStatus.NOT_FOUND),
    BATT_NOT_FOUND(4001, "Battery Type Not Found", HttpStatus.NOT_FOUND),
    VEHICLE_CATE_NOT_FOUND(4002, "Vehicle Category Not Found", HttpStatus.NOT_FOUND),
    BRAND_EXISTS(4003, "Brand name already exists", HttpStatus.BAD_REQUEST),
    VERSION_NOT_FOUND(4004, "Version not found", HttpStatus.NOT_FOUND),
    MODEL_NOT_FOUND(4005, "Model name not found", HttpStatus.NOT_FOUND),
    TYPE_NOT_FOUND(4006, "Type not found", HttpStatus.NOT_FOUND),
    VEHICLE_NOT_FOUND(4007, "Vehicle not found", HttpStatus.NOT_FOUND),
    CATEGORY_NOT_FOUND(4008, "Category not found", HttpStatus.NOT_FOUND),
    BATTERY_NOT_FOUND(4009, "Battery Not Found", HttpStatus.NOT_FOUND),
    INVALID_COMPARE(4010, "Cannot compare different products type", HttpStatus.NOT_FOUND),
    INVALID_REQUEST(4011, "Unsupported product type", HttpStatus.BAD_REQUEST),
    NO_SUGGESTIONS_FOUND(4012, "No suggestions found", HttpStatus.NOT_FOUND),
    MODEL_NAME_REQUIRED(4013, "Model not found", HttpStatus.NOT_FOUND),
    VERSION_NAME_REQUIRED(4014, "Version Name Is Required", HttpStatus.BAD_REQUEST),
    BRAND_NAME_REQUIRED(4015, "Brand Name Is Required", HttpStatus.BAD_REQUEST),
    MODEL_EXISTS(4016, "Model Exists", HttpStatus.BAD_REQUEST),
    VERSION_EXISTS(4017, "Version Exists", HttpStatus.BAD_REQUEST),
    BATTERY_BRAND_NOT_FOUND(4018, "Battery Brand Not found", HttpStatus.NOT_FOUND),
    BATTERY_TYPE_NAME_REQUIRED(4019, "Battery Type Name is required", HttpStatus.BAD_REQUEST),

    REPORT_NOT_FOUND(4000, "Report not found", HttpStatus.NOT_FOUND),
    VEHICLE_CATEGORY_NOT_FOUND(4001, "Category not found", HttpStatus.NOT_FOUND),

    // --- OAuth2 / External Errors (Codes 1900 - 1999) ---
    UNSUPPORTED_OAUTH2_PROVIDER(1900, "Unsupported OAuth2 provider", HttpStatus.BAD_REQUEST),
    OAUTH2_GOOGLE_PROCESS_FAILED(1901, "Error processing Google login", HttpStatus.BAD_REQUEST),
    OAUTH2_FACEBOOK_PROCESS_FAILED(1902, "Error processing Facebook login", HttpStatus.BAD_REQUEST),

    // -NOTIFICATION (2000-3000)
    NO_READ_NOTIFICATIONS(2000, "Unread notifications", HttpStatus.BAD_REQUEST),

    // -APPROVAL (3100-3200)
    DATE_MUST_REQUIRED(3100, "Date is required", HttpStatus.BAD_REQUEST);



    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}