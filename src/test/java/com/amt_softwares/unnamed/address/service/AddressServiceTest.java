package com.amt_softwares.unnamed.address.service;

import com.amt_softwares.unnamed.address.dto.AddressResponse;
import com.amt_softwares.unnamed.address.dto.CreateAddressRequest;
import com.amt_softwares.unnamed.address.dto.UpdateAddressRequest;
import com.amt_softwares.unnamed.address.entity.Address;
import com.amt_softwares.unnamed.address.repository.AddressRepository;
import com.amt_softwares.unnamed.user.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    private static final String USER_ROLE = "user";

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    private AddressService addressService;
    private UUID userId;

    @BeforeEach
    void setUp() {
        addressService = new AddressService(addressRepository, userRoleRepository);
        userId = UUID.randomUUID();
    }

    @Test
    void addAddressCreatesFirstAddressAsDefaultAndCleansInput() {
        CreateAddressRequest request = createRequest();
        request.setLabel("  Home  ");
        request.setAddressLine2("   ");

        when(userRoleRepository.existsByUserIdAndRole(userId, USER_ROLE)).thenReturn(true);
        when(addressRepository.existsByUserId(userId)).thenReturn(false);
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AddressResponse response = addressService.addAddress(userId, USER_ROLE, request);

        ArgumentCaptor<Address> addressCaptor = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository).save(addressCaptor.capture());
        Address savedAddress = addressCaptor.getValue();

        assertThat(savedAddress.getUserId()).isEqualTo(userId);
        assertThat(savedAddress.getLabel()).isEqualTo("Home");
        assertThat(savedAddress.getAddressLine1()).isEqualTo("221B Baker Street");
        assertThat(savedAddress.getAddressLine2()).isNull();
        assertThat(savedAddress.isDefaultAddress()).isTrue();
        assertThat(savedAddress.getCreatedAt()).isNotNull();
        assertThat(savedAddress.getUpdatedAt()).isNotNull();
        assertThat(response.isDefaultAddress()).isTrue();
        assertThat(response.getLabel()).isEqualTo("Home");
    }

    @Test
    void addAddressCreatesNonDefaultAddressWhenUserAlreadyHasAddresses() {
        CreateAddressRequest request = createRequest();

        when(userRoleRepository.existsByUserIdAndRole(userId, USER_ROLE)).thenReturn(true);
        when(addressRepository.existsByUserId(userId)).thenReturn(true);
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AddressResponse response = addressService.addAddress(userId, USER_ROLE, request);

        assertThat(response.isDefaultAddress()).isFalse();
    }

    @Test
    void addAddressRejectsNonUserAuthenticatedRole() {
        CreateAddressRequest request = createRequest();

        assertThatThrownBy(() -> addressService.addAddress(userId, "place", request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.FORBIDDEN);

        verify(addressRepository, never()).save(any());
    }

    @Test
    void addAddressRejectsUserWithoutStoredUserRole() {
        CreateAddressRequest request = createRequest();

        when(userRoleRepository.existsByUserIdAndRole(userId, USER_ROLE)).thenReturn(false);

        assertThatThrownBy(() -> addressService.addAddress(userId, USER_ROLE, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.FORBIDDEN);

        verify(addressRepository, never()).save(any());
    }

    @Test
    void updateAddressOnlyUpdatesOwnedAddressAndPreservesDefaultFlag() {
        UUID addressId = UUID.randomUUID();
        Address existingAddress = address(addressId, userId, true);
        UpdateAddressRequest request = updateRequest();
        request.setLabel("  Office  ");
        request.setAddressLine2("");

        when(userRoleRepository.existsByUserIdAndRole(userId, USER_ROLE)).thenReturn(true);
        when(addressRepository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.of(existingAddress));
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AddressResponse response = addressService.updateAddress(userId, USER_ROLE, addressId, request);

        assertThat(response.getLabel()).isEqualTo("Office");
        assertThat(response.getAddressLine1()).isEqualTo("Updated line");
        assertThat(response.getAddressLine2()).isNull();
        assertThat(response.isDefaultAddress()).isTrue();
        verify(addressRepository).findByIdAndUserId(addressId, userId);
    }

    @Test
    void updateAddressReturnsNotFoundWhenAddressDoesNotBelongToUser() {
        UUID addressId = UUID.randomUUID();

        when(userRoleRepository.existsByUserIdAndRole(userId, USER_ROLE)).thenReturn(true);
        when(addressRepository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.updateAddress(userId, USER_ROLE, addressId, updateRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(addressRepository, never()).save(any());
    }

    @Test
    void deleteAddressDeletesOnlyOwnedAddress() {
        UUID addressId = UUID.randomUUID();
        Address existingAddress = address(addressId, userId, false);

        when(userRoleRepository.existsByUserIdAndRole(userId, USER_ROLE)).thenReturn(true);
        when(addressRepository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.of(existingAddress));

        addressService.deleteAddress(userId, USER_ROLE, addressId);

        verify(addressRepository).delete(existingAddress);
    }

    @Test
    void setDefaultAddressClearsExistingDefaultAndSetsRequestedAddress() {
        UUID addressId = UUID.randomUUID();
        Address existingAddress = address(addressId, userId, false);

        when(userRoleRepository.existsByUserIdAndRole(userId, USER_ROLE)).thenReturn(true);
        when(addressRepository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.of(existingAddress));
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AddressResponse response = addressService.setDefaultAddress(userId, USER_ROLE, addressId);

        verify(addressRepository).clearDefaultForUser(userId);
        assertThat(response.isDefaultAddress()).isTrue();
    }

    @Test
    void getAddressesReturnsMappedAddresses() {
        Address defaultAddress = address(UUID.randomUUID(), userId, true);
        defaultAddress.setLabel("Home");
        Address otherAddress = address(UUID.randomUUID(), userId, false);
        otherAddress.setLabel("Work");

        when(userRoleRepository.existsByUserIdAndRole(userId, USER_ROLE)).thenReturn(true);
        when(addressRepository.findByUserIdOrderByDefaultAddressDescCreatedAtDesc(userId))
                .thenReturn(List.of(defaultAddress, otherAddress));

        List<AddressResponse> responses = addressService.getAddresses(userId, USER_ROLE);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getLabel()).isEqualTo("Home");
        assertThat(responses.get(0).isDefaultAddress()).isTrue();
        assertThat(responses.get(1).getLabel()).isEqualTo("Work");
        assertThat(responses.get(1).isDefaultAddress()).isFalse();
    }

    private CreateAddressRequest createRequest() {
        CreateAddressRequest request = new CreateAddressRequest();
        request.setAddressLine1("  221B Baker Street  ");
        request.setAddressLine2("Near park");
        request.setCity("  Bengaluru  ");
        request.setState("  Karnataka  ");
        request.setPincode("  560001  ");
        request.setLatitude(new BigDecimal("12.9716"));
        request.setLongitude(new BigDecimal("77.5946"));
        return request;
    }

    private UpdateAddressRequest updateRequest() {
        UpdateAddressRequest request = new UpdateAddressRequest();
        request.setAddressLine1("  Updated line  ");
        request.setAddressLine2("Updated line 2");
        request.setCity("  Mumbai  ");
        request.setState("  Maharashtra  ");
        request.setPincode("  400001  ");
        request.setLatitude(new BigDecimal("19.0760"));
        request.setLongitude(new BigDecimal("72.8777"));
        return request;
    }

    private Address address(UUID addressId, UUID ownerId, boolean defaultAddress) {
        return Address.builder()
                .id(addressId)
                .userId(ownerId)
                .label("Home")
                .addressLine1("Line 1")
                .addressLine2("Line 2")
                .city("City")
                .state("State")
                .pincode("123456")
                .latitude(new BigDecimal("10.0000"))
                .longitude(new BigDecimal("20.0000"))
                .defaultAddress(defaultAddress)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();
    }
}
