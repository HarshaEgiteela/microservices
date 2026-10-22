
package com.example.userservice.controller;

import com.example.userservice.dto.AddressRequest;
import com.example.userservice.dto.AddressResponse;
import com.example.userservice.service.AddressService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users/me/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public List<AddressResponse> getAddresses(
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();

        return addressService.getAddresses(userId);
    }

    @GetMapping("/{addressId}")
    public AddressResponse getAddress(
            Authentication authentication,
            @PathVariable Long addressId
    ) {
        Long userId = (Long) authentication.getPrincipal();

        return addressService.getAddress(userId, addressId);
    }

    @PostMapping
    public ResponseEntity<AddressResponse> addAddress(
            Authentication authentication,
            @Valid @RequestBody AddressRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(addressService.addAddress(userId, request));
    }

    @PutMapping("/{addressId}")
    public AddressResponse updateAddress(
            Authentication authentication,
            @PathVariable Long addressId,
            @Valid @RequestBody AddressRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();

        return addressService.updateAddress(
                userId,
                addressId,
                request
        );
    }

    @PatchMapping("/{addressId}/default")
    public AddressResponse setDefault(
            Authentication authentication,
            @PathVariable Long addressId
    ) {
        Long userId = (Long) authentication.getPrincipal();

        return addressService.setDefault(userId, addressId);
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(
            Authentication authentication,
            @PathVariable Long addressId
    ) {
        Long userId = (Long) authentication.getPrincipal();

        addressService.deleteAddress(userId, addressId);

        return ResponseEntity.noContent().build();
    }
}