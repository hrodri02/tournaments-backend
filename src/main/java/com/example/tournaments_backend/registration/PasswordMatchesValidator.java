package com.example.tournaments_backend.registration;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, Object> {
    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context){
        RegistrationRequest req = (RegistrationRequest) obj;
        if (req.getMatchingPassword() == null) {
            return false; // Avoid NullPointerException
        }

        boolean isValid = req.getPassword().equals(req.getMatchingPassword());

        if (!isValid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Passwords don't match")
                   .addPropertyNode("matchingPassword") // Attach the error to a specific field
                   .addConstraintViolation();
        }

        return isValid;
    }
}
