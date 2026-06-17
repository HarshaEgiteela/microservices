package com.example.userservice.controller;

import com.example.userservice.entity.User;
import com.example.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository repository;

    @GetMapping
    public List<User> getAllUsers() {
        return repository.findAll();
    }

    @PostMapping
    public User saveUser(@RequestBody User user) {
        return repository.save(user);
    }

    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return repository.findById(id).orElse(null);
    }
}