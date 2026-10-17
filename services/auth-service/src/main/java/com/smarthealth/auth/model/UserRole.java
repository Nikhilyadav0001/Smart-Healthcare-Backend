package com.smarthealth.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "user_roles")
public class UserRole {

    @Id
    private UserRoleId id;

    protected UserRole() {
    }

    public UserRole(UserRoleId id) {
        this.id = id;
    }

    public UserRoleId getId() {
        return id;
    }

    public void setId(UserRoleId id) {
        this.id = id;
    }

    @jakarta.persistence.Embeddable
    public static class UserRoleId {

        @Column(name = "user_id", nullable = false)
        private UUID userId;

        @Column(nullable = false, length = 100)
        private String role;

        protected UserRoleId() {
        }

        public UserRoleId(UUID userId, String role) {
            this.userId = userId;
            this.role = role;
        }

        public UUID getUserId() {
            return userId;
        }

        public void setUserId(UUID userId) {
            this.userId = userId;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof UserRoleId that)) {
                return false;
            }
            return userId.equals(that.userId)
                    && role.equals(that.role);
        }

        @Override
        public int hashCode() {
            return 31 * userId.hashCode() + role.hashCode();
        }
    }
}