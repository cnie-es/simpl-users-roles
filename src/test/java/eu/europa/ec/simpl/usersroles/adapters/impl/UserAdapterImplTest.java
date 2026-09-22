package eu.europa.ec.simpl.usersroles.adapters.impl;

import static eu.europa.ec.simpl.common.test.TestUtil.a;
import static eu.europa.ec.simpl.common.test.TestUtil.aListOf;
import static eu.europa.ec.simpl.common.test.TestUtil.an;
import static eu.europa.ec.simpl.common.test.TestUtil.anUUID;
import static eu.europa.ec.simpl.usersroles.adapters.UserAdapter.*;
import static org.assertj.core.api.Assertions.*;
import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.*;

import eu.europa.ec.simpl.usersroles.adapters.RoleAdapter;
import eu.europa.ec.simpl.usersroles.adapters.UserAdapter;
import eu.europa.ec.simpl.usersroles.adapters.mappers.KeycloakMapperImpl;
import eu.europa.ec.simpl.usersroles.exceptions.KeycloakException;
import eu.europa.ec.simpl.usersroles.models.User;
import eu.europa.ec.simpl.usersroles.models.UserFilter;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.instancio.Instancio;
import org.instancio.Model;
import org.instancio.junit.InstancioSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Answers;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class UserAdapterImplTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    RealmResource realm;

    @Spy
    KeycloakMapperImpl keycloakMapper;

    @Mock
    RoleAdapter roleAdapter;

    @InjectMocks
    UserAdapterImpl adapter;

    @Test
    void createUser() throws URISyntaxException {

        // Given
        var expectedUserId = anUUID();
        var representation = an(UserRepresentation.class);
        representation.setId(expectedUserId.toString());
        given(realm.users().create(any(UserRepresentation.class)))
                .willReturn(Response.created(new URI("http://authority/" + expectedUserId))
                        .build());
        given(realm.users().get(expectedUserId.toString()).toRepresentation()).willReturn(representation);
        given(realm.users().get(representation.getId()).roles().realmLevel().listAll())
                .willReturn(aListOf(RoleRepresentation.class));
        // When
        var createUserOutcome = adapter.createUser(new CreateUserArgs.Default(a(User.class)));
        // Then
        then(realm.users()).should().create(any(UserRepresentation.class));

        // Then
        assertThat(createUserOutcome)
                .isInstanceOfSatisfying(
                        UserAdapter.CreateUserOutcome.Success.class,
                        outcome -> assertThat(outcome.user().getId()).isEqualTo(expectedUserId));
    }

    @Test
    void whenCreateUser_WhenKeycloakLocationIsInvalid_ShouldThrowIllegalStateException() throws URISyntaxException {

        given(realm.users().create(any(UserRepresentation.class)))
                .willReturn(Response.created(new URI("http://malformedUrl")).build());

        assertThatThrownBy(() -> adapter.createUser(new CreateUserArgs.Default(a(User.class))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createUserFail() {
        // When
        given(realm.users().create(any())).willReturn(Response.serverError().build());
        // Then
        assertThatThrownBy(() -> adapter.createUser(new CreateUserArgs.Default(a(User.class))))
                .isInstanceOf(KeycloakException.class);
    }

    @Test
    void getUserByEmail() {
        // Given
        var expectedUserEmail = Instancio.gen().net().email().get();
        var userRepresentation = a(UserRepresentation.class);
        userRepresentation.setId(anUUID().toString());
        userRepresentation.setEmail(expectedUserEmail);
        var roleRepresentations = aListOf(RoleRepresentation.class);
        given(realm.users().get(anyString()).roles().realmLevel().listAll()).willReturn(roleRepresentations);
        given(realm.users().searchByEmail(eq(expectedUserEmail), any())).willReturn(List.of(userRepresentation));

        // When
        var actual = adapter.getUserByEmail(expectedUserEmail);

        // Then
        assertThat(actual).isPresent();
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void getUserRoles(UUID id) {
        var roleRepresentations = Instancio.ofList(roleRepresentation()).create();
        when(realm.users().get(id.toString()).roles().realmLevel().listAll()).thenReturn(roleRepresentations);
        var actual = adapter.getUserRoles(id);

        assertThat(actual).hasSize(roleRepresentations.size());
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void getUserRolesUserNotFound(UUID id) {
        var usersResource = mock(UsersResource.class);

        when(realm.users()).thenReturn(usersResource);
        ClientErrorException clientErrorException = new ClientErrorException(Response.Status.BAD_REQUEST);
        when(usersResource.get(id.toString())).thenThrow(clientErrorException);

        assertThrows(KeycloakException.class, () -> adapter.getUserRoles(id));
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void getUserById(UUID id) {
        var ur = an(UserRepresentation.class);

        ur.setId(id.toString());
        var roleRepresentations = aListOf(RoleRepresentation.class);
        given(realm.users().get(anyString()).roles().realmLevel().listAll()).willReturn(roleRepresentations);
        given(realm.users().get(id.toString()).toRepresentation()).willReturn(ur);

        var actual = adapter.getUser(id);

        // Then
        assertThat(actual)
                .isPresent()
                .hasValueSatisfying(user -> assertThat(user.getId().toString()).isEqualTo(ur.getId()));
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void testGetUserByIdWhenUserNotFoundWillReturnOptionalEmpty(UUID id) {
        // Given
        var clientErrorException = generateClientErrorException();

        // Configure the mock to throw an exception when toRepresentation is called
        given(realm.users().get(id.toString())).willThrow(clientErrorException);

        // Then
        var actual = adapter.getUser(id);

        assertThat(actual).isEmpty();
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void updateUserNotFound(UUID id) {
        var userDTO = a(User.class);
        var clientErrorException = generateClientErrorException();
        given(realm.users().get(id.toString())).willThrow(clientErrorException);

        // Then
        assertThrows(KeycloakException.class, () -> adapter.updateUser(id, userDTO));
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void deleteUserNotFound(UUID id) {

        var clientErrorException = generateClientErrorException();
        when(realm.users().delete(id.toString())).thenThrow(clientErrorException);

        // Then
        assertThrows(KeycloakException.class, () -> adapter.deleteUser(id));
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void deleteUser_whenStatusIsNoContent_willNotThrowException(UUID id) {
        var usersResource = mock(UsersResource.class);
        var response = mock(Response.class);
        var status = Response.Status.NO_CONTENT.getStatusCode();
        given(response.getStatus()).willReturn(status);
        given(usersResource.delete(id.toString())).willReturn(response);
        given(realm.users()).willReturn(usersResource);

        assertDoesNotThrow(() -> adapter.deleteUser(id));
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void deleteUser_whenStatusNotNoConent_willThrowKeycloakException(UUID id) {

        var usersResource = mock(UsersResource.class);
        var response = mock(Response.class);
        var status = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        given(response.getStatus()).willReturn(status);
        given(usersResource.delete(id.toString())).willReturn(response);
        given(realm.users()).willReturn(usersResource);

        // Then
        assertThrows(KeycloakException.class, () -> adapter.deleteUser(id));
    }

    @Test
    void logout_success() {
        var sid = "junit-sid";
        willDoNothing().given(realm).deleteSession(eq(sid), anyBoolean());
        assertDoesNotThrow(() -> adapter.logout(new LogoutArgs.Default(sid)));
    }

    @Test
    void logout_willThrowClientException() {
        var exception = generateClientErrorException();
        var sid = "junit-sid";
        willThrow(exception).given(realm).deleteSession(any(), anyBoolean());

        assertThrows(KeycloakException.class, () -> adapter.logout(new LogoutArgs.Default(sid)));
    }

    @Test
    void search_svcAccountsAtStart() {

        var users = Instancio.ofList(userRepresentation()).size(20).create();
        List<User> validUsers = new ArrayList<>();
        var filter = new UserFilter();

        var usersSize = users.size();

        for (int i = 0; i < usersSize; i++) {

            if (i <= 4) {
                users.get(i).setUsername("service-account-" + i);
                users.get(i).setServiceAccountClientId("service-account-client-" + i);
            } else {
                users.get(i).setUsername("user-" + i);
                users.get(i).setServiceAccountClientId(null);
                validUsers.add(keycloakMapper.toUser(users.get(i)));
            }
        }

        var expectedContent = validUsers.stream().limit(10).toList();

        // Given
        var roleRepresentations = aListOf(2, roleRepresentation());
        given(realm.users().get(anyString()).roles().realmLevel().listAll()).willReturn(roleRepresentations);
        stubPagedSearch(users);

        // When
        var page = adapter.getUsers(filter, PageRequest.of(0, 10, Sort.unsorted()));

        assertThat(page.total()).isEqualTo(validUsers.size());
        assertThat(page.items()).hasSize(10);
        assertThat(page.page()).isZero();
        assertThat(page.pageSize()).isEqualTo(10);
        assertThat(page.items()).isEqualTo(expectedContent);
    }

    @Test
    void search_svcAccountsEmptyUsers() {

        var filter = new UserFilter();

        given(realm.users()
                        .search(
                                nullable(String.class),
                                nullable(String.class),
                                nullable(String.class),
                                nullable(String.class),
                                ArgumentMatchers.<Integer>any(),
                                ArgumentMatchers.<Integer>any(),
                                ArgumentMatchers.<Boolean>any(),
                                eq(false),
                                eq(false)))
                .willReturn(Collections.emptyList());

        var page = adapter.getUsers(filter, PageRequest.of(1, 10, Sort.unsorted()));

        assertThat(page.total()).isZero();
        assertThat(page.items()).isEmpty();
        assertThat(page.page()).isOne();
        assertThat(page.pageSize()).isEqualTo(10);
        assertThat(page.items()).isEqualTo(Collections.emptyList());
    }

    @Test
    void search_noSvcAccounts() {

        var users = Instancio.ofList(userRepresentation()).size(20).create();
        users.forEach(u -> u.setServiceAccountClientId(null));

        var filter = new UserFilter();

        var expectedContent =
                keycloakMapper.toUserList(users.stream().skip(10).limit(10).toList());

        // Given
        var roleRepresentations = aListOf(2, roleRepresentation());
        given(realm.users().get(anyString()).roles().realmLevel().listAll()).willReturn(roleRepresentations);
        stubPagedSearch(users);

        // When
        var page = adapter.getUsers(filter, PageRequest.of(1, 10, Sort.unsorted()));

        assertThat(page.total()).isEqualTo(users.size());
        assertThat(page.items()).hasSize(10);
        assertThat(page.page()).isOne();
        assertThat(page.pageSize()).isEqualTo(10);
        assertThat(page.items()).isEqualTo(expectedContent);
    }

    @Test
    void search_svcAccountsAtStartPage1() {

        var users = Instancio.ofList(userRepresentation()).size(30).create();
        List<User> validUsers = new ArrayList<>();
        var filter = new UserFilter();

        var usersSize = users.size();

        for (int i = 0; i < usersSize; i++) {

            if (i <= 15) {
                users.get(i).setUsername("service-account-" + i);
                users.get(i).setServiceAccountClientId("service-account-client-" + i);
            } else {
                users.get(i).setUsername("user-" + i);
                users.get(i).setServiceAccountClientId(null);
                validUsers.add(keycloakMapper.toUser(users.get(i)));
            }
        }

        var expectedContent = validUsers.stream().skip(10).limit(10).toList();

        // Given
        var roleRepresentations = aListOf(2, roleRepresentation());
        given(realm.users().get(anyString()).roles().realmLevel().listAll()).willReturn(roleRepresentations);
        stubPagedSearch(users);

        // When
        var page = adapter.getUsers(filter, PageRequest.of(1, 10, Sort.unsorted()));

        assertThat(page.total()).isEqualTo(validUsers.size());
        assertThat(page.items()).hasSize(4);
        assertThat(page.page()).isOne();
        assertThat(page.pageSize()).isEqualTo(10);
        assertThat(page.items()).isEqualTo(expectedContent);
    }

    @Test
    void search_svcAccountsAtEnd() {

        var users = Instancio.ofList(userRepresentation()).size(20).create();
        List<User> validUsers = new ArrayList<>();
        var filter = new UserFilter();

        var usersSize = users.size();

        for (int i = 0; i < usersSize; i++) {

            if (i <= 4 || i >= 10) {
                users.get(i).setUsername("user-" + i);
                users.get(i).setServiceAccountClientId(null);
                validUsers.add(keycloakMapper.toUser(users.get(i)));
            } else {
                users.get(i).setUsername("service-account-" + i);
                users.get(i).setServiceAccountClientId("service-account-client-" + i);
            }
        }

        var expectedContent = validUsers.stream().limit(10).toList();

        // Given
        var roleRepresentations = aListOf(2, roleRepresentation());
        given(realm.users().get(anyString()).roles().realmLevel().listAll()).willReturn(roleRepresentations);
        stubPagedSearch(users);

        // When
        var page = adapter.getUsers(filter, PageRequest.of(0, 10, Sort.unsorted()));

        assertThat(page.total()).isEqualTo(validUsers.size());
        assertThat(page.items()).hasSize(10);
        assertThat(page.page()).isZero();
        assertThat(page.pageSize()).isEqualTo(10);
        assertThat(page.items()).isEqualTo(expectedContent);
    }

    @Test
    void search_svcAccountsAtMiddlePage1() {

        var users = Instancio.ofList(userRepresentation()).size(30).create();
        List<User> validUsers = new ArrayList<>();
        var filter = new UserFilter();

        var usersSize = users.size();

        for (int i = 0; i < usersSize; i++) {

            if ((i >= 10 && i <= 12) || i >= 18) {
                users.get(i).setUsername("user-" + i);
                users.get(i).setServiceAccountClientId(null);
                validUsers.add(keycloakMapper.toUser(users.get(i)));
            } else {
                users.get(i).setUsername("service-account-" + i);
                users.get(i).setServiceAccountClientId("service-account-client-" + i);
            }
        }

        var expectedContent = validUsers.stream().skip(10).limit(10).toList();

        // Given
        var roleRepresentations = aListOf(2, roleRepresentation());
        given(realm.users().get(anyString()).roles().realmLevel().listAll()).willReturn(roleRepresentations);
        stubPagedSearch(users);

        // When
        var page = adapter.getUsers(filter, PageRequest.of(1, 10, Sort.unsorted()));

        assertThat(page.total()).isEqualTo(validUsers.size());
        assertThat(page.items()).hasSize(5);
        assertThat(page.page()).isOne();
        assertThat(page.pageSize()).isEqualTo(10);
        assertThat(page.items()).isEqualTo(expectedContent);
    }

    @Test
    void search_svcAccountsAtEnd1() {

        var users = Instancio.ofList(userRepresentation()).size(40).create();
        List<User> validUsers = new ArrayList<>();
        var filter = new UserFilter();

        var usersSize = users.size();

        for (int i = 0; i < usersSize; i++) {

            if (i <= 24) {
                users.get(i).setUsername("service-account-" + i);
                users.get(i).setServiceAccountClientId("service-account-client-" + i);
            } else {
                users.get(i).setUsername("user-" + i);
                users.get(i).setServiceAccountClientId(null);
                validUsers.add(keycloakMapper.toUser(users.get(i)));
            }
        }

        var expectedContent = validUsers.stream().skip(10).limit(10).toList();

        // Given
        var roleRepresentations = aListOf(2, roleRepresentation());
        given(realm.users().get(anyString()).roles().realmLevel().listAll()).willReturn(roleRepresentations);
        stubPagedSearch(users);

        // When
        var page = adapter.getUsers(filter, PageRequest.of(1, 10, Sort.unsorted()));

        assertThat(page.total()).isEqualTo(validUsers.size());
        assertThat(page.items()).hasSize(5);
        assertThat(page.page()).isOne();
        assertThat(page.pageSize()).isEqualTo(10);
        assertThat(page.items()).isEqualTo(expectedContent);
    }

    @Test
    void searchUnpaged() {

        var users = Instancio.ofList(userRepresentation()).size(20).create();
        List<User> validUsers = new ArrayList<>();
        var filter = new UserFilter();

        var usersSize = users.size();

        for (int i = 0; i < usersSize; i++) {

            if (i <= 4 || i >= 10) {
                users.get(i).setUsername("user-" + i);
                users.get(i).setServiceAccountClientId(null);
                validUsers.add(keycloakMapper.toUser(users.get(i)));
            } else {
                users.get(i).setUsername("service-account-" + i);
                users.get(i).setServiceAccountClientId("service-account-client-" + i);
            }
        }

        // Given
        var roleRepresentations = aListOf(2, roleRepresentation());
        given(realm.users().get(anyString()).roles().realmLevel().listAll()).willReturn(roleRepresentations);
        stubUnpagedSearch(users);

        // When
        var page = adapter.getUsers(filter, Pageable.unpaged());

        assertThat(page.total()).isEqualTo(validUsers.size());
        assertThat(page.items()).hasSize(validUsers.size());
        assertThat(page.items()).isEqualTo(validUsers);
    }

    private ClientErrorException generateClientErrorException() {
        return new ClientErrorException("Client error", Response.Status.BAD_REQUEST);
    }

    private Model<UserRepresentation> userRepresentation() {
        return Instancio.of(UserRepresentation.class)
                .generate(field(UserRepresentation::getId), gen -> gen.text().uuid())
                .toModel();
    }

    private Model<RoleRepresentation> roleRepresentation() {
        return Instancio.of(RoleRepresentation.class)
                .generate(field(RoleRepresentation::getId), gen -> gen.text().uuid())
                .toModel();
    }

    private void stubPagedSearch(List<UserRepresentation> users) {

        given(realm.users()
                        .search(
                                nullable(String.class),
                                nullable(String.class),
                                nullable(String.class),
                                nullable(String.class),
                                ArgumentMatchers.<Integer>any(),
                                ArgumentMatchers.<Integer>any(),
                                ArgumentMatchers.<Boolean>any(),
                                eq(false),
                                eq(false)))
                .willAnswer(inv -> {
                    Integer firstObj = inv.getArgument(4, Integer.class);
                    Integer maxObj = inv.getArgument(5, Integer.class);

                    int first = firstObj == null ? 0 : firstObj;
                    int max = maxObj == null ? users.size() : maxObj;

                    if (first < 0) first = 0;
                    if (max <= 0) return List.of();
                    if (first >= users.size()) return List.of();

                    int toIndex = Math.min(first + max, users.size());
                    return new ArrayList<>(users.subList(first, toIndex));
                });
    }

    private void stubSearchServiceAccounts(List<UserRepresentation> users) {

        given(realm.users()
                        .search(
                                eq("service-account-"),
                                nullable(String.class),
                                nullable(String.class),
                                nullable(String.class),
                                ArgumentMatchers.<Integer>any(),
                                ArgumentMatchers.<Integer>any(),
                                ArgumentMatchers.<Boolean>any(),
                                eq(false),
                                eq(false)))
                .willAnswer(inv -> {
                    List<UserRepresentation> svc = users.stream()
                            .filter(u -> u.getServiceAccountClientId() != null)
                            .toList();

                    Integer firstObj = inv.getArgument(4, Integer.class);
                    Integer maxObj = inv.getArgument(5, Integer.class);

                    int first = firstObj == null ? 0 : firstObj;
                    int max = maxObj == null ? svc.size() : maxObj;

                    if (first < 0) first = 0;
                    if (max <= 0) return List.of();
                    if (first >= svc.size()) return List.of();

                    int toIndex = Math.min(first + max, svc.size());
                    return new ArrayList<>(svc.subList(first, toIndex));
                });
    }

    private void stubUnpagedSearch(List<UserRepresentation> users) {

        given(realm.users()
                        .search(
                                nullable(String.class),
                                nullable(String.class),
                                nullable(String.class),
                                nullable(String.class),
                                ArgumentMatchers.<Integer>isNull(),
                                ArgumentMatchers.<Integer>isNull(),
                                ArgumentMatchers.<Boolean>any(),
                                eq(false),
                                eq(false)))
                .willReturn(new ArrayList<>(users));
    }

    record BenchmarkResult(String name, long avgMs, double avgSearchCalls) {}
}
