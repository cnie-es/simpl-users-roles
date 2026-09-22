package eu.europa.ec.simpl.usersroles.models;

import java.util.List;
import org.springframework.data.domain.Pageable;

public record UserPage(Integer pageSize, Integer page, long total, List<User> items, Pageable pageable) {}
