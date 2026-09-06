package com.aegorov.knowledgeplatform.documentservice.application.port;

import java.io.IOException;
import java.io.InputStream;

public interface FileStorage {

    //todo: implement s3 storage
    void store(String storageKey, InputStream content, long contentLength, String contentType) throws IOException;

    void delete(String storageKey) throws IOException;
}
