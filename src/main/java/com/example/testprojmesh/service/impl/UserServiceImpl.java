package com.example.testprojmesh.service.impl;

import com.example.testprojmesh.api.error.ApiException;
import com.example.testprojmesh.dao.entity.ContactEntity;
import com.example.testprojmesh.dao.entity.UserEntity;
import com.example.testprojmesh.dao.repository.UserRepository;
import com.example.testprojmesh.dao.repository.UserSpecifications;
import com.example.testprojmesh.service.UserService;
import com.example.testprojmesh.service.model.ContactView;
import com.example.testprojmesh.service.model.PageView;
import com.example.testprojmesh.service.model.ProfileView;
import com.example.testprojmesh.service.model.UserSearchCriteria;
import com.example.testprojmesh.service.model.UserSummary;
import com.example.testprojmesh.util.ContactNormalizer;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository users;

    @Override
    @Transactional(readOnly = true)
    public ProfileView profile(long viewerId, long userId) {
        UserEntity user = users.findById(userId)
            .orElseThrow(() -> ApiException.notFound("User not found: " + userId));
        return new ProfileView(
            user.getId(),
            user.getName(),
            user.getDateOfBirth(),
            contactViews(user.getEmails()),
            contactViews(user.getPhones()),
            viewerId == userId ? user.getAccount().getBalance() : null);
    }

    @Override
    @Transactional(readOnly = true)
    public PageView<UserSummary> search(UserSearchCriteria criteria, int page, int size) {
        Specification<UserEntity> spec = Specification.unrestricted();
        if (criteria.dateOfBirthAfter() != null) {
            spec = spec.and(UserSpecifications.bornAfter(criteria.dateOfBirthAfter()));
        }
        if (hasText(criteria.namePrefix())) {
            spec = spec.and(UserSpecifications.nameStartsWith(criteria.namePrefix().trim()));
        }
        if (hasText(criteria.email())) {
            spec = spec.and(UserSpecifications.hasEmail(ContactNormalizer.email(criteria.email())));
        }
        if (hasText(criteria.phone())) {
            spec = spec.and(UserSpecifications.hasPhone(ContactNormalizer.phone(criteria.phone())));
        }
        var result = users.findAll(spec, PageRequest.of(page, size, Sort.by("id")));
        return PageView.of(result.map(UserServiceImpl::summary));
    }

    private static List<ContactView> contactViews(List<? extends ContactEntity> contacts) {
        return contacts.stream().map(c -> new ContactView(c.getId(), c.getValue())).toList();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static UserSummary summary(UserEntity user) {
        return new UserSummary(
            user.getId(),
            user.getName(),
            user.getDateOfBirth(),
            contactViews(user.getEmails()),
            contactViews(user.getPhones()));
    }
}
