package io.yec.fresno.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * User.java
 *
 * @author congye
 * @create 2016/06/30 5:28 PM
 */
@Data
@AllArgsConstructor(staticName = "createUser")
public class User implements Serializable {

    private static final long serialVersionUID = 4146658567517218423L;

    private Long id;

    private String name;

    private String password;

}
