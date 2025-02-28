package com.api.expo.repository;

import org.springframework.data.repository.CrudRepository;

import com.api.expo.models.PasswordResetToken;

public interface PasswordResetTokenRepository extends CrudRepository<PasswordResetToken, Long> {


    
}
