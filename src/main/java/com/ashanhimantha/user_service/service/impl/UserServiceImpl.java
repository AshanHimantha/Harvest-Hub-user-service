package com.ashanhimantha.user_service.service.impl;

import com.ashanhimantha.user_service.dto.request.AddressRequest;
import com.ashanhimantha.user_service.dto.request.CreateAdminUserRequest;
import com.ashanhimantha.user_service.dto.response.CognitoUserResponse;
import com.ashanhimantha.user_service.entity.Address;
import com.ashanhimantha.user_service.repository.AddressRepository;
import com.ashanhimantha.user_service.service.CognitoUserService;
import com.ashanhimantha.user_service.service.CognitoUserService.PaginatedUserResponse;
import com.ashanhimantha.user_service.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final CognitoUserService cognitoUserService;
    private final AddressRepository addressRepository;

    public UserServiceImpl(CognitoUserService cognitoUserService, AddressRepository addressRepository) {
        this.cognitoUserService = cognitoUserService;
        this.addressRepository = addressRepository;
    }

    // === Cognito User Management Implementations ===
    @Override
    public CognitoUserResponse getCognitoUserProfile(String username) {
        return cognitoUserService.getUserProfileByUsername(username);
    }

    @Override
    public PaginatedUserResponse getAllCognitoUsers(int limit, String nextToken) {
        return cognitoUserService.listUsers(limit, nextToken);
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


    // === Local Address Management Implementations ===
    @Override
    public Address addAddressForUser(String userId, AddressRequest addressRequest) {
        Address newAddress = new Address();
        newAddress.setUserId(userId);
        newAddress.setStreet(addressRequest.getStreet());
        newAddress.setCity(addressRequest.getCity());
        newAddress.setState(addressRequest.getState());
        newAddress.setPostalCode(addressRequest.getPostalCode());
        newAddress.setCountry(addressRequest.getCountry());
        return addressRepository.save(newAddress);
    }

    @Override
    public List<Address> getAddressesForUser(String userId) {
        return addressRepository.findByUserId(userId);
    }

    @Override
    public Optional<Address> updateUserAddress(String userId, Long addressId, AddressRequest addressRequest) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .map(addressToUpdate -> {
                    addressToUpdate.setStreet(addressRequest.getStreet());
                    addressToUpdate.setCity(addressRequest.getCity());
                    addressToUpdate.setState(addressRequest.getState());
                    addressToUpdate.setPostalCode(addressRequest.getPostalCode());
                    addressToUpdate.setCountry(addressRequest.getCountry());
                    return addressRepository.save(addressToUpdate);
                });
    }

    @Override
    @Transactional
    public boolean deleteUserAddress(String userId, Long addressId) {
        if (addressRepository.findByIdAndUserId(addressId, userId).isPresent()) {
            addressRepository.deleteById(addressId);
            return true;
        }
        return false;
    }

    @Override
    public List<CognitoUserResponse> searchCognitoUsersByEmail(String email) {
        return cognitoUserService.searchUsersByEmail(email);
    }
}