package com.ashanhimantha.user_service.service.impl;

import com.ashanhimantha.user_service.dto.request.AddressRequest;
import com.ashanhimantha.user_service.dto.request.CreateAdminUserRequest;
import com.ashanhimantha.user_service.dto.response.CognitoUserResponse;
import com.ashanhimantha.user_service.entity.Address;
import com.ashanhimantha.user_service.repository.AddressRepository;
import com.ashanhimantha.user_service.service.CognitoUserService;
import com.ashanhimantha.user_service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {


    private final CognitoUserService cognitoUserService;
    private final AddressRepository addressRepository;

    @Autowired
    public UserServiceImpl(CognitoUserService cognitoUserService, AddressRepository addressRepository) {

        this.cognitoUserService = cognitoUserService;
        this.addressRepository = addressRepository;
    }


        @Override
    public CognitoUserResponse getCognitoUserProfile(String userId) {
        Map<String, String> userAttributes = cognitoUserService.getUserAttributes(userId);
        List<String> userGroups = cognitoUserService.getUserGroups(userId);
        return CognitoUserResponse.fromCognitoAttributes(userId, userAttributes, userGroups);
    }

    @Override
    public List<CognitoUserResponse> getAllCognitoUsers(int page, int limit) {
        return cognitoUserService.listUsers(page, limit).stream()
                .map(CognitoUserResponse::fromUserType)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<CognitoUserResponse> searchUsersByEmail(String email) {
        return cognitoUserService.searchUsersByEmail(email).stream()
                .map(CognitoUserResponse::fromUserType)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public CognitoUserResponse createCognitoAdminUser(CreateAdminUserRequest request) {
        return cognitoUserService.createAdminUser(request);
    }

    @Override
    public void syncCognitoUserRoles(String username, List<String> newRoles) {
        cognitoUserService.syncUserRoles(username, newRoles);
    }

    @Override
    public void updateCognitoUserStatus(String username, boolean enable) {
        cognitoUserService.updateUserStatus(username, enable);
    }




    // Implement the 'add' method
    @Override
    public Address addAddressForUser(String userId, AddressRequest addressRequest) {
        // Create a new Address entity
        Address newAddress = new Address();

        // Set the userId directly from the JWT
        newAddress.setUserId(userId);

        // Map the rest of the fields from the request
        newAddress.setStreet(addressRequest.getStreet());
        newAddress.setCity(addressRequest.getCity());
        newAddress.setState(addressRequest.getState());
        newAddress.setPostalCode(addressRequest.getPostalCode());
        newAddress.setCountry(addressRequest.getCountry());

        // Save the new address to the database
        return addressRepository.save(newAddress);
    }

    // Implement the 'get' method
    @Override
    public List<Address> getAddressesForUser(String userId) {
        // We need to create a custom method in the repository for this
        return addressRepository.findByUserId(userId);
    }

    @Override
    public Optional<Address> updateUserAddress(String userId, Long addressId, AddressRequest addressRequest) {
        // 1. Find the address ONLY if it matches both the addressId and the userId.
        // This is a critical security check.
        Optional<Address> existingAddressOptional = addressRepository.findByIdAndUserId(addressId, userId);

        // 2. If the address doesn't exist or doesn't belong to the user, return empty.
        if (existingAddressOptional.isEmpty()) {
            return Optional.empty();
        }

        // 3. If it exists, update its fields.
        Address addressToUpdate = existingAddressOptional.get();
        addressToUpdate.setStreet(addressRequest.getStreet());
        addressToUpdate.setCity(addressRequest.getCity());
        addressToUpdate.setState(addressRequest.getState());
        addressToUpdate.setPostalCode(addressRequest.getPostalCode());
        addressToUpdate.setCountry(addressRequest.getCountry());

        // 4. Save the updated address to the database and return it.
        return Optional.of(addressRepository.save(addressToUpdate));
    }

}