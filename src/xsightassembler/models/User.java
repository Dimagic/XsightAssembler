package xsightassembler.models;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import xsightassembler.utils.Strings;
import xsightassembler.utils.Utils;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "users")
public class User {

    public User() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Column(name = "first_name", length = 64, nullable = false)
    private String firstName;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @Column(name = "last_name", length = 64, nullable = false)
    private String lastName;

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @Column(name = "login", length = 64, nullable = false, unique = true)
    private String login;

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    @Column(name = "password", nullable = false)
    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = Utils.stringToHash(password);
    }

    @Column(name = "email")
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Column(name = "user_role", nullable = false, length = 1)
    private int userRole;

    public int getUserRole() {
        return userRole;
    }

    public String getUserRoleName() {
        return Strings.getUserRoleMap().get(userRole);
    }

    public void setUserRole(int userRole) {
        this.userRole = userRole;
    }

    @Column(name = "user_status", nullable = false, length = 1)
    private int userStatus;

    public int getUserStatus() {
        return userStatus;
    }

    public String getUserStatusName(){
        return getUserStatus() == 1 ? "Enabled": "Disabled";
    }

    public void setUserStatus(int userStatus) {
        this.userStatus = userStatus;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_login")
    private Date lastLogin;

    public Date getLastLogin() {
        return lastLogin;
    }

    public String getLastLoginString() {
        return Utils.getFormattedDate(getLastLogin());
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }
}
