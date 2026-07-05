package io.chicaodw.platform.auth.application;

import io.chicaodw.platform.auth.domain.User;
import io.chicaodw.platform.auth.infrastructure.persistence.UserRepository;
import io.chicaodw.platform.common.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User create(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new BusinessRuleException("Email already in use: " + user.getEmail());
        }
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public List<User> findByCompanyId(UUID companyId) {
        return userRepository.findByCompanyId(companyId);
    }
}
