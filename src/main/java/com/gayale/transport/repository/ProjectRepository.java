package com.gayale.transport.repository;

import com.gayale.transport.model.Project;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProjectRepository extends MongoRepository<Project, String> {
    List<Project> findByStatus(Project.ProjectStatus status);
    List<Project> findByClient(String client);
}
