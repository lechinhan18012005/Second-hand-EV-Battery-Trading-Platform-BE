package com.evdealer.evdealermanagement.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.core.MethodParameter;

import java.lang.reflect.Method;
import java.util.Set;

@Component
public class JsonValidationUtils {

    private static Validator validator;

    @Autowired
    public JsonValidationUtils(Validator validator) {
        JsonValidationUtils.validator = validator;
    }

    public static <T> T parseAndValidateJson(
            String jsonData,
            Class<T> clazz,
            Object controllerInstance,
            String methodName,
            Class<?>... paramTypes
    ) throws Exception {

        T request = BeanUtils.instantiateClass(clazz);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(request, clazz.getSimpleName());

        // 1) Parse JSON
        if (jsonData != null && !jsonData.isBlank()) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            try {
                request = mapper.readValue(jsonData, clazz);

            } catch (JsonMappingException e) {
                String fieldName = "data";
                if (!e.getPath().isEmpty()) {
                    fieldName = e.getPath().get(0).getFieldName();
                }
                bindingResult.addError(new FieldError(
                        clazz.getSimpleName(),
                        fieldName,
                        "Dữ liệu không hợp lệ cho trường '" + fieldName + "': " + e.getOriginalMessage()
                ));
                throwValidationException(controllerInstance, methodName, paramTypes, bindingResult);
            } catch (JsonProcessingException e) {
                bindingResult.addError(new FieldError(
                        clazz.getSimpleName(),
                        "data",
                        "JSON không hợp lệ: " + e.getOriginalMessage()
                ));
                throwValidationException(controllerInstance, methodName, paramTypes, bindingResult);
            }
        }

        // 2) Validate Bean
        Set<ConstraintViolation<T>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            for (ConstraintViolation<T> v : violations) {
                bindingResult.addError(new FieldError(
                        clazz.getSimpleName(),
                        v.getPropertyPath().toString(),
                        v.getMessage()
                ));
            }
            throwValidationException(controllerInstance, methodName, paramTypes, bindingResult);
        }

        return request;
    }

    private static void throwValidationException(
            Object controllerInstance,
            String methodName,
            Class<?>[] paramTypes,
            BeanPropertyBindingResult bindingResult
    ) throws Exception {
        Method method = controllerInstance.getClass().getMethod(methodName, paramTypes);
        MethodParameter param = new MethodParameter(method, 0);
        throw new MethodArgumentNotValidException(param, bindingResult);
    }
}
