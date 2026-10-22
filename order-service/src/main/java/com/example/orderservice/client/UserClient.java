
package com.example.orderservice.client;

import com.example.orderservice.dto.AddressResponse;
import com.example.orderservice.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class UserClient {

    private final RestTemplate restTemplate;

    public AddressResponse getAddress(
            Long addressId,
            String token
    ) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);

            ResponseEntity<AddressResponse> response =
                    restTemplate.exchange(
                            "http://user-service/users/me/addresses/"
                                    + addressId,
                            HttpMethod.GET,
                            new HttpEntity<>(headers),
                            AddressResponse.class
                    );

            AddressResponse address = response.getBody();

            if (address == null) {
                throw new ResourceNotFoundException(
                        "Delivery address not found: " + addressId
                );
            }

            return address;

        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException(
                    "Delivery address not found: " + addressId
            );
        }
    }
}
