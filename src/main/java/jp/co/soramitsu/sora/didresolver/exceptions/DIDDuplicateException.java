package jp.co.soramitsu.sora.didresolver.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class DIDDuplicateException extends RuntimeException {

    public DIDDuplicateException(String did) {
        super("DID " + did + " is already registered");
    }
}