package fctreddit.api.data;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Represents a User in the system
 */
@Entity
public class User {
    private String email;
    @Id
    private String userId;
    private String fullName;
    private String password;
    private String avatarUrl;

    public User() {
    }

    public User(String userId, String fullName, String email, String password) {
        super();
        this.email = email;
        this.userId = userId;
        this.fullName = fullName;
        this.password = password;
        this.avatarUrl = null;
    }

    public User(String userId, String fullName, String email, String password, String avatarUrl) {
        this(userId, fullName, email, password);
        this.avatarUrl = avatarUrl;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((email == null) ? 0 : email.hashCode());
        result = prime * result + ((fullName == null) ? 0 : fullName.hashCode());
        result = prime * result + ((password == null) ? 0 : password.hashCode());
        result = prime * result + ((userId == null) ? 0 : userId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        User other = (User) obj;
        if (email == null) {
            if (other.email != null)
                return false;
        } else if (!email.equals(other.email))
            return false;
        if (fullName == null) {
            if (other.fullName != null)
                return false;
        } else if (!fullName.equals(other.fullName))
            return false;
        if (password == null) {
            if (other.password != null)
                return false;
        } else if (!password.equals(other.password))
            return false;
        if (userId == null) {
            if (other.userId != null)
                return false;
        } else if (!userId.equals(other.userId))
            return false;
        return true;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getAvatarUrl() {
        return this.avatarUrl;
    }

    @Override
    public String toString() {
        return "User [email=" + email + ", userId=" + userId + ", fullName=" + fullName + ", password=" + password
                + ", avatarUrl=" + avatarUrl + "]";
    }

    private boolean isStringValid(String string) {
        return string != null && !string.isBlank();
    }

    public boolean canBuild() {
        return (isStringValid(email) && isStringValid(userId) && isStringValid(fullName) && isStringValid(password));
    }

    public boolean canUpdateUser(User other) {
        return !isStringValid(other.getUserId());
    }

    public void updateUser(User other) {
        if (isStringValid(other.getEmail()))
            this.email = other.getEmail();
        if (isStringValid(other.getFullName()))
            this.fullName = other.getFullName();
        if (isStringValid(other.getPassword()))
            this.password = other.getPassword();
        if (isStringValid(other.getAvatarUrl()))
            this.avatarUrl = other.getAvatarUrl();
    }
}