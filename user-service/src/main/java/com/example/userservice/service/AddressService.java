
package com.example.userservice.service;

import com.example.userservice.dto.AddressRequest;
import com.example.userservice.dto.AddressResponse;
import com.example.userservice.entity.Address;
import com.example.userservice.entity.User;
import com.example.userservice.exception.ResourceNotFoundException;
import com.example.userservice.repository.AddressRepository;
import com.example.userservice.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AddressResponse> getAddresses(Long userId) {
        return addressRepository
                .findByUserIdOrderByIdAsc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AddressResponse getAddress(
            Long userId,
            Long addressId
    ) {
        return toResponse(findAddress(userId, addressId));
    }

    @Transactional
    public AddressResponse addAddress(
            Long userId,
            AddressRequest request
    ) {
        User user = lockAndFindUser(userId);

        Address address = new Address();
        address.setUser(user);
        copyFields(address, request);

        boolean firstAddress =
                addressRepository.countByUserId(userId) == 0;

        address.setDefaultAddress(firstAddress);

        return toResponse(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse updateAddress(
            Long userId,
            Long addressId,
            AddressRequest request
    ) {
        Address address = findAddress(userId, addressId);

        copyFields(address, request);

        return toResponse(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse setDefault(
            Long userId,
            Long addressId
    ) {
        lockAndFindUser(userId);

        Address selected = findAddress(userId, addressId);

        List<Address> addresses =
                addressRepository.findByUserIdOrderByIdAsc(userId);

        for (Address address : addresses) {
            address.setDefaultAddress(
                    address.getId().equals(selected.getId())
            );
        }

        addressRepository.saveAll(addresses);

        return toResponse(selected);
    }

    @Transactional
    public void deleteAddress(
            Long userId,
            Long addressId
    ) {
        lockAndFindUser(userId);

        Address address = findAddress(userId, addressId);

        boolean wasDefault = address.isDefaultAddress();

        addressRepository.delete(address);
        addressRepository.flush();

        if (wasDefault) {
            List<Address> remaining =
                    addressRepository.findByUserIdOrderByIdAsc(userId);

            if (!remaining.isEmpty()) {
                remaining.get(0).setDefaultAddress(true);
            }
        }
    }

    private User lockAndFindUser(Long userId) {
        addressRepository.lockUser(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );

        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );
    }

    private Address findAddress(
            Long userId,
            Long addressId
    ) {
        return addressRepository
                .findByIdAndUserId(addressId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Address not found"
                        )
                );
    }

    private void copyFields(
            Address address,
            AddressRequest request
    ) {
        address.setLabel(request.label().trim());
        address.setRecipientName(request.recipientName().trim());
        address.setPhone(request.phone().trim());
        address.setAddressLine1(request.addressLine1().trim());
        address.setAddressLine2(request.addressLine2());
        address.setCity(request.city().trim());
        address.setState(request.state().trim());
        address.setPostalCode(request.postalCode().trim());
        address.setCountry(request.country().trim());
    }

    private AddressResponse toResponse(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getLabel(),
                address.getRecipientName(),
                address.getPhone(),
                address.getAddressLine1(),
                address.getAddressLine2(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getCountry(),
                address.isDefaultAddress()
        );
    }
}