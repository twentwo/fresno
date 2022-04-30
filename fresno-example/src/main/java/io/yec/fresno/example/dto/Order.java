package io.yec.fresno.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Order
 *
 * @author baijiu.yec
 * @since 2022/04/30
 */
@Data
@AllArgsConstructor(staticName = "createOrder")
public class Order implements Serializable {

    private static final long serialVersionUID = -7417668591949214343L;

    private Long id;

    private String orderNo;

    private String title;

//    private Date submitTime;

}
