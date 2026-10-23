
package com.example.project.DTO.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 5926468583005150707L;

    private String email;
    private String password;
    private Boolean rememberMe;
}
