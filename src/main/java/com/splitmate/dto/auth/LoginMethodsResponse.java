package com.splitmate.dto.auth;

public record LoginMethodsResponse(
        boolean passwordSet,
        boolean googleLinked) {

}
