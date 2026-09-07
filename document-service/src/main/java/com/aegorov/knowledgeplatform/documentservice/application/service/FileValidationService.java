package com.aegorov.knowledgeplatform.documentservice.application.service;

import java.io.IOException;
import java.util.function.BiFunction;

public interface FileValidationService<FILE, PROPS, RESULT> {

    void validate(BiFunction<FILE, PROPS, RESULT> validation, FILE file) throws IOException;
}
