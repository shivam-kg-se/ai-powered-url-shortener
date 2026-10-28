package com.uss.service;

import com.uss.bindings.LoginRequest;
import com.uss.models.User;
import com.uss.repository.UserRepo;
import com.uss.security.jwt.JwtUtils;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserService {

    private UserRepo userRepository;
    private JwtUtils jwtUtils;
    private AuthenticationManager authenticationManager;

//    loginUser-> authenticateUser
    public JwtAuthenticationResponse loginUser(LoginRequest loginRequest){
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = authentication.getPrincipal() == null ? null : (UserDetailsImpl) authentication.getPrincipal();
        String jwt = jwtUtils.generateToken(userDetails);
        return new JwtAuthenticationResponse(jwt);
    }

    public User findByUsername(String name) {
        return userRepository
                .findByUsername(name)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + name));
    }
}
