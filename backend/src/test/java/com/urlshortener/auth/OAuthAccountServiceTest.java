package com.urlshortener.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.urlshortener.exception.InvalidCredentialsException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OAuthAccountServiceTest {

    @Mock
    private UserRepository userRepository;

    private OAuthAccountService service;

    @BeforeEach
    void setUp() {
        service = new OAuthAccountService(userRepository);
    }

    @Test
    void createsOAuthUserWhenNew() {
        when(userRepository.findByAuthProviderAndProviderSubject(AuthProvider.GOOGLE, "sub-1"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = service.findOrCreate(AuthProvider.GOOGLE, "sub-1", "User@Example.com");

        assertThat(user.getEmail()).isEqualTo("user@example.com");
        assertThat(user.getAuthProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(user.getProviderSubject()).isEqualTo("sub-1");
        assertThat(user.getPasswordHash()).isNull();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getAuthProvider()).isEqualTo(AuthProvider.GOOGLE);
    }

    @Test
    void returnsExistingByProviderSubject() {
        User existing = User.oauth(UUID.randomUUID(), "user@example.com", AuthProvider.GITHUB, "99");
        when(userRepository.findByAuthProviderAndProviderSubject(AuthProvider.GITHUB, "99"))
                .thenReturn(Optional.of(existing));

        User user = service.findOrCreate(AuthProvider.GITHUB, "99", "user@example.com");

        assertThat(user).isSameAs(existing);
    }

    @Test
    void linksByEmailWhenProviderSubjectUnknown() {
        User local = new User(UUID.randomUUID(), "user@example.com", "hashed");
        when(userRepository.findByAuthProviderAndProviderSubject(AuthProvider.GOOGLE, "sub-2"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(local));

        User user = service.findOrCreate(AuthProvider.GOOGLE, "sub-2", "user@example.com");

        assertThat(user).isSameAs(local);
    }

    @Test
    void rejectsMissingEmail() {
        assertThatThrownBy(() -> service.findOrCreate(AuthProvider.GOOGLE, "sub-1", " "))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
