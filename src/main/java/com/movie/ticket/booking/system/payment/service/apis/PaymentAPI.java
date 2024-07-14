package com.movie.ticket.booking.system.payment.service.apis;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public class PaymentAPI {

    @GetMapping
    public String createPayments(){
        return "Hey! your payment was succesfull";
    }
}
