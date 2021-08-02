package xsightassembler.models;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import xsightassembler.utils.Utils;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "history")
public class History implements Comparable<History>{

    public History() {
        this.date = new Date();
    }

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date date;

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getStringDate(){
        return Utils.getFormattedDate(getDate());
    }

    @Column(nullable = false, length = 128)
    private String fieldChange;

    public String getFieldChange() {
        return fieldChange;
    }

    public void setFieldChange(String fieldChange) {
        this.fieldChange = fieldChange;
    }

    @Column(nullable = false, length = 128)
    private String oldValue;

    public String getOldValue() {
        if (oldValue == null){
            return "";
        }
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    @Column(length = 128)
    private String newValue;

    public String getNewValue() {
        if (newValue == null){
            return "";
        }
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    @Column(name = "comment")
    private String comment;

    public String getComment() {
        if (comment == null) {
            return "";
        }
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    public User getUser() {
        return user;
    }

    public String getUserLogin() {
        return user.getLogin();
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Long getDateMilliseconds(){
        return getDate().getTime();
    }

    @Override
    public int compareTo(History history) {
        return date.compareTo(history.getDate());
    }
}
