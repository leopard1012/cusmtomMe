package com.custom.me.order.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column
    private String orderNumber;
    @Column
    private String orderCode;
    @Column
    private String orderDate;
    @Column
    private String orderStatus;
    @Column
    private String sendDate;
}
