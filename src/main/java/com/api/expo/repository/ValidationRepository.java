package com.api.expo.repository;
import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.api.expo.models.User;
import com.api.expo.models.Validation;

public interface ValidationRepository extends CrudRepository <Validation, Integer> {
    
    Validation findByCode(String code);

    List<Validation> findByUser(User user);

    void deleteByUserAndActivationIsNull(User user);

}
