package com.smarthealth.auth.service;

import com.smarthealth.auth.model.AuthenticatedUser;
import com.smarthealth.auth.model.User;
import com.smarthealth.auth.model.UserRole;
import com.smarthealth.auth.repository.UserRepository;
import com.smarthealth.auth.repository.UserRoleRepository;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    public AuthUserDetailsService(
            UserRepository userRepository,
            UserRoleRepository userRoleRepository) {

        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        User user = userRepository.findByEmailIgnoreCase(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found"
                        ));

        List<SimpleGrantedAuthority> authorities =
                userRoleRepository.findByIdUserId(user.getId())
                        .stream()
                        .map(UserRole::getId)
                        .map(UserRole.UserRoleId::getRole)
                        .map(role -> new SimpleGrantedAuthority(
                                "ROLE_" + role
                        ))
                        .toList();

        return new AuthenticatedUser(user, authorities);
    }
}