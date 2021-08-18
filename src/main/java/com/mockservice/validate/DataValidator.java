package com.mockservice.validate;

public interface DataValidator {

    boolean applicable(String data);
    void validate(String data, String schema);
}
