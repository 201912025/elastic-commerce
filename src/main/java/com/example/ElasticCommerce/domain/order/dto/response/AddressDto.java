package com.example.ElasticCommerce.domain.order.dto.response;

import com.example.ElasticCommerce.domain.order.entity.Address;

public record AddressDto(
        String recipientName,
        String street,
        String city,
        String postalCode,
        String phoneNumber
) {
    public static AddressDto from(Address a) {
        return new AddressDto(
                a.getRecipientName(),
                a.getStreet(),
                a.getCity(),
                a.getPostalCode(),
                a.getPhoneNumber()
        );
    }
}
