package com.uss.service;

import com.uss.models.User;
import com.uss.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepo userRepo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepo.findByUsername(username)
                .orElseThrow(()->new UsernameNotFoundException("user not found with usernaem  : "+username));

        return UserDetailsImpl.build(user);
    }

    public UserDetails loadUserById(Long id) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("user not found with id: " + id));
        return UserDetailsImpl.build(user);
    }
}
