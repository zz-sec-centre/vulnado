package com.scalesec.vulnado;

import org.springframework.boot.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.stereotype.*;
import org.springframework.beans.factory.annotation.*;
import java.io.Serializable;

@RestController
@EnableAutoConfiguration
public class LoginController {
  @Value("${app.secret}")
  private String secret;

  @CrossOrigin(origins = "*")
  @RequestMapping(value = "/login", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
}

class LoginRequest implements Serializable {
  public String username;
  public String password;
}

class LoginResponse implements Serializable {
  public String token;
  public LoginResponse(String msg) { this.token = msg; }
}

@ResponseStatus(HttpStatus.UNAUTHORIZED)
class Unauthorized extends RuntimeException {
  public Unauthorized(String exception) {
    super(exception);
  }
}
