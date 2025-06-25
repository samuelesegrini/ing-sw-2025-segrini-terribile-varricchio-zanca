package it.polimi.ingsw.server.model.domain.ship;

import it.polimi.ingsw.server.model.enums.GameLevel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ShipValidationServiceTest {

    @Test

    void test(){

        ShipValidationService service;
        service = new ShipValidationService();

        List<String> errors;
        errors = new ArrayList<>();
        errors.add("error1");
        errors.add("error2");
        errors.add("error3");

        List<String> warnings;
        warnings = new ArrayList<>();
        warnings.add("warning1");
        warnings.add("warning2");
        warnings.add("warning3");

        ShipValidationService.ValidationResult result;
        result = new ShipValidationService.ValidationResult(true, errors, warnings);

        assertEquals(true, result.isValid());
        assertEquals(errors, result.getErrors());
        assertEquals(warnings, result.getWarnings());


    }

}